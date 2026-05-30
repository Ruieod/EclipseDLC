package code.essence.main;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FontGenerator {
    public static final String CHARSET = "\"!\\\"#$%&\\\\()*+,-./ 0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_`abcdefghijklmnopqrstuvwxyz{|}~ЁАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюяё\"";
    public static void main(String[] args) {
        
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            
        }
        String fontFolderPath = "mtsdf-font/";
        Path outputPath = initFile(fontFolderPath);
        if (outputPath == null) {
            System.err.println("Не удалось инициализировать папку вывода.");
            return;
        }

        generate(fontFolderPath, outputPath, "ghost");
    }

    @SuppressWarnings("SameParameterValue")
    private static void generate(String fontFolderPath, Path outputPath, String fontPath) {

        File fontFolder = new File(fontFolderPath + fontPath);
        File[] fontFiles = fontFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".otf"));
        if (fontFiles != null && fontFiles.length > 0) {
            
            File atlasGenExe = new File(fontFolderPath + "atlas-gen.exe");
            if (!atlasGenExe.exists()) {
                System.err.println("Не найден atlas-gen.exe в папке: " + atlasGenExe.getAbsolutePath());
                return;
            }
            
            ExecutorService executor = Executors.newFixedThreadPool(fontFiles.length);
            List<Process> processes = new ArrayList<>();
            for (File fontFile : fontFiles) {
                executor.execute(() -> {
                    try {
                        String fontFileName = fontFile.getName().replaceFirst("[.][^.]+$", "");
                        Path charsetPath = Path.of(fontFolderPath + "charset.txt").toAbsolutePath();
                        Path imageOutPath = outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_") + ".png").toAbsolutePath();
                        Path jsonOutPath = outputPath.resolve(fontFileName.toLowerCase().replaceAll("-", "_") + ".json").toAbsolutePath();
                        
                        
                        ProcessBuilder processBuilder = new ProcessBuilder(
                                atlasGenExe.getAbsolutePath(),
                                "-font", fontFile.getAbsolutePath(),
                                "-charset", charsetPath.toString(),
                                "-type", "mtsdf",
                                "-format", "png",
                                "-imageout", imageOutPath.toString(),
                                "-json", jsonOutPath.toString(),
                                "-size", "64",
                                "-pxrange", "10"
                        );
                        Process process = processBuilder.start();
                        processes.add(process);
                        int exitCode = process.waitFor();
                        if (exitCode == 0) {
                            System.out.println("Атлас для шрифта " + fontFileName + " успешно создан.");
                        } else {
                            System.out.println("Ошибка при создании атласа для шрифта " + fontFileName);
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Ошибка при выполнении команды для шрифта " + fontFile.getName() + ": " + e.getMessage());
                    }
                });
            }
            executor.shutdown();
            try {
                if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS))
                    System.out.println("Процесс завершён.");
            } catch (InterruptedException e) {
                System.err.println("Ошибка при ожидании завершения потоков: " + e.getMessage());
            }
            for (Process process : processes) {
                process.destroy();
            }
        } else {
            System.out.println("Не найдены файлы шрифтов в указанной папке.");
        }
    }

    private static Path initFile(String fontFolderPath) {
        Path outputPath = Path.of(fontFolderPath + "out");
        if (Files.notExists(outputPath)) {
            try {
                Files.createDirectories(outputPath);
            } catch (IOException e) {
                System.err.println("Ошибка при создании папки: " + e.getMessage());
                return null;
            }
        }
        Path charsetPath = Path.of(fontFolderPath + "charset.txt");
        if (Files.notExists(charsetPath)) {
            try {
                Files.createFile(charsetPath);
                Files.write(charsetPath, CHARSET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Ошибка при создании charset.txt: " + e.getMessage());
                return null;
            }
        }
        return outputPath;
    }
}