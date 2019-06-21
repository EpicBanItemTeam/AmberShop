package io.izzel.ambershop.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import lombok.var;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.InventoryTransformations;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.MainPlayerInventory;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Inventories {

    public int empty(Inventory inventory, ItemStack type) {
        var total = 0;
        for (Slot slot : inventory.<Slot>slots()) {
            val peek = slot.peek();
            if (peek.isPresent()) {
                val item = peek.get();
                val copy = item.copy();
                copy.setQuantity(1);
                if (copy.equalTo(type)) {
                    total += item.getMaxStackQuantity() - item.getQuantity();
                }
            } else {
                total += type.getMaxStackQuantity();
            }
        }
        return total;
    }

    public int count(Inventory inventory, ItemStack type) {
        var total = 0;
        for (Slot slot : inventory.<Slot>slots()) {
            val peek = slot.peek();
            if (peek.isPresent()) {
                val item = peek.get();
                val copy = item.copy();
                copy.setQuantity(1);
                if (copy.equalTo(type)) {
                    total += item.getQuantity();
                }
            }
        }
        return total;
    }

    public List<ItemStack> take(Inventory inv, int count, ItemStack type) {
        val ret = new ArrayList<ItemStack>();
        int remaining = count;
        for (Slot slot : inv.<Slot>slots()) {
            if (remaining <= 0) break;
            val i = Math.min(slot.totalItems(), remaining);
            if (i == 0) continue;
            val item = slot.poll(i);
            if (item.isPresent()) {
                val copy = item.get().copy();
                copy.setQuantity(1);
                if (copy.equalTo(type)) {
                    remaining -= i;
                    ret.add(item.get());
                } else {
                    slot.offer(item.get()); // give back
                }
            }
        }
        return ret;
    }

    public Inventory getMainInventory(Player player) {
        return player.getInventory()
                .query(QueryOperationTypes.INVENTORY_TYPE.of(MainPlayerInventory.class))
                .transform(InventoryTransformations.PLAYER_MAIN_HOTBAR_FIRST);
    }

}
