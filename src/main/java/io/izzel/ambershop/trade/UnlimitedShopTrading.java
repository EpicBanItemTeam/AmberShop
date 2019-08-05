package io.izzel.ambershop.trade;

import io.izzel.ambershop.util.Inventories;
import io.izzel.ambershop.util.OperationResult;
import io.izzel.ambershop.util.Util;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class UnlimitedShopTrading implements Trading {

    private final Inventory playerInv;

    private final UUID playerAccount;

    private final int count;

    private final double price;

    private final ItemStack type;

    private final boolean sell;

    @Override
    public OperationResult performTransaction() {
        if (sell) {
            if (Inventories.count(playerInv, type) < count)
                return OperationResult.fail("trade.transaction-results.sold-out");
            val r = Util.performEconomy(playerAccount, BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(count)), false);
            if (r.isFail()) return r;
            Inventories.take(playerInv, count, type);
        } else {
            if (Inventories.empty(playerInv, type) < count)
                return OperationResult.fail("trade.transaction-results.inventory-full");
            val r = Util.performEconomy(playerAccount, BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(count)), true);
            if (r.isFail()) return r;
            provide(count).forEach(playerInv::offer);
        }
        return OperationResult.of("trade.transaction-results.success");
    }

    private List<ItemStack> provide(int amount) {
        val ret = new ArrayList<ItemStack>();
        while (amount > 0) {
            int count = Math.min(amount, 64);
            amount -= count;
            val item = type.copy();
            item.setQuantity(count);
            ret.add(item);
        }
        return ret;
    }

}
