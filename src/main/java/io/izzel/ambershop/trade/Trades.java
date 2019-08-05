package io.izzel.ambershop.trade;

import io.izzel.ambershop.data.ShopRecord;
import io.izzel.ambershop.util.Inventories;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;

@UtilityClass
public class Trades {

    public Trading playerShopTrade(Player player, ShopRecord record, int amount, boolean sell) {
        val location = new Location<>(Sponge.getServer().getWorld(record.world).orElse(player.getWorld()), record.x, record.y, record.z);
        val chestInv = ((TileEntityCarrier) location.getTileEntity().get()).getInventory();
        val playerInv = Inventories.getMainInventory(player);
        val playerUid = player.getUniqueId();
        if (record.isUnlimited()) {
            return new UnlimitedShopTrading(playerInv, playerUid, amount, Math.abs(record.price), record.getItemType().createStack(), sell);
        } else {
            if (sell) {
                return new PlayerShopTrading(playerInv, chestInv, playerUid, record.owner, amount,
                    Math.abs(record.price), record.getItemType().createStack(), false);
            } else {
                return new PlayerShopTrading(chestInv, playerInv, record.owner, playerUid, amount,
                    Math.abs(record.price), record.getItemType().createStack(), true);
            }
        }
    }

}
