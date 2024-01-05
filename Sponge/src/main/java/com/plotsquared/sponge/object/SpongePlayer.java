package com.plotsquared.sponge.object;

import com.flowpowered.math.vector.Vector3d;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.PlotGameMode;
import com.intellectualcrafters.plot.util.PlotWeather;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.sponge.util.SpongeUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.TargetedLocationData;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class SpongePlayer extends PlotPlayer
{
    public final Supplier<Player> player;
    private UUID uuid;
    private String name;

    public SpongePlayer(Player player)
    {
        this.player = () -> Sponge.getServer().getPlayer(player.getUniqueId()).orElse(null);
        this.uuid = player.getUniqueId();
        super.populatePersistentMetaMap();
    }

    @Override
    public RequiredType getSuperCaller()
    {
        return RequiredType.PLAYER;
    }

    @Override
    public Location getLocation()
    {
        Location location = super.getLocation();
        if (location == null) {
            return SpongeUtil.getLocation(this.player.get());
        } else {
            return location;
        }
    }

    @Override
    public Location getLocationFull()
    {
        return SpongeUtil.getLocationFull(this.player.get());
    }

    @Override
    public UUID getUUID()
    {
        if (this.uuid == null) {
            this.uuid = UUIDHandler.getUUID(this);
        }
        return this.uuid;
    }

    @Override
    public long getLastPlayed()
    {
        return this.player.get().lastPlayed().get().toEpochMilli();
    }

    @Override
    public boolean hasPermission(String permission)
    {
        return this.player.get().hasPermission(permission);
    }

    @Override
    public boolean isPermissionSet(String permission)
    {
        Tristate state = this.player.get().getPermissionValue(this.player.get().getActiveContexts(), permission);
        return state != Tristate.UNDEFINED;
    }

    @Override
    public void sendMessage(String message)
    {
        if (!StringMan.isEqual(this.getMeta("lastMessage"), message) || (System.currentTimeMillis() - this.<Long>getMeta("lastMessageTime") > 5000)) {
            setMeta("lastMessage", message);
            setMeta("lastMessageTime", System.currentTimeMillis());
            this.player.get().sendMessage(ChatTypes.CHAT, TextSerializers.LEGACY_FORMATTING_CODE.deserialize(message));
        }
    }

    @Override
    public void teleport(Location location)
    {
        if (!this.player.get().getPlayer().isPresent() || !this.player.get().getPlayer().get().getWorldUniqueId().isPresent()) {
            return;
        }
        String worldUUID = this.player.get().getPlayer().get().getWorldUniqueId().get().toString();
        if (!worldUUID.equals(location.getWorld())) {
            this.player.get().transferToWorld(location.getWorld(), new Vector3d(location.getX(), location.getY(), location.getZ()));
        } else {
            org.spongepowered.api.world.Location<World> current = this.player.get().getLocation();
            current = current.setPosition(new Vector3d(location.getX(), location.getY(), location.getZ()));
            this.player.get().setLocation(current);
        }
    }

    @Override
    public boolean isOnline()
    {
        return this.player.get() != null;
    }

    @Override
    public String getName()
    {
        if (this.name == null) {
            this.name = this.player.get().getName();
        }
        return this.name;
    }

    @Override
    public void setCompassTarget(Location location)
    {
        Optional<TargetedLocationData> target = this.player.get().getOrCreate(TargetedLocationData.class);
        if (target.isPresent()) {
            target.get().set(Keys.TARGETED_LOCATION, SpongeUtil.getLocation(location).getPosition());
        } else {
            PS.debug("Failed to set compass target.");
        }
    }

    @Override
    public void setWeather(PlotWeather weather)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

    @Override
    public PlotGameMode getGameMode()
    {
        GameMode gamemode = this.player.get().getGameModeData().type().get();
        if (gamemode == GameModes.ADVENTURE) {
            return PlotGameMode.ADVENTURE;
        } else if (gamemode == GameModes.CREATIVE) {
            return PlotGameMode.CREATIVE;
        } else if (gamemode == GameModes.SPECTATOR) {
            return PlotGameMode.SPECTATOR;
        } else if (gamemode == GameModes.SURVIVAL) {
            return PlotGameMode.SURVIVAL;
        } else {
            return PlotGameMode.NOT_SET;
        }
    }

    @Override
    public void setGameMode(PlotGameMode gameMode)
    {
        switch (gameMode) {
            case ADVENTURE:
                this.player.get().offer(Keys.GAME_MODE, GameModes.ADVENTURE);
                return;
            case CREATIVE:
                this.player.get().offer(Keys.GAME_MODE, GameModes.CREATIVE);
                return;
            case SPECTATOR:
                this.player.get().offer(Keys.GAME_MODE, GameModes.SPECTATOR);
                return;
            case SURVIVAL:
                this.player.get().offer(Keys.GAME_MODE, GameModes.SURVIVAL);
                return;
            case NOT_SET:
                this.player.get().offer(Keys.GAME_MODE, GameModes.NOT_SET);
        }
    }

    @Override
    public void setTime(long time)
    {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }

    @Override
    public boolean getFlight()
    {
        Optional<Boolean> flying = player.get().get(Keys.CAN_FLY);
        return flying.isPresent() && flying.get();
    }

    @Override
    public void setFlight(boolean fly)
    {
        this.player.get().offer(Keys.IS_FLYING, fly);
        this.player.get().offer(Keys.CAN_FLY, fly);
    }

    @Override
    public void playMusic(Location location, int id)
    {
        switch (id) {
            case 0:
                //Placeholder because Sponge doesn't have a stopSound() implemented yet.
                this.player.get().playSound(SoundTypes.BLOCK_CLOTH_PLACE, SpongeUtil.getLocation(location).getPosition(), 0);
                break;
            case 2256:
                this.player.get().playSound(SoundTypes.RECORD_11, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2257:
                this.player.get().playSound(SoundTypes.RECORD_13, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2258:
                this.player.get().playSound(SoundTypes.RECORD_BLOCKS, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2259:
                this.player.get().playSound(SoundTypes.RECORD_CAT, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2260:
                this.player.get().playSound(SoundTypes.RECORD_CHIRP, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2261:
                this.player.get().playSound(SoundTypes.RECORD_FAR, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2262:
                this.player.get().playSound(SoundTypes.RECORD_MALL, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2263:
                this.player.get().playSound(SoundTypes.RECORD_MELLOHI, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2264:
                this.player.get().playSound(SoundTypes.RECORD_STAL, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2265:
                this.player.get().playSound(SoundTypes.RECORD_STRAD, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2266:
                this.player.get().playSound(SoundTypes.RECORD_WAIT, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
            case 2267:
                this.player.get().playSound(SoundTypes.RECORD_WARD, SpongeUtil.getLocation(location).getPosition(), 1);
                break;
        }
    }

    @Override
    public void kick(String message)
    {
        this.player.get().kick(SpongeUtil.getText(message));
    }

    @Override
    public void stopSpectating()
    {
        //Not Implemented
    }

    @Override
    public boolean isBanned()
    {
        Optional<BanService> service = Sponge.getServiceManager().provide(BanService.class);
        return service.isPresent() && service.get().isBanned(this.player.get().getProfile());
    }
}
