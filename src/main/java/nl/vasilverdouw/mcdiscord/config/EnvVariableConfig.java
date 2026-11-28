package nl.vasilverdouw.mcdiscord.config;

public class EnvVariableConfig implements IConfig {
    @Override
    public String getConfigValue(String key) {
        return System.getenv(key);
    }
}
