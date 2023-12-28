package ru.synergy.utils;

import org.telegram.telegrambots.meta.api.objects.File;
import ru.synergy.functions.ImageOperation;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;

public class PhotoMessageUtils {
    static final String directoryPath = "./src/main/java/ru/synergy/images/";
    static String extension = null;

    public static List<String> savePhotos(List<File> files, String botToken) throws IOException {
        Random random = new Random();
        ArrayList<String> paths = new ArrayList<>();
        String path = files.get(0).getFilePath();
        extension = path.substring(path.indexOf('.') + 1); //расширения файла
        for (File file : files) {
            final String imageUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
            final String localFileName = directoryPath + new Date().getTime() + random.nextLong() + "." + extension;
            saveImage(imageUrl, localFileName);
            paths.add(localFileName);
        }
        return paths;
    }

    public static void saveImage(String url, String fileName) throws IOException {
        try (InputStream inputStream = new URL(url).openStream();
             OutputStream outputStream = new FileOutputStream(fileName)) {
            byte[] b = new byte[2048];  // будем записывать патчами по 2Кбайт
            int length;
            while ((length = inputStream.read(b)) != -1) { // пока имеются данные для считывания
                outputStream.write(b, 0, length);
            }
        }
    }

    public static void processingImage(String fileName, ImageOperation operation) throws Exception { // Обработка полученной фотографии
        final BufferedImage image = ImageUtils.getImage(fileName);
        final RgbMaster rgbMaster = new RgbMaster(image);
        ImageUtils imageUtilsJpg = new ImageUtils(extension);
        imageUtilsJpg.saveImage(rgbMaster.changeImage(operation), fileName);
    }

    public static void cleaningDirectory() {
        java.io.File folder = new java.io.File(directoryPath);
        for (java.io.File file : Objects.requireNonNull(folder.listFiles())) {
            if (file != null) {
                file.delete();
            }
        }
    }
}
