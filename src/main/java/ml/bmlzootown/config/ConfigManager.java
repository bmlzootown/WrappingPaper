package ml.bmlzootown.config;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    public static Configuration getConfig(File file) {
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveConfig(Configuration config, File file) {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
