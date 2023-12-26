package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.w3c.dom.stylesheets.LinkStyle;
import ru.synergy.functions.FilterOperation;
import ru.synergy.utils.ImageUtils;
import ru.synergy.utils.PhotoMessageUtils;
import ru.synergy.utils.RgbMaster;

import java.awt.image.BufferedImage;
import java.io.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    String botToken;

    public Bot(String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return "asdhgfVbot";
    }

    @Override
    public void onUpdateReceived(Update update) {   // Создание новой сессии с ботом тоже является Update
        //1. Прием сообщения
        Message message = update.getMessage();              // возвращает написанное боту сообщение

        //2. Сохранение присланных в сообщении фотографий,создание списка путей к сохраненным фото
        List<String> photoPaths = PhotoMessageUtils.savePhotos(getFilesByMessage(message), this.botToken);

        //3. Обработка полученных фотографий и отправка пользователю
        for (String path : photoPaths) {
            PhotoMessageUtils.processingImage(path);
            // Конструирование отправляемого фото для отправки
            SendPhoto sendPhoto = preparePhotoMessage(path, message.getChatId());
            // Отправка ответного сообщения пользователю
            try {
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SendPhoto preparePhotoMessage(String localPath, Long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId); //присвоение сообщению id чата, в котором боту было отправлено сообщение
        InputFile newFile = new InputFile();
        newFile.setMedia(new File(localPath));
        sendPhoto.setPhoto(newFile);
//        sendPhoto.setCaption("edited image"); // подпись к фото
        return sendPhoto;
    }

    private List<org.telegram.telegrambots.meta.api.objects.File> getFilesByMessage(Message message) {
        List<PhotoSize> photoSizes = message.getPhoto();  // получение фотографий (первой из списка) из сообщения
        List<org.telegram.telegrambots.meta.api.objects.File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileId = photoSize.getFileId();
            try {
                files.add(sendApiMethod(new GetFile(fileId)));
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        return files;
    }
}
