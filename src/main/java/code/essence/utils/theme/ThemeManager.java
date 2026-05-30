package code.essence.utils.theme;

import code.essence.Essence;
import code.essence.features.module.setting.implement.ColorSetting;
import code.essence.display.hud.Notifications;
import code.essence.utils.client.sound.SoundManager;
import code.essence.features.impl.render.Hud;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String THEME_EXTENSION = ".json";
    private static final String DEFAULT_THEME_NAME = "default_dark";


    public static File getThemeDir() {
        try {
            File filesDir = Essence.getInstance().getClientInfoProvider().filesDir();
            File themeDir = new File(filesDir, "themes");
            if (!themeDir.exists()) {
                themeDir.mkdirs();
            }
            return themeDir;
        } catch (Exception e) {
            return new File("Essence/files/themes");
        }
    }


    public static final ColorSetting primaryColor = new ColorSetting("Основной цвет", "Основной цвет клиента")
            .setColor(new Color(255, 101, 57, 255).getRGB()).presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);

    public static final ColorSetting secondaryColor = new ColorSetting("Вторичный цвет", "Вторичный цвет клиента")
            .setColor(new Color(255, 101, 57, 255).getRGB()).presets(0xFF6C9AFD, 0xFF8C7FFF, 0xFFFFA576, 0xFFFF7B7B);

    public static final ColorSetting BackgroundGui = new ColorSetting("Цвет заднего фона", "Цвет заднего фона в гуи")
            .setColor(new Color(17, 17, 20, 255).getRGB());

    public static final ColorSetting offModuleColor = new ColorSetting("Цвет выкл. модулей", "Цвет заднего фона в гуи")
            .setColor(new Color(32, 32, 35, 255).getRGB());

    public static final ColorSetting BackgroundSettings = new ColorSetting("Цвет настроек модуля", "Цвет заднего фона в гуи")
            .setColor(new Color(27, 27, 30, 255).getRGB());

    public static final ColorSetting textColor = new ColorSetting("Цвет текста", "Основной цвет текста в GUI")
            .setColor(new Color(255, 255, 255, 255).getRGB());

    static {
        try {
            Files.createDirectories(getThemeDir().toPath());
            initializeDefaultThemes();
            loadDefaultTheme();
        } catch (IOException e) {
            System.err.println("Failed to create theme directory: " + e.getMessage());
        }
    }


    private static void initializeDefaultThemes() {

        createDefault_darkTheme();
        createDefault_lightTheme();
        createDefault_blurTheme();
    }


    private static void createDefault_darkTheme() {
        try {
            JsonObject theme = new JsonObject();
            theme.addProperty("name", "default_dark");
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", System.currentTimeMillis());

            JsonObject colors = new JsonObject();
            colors.addProperty("Первый цвет", new Color(108, 101, 204, 255).getRGB());
            colors.addProperty("Второй цвет", new Color(177, 149, 214, 255).getRGB());
            colors.addProperty("Цвет заднего фона", new Color(17, 17, 20, 255).getRGB());
            colors.addProperty("Цвет выкл. модулей", new Color(32, 32, 35, 255).getRGB());
            colors.addProperty("Цвет настроек модуля", new Color(27, 27, 30, 180).getRGB());
            colors.addProperty("Цвет текста", new Color(255, 255, 255, 255).getRGB());
            theme.add("colors", colors);

            File themeFile = new File(getThemeDir(), "default_dark" + THEME_EXTENSION);
            String fileName = themeFile.getAbsolutePath();
            try (FileWriter writer = new FileWriter(fileName)) {
                GSON.toJson(theme, writer);
            }

            System.out.println("Default theme created");
        } catch (IOException e) {
            System.err.println("Failed to create default theme: " + e.getMessage());
        }
    }


    private static void createDefault_lightTheme() {
        try {
            JsonObject theme = new JsonObject();
            theme.addProperty("name", "default_light");
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", System.currentTimeMillis());

            JsonObject colors = new JsonObject();
            colors.addProperty("Первый цвет", new Color(108, 101, 204, 255).getRGB());
            colors.addProperty("Второй цвет", new Color(177, 149, 214, 255).getRGB());
            colors.addProperty("Цвет заднего фона", new Color(233, 233, 233, 255).getRGB());
            colors.addProperty("Цвет выкл. модулей", new Color(255, 255, 255, 255).getRGB());
            colors.addProperty("Цвет настроек модуля", new Color(245, 245, 245, 255).getRGB());
            colors.addProperty("Цвет текста", new Color(0, 0, 0, 255).getRGB());
            theme.add("colors", colors);

            File themeFile = new File(getThemeDir(), "default_light" + THEME_EXTENSION);
            String fileName = themeFile.getAbsolutePath();
            try (FileWriter writer = new FileWriter(fileName)) {
                GSON.toJson(theme, writer);
            }

            System.out.println("Classic theme created");
        } catch (IOException e) {
            System.err.println("Failed to create classic theme: " + e.getMessage());
        }
    }


    private static void createDefault_blurTheme() {
        try {
            JsonObject theme = new JsonObject();
            theme.addProperty("name", "default_blur");
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", System.currentTimeMillis());

            JsonObject colors = new JsonObject();
            colors.addProperty("Первый цвет", new Color(108, 101, 204, 255).getRGB());
            colors.addProperty("Второй цвет", new Color(177, 149, 214, 255).getRGB());
            colors.addProperty("Цвет заднего фона", new Color(233, 233, 233, 0).getRGB());
            colors.addProperty("Цвет выкл. модулей", new Color(255, 255, 255, 14).getRGB());
            colors.addProperty("Цвет настроек модуля", new Color(245, 245, 245, 0).getRGB());
            colors.addProperty("Цвет текста", new Color(255, 255, 255, 255).getRGB());
            theme.add("colors", colors);

            File themeFile = new File(getThemeDir(), "default_blur" + THEME_EXTENSION);
            String fileName = themeFile.getAbsolutePath();
            try (FileWriter writer = new FileWriter(fileName)) {
                GSON.toJson(theme, writer);
            }

            System.out.println("Blur theme created");
        } catch (IOException e) {
            System.err.println("Failed to create blur theme: " + e.getMessage());
        }
    }


    private static void loadDefaultTheme() {
        if (themeExists("default")) {
            loadTheme("default");
            System.out.println("Default theme loaded");
        } else {
            if (themeExists("default_dark")) {
                loadTheme("default_dark");
                System.out.println("Default dark theme loaded");
            }
        }
    }


    public static boolean saveTheme(String themeName) {
        try {
            JsonObject theme = new JsonObject();


            theme.addProperty("name", themeName);
            theme.addProperty("version", "1.0");
            theme.addProperty("timestamp", System.currentTimeMillis());

            JsonObject colors = new JsonObject();
            colors.addProperty("Первый цвет", primaryColor.getColor());
            colors.addProperty("Второй цвет", secondaryColor.getColor());
            colors.addProperty("Цвет заднего фона", BackgroundGui.getColor());
            colors.addProperty("Цвет выкл. модулей", offModuleColor.getColor());
            colors.addProperty("Цвет настроек модуля", BackgroundSettings.getColor());
            colors.addProperty("Цвет текста", textColor.getColor());
            theme.add("colors", colors);


            File themeFile = new File(getThemeDir(), themeName + THEME_EXTENSION);
            try (FileWriter writer = new FileWriter(themeFile)) {
                GSON.toJson(theme, writer);
            }

            if (!themeName.equals("default")) {
                SoundManager.playSound(SoundManager.DISABLE_MODULE);
                Notifications.getInstance().addList("Тема «" + themeName + "» сохранена!", 2000, false);
            }
            
            return true;

        } catch (IOException e) {
            System.err.println("Failed to save theme: " + e.getMessage());
            return false;
        }
    }


    public static boolean loadTheme(String themeName) {
        try {
            File themeFile = new File(getThemeDir(), themeName + THEME_EXTENSION);

            if (!themeFile.exists()) {
                System.err.println("Theme file not found: " + themeName);
                return false;
            }

            String content = Files.readString(themeFile.toPath());
            if (content == null) {
                System.err.println("Theme file is empty: " + themeName);
                return false;
            }

            String trimmed = content.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                System.err.println("Theme file is not valid JSON: " + themeName);
                return false;
            }

            JsonObject theme;
            try {
                theme = JsonParser.parseString(trimmed).getAsJsonObject();
            } catch (Exception parseException) {
                System.err.println("Failed to parse theme JSON (" + themeName + "): " + parseException.getMessage());
                return false;
            }


            if (theme.has("colors")) {
                JsonObject colors = theme.getAsJsonObject("colors");
                if (colors.has("Первый цвет")) {
                    primaryColor.setColor(colors.get("Первый цвет").getAsInt());
                }
                if (colors.has("Второй цвет")) {
                    secondaryColor.setColor(colors.get("Второй цвет").getAsInt());
                }
                if (colors.has("Цвет заднего фона")) {
                    BackgroundGui.setColor(colors.get("Цвет заднего фона").getAsInt());
                }
                if (colors.has("Цвет выкл. модулей")) {
                    offModuleColor.setColor(colors.get("Цвет выкл. модулей").getAsInt());
                }
                if (colors.has("Цвет настроек модуля")) {
                    BackgroundSettings.setColor(colors.get("Цвет настроек модуля").getAsInt());
                }
                if (colors.has("Цвет текста")) {
                    textColor.setColor(colors.get("Цвет текста").getAsInt());
                }
            }
            return true;

        } catch (IOException e) {
            System.err.println("Failed to load theme: " + e.getMessage());
            return false;
        }
    }


    public static void saveDefaultTheme() {
        saveTheme("default");
    }


    public static String[] getAvailableThemes() {
        File themeDir = getThemeDir();
        if (!themeDir.exists() || !themeDir.isDirectory()) {
            return new String[0];
        }

        File[] files = themeDir.listFiles((dir, name) -> name.endsWith(THEME_EXTENSION));
        if (files == null) {
            return new String[0];
        }

        List<ThemeInfo> themes = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            String themeName = fileName.substring(0, fileName.length() - THEME_EXTENSION.length());
            if (!themeName.equals("default")) {
                long timestamp = getThemeTimestamp(file);
                themes.add(new ThemeInfo(themeName, timestamp));
            }
        }

        themes.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

        return themes.stream().map(t -> t.name).toArray(String[]::new);
    }

    private static long getThemeTimestamp(File themeFile) {
        try {
            String content = Files.readString(themeFile.toPath());
            JsonObject theme = JsonParser.parseString(content).getAsJsonObject();
            if (theme.has("timestamp")) {
                return theme.get("timestamp").getAsLong();
            }
        } catch (Exception e) {
        }
        return themeFile.lastModified();
    }

    private static class ThemeInfo {
        String name;
        long timestamp;

        ThemeInfo(String name, long timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }
    }


    public static boolean deleteTheme(String themeName) {
        if (themeName.equals("default_dark") || themeName.equals("default_light") || themeName.equals("default_blur")) {
            System.out.println("Cannot delete default theme: " + themeName);
            return false;
        }
        
        try {
            File themeFile = new File(getThemeDir(), themeName + THEME_EXTENSION);

            if (themeFile.exists()) {
                boolean deleted = themeFile.delete();
                if (deleted) {
                    System.out.println("Theme deleted: " + themeName);
                    SoundManager.playSound(SoundManager.DISABLE_MODULE);
                    Notifications.getInstance().addList("Тема «" + themeName + "» удалена!", 2000, true);
                }
                return deleted;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Failed to delete theme: " + e.getMessage());
            return false;
        }
    }


    public static boolean themeExists(String themeName) {
        File themeFile = new File(getThemeDir(), themeName + THEME_EXTENSION);
        return themeFile.exists();
    }
    

    public static void recreateDefaultThemes() {
        createDefault_darkTheme();
        createDefault_lightTheme();
        System.out.println("Default themes recreated with original colors");
    }
}
