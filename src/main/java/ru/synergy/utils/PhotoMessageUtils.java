package ru.synergy.utils;

import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.synergy.functions.FilterOperation;

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
    public static List<String> savePhotos (List<File> files, String botToken) {
        Random random = new Random();
        ArrayList<String> paths = new ArrayList<>();
        for (File file : files) {
            try {
                final String imageUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
                final String localFileName = "./src/main/java/ru/synergy/images/" + new Date().getTime() + random.nextLong() + ".png";
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

    public static void processingImage(String fileName) { // Обработка полученной фотографии
        try {
            final BufferedImage image =  ImageUtils.getImage(fileName);
            final RgbMaster rgbMaster = new RgbMaster(image);
            ImageUtils imageUtilsJpg = new ImageUtils("png");
            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::onlyRed),
                    fileName);
//            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::greyScale),
//                    fileName);
//            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::onlyRed), "./src/newPhoto2.png");
//            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::onlyGrey), "./src/newPhoto3.png");
//            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::onlyBlue), "./src/newPhoto4.png);
//            imageUtilsJpg.saveImage(rgbMaster.changeImage(FilterOperation::sepia), "./src/newPhoto5.png");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
