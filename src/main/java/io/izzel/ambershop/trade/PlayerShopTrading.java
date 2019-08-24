package io.izzel.ambershop.trade;

import io.izzel.ambershop.AmberShop;
import io.izzel.ambershop.util.Inventories;
import io.izzel.ambershop.util.OperationResult;
import io.izzel.ambershop.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.var;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final boolean showTax;

    @Override
    public OperationResult performTransaction() {
        if (Inventories.count(fromInv, type) < count)
            return OperationResult.fail("trade.transaction-results.sold-out");
        if (Inventories.empty(toInv, type) < count)
            return OperationResult.fail("trade.transaction-results.inventory-full");

        val price = BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(this.price));
        var sr = Util.performEconomy(fromAccount, price, false);
        if (sr.isFail()) return sr;
        val settings = AmberShop.SINGLETON.getConfig().get().shopSettings.taxSettings;
        val taxed = settings.enable ? price.multiply(BigDecimal.valueOf(1D - settings.tax)).setScale(2, RoundingMode.HALF_UP) : price;
        var cr = Util.performEconomy(toAccount, taxed, true);
        if (cr.isFail()) {
            Util.performEconomy(fromAccount, price, true);
            return cr;
        }

        val items = Inventories.take(fromInv, count, type);
        var success = 0;
        val iterator = items.iterator();
        while (iterator.hasNext()) {
            val item = iterator.next();
            success += item.getQuantity();
            toInv.offer(item);
            success -= item.getQuantity();
            if (item.getQuantity() == 0) iterator.remove();
        }

        if (success == count || items.isEmpty()) {
            if (showTax) {
                return OperationResult.of("trade.transaction-results.sold-after-tax", taxed);
            } else {
                return OperationResult.of("trade.transaction-results.success");
            }
        }

        // usually cannot happen, just in case
        val refund = BigDecimal.valueOf(count - success).multiply(BigDecimal.valueOf(this.price));
        sr = Util.performEconomy(fromAccount, refund, true);
        if (sr.isFail()) return sr;
        cr = Util.performEconomy(toAccount, refund, false);
        if (cr.isFail()) return cr;

        items.forEach(fromInv::offer);

        if (showTax) {
            return OperationResult.of("trade.transaction-results.sold-after-tax", taxed);
        } else {
            return OperationResult.of("trade.transaction-results.success");
        }
    }

}
