package io.izzel.ambershop.unsafe;

import com.google.inject.ImplementedBy;
import io.izzel.ambershop.unsafe.impl.SimpleAmberDisplay;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

@ImplementedBy(SimpleAmberDisplay.class)
public interface AmberDisplay {

    void sendSign(Player player, Location<World> location, Direction direction, List<Text> lines);

    void resetSign(Player player, Location<World> location);

    Optional<Direction> getSign(Player player, Location<World> location);

    void sendDroppedItem(Player player, Location<World> location, ItemStack type);

    void resetDroppedItem(Player player, Location<World> location);

}
