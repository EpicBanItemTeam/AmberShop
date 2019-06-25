package io.izzel.ambershop.trade;

import io.izzel.ambershop.util.Inventories;
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
    public TransactionResult performTransaction() {
        if (sell) {
            if (Inventories.count(playerInv, type) < count) return TransactionResult.SOLD_OUT;
            val r = Util.performEconomy(playerAccount, BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(count)), false);
            if (r.rollback) return r;
            Inventories.take(playerInv, count, type);
        } else {
            if (Inventories.empty(playerInv, type) < count) return TransactionResult.INVENTORY_FULL;
            val r = Util.performEconomy(playerAccount, BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(count)), true);
            if (r.rollback) return r;
            provide(count).forEach(playerInv::offer);
        }
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

}
