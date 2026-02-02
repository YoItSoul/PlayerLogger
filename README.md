# PlayerLogger

A Hytale server plugin that tracks player statistics with cloud sync and web dashboard.

[![Discord](https://img.shields.io/badge/Discord-Join-7289DA)](https://discord.gg/CnJuHve8gn)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436)](https://curseforge.com/hytale/mods/player-logger)

---

## Features

- **Player Tracking** - Playtime, online status, session history
- **Combat Stats** - PvP kills, mob kills, deaths, damage dealt
- **Block Stats** - Blocks placed and broken
- **Cloud Sync** - View stats at [hytaletravelers.com/stats](https://hytaletravelers.com/stats)
- **In-Game Dashboard** - Interactive UI with sorting and search
- **Discord Webhooks** - Real-time notifications
- **Server Browser** - Get discovered at [hytaletravelers.com/servers](https://hytaletravelers.com/servers)

---

## Quick Start

1. Download from [CurseForge](https://curseforge.com/hytale/mods/player-logger)
2. Place in your server's `mods` folder
3. Start your server

Stats are tracked and synced automatically.

---

## Commands

| Command | Description |
|---------|-------------|
| `/list` | Open the stats dashboard |
| `/players` | Alias for /list |
| `/pl ui` | Open the stats dashboard |
| `/pl help` | Show available commands |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/pl wipe <player>` | `playerlogger.command.wipe` | Remove a player's data |
| `/pl wipe all` | `playerlogger.command.wipe` | Wipe all player data |
| `/pl reset <category>` | `playerlogger.command.reset` | Reset stats for all players |
| `/pl reset <category> <player>` | `playerlogger.command.reset` | Reset stats for one player |

**Categories:** `all`, `combat`, `blocks`, `playtime`, `kills`, `deaths`, `damage`

---

## Configuration

Edit `config.json` in the plugin's data folder:

```json
{
  "pushEnabled": true,
  "pushUrl": "https://api.hytaletravelers.com",
  "pushIntervalSeconds": 30,
  "serverName": "",
  "publicListing": true,
  "webEnabled": false,
  "webPort": 8080,
  "webhookEnabled": false,
  "webhookUrl": ""
}
```

### Discord Webhooks

```json
{
  "webhookEnabled": true,
  "webhookUrl": "https://discord.com/api/webhooks/...",
  "webhookPlayerJoin": true,
  "webhookPlayerLeave": true,
  "webhookPlayerDeath": true,
  "webhookPlayerKill": true,
  "webhookDailyLeaderboard": true,
  "webhookDailyLeaderboardHour": 12
}
```

---

## Project Structure

```
src/main/java/com/hytaletravelers/playerlogger/
├── PlayerLoggerPlugin.java          # Plugin entry point
├── commands/
│   ├── PlayerLoggerPluginCommand.java
│   ├── ListCommand.java
│   ├── HelpSubCommand.java
│   ├── UISubCommand.java
│   ├── WipeSubCommand.java
│   └── ResetSubCommand.java
├── config/
│   └── PluginConfig.java
├── data/
│   ├── PlayerData.java
│   └── PlayerDataManager.java
├── listeners/
│   └── PlayerListener.java
├── systems/
│   ├── DamageTrackingSystem.java
│   ├── DeathTrackingSystem.java
│   ├── BlockBreakTrackingSystem.java
│   └── BlockPlaceTrackingSystem.java
├── ui/
│   └── PlayerLoggerDashboardUI.java
├── update/
│   └── UpdateChecker.java
├── webhook/
│   └── DiscordWebhookService.java
└── web/
    ├── DataPushService.java
    └── WebServer.java
```

---

## Building

```bash
git clone https://github.com/HytaleTravelers/PlayerLogger.git
cd PlayerLogger
./gradlew build
```

Output JAR: `build/libs/`

### Gradle Tasks

| Task | Description |
|------|-------------|
| `build` | Build the plugin JAR |
| `bumpPatch` | Increment patch version (1.0.X) |
| `bumpMinor` | Increment minor version (1.X.0) |
| `bumpMajor` | Increment major version (X.0.0) |
| `release` | Bump patch and build for release |
| `deployToServer` | Copy JAR to server/mods/ |

---

## API

See [API.md](API.md) for the public REST API documentation.

### Local API

Enable `webEnabled` in config to host your own JSON API:

- `GET /api/players` - List all players with stats
- `GET /api/stats` - Server-wide statistics

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## Support

- **Discord:** [discord.gg/CnJuHve8gn](https://discord.gg/CnJuHve8gn)
- **Issues:** [GitHub Issues](https://github.com/HytaleTravelers/PlayerLogger/issues)

---

## License

MIT License - see [LICENSE](LICENSE) for details.

---

Made by [HytaleTravelers](https://hytaletravelers.com)
