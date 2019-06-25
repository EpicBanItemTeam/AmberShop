package io.izzel.ambershop.trade;

import io.izzel.ambershop.util.Inventories;
import io.izzel.ambershop.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import java.math.BigDecimal;
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

    @Override
    public TransactionResult performTransaction() {
        if (Inventories.count(fromInv, type) < count) return TransactionResult.SOLD_OUT;
        if (Inventories.empty(toInv, type) < count) return TransactionResult.INVENTORY_FULL;
        val items = Inventories.take(fromInv, count, type);

        val price = BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(this.price));
        var sr = Util.performEconomy(fromAccount, price, false);
        if (sr.rollback) return sr;
        var cr = Util.performEconomy(toAccount, price, true);
        if (cr.rollback) {
            Util.performEconomy(fromAccount, price, true);
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
        sr = Util.performEconomy(fromAccount, refund, true);
        if (sr.rollback) return sr;
        cr = Util.performEconomy(toAccount, refund, false);
        if (cr.rollback) return cr;

        items.forEach(fromInv::offer);

        return TransactionResult.SUCCESS;
    }

}
