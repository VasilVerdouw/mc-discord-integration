package nl.vasilverdouw.mcdiscord.config;

public interface IConfig {
    String getBotToken();

    /** Returns the optional guild ID, or an empty string to use the first available guild. */
    String getGuildId();
}
