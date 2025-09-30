package moe.minacle.minecraft.plugins.revaulting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bstats.bukkit.Metrics;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public final class Plugin extends JavaPlugin implements Listener {

    private static final int BSTATS_PLUGIN_ID = 22418;

    private final @NotNull NamespacedKey REWARDED_PLAYERS_KEY = Objects.requireNonNull(namespacedKey("rewarded_players"));

    private final @NotNull NamespacedKey REWARDED_COUNTS_KEY = Objects.requireNonNull(namespacedKey("rewarded_counts"));

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
        final Collection<UUID> newRewardedPlayers;
        final PersistentDataContainer persistentDataContainer;
        final List<int[]> rewardedPlayers;
        final List<Integer> rewardedCounts;
        if (block.getType() != Material.VAULT)
            return;
        vault = (Vault)block.getState();
        newRewardedPlayers = vault.getRewardedPlayers();
        for (final UUID player : newRewardedPlayers)
            vault.removeRewardedPlayer(player);
        persistentDataContainer = vault.getPersistentDataContainer();
        rewardedPlayers =
            new ArrayList<>(
                persistentDataContainer.getOrDefault(
                    REWARDED_PLAYERS_KEY,
                    PersistentDataType.LIST.integerArrays(),
                    List.of()));
        rewardedCounts =
            new ArrayList<>(
                persistentDataContainer.getOrDefault(
                    REWARDED_COUNTS_KEY,
                    PersistentDataType.LIST.integers(),
                    List.of()));
        for (final UUID player : newRewardedPlayers) {
            final int[] intArrayUUID =
                new int[] {
                    (int)(player.getMostSignificantBits() >> 32),
                    (int)player.getMostSignificantBits(),
                    (int)(player.getLeastSignificantBits() >> 32),
                    (int)player.getLeastSignificantBits(),
                };
            int playerIndex = -1;
            for (int index = 0; index < rewardedPlayers.size(); index++) {
                if (rewardedPlayers.get(index)[0] == intArrayUUID[0] &&
                    rewardedPlayers.get(index)[1] == intArrayUUID[1] &&
                    rewardedPlayers.get(index)[2] == intArrayUUID[2] &&
                    rewardedPlayers.get(index)[3] == intArrayUUID[3]
                ) {
                    playerIndex = index;
                    break;
                }
            }
            if (playerIndex == -1) {
                rewardedPlayers.add(intArrayUUID);
                rewardedCounts.add(1);
            }
            else
                rewardedCounts.set(playerIndex, rewardedCounts.get(playerIndex) + 1);
        }
        persistentDataContainer.set(REWARDED_PLAYERS_KEY, PersistentDataType.LIST.integerArrays(), rewardedPlayers);
        persistentDataContainer.set(REWARDED_COUNTS_KEY, PersistentDataType.LIST.integers(), rewardedCounts);
        vault.update();
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
