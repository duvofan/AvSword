# AvSword

**AvSword** is a lightweight and optimized combat plugin that introduces unique sword abilities. Designed for PvP servers.

AvSword supports **Minecraft 1.16+** and includes seamless integration for **Custom Model Data**, making it perfect for servers using resource packs.

### Why AvSword?
* **Optimized:** Maintains performance even during intense combat.
* **Highly Configurable:** All settings can be edited through `config.yml` or modified instantly using in-game admin commands.
* **Safe Zones:** Built-in protection system that disables abilities inside **WorldGuard regions** or **custom-defined** safe zones.

### Special Effects
AvSword features unique, **custom-coded** mechanics that act immediately on usage:
* **Enderman:** Teleport forward instantly.
* **Dragon:** Launch a Dragon fireball.
* **Spider:** Place a cobweb under the target to immobilize them.
* **Phantom:** Temporarily disable the target’s Elytra, preventing them from using it to fly.

### Requirements
* **Server Software:** Spigot or Paper (recommended).
* **Minecraft Version:** 1.16+.
* **Optional:** WorldGuard — AvSword automatically respects region protections.

### Commands & Permissions
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/avsword give <player> <sword>` | Give a custom sword to a player. | `avsword.admin` |
| `/avsword bind <sword>` | Bind an ability to the item in hand. | `avsword.admin` |
| `/avsword list` | List all available swords. | `avsword.admin` |
| `/avsword reload` | Reload configuration files. | `avsword.admin` |
| `/avsword create <name> <model_id> <effect>` | Create a new custom sword. | `avsword.admin` |
| `/avsword edit <sword> <setting> <value>` | Edit settings of an existing sword. | `avsword.admin` |

### Language Support
AvSword supports **Turkish** and **English**. You can set your preferred language in `config.yml` (`language: tr` or `language: en`). All messages, commands, and descriptions will display in the selected language.

### Support
If you find a bug or have a suggestion, please contact us on [Discord](https://discord.gg/AnBGXUzTpK)
