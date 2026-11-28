package nl.vasilverdouw.mcdiscord.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig implements IConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.ConfigValue<String> botToken;
    private final ModConfigSpec.ConfigValue<String> guildId;

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
    public String getGuildId() {
        return guildId.get();
    }
}
