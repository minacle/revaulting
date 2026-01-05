package moe.minacle.minecraft.plugins.revaulting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.file.FileConfiguration;

public final class Configuration {

    private static final @NotNull String DELAY_KEY = "delay";

    private static final @NotNull String OMINOUS_VAULT_KEY = "ominous-vault";

    private static final @NotNull String VAULT_KEY = "vault";

    public static abstract class Section {

        private final @NotNull String path;

        private final @NotNull String sectionKey;

        private final @Nullable Section supersection;

        protected Section(final @NotNull String sectionKey, final @Nullable Section supersection) {
            if (supersection == null)
                path = sectionKey;
            else
                path = String.format("%s.%s", supersection.getPath(), sectionKey);
            this.sectionKey = sectionKey;
            this.supersection = supersection;
        }

        protected @NotNull String getPath() {
            return path;
        }

        protected @NotNull String getPathForKey(final @NotNull String key) {
            return String.format("%s.%s", getPath(), key);
        }

        protected @NotNull String getSectionKey() {
            return sectionKey;
        }

        protected @Nullable Section getSupersection() {
            return supersection;
        }
    }

    public final class DelaySection extends Section {

        private @Nullable Integer vault;

        private @Nullable Integer ominousVault;

        DelaySection() {
            super(DELAY_KEY, null);
        }

        public int getVault() {
            if (vault == null)
                vault = Math.max(getFileConfiguration().getInt(getPathForKey(VAULT_KEY), 0), 0);
            return vault;
        }

        public int getOminousVault() {
            if (ominousVault == null)
                ominousVault = Math.max(getFileConfiguration().getInt(getPathForKey(OMINOUS_VAULT_KEY), 0), 0);
            return ominousVault;
        }
    }

    private @NotNull FileConfiguration fileConfiguration;

    private @NotNull DelaySection delay;

    public Configuration(final @NotNull Plugin plugin) {
        fileConfiguration = plugin.getConfig();
        delay = new DelaySection();
    }

    public @NotNull DelaySection getDelay() {
        return delay;
    }

    private @NotNull FileConfiguration getFileConfiguration() {
        return fileConfiguration;
    }
}
