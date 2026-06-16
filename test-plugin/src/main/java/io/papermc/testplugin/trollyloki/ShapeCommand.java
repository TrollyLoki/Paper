package io.papermc.testplugin.trollyloki;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import org.jspecify.annotations.NullMarked;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public final class ShapeCommand {
    private ShapeCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand(Plugin plugin) {
        return Commands.literal("shape")
            .then(Commands.literal("block").then(blockArguments(plugin, Color.LIME, BlockData::getShape, Block::getShape)))
            .then(Commands.literal("fluid").then(blockArguments(plugin, Color.AQUA, BlockData::getFluidShape, Block::getFluidShape)))
            .then(Commands.literal("collision").then(blockArguments(plugin, Color.ORANGE, BlockData::getCollisionShape, Block::getCollisionShape)));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> blockArguments(Plugin plugin, Color color,
        BiFunction<BlockData, Location, VoxelShape> blockDataShapeGetter,
        Function<Block, VoxelShape> blockShapeGetter
    ) {
        return Commands.argument("blockPosition", ArgumentTypes.blockPosition())
            .executes(context -> {
                return execute(context, plugin, color, location -> blockShapeGetter.apply(location.getBlock()));
            })
            .then(Commands.argument("blockState", ArgumentTypes.blockState())
                .executes(context -> {
                    BlockData blockData = context.getArgument("blockState", BlockState.class).getBlockData();
                    return execute(context, plugin, color, location -> blockDataShapeGetter.apply(blockData, location));
                })
            );
    }

    private static int execute(CommandContext<CommandSourceStack> context, Plugin plugin, Color color, Function<Location, VoxelShape> shapeGetter) throws CommandSyntaxException {
        BlockPosition blockPosition = context.getArgument("blockPosition", BlockPositionResolver.class).resolve(context.getSource());

        Location location = blockPosition.toLocation(context.getSource().getLocation().getWorld());
        VoxelShape shape = shapeGetter.apply(location);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                visualizeShape(location, shape, color);

                tick++;
                if (tick >= 100) cancel();
            }
        }.runTaskTimer(plugin, 0, 0);

        return Command.SINGLE_SUCCESS;
    }

    private static Vector randomWithinBlock() {
        return new Vector(Math.random(), Math.random(), Math.random());
    }

    private static boolean shapeContains(Collection<BoundingBox> shape, Vector position) {
        for (BoundingBox box : shape) {
            if (box.contains(position)) {
                return true;
            }
        }
        return false;
    }

    public static void visualizeShape(Location location, VoxelShape shape, Color color) {
        Collection<BoundingBox> boxes = shape.getBoundingBoxes();
        Vector blockPosition = location.toVector();

        //TODO: This is only correct when the shape has at most two bounding boxes
        double volume = 0;
        BoundingBox prev = null;
        for (BoundingBox box : boxes) {
            volume += box.getVolume();
            if (prev != null) {
                try {
                    volume -= box.clone().intersection(prev).getVolume();
                } catch (IllegalArgumentException ignored) {
                    // no overlap
                }
            }
            prev = box;
        }

        Supplier<Vector> randomPosition = () -> {
            Vector position;
            do {
                position = randomWithinBlock();
            } while (!shapeContains(boxes, position));
            return position.add(blockPosition);
        };

        Object particleData = new Particle.DustOptions(color, 1f);

        World world = location.getWorld();
        for (int i = 0; i < Math.min(50 * volume, 1000); i++) {
            Vector position = randomPosition.get();
            world.spawnParticle(Particle.DUST, position.getX(), position.getY(), position.getZ(), 1, particleData);
        }

    }

}
