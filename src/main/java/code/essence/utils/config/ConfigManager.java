package code.essence.utils.config;

import code.essence.Essence;
import code.essence.display.hud.Notifications;
import code.essence.utils.client.sound.SoundManager;
import code.essence.features.impl.render.Hud;
import code.essence.utils.client.managers.file.exception.FileLoadException;
import code.essence.utils.client.managers.file.exception.FileSaveException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final String CONFIG_EXTENSION = ".json";




    public static boolean saveConfig(String configName) {
        try {
            Essence.getInstance().getFileController().saveFile(configName + CONFIG_EXTENSION);
            System.out.println("Config saved: " + configName);


            if (!configName.equals("default")) {
                Hud hud = Hud.getInstance();
                if (hud != null) {
                    SoundManager.playSound(SoundManager.ENABLE_MODULE);
                }
                Notifications.getInstance().addList("Конфиг «" + configName + "» сохранен!", 2000, false);
            }
            
            return true;
        } catch (FileSaveException e) {
            System.err.println("Failed to save config: " + e.getMessage());
            return false;
        }
    }


    public static boolean loadConfig(String configName) {
        try {
            Essence.getInstance().getFileController().loadFile(configName + CONFIG_EXTENSION);
            return true;
        } catch (FileLoadException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return false;
        }
    }
    

    public static void saveDefaultConfig() {
        try {
            Essence.getInstance().getFileController().saveFile("default" + CONFIG_EXTENSION);
        } catch (FileSaveException e) {
            System.err.println("Failed to auto-save default config: " + e.getMessage());
        }
    }


    public static String[] getAvailableConfigs() {
        File[] configFiles = Essence.getInstance().getClientInfoProvider().configsDir().listFiles();
        if (configFiles == null) {
            return new String[0];
        }

        List<String> configs = new ArrayList<>();
        for (File configFile : configFiles) {
            if (configFile.isFile() && configFile.getName().endsWith(CONFIG_EXTENSION)) {
                String configName = configFile.getName().replace(CONFIG_EXTENSION, "");
                configs.add(configName);
            }
        }

        return configs.toArray(new String[0]);
    }


    public static boolean deleteConfig(String configName) {
        try {
            File configFile = new File(Essence.getInstance().getClientInfoProvider().configsDir(), configName + CONFIG_EXTENSION);

            if (configFile.exists()) {
                boolean deleted = configFile.delete();
                if (deleted) {
                        Hud hud = Hud.getInstance();
                        if (hud != null) {
                            SoundManager.playSound(SoundManager.DISABLE_MODULE);
                        }
                        Notifications.getInstance().addList("Конфиг «" + configName + "» удален!", 2000, true);
                    }
                return deleted;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Failed to delete config: " + e.getMessage());
            return false;
        }
    }


    public static boolean configExists(String configName) {
        File configFile = new File(Essence.getInstance().getClientInfoProvider().configsDir(), configName + CONFIG_EXTENSION);
        return configFile.exists();
    }



    public static void loadDefaultConfig() {
        if (configExists("default")) {
            loadConfig("default");
            System.out.println("Default config loaded");
        } else {
            System.out.println("Default config not found, creating new one");
            saveDefaultConfig();
        }
    }




}
