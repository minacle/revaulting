# Revaulting

Revaulting is a simple PaperMC plugin that removes the one-time limitation of Vaults and Ominous Vaults introduced in Minecraft 1.21’s Trial Chambers.

## ✨ Features

- **Multiple Openings**
  By default, Vaults and Ominous Vaults can only be opened once per player.
  Revaulting removes this restriction, allowing players to open them multiple times.

- **Persistent Tracking**
  The plugin uses Minecraft’s Persistent Data system to record how many times each player has opened a Vault.
  This means that other plugins can also read and make use of this information if needed.

- **Lightweight & Seamless**
  No commands, no complicated setup. Just drop it in your plugins folder and enjoy.

## 🔧 Installation

1. Download the latest release of Revaulting.
2. Place the `.jar` file into your server’s `plugins/` directory.
3. Restart (or reload) your PaperMC server.
4. Vaults and Ominous Vaults in Trial Chambers are now reusable!

## ⚙️ Configuration

Revaulting is designed to work out-of-the-box, but you can optionally adjust cooldown reset timing.

- **Config file location**: `plugins/Revaulting/config.yml`
  - The file is generated on first run.
- **Time unit**: values are in **ticks** (`20 ticks = 1 second`).
- **Valid values**: non-negative integers. Negative values (if set) are treated as `0`.

### Options

```yml
delay:
  # Cooldown delay (ticks) for normal Vault blocks; 0 means immediate reset
  vault: 0

  # Cooldown delay (ticks) for ominous Vault blocks; 0 means immediate reset
  ominous-vault: 0
```

- `delay.vault`
  - Per-player cooldown reset delay for **normal** Vault blocks.
- `delay.ominous-vault`
  - Per-player cooldown reset delay for **ominous** Vault blocks.

After changing the config, restart the server to apply it.

## 📊 Data Storage

Revaulting stores the number of times each player has opened a Vault using Persistent Data Containers.
This data can be accessed by other plugins for advanced features or statistics.

## 📜 Compatibility

- **Minecraft Version**: 1.21.4 and above
- **Server**: [PaperMC](https://papermc.io) (and forks supporting Paper plugins)

## 🤝 Contributing

Issues and pull requests are welcome!
If you have suggestions for improvements, feel free to share them.

## 📄 License

This project is released under the [Unlicense](https://unlicense.org).
You are free to use, modify, and distribute it without restriction.

---

![bStats](https://bstats.org/signatures/bukkit/Revaulting.svg)
