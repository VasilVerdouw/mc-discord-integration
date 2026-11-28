# McDiscord

A server-side mod for Minecraft 1.21.1 (NeoForge, Java 21) that shows who is playing on your server in Discord.

- Updates the bot's activity with the online player count and player names.
- Gives matching Discord members an `Online` role while they are playing and removes it when they leave.
- Updates automatically when players join or leave.

## Setup

1. Install the mod JAR in your NeoForge server's `mods` folder. Players do not need to install it.
2. Create a Discord bot in the Discord Developer Portal, enable **Server Members Intent**, and invite it to your Discord server with **Manage Roles** permission.
3. Place the bot's role above the `Online` role in your Discord role list. The mod creates `Online` if it does not exist.
4. For each player, create a Discord role named exactly after their Minecraft username and assign it to their Discord account. Matching is case-insensitive and uses these roles, rather than Discord usernames or nicknames.
5. Start the Minecraft server once to generate `config/mcdiscord-common.toml`, then stop it, fill in the values below, and restart it.

| Config key | Description |
| --- | --- |
| `DISCORD_BOT_TOKEN` | Required. Your bot's token; keep it private. |
| `DISCORD_GUILD_ID` | Your Discord server ID. Optional; if omitted, the bot uses the first server it finds. Set this when the bot belongs to multiple servers. |

Both values default to empty strings. The generated TOML file includes comments explaining where to get them. Get the bot token from your application's **Bot** page in the [Discord Developer Portal](https://discord.com/developers/applications). To copy a Discord server ID, enable **User Settings > Advanced > Developer Mode** in Discord, then right-click the server icon and choose **Copy Server ID**.

Configuration uses NeoForge's `ModConfigSpec` through the mod's `IConfig` interface. It is registered as `COMMON`, so it is stored in the server's `config` directory and is not synced to players. Environment variables are no longer used by the mod. Keep the config file private because it contains the bot token, and restart the server after editing it.

## Commands and troubleshooting

Use `/mcdiscord refresh` to update the bot's activity and roles manually (requires operator permission level 2). The bot syncs after connecting and rechecks roles periodically. On normal shutdown it attempts to clear the Online roles before disconnecting. After a crash, those roles may remain until the bot reconnects and syncs again.

If roles do not update, check the server logs, the bot's permissions and role position, Server Members Intent, and the player's username role. The `Online` role is managed for all members of the selected Discord server, so use it only for this integration.

## Building from source

With Java 21 installed, run `./gradlew build` (`.\gradlew.bat build` on Windows). Build output is written to `build/libs/`.
