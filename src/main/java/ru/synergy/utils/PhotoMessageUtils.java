package ru.synergy.utils;

import org.telegram.telegrambots.meta.api.objects.File;
import ru.synergy.functions.FilterOperation;
import ru.synergy.functions.ImageOperation;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class PhotoMessageUtils {
    public static List<String> savePhotos(List<File> files, String botToken, String extension) {
        Random random = new Random();
        ArrayList<String> paths = new ArrayList<>();
        for (File file : files) {
            try {
                final String imageUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
                final String localFileName = "./src/main/java/ru/synergy/images/"
                        + new Date().getTime() + random.nextLong() + "." + extension;
                saveImage(imageUrl, localFileName);
                paths.add(localFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    public static void processingImage(String fileName, String filter, String extension) { // Обработка полученной фотографии
        try {
            final BufferedImage image = ImageUtils.getImage(fileName);
            final RgbMaster rgbMaster = new RgbMaster(image);
            ImageUtils imageUtilsJpg = new ImageUtils(extension);
            ImageOperation operation = null;
            switch (filter) {
                case "grey scale":
                    operation = FilterOperation::greyScale;
                    break;
                case "red":
                    operation = FilterOperation::onlyRed;
                    break;
                case "grey":
                    operation = FilterOperation::onlyGrey;
                    break;
                case "blue":
                    operation = FilterOperation::onlyBlue;
                    break;
                case "sepia":
                    operation = FilterOperation::sepia;
                    break;
            }
            imageUtilsJpg.saveImage(rgbMaster.changeImage(operation), fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTextFilterNames(String[] filterNames) {
        String text;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Change filter:\n");
        for (String filter : filterNames) {
            stringBuilder.append(filter).append("\n");
        }
        text = stringBuilder.toString();
        return text;
    }

    public static boolean isCorrectFilter(String text, String[] filterNames) {
        boolean correctFilter = false;
        for (String filter : filterNames) {
            if (text.equalsIgnoreCase(filter)) {
                correctFilter = true;
                break;
            }
        }
        return correctFilter;
    }
}
