package io.izzel.ambershop.trade;

import com.google.common.collect.ImmutableList;
import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.util.Inventories;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.transaction.ResultType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
class PlayerShopTrading implements Trading {

    // buy: fromInv = chest, to = player
    // sell: fromInv = player, to = chest
    private final Inventory fromInv, toInv;

    private final UUID fromAccount, toAccount;

    private final int count;

    private final double price;

    private final ItemStack type;

    private final boolean unlimited;

    @Override
    public TransactionResult performTransaction() {
        if (!unlimited && Inventories.count(fromInv, type) < count) return TransactionResult.SOLD_OUT;
        if (Inventories.empty(toInv, type) < count) return TransactionResult.INVENTORY_FULL;
        val items = unlimited ? provide(count) : Inventories.take(fromInv, count, type);

        val price = BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(this.price));
        var sr = performEconomy(fromAccount, price, false);
        if (sr.rollback) return sr;
        var cr = performEconomy(toAccount, price, true);
        if (cr.rollback) {
            performEconomy(fromAccount, price, true);
            return cr;
        }

        var success = 0;
        val iterator = items.iterator();
        while (iterator.hasNext()) {
            val item = iterator.next();
            success += item.getQuantity();
            toInv.offer(item);
            success -= item.getQuantity();
            if (item.getQuantity() == 0) iterator.remove();
        }

        if (success == count || items.isEmpty()) return TransactionResult.SUCCESS;

        // usually cannot happen, just in case
        val refund = BigDecimal.valueOf(count - success).multiply(BigDecimal.valueOf(this.price));
        sr = performEconomy(fromAccount, refund, true);
        if (sr.rollback) return sr;
        cr = performEconomy(toAccount, refund, false);
        if (cr.rollback) return cr;

        if (!unlimited) items.forEach(fromInv::offer);

        return TransactionResult.SUCCESS;
    }

    private List<ItemStack> provide(int amount) {
        val ret = new ArrayList<ItemStack>();
        while (amount > 0) {
            int count = amount > 64 ? 64 : amount;
            amount -= count;
            val item = type.copy();
            item.setQuantity(count);
            ret.add(item);
        }
        return ret;
    }

    private TransactionResult performEconomy(UUID uuid, BigDecimal price, boolean isWithdraw) {
        val eco = Sponge.getServiceManager().provideUnchecked(EconomyService.class);
        val playerAcc = eco.getOrCreateAccount(uuid).get();
        val ctx = EventContext.builder().add(EventContextKeys.PLUGIN, AmberShop.SINGLETON.container).build();
        val cause = Cause.of(ctx, AmberShop.SINGLETON.container);
        val currency = eco.getDefaultCurrency();
        val result = isWithdraw ? playerAcc.withdraw(currency, price, cause) : playerAcc.deposit(currency, price, cause);
        return result.getResult() == ResultType.SUCCESS ? TransactionResult.SUCCESS : TransactionResult.ECONOMY_ISSUE;
    }

}
