package nl.vasilverdouw.mcdiscord.config;

public class EnvVariableConfig implements IConfig {
    @Override
    public String getBotToken() {
        return System.getenv().getOrDefault("DISCORD_BOT_TOKEN", "");
    }

    @Override
    public String getGuildId() {
        return System.getenv().getOrDefault("DISCORD_GUILD_ID", "");
    }

    @Override
    public String getOnlineUpdatesChannelId() {
        return System.getenv().getOrDefault("DISCORD_ONLINE_UPDATES_CHANNEL_ID", "");
    }
}
