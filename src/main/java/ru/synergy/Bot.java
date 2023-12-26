package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.synergy.utils.PhotoMessageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    String botToken;
    List<String> photoPaths;
    final String[] filterNames = {"grey scale", "red", "grey", "blue", "sepia"};

    public Bot(String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return "asdhgfVbot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        //1. Прием сообщения
        Message message = update.getMessage();              // возвращает написанное боту сообщение
        Long chatId = message.getChatId();

        if (message.hasPhoto()) {
            //Сохранение присланных в сообщении фотографий, создание списка путей к сохраненным фото
            this.photoPaths = PhotoMessageUtils.savePhotos(getFilesByMessage(message), this.botToken);
            // Отправка ответного сообщения пользователю c текстом команд для выбора фильтра
            sendText(chatId, getTextFilterNames(filterNames));
        } else if (message.hasText() && !this.photoPaths.isEmpty()) {
            String text = message.getText();
            boolean correctFilter = isCorrectFilter(text);
            if (correctFilter) {
                // Обработка полученных фотографий и отправка пользователю
                for (String path : this.photoPaths) {
                    PhotoMessageUtils.processingImage(path, text);
                    // Конструирование отправляемого фото для отправки
                    SendPhoto sendPhoto = preparePhotoMessage(path, message.getChatId());
                    try {
                        execute(sendPhoto); // Отправка ответного сообщения пользователю c отредактированным фото
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        } else if (message.hasText() && this.photoPaths.isEmpty()) {
            sendText(chatId, "send photos");
        } else {
            sendText(chatId, "your message is invalid");
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTextFilterNames(String[] filterNames) {
        String text;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Change filter:\n");
        for (String filter : filterNames) {
            stringBuilder.append(filter).append("\n");
        }
        text = stringBuilder.toString();
        return text;
    }

    private boolean isCorrectFilter(String text) {
        boolean correctFilter = false;
        for (String filter : filterNames) {
            if (text.equals(filter)) {
                correctFilter = true;
                break;
            }
        }
        return correctFilter;
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
