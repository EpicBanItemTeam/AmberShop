package io.izzel.ambershop.mixin;

import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

public interface AmberPlayer {

    void sendSign(Location<World> location, Direction direction, List<Text> lines);

    void resetSign(Location<World> location);

    Optional<Direction> getSign(Location<World> location);

    void sendDroppedItem(Location<World> location, ItemStack type);

    void resetDroppedItem(Location<World> location);

}
