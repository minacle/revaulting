package moe.minacle.minecraft.plugins.revaulting;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTType;
import de.tr7zw.nbtapi.handler.NBTHandlers;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.nbtapi.utils.Metrics;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class Plugin extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 22418;

    private static final @NotNull UUID UUID_ZERO = new UUID(0, 0);

    private final @NotNull String REWARDED_PLAYERS = Objects.requireNonNull(namespacedKey("rewarded_players")).toString();

    private final @NotNull String REWARDED_COUNTS = Objects.requireNonNull(namespacedKey("rewarded_counts")).toString();

    private @Nullable ScheduledTask vaultManipulationTask;

    private @Nullable NamespacedKey namespacedKey(final @NotNull String key) {
        return NamespacedKey.fromString(key, this);
    }

    private @NotNull Set<BlockPosition> collectBlockPositions(final @NotNull World world) {
        final List<Player> players = world.getPlayers();
        final HashSet<BlockPosition> blockPositions = HashSet.newHashSet(343 * players.size());
        for (final Player player : players) {
            final Location location;
            final int blockX;
            final int blockY;
            final int blockZ;
            if (!player.isConnected() || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            location = player.getLocation();
            if (location == null)
                continue;
            blockX = location.getBlockX();
            blockY = location.getBlockY();
            blockZ = location.getBlockZ();
            for (int x = blockX - 3; x < blockX + 3; x++)
                for (int y = blockY - 3; y < blockY + 3; y++)
                    for (int z = blockZ - 3; z < blockZ + 3; z++)
                        blockPositions.add(Position.block(x, y, z));
        }
        return blockPositions;
    }

    private @NotNull Set<Block> collectVaults(final @NotNull World world, final @NotNull Set<BlockPosition> blockPositions) {
        final HashSet<Block> vaults = new HashSet<>();
        for (final BlockPosition blockPosition : blockPositions) {
            final Block block = world.getBlockAt(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
            if (block.getType() == Material.VAULT)
                vaults.add(block);
        }
        return vaults;
    }

    private void manipulateVault(final @NotNull Block block) {
        final Vault vault;
        final List<UUID> newRewardedPlayers;
        if (block.getType() != Material.VAULT)
            return;
        vault = (Vault)block.getState();
        newRewardedPlayers =
            NBT.modify(
                vault,
                (nbt) -> {
                    final ReadWriteNBT serverData = nbt.getCompound("server_data");
                    final ReadWriteNBTList<UUID> rewardedPlayers;
                    final List<UUID> result;
                    if (serverData == null)
                        return null;
                    rewardedPlayers = serverData.getUUIDList("rewarded_players");
                    if (rewardedPlayers == null)
                        return null;
                    result = rewardedPlayers.toListCopy();
                    rewardedPlayers.clear();
                    return result;
                });
        if (newRewardedPlayers == null || newRewardedPlayers.isEmpty())
            return;
        NBT.modifyPersistentData(
            vault,
            (nbt) -> {
                ReadWriteNBTList<UUID> rewardedPlayers = nbt.getUUIDList(REWARDED_PLAYERS);
                ReadWriteNBTList<Integer> rewardedCounts = nbt.getIntegerList(REWARDED_COUNTS);
                final int rewardedPlayersSize;
                final int rewardedCountsSize;
                int index;
                if (rewardedPlayers == null) {
                    ReadWriteNBT uuidList = NBT.parseNBT("[[I;0,0,0,0]]");
                    nbt.set(REWARDED_PLAYERS, uuidList, NBTHandlers.STORE_READWRITE_TAG);
                    rewardedPlayers = nbt.getUUIDList(REWARDED_PLAYERS);
                }
                if (rewardedCounts == null) {
                    ReadWriteNBT integerList = NBT.parseNBT("[0]");
                    nbt.set(REWARDED_COUNTS, integerList, NBTHandlers.STORE_READWRITE_TAG);
                    rewardedCounts = nbt.getIntegerList(REWARDED_COUNTS);
                }
                rewardedPlayersSize = rewardedPlayers.size();
                rewardedCountsSize = rewardedCounts.size();
                if (rewardedPlayersSize > rewardedCountsSize)
                    for (index = rewardedCountsSize; index < rewardedPlayersSize; index++)
                        rewardedCounts.add(1);
                else if (rewardedPlayersSize < rewardedCountsSize)
                    for (index = rewardedPlayersSize; index < rewardedCountsSize; index++)
                        rewardedCounts.remove(rewardedPlayersSize);
                for (final UUID player : newRewardedPlayers) {
                    index = rewardedPlayers.indexOf(player);
                    if (index == -1) {
                        rewardedPlayers.add(player);
                        rewardedCounts.add(1);
                    }
                    else
                        rewardedCounts.set(index, rewardedCounts.get(index) + 1);
                }
                if (rewardedPlayers.get(0).equals(Plugin.UUID_ZERO)) {
                    rewardedPlayers.remove(0);
                    rewardedCounts.remove(0);
                }
                if (nbt.hasTag("__nbtapi", NBTType.NBTTagString))
                    nbt.removeKey("__nbtapi");
            });
    }

    // MARK: JavaPlugin

    @Override
    public void onEnable() {
        super.onEnable();
        final Server server = getServer();
        new Metrics(this, BSTATS_PLUGIN_ID);
        server.getPluginManager().registerEvents(this, this);
        vaultManipulationTask =
            server.getGlobalRegionScheduler().runAtFixedRate(
                this,
                ($0) -> {
                    for (final World world : server.getWorlds())
                        for (final Block block : collectVaults(world, collectBlockPositions(world)))
                            manipulateVault(block);
                },
                20,
                20);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (vaultManipulationTask != null) {
            if (!vaultManipulationTask.isCancelled())
                vaultManipulationTask.cancel();
            vaultManipulationTask = null;
        }
    }
}
