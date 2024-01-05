package com.plotsquared.sponge.listener;

import com.flowpowered.math.vector.Vector3d;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.*;
import com.intellectualcrafters.plot.util.*;
import com.plotsquared.listener.PlotListener;
import com.plotsquared.sponge.util.SpongeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.*;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.ExplosionEvent.Detonate;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;

@SuppressWarnings("Guava")
public class MainListener
{
    private static final List<ItemType> BLOCKED_ITEMS = new ArrayList<>(Arrays.asList(
            ItemTypes.FISHING_ROD,
            ItemTypes.FLINT_AND_STEEL,
            ItemTypes.SHEARS,
            ItemTypes.LEAD,
            ItemTypes.NAME_TAG,
            ItemTypes.SPLASH_POTION,
            ItemTypes.SNOWBALL,
            ItemTypes.EGG,
            ItemTypes.FIRE_CHARGE,
            ItemTypes.BUCKET,
            ItemTypes.PAINTING,
            ItemTypes.ITEM_FRAME,
            ItemTypes.ARMOR_STAND,
            ItemTypes.DYE,
            ItemTypes.BOAT,
            ItemTypes.DARK_OAK_BOAT,
            ItemTypes.ACACIA_BOAT,
            ItemTypes.JUNGLE_BOAT,
            ItemTypes.BIRCH_BOAT,
            ItemTypes.SPRUCE_BOAT,
            ItemTypes.MINECART,
            ItemTypes.HOPPER_MINECART,
            ItemTypes.TNT_MINECART,
            ItemTypes.FURNACE_MINECART,
            ItemTypes.CHEST_MINECART,
            ItemTypes.COMMAND_BLOCK_MINECART,
            ItemTypes.MONSTER_EGG
    ));

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onSpawnEntity(SpawnEntityEvent event)
    {
        event.filterEntities(entity -> {
            if (!PS.get().hasPlotArea(entity.getWorld().getName())) {
                return true;
            }
            return entity instanceof Player
                    || entity.getType() == EntityTypes.ITEM
                    || entity.getType() == EntityTypes.ARMOR_STAND
                    || entity.getType() == EntityTypes.PAINTING
                    || entity.getType() == EntityTypes.ITEM_FRAME;
        });
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onItemUse(InteractItemEvent event, @Root Player player)
    {
        if (event.getItemStack() == ItemStackSnapshot.NONE) {
            return;
        }

        Vector3d interactionPoint = event.getInteractionPoint().orElse(player.getLocation().getPosition());
        Location<World> location = new Location<>(player.getWorld(), interactionPoint);

        // Handle hitting entities
        boolean hasHitEntity = event.getContext().containsKey(EventContextKeys.ENTITY_HIT);
        if (hasHitEntity) {
            Entity hitEntity = event.getContext().get(EventContextKeys.ENTITY_HIT).get();
            if (hitEntity instanceof Living && !(hitEntity instanceof ArmorStand)) {
                return;
            }

            location = hitEntity.getLocation();
        }

        if (!this.canUseItem(SpongeUtil.getPlayer(player), location, event.getItemStack().createStack())) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityInteract(InteractEntityEvent event, @Root Player player)
    {
        Entity targetEntity = event.getTargetEntity();
        Optional<Vector3d> optionalInteractionPoint = event.getInteractionPoint();

        Vector3d blockPosition = optionalInteractionPoint.orElseGet(() -> targetEntity.getLocation().getPosition());
        Location<World> l = new Location<>(targetEntity.getWorld(), blockPosition);

        // Check item usage such as shears on sheeps
        Optional<ItemStack> itemStack = player.getItemInHand(HandTypes.MAIN_HAND);
        if (itemStack.isPresent()) {
            if (!this.canUseItem(SpongeUtil.getPlayer(player), l, itemStack.get())) {
                event.setCancelled(true);
                return;
            }
        }

        if ((targetEntity instanceof Living) && !(targetEntity instanceof ArmorStand) && !(targetEntity instanceof Animal)) {
            return;
        }

        boolean canInteractWithEntity;
        if (targetEntity instanceof Hanging || targetEntity instanceof ArmorStand || targetEntity instanceof Minecart || targetEntity instanceof Boat) {
            canInteractWithEntity = this.canDo(SpongeUtil.getPlayer(player), l, C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.BREAK);
        } else {
            canInteractWithEntity = this.canDo(SpongeUtil.getPlayer(player), l, C.PERMISSION_ADMIN_INTERACT_ROAD, C.PERMISSION_ADMIN_INTERACT_OTHER, C.PERMISSION_ADMIN_INTERACT_UNOWNED, Flags.USE);
        }
        if (!canInteractWithEntity) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onInteractBlock(InteractBlockEvent event, @Root Player player)
    {
        // If AIR or NONE then return
        if (event.getTargetBlock() == BlockSnapshot.NONE || event.getTargetBlock().getState().getType() == BlockTypes.AIR) {
            return;
        }

        Optional<Location<World>> optionalLocation = event.getTargetBlock().getLocation();
        if (!optionalLocation.isPresent()) {
            return;
        }

        Location<World> blockLocation = optionalLocation.get();

        // Check item usage such as bonemeal on seeds
        Optional<ItemStack> itemStack = player.getItemInHand(HandTypes.MAIN_HAND);
        if (itemStack.isPresent()) {
            if (!this.canUseItem(SpongeUtil.getPlayer(player), blockLocation, itemStack.get())) {
                event.setCancelled(true);
                return;
            }
        }

        if (!this.canInteract(SpongeUtil.getPlayer(player), blockLocation, event.getTargetBlock(), C.PERMISSION_ADMIN_INTERACT_ROAD, C.PERMISSION_ADMIN_INTERACT_OTHER, C.PERMISSION_ADMIN_INTERACT_UNOWNED, Flags.USE)) {
            event.setCancelled(true);
        }
    }

    // Avoid explosions in plot worlds
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onExplosion(ExplosionEvent event)
    {
        if (!(event instanceof Detonate)) {
            return;
        }
        Detonate detonateEvent = (Detonate) event;
        if (PS.get().hasPlotArea(detonateEvent.getTargetWorld().getName())) {
            detonateEvent.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onChangeBlockPre(ChangeBlockEvent.Pre event)
    {
        User user;
        Object root = event.getCause().root();
        if (root instanceof User) {
            user = (User) root;
        } else {
            user = event.getContext().get(EventContextKeys.OWNER)
                    .orElse(event.getContext().get(EventContextKeys.NOTIFIER)
                            .orElse(null));
        }

        // ++ Manage pistons
        if (root instanceof LocatableBlock) {
            BlockType blockType = ((LocatableBlock) root).getBlockState().getType();
            if (blockType == BlockTypes.PISTON
                    || blockType == BlockTypes.STICKY_PISTON
                    || blockType == BlockTypes.PISTON_HEAD
                    || blockType == BlockTypes.PISTON_EXTENSION) {
                if (user == null || !user.getPlayer().isPresent()) {
                    return;
                }
                if (!this.canDo(SpongeUtil.getPlayer(user.getPlayer().get()), ((LocatableBlock) root).getLocation(), C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.BREAK)) {
                    return;
                }
            }
        }
        // -- Manage pistons

        for (Location<World> l : event.getLocations()) {
            if (user == null) {
                com.intellectualcrafters.plot.object.Location location = SpongeUtil.getLocation(l);
                PlotArea area = location.getPlotArea();
                if (area != null) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                if (!user.getPlayer().isPresent()) {
                    if (PS.get().hasPlotArea(l.getExtent().getName())) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if (!this.canDo(SpongeUtil.getPlayer(user.getPlayer().get()), l, C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.PLACE)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onChangeBlockBreak(ChangeBlockEvent.Break event)
    {
        Object root = event.getCause().root();

        // Piston retracts inside
        if (root == Sponge.getGame() && event.getCause().all().size() == 1
                && !event.getContext().asMap().containsKey(EventContextKeys.OWNER)
                && !event.getContext().asMap().containsKey(EventContextKeys.NOTIFIER)) {
            return;
        }

        // Snow and block random tick
        if (event.getCause().getContext().asMap().isEmpty() && event.getCause().all().size() == 1
                && root instanceof LocatableBlock) {
            return;
        }

        // DynamicLiquid random tick
        if (event.getCause().getContext().containsKey(EventContextKeys.LIQUID_BREAK)
                && event.getCause().getContext().asMap().size() == 1 && event.getCause().all().size() == 1
                && root instanceof LocatableBlock) {
            return;
        }

        User user;
        if (root instanceof User) {
            user = (User) root;
        } else {
            user = event.getContext().get(EventContextKeys.OWNER)
                    .orElse(event.getContext().get(EventContextKeys.NOTIFIER)
                            .orElse(null));
        }

        boolean isCustomSpawnType = event.getContext().get(EventContextKeys.SPAWN_TYPE).isPresent() && event.getContext().get(EventContextKeys.SPAWN_TYPE).get() == SpawnTypes.CUSTOM;
        if (isCustomSpawnType) {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            if (user == null) {
                Optional<Location<World>> optionalLocation = transaction.getFinal().getLocation();
                if (optionalLocation.isPresent()) {
                    com.intellectualcrafters.plot.object.Location location = SpongeUtil.getLocation(transaction.getOriginal().getLocation().get());
                    PlotArea area = location.getPlotArea();
                    if (area != null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            } else {
                // ++ Manage pistons
                BlockType blockType = transaction.getOriginal().getState().getType();
                if (blockType == BlockTypes.PISTON_HEAD || blockType == BlockTypes.PISTON_EXTENSION) {
                    event.setCancelled(false);
                    return;
                }
                // -- Manage pistons
                if (!user.getPlayer().isPresent()) {
                    return;
                }
                Location<World> location = transaction.getOriginal().getLocation().orElse(null);
                if (!this.canDo(SpongeUtil.getPlayer(user.getPlayer().get()), location, C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.BREAK)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onChangeBlockPlace(ChangeBlockEvent.Place event)
    {
        Object root = event.getCause().root();

        // Piston extends inside
        if (root instanceof TileEntity && ((TileEntity) root).getType() == TileEntityTypes.PISTON) {
            return;
        }

        // BlockTickPhaseState - randomTickBlock
        if (event.getCause().getContext().asMap().isEmpty() && event.getCause().all().size() == 1
                && root instanceof LocatableBlock) {
            return;
        }

        // Piston retracts inside
        if (root == Sponge.getGame() && event.getCause().all().size() == 1
                && !event.getContext().asMap().containsKey(EventContextKeys.OWNER)
                && !event.getContext().asMap().containsKey(EventContextKeys.NOTIFIER)) {
            return;
        }

        User user;
        if (root instanceof User) {
            user = (User) root;
        } else {
            user = event.getContext().get(EventContextKeys.OWNER)
                    .orElse(event.getContext().get(EventContextKeys.NOTIFIER)
                            .orElse(null));
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            if (user == null) {
                Optional<Location<World>> optionalLocation = transaction.getFinal().getLocation();
                if (optionalLocation.isPresent()) {
                    com.intellectualcrafters.plot.object.Location location = SpongeUtil.getLocation(transaction.getOriginal().getLocation().get());
                    PlotArea area = location.getPlotArea();
                    if (area != null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            } else {
                // ++ Manage pistons
                BlockType blockType = transaction.getFinal().getState().getType();
                if (blockType == BlockTypes.PISTON_HEAD || blockType == BlockTypes.PISTON_EXTENSION) {
                    event.setCancelled(false);
                    return;
                }
                // -- Manage pistons
                if (!transaction.getFinal().getLocation().isPresent()) {
                    return;
                }
                if (!user.getPlayer().isPresent()) {
                    return;
                }
                if (!this.canDo(SpongeUtil.getPlayer(user.getPlayer().get()), transaction.getFinal().getLocation().get(), C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.PLACE)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        SpongeUtil.getPlayer(player).unregister();
        PlotPlayer pp = SpongeUtil.getPlayer(player);
        // Now
        String name = pp.getName();
        StringWrapper sw = new StringWrapper(name);
        UUID uuid = pp.getUUID();
        UUIDHandler.add(sw, uuid);

        com.intellectualcrafters.plot.object.Location loc = pp.getLocation();
        PlotArea area = loc.getPlotArea();
        Plot plot;
        if (area != null) {
            plot = area.getPlot(loc);
            if (plot != null) {
                PlotListener.plotEntry(pp, plot);
            }
        }
        // Delayed

        // Async
        TaskManager.runTaskLaterAsync(() -> EventUtil.manager.doJoinTask(pp), 20);
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event)
    {
        Player player = event.getTargetEntity();
        PlotPlayer pp = SpongeUtil.getPlayer(player);
        pp.unregister();
    }

    @Listener
    public void onMove(MoveEntityEvent event)
    {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }
        Location<World> from = event.getFromTransform().getLocation();
        Location<World> to = event.getToTransform().getLocation();
        int x2;
        if (MathMan.roundInt(from.getX()) != (x2 = MathMan.roundInt(to.getX()))) {
            Player player = (Player) event.getTargetEntity();
            PlotPlayer plotPlayer = SpongeUtil.getPlayer(player);
            com.intellectualcrafters.plot.object.Location loc = SpongeUtil.getLocation(to);
            plotPlayer.setMeta("location", loc);
            PlotArea area = loc.getPlotArea();
            if (area == null) {
                plotPlayer.deleteMeta("lastplot");
                return;
            }
            Plot now = area.getPlotAbs(loc);
            Plot lastPlot = plotPlayer.getMeta("lastplot");
            if (now == null) {
                if (lastPlot != null && !PlotListener.plotExit(plotPlayer, lastPlot)) {
                    if (lastPlot.equals(SpongeUtil.getLocation(from).getPlot())) {
                        player.setLocation(from);
                    } else {
                        player.setLocation(player.getWorld().getSpawnLocation());
                    }
                    event.setCancelled(true);
                    return;
                }
            } else if (now.equals(lastPlot)) {
                return;
            } else if (!PlotListener.plotEntry(plotPlayer, now)) {
                player.setLocation(from);
                event.setCancelled(true);
                return;
            }
            int border = area.getBorder();
            if (x2 > border) {
                to.sub(x2 - border + 4, 0, 0);
                player.setLocation(to);
                MainUtil.sendMessage(plotPlayer, C.BORDER);
                return;
            } else if (x2 < -border) {
                to.add(border - x2 + 4, 0, 0);
                player.setLocation(to);
                MainUtil.sendMessage(plotPlayer, C.BORDER);
                return;
            }
            return;
        }
        int z2;
        if (MathMan.roundInt(from.getZ()) != (z2 = MathMan.roundInt(to.getZ()))) {
            Player player = (Player) event.getTargetEntity();
            PlotPlayer pp = SpongeUtil.getPlayer(player);
            com.intellectualcrafters.plot.object.Location loc = SpongeUtil.getLocation(to);
            pp.setMeta("location", loc);
            PlotArea area = loc.getPlotArea();
            if (area == null) {
                pp.deleteMeta("lastplot");
                return;
            }
            Plot now = area.getPlotAbs(loc);
            Plot lastPlot = pp.getMeta("lastplot");
            if (now == null) {
                if (lastPlot != null && !PlotListener.plotExit(pp, lastPlot)) {
                    if (lastPlot.equals(SpongeUtil.getLocation(from).getPlot())) {
                        player.setLocation(from);
                    } else {
                        player.setLocation(player.getWorld().getSpawnLocation());
                    }
                    event.setCancelled(true);
                    return;
                }
            } else if (now.equals(lastPlot)) {
                return;
            } else if (!PlotListener.plotEntry(pp, now)) {
                player.setLocation(from);
                event.setCancelled(true);
                return;
            }
            int border = area.getBorder();
            if (z2 > border) {
                to.add(0, 0, z2 - border - 4);
                player.setLocation(to);
                MainUtil.sendMessage(pp, C.BORDER);
            } else if (z2 < -border) {
                to.add(0, 0, border - z2 + 4);
                player.setLocation(to);
                MainUtil.sendMessage(pp, C.BORDER);
            }
        }
    }

    @Listener(order = Order.EARLY, beforeModifications = true)
    public void onEntityAttacked(AttackEntityEvent event)
    {
        User user;
        Object root = event.getCause().root();
        if (root instanceof User) {
            user = (User) root;
        } else {
            user = event.getContext().get(EventContextKeys.OWNER)
                    .orElse(event.getContext().get(EventContextKeys.NOTIFIER)
                            .orElse(null));
        }
        if (user == null || !user.getPlayer().isPresent()) {
            return;
        }
        Player player = user.getPlayer().get();
        Entity targetEntity = event.getTargetEntity();
        if (targetEntity instanceof Hanging || targetEntity instanceof ArmorStand || targetEntity instanceof Minecart || targetEntity instanceof Boat) {
            if (!this.canDo(SpongeUtil.getPlayer(player), targetEntity.getLocation(), C.PERMISSION_ADMIN_BUILD_ROAD, C.PERMISSION_ADMIN_BUILD_OTHER, C.PERMISSION_ADMIN_BUILD_UNOWNED, Flags.BREAK)) {
                event.setCancelled(true);
            }
        }
    }

    // Avoid attacks to players in plot worlds
    @Listener(order = Order.EARLY, beforeModifications = true)
    public void onPlayerAttacked(AttackEntityEvent event, @Getter(value = "getTargetEntity") Player attackedPlayer)
    {
        if (PS.get().hasPlotArea(event.getTargetEntity().getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    // Avoid projectile impact in plot worlds
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onCollideEntityImpact(CollideEntityEvent.Impact event)
    {
        Object root = event.getCause().root();

        if (!(root instanceof Projectile)) {
            return;
        }

        for (Entity entity : event.getEntities()) {
            if (PS.get().hasPlotArea(entity.getWorld().getName())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean canUseItem(PlotPlayer player, Location<World> l, ItemStack itemStack)
    {
        if (!BLOCKED_ITEMS.contains(itemStack.getType())) {
            return true;
        }

        return this.canDo(player, l, C.PERMISSION_ADMIN_INTERACT_ROAD, C.PERMISSION_ADMIN_INTERACT_OTHER, C.PERMISSION_ADMIN_INTERACT_UNOWNED, Flags.USE);
    }

    private boolean canInteract(PlotPlayer player, Location<World> l, BlockSnapshot blockSnapshot, C permissionRoad, C permissionOther, C permissionUnowned, Flag<HashSet<PlotBlock>> flagType)
    {
        Block block = ((IBlockState) blockSnapshot.getState()).getBlock();

        if ((!(block instanceof BlockContainer) && !(block instanceof BlockDoor) && !(block instanceof BlockTrapDoor)) || blockSnapshot.getState().getId().contains("waystones:waystone")) {
            return true;
        }

        com.intellectualcrafters.plot.object.Location location = SpongeUtil.getLocation(l);
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return true;
        }
        Plot plot = area.getPlot(location);
        if (plot == null) {
            return Permissions.hasPermission(player, permissionRoad);
        } else if (plot.hasOwner()) {
            if (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, permissionOther)) {
                if (block instanceof BlockDoor || block instanceof BlockTrapDoor) {
                    return plot.getFlag(Flags.DOOR).or(true);
                }
                com.google.common.base.Optional<HashSet<PlotBlock>> flag = plot.getFlag(flagType);
                return flag.isPresent() && flag.get().contains(SpongeUtil.getPlotBlock(l.getBlock()));
            }
        } else {
            return Permissions.hasPermission(player, permissionUnowned);
        }
        return true;
    }

    private boolean canDo(PlotPlayer player, Location<World> l, C permissionRoad, C permissionOther, C permissionUnowned, Flag<HashSet<PlotBlock>> flagType)
    {
        com.intellectualcrafters.plot.object.Location location = SpongeUtil.getLocation(l);
        PlotArea area = location.getPlotArea();
        if (area == null) {
            return true;
        }
        Plot plot = area.getPlot(location);
        if (plot == null) {
            return Permissions.hasPermission(player, permissionRoad);
        } else if (plot.hasOwner()) {
            if (!plot.isAdded(player.getUUID()) && !Permissions.hasPermission(player, permissionOther)) {
                com.google.common.base.Optional<HashSet<PlotBlock>> flag = plot.getFlag(flagType);
                return flag.isPresent() && flag.get().contains(SpongeUtil.getPlotBlock(l.getBlock()));
            }
        } else {
            return Permissions.hasPermission(player, permissionUnowned);
        }
        return true;
    }
}
