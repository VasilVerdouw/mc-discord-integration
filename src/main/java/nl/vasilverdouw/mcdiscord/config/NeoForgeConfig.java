package nl.vasilverdouw.mcdiscord.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig implements IConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.ConfigValue<String> botToken;
    private final ModConfigSpec.ConfigValue<String> guildId;
    private final ModConfigSpec.ConfigValue<String> onlineUpdatesChannelId;

    public NeoForgeConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        botToken = builder.comment(
                "Discord bot token. Leave empty to keep the bot disabled.",
                "Get it at https://discord.com/developers/applications: select your application,",
                "open Bot, then use Reset Token to generate and copy the token. Keep it private.",
                "Restart the Minecraft server after changing this value.")
                .define("DISCORD_BOT_TOKEN", "");
        guildId = builder.comment(
                "Discord server (guild) ID. Leave empty to use the first server the bot finds.",
                "In Discord, enable User Settings > Advanced > Developer Mode,",
                "then right-click your server icon and choose Copy Server ID.",
                "Restart the Minecraft server after changing this value.")
                .define("DISCORD_GUILD_ID", "");
        onlineUpdatesChannelId = builder.comment(
                "Discord channel ID for online updates. Leave empty to disable online updates.",
                "This will send a message with the current online players to the specified channel and keep it updated.",
                "In Discord, enable User Settings > Advanced > Developer Mode,",
                "then right-click your channel and choose Copy Channel ID.",
                "Restart the Minecraft server after changing this value.")
                .define("DISCORD_ONLINE_UPDATES_CHANNEL_ID", "");
        spec = builder.build();
    }

    public ModConfigSpec getSpec() {
        return spec;
    }

    @Override
    public String getBotToken() {
        return botToken.get();
    }

    @Override
    public String getOnlineUpdatesChannelId() {
        return onlineUpdatesChannelId.get();
    }

    @Override
    public String getGuildId() {
        return guildId.get();
    }
}
