package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.io.*;
import java.io.File;
import java.net.URL;

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
        Message message = update.getMessage();              // возвращает написанное боту сообщение
        String responseDate = message.getDate().toString(); // время сообщения
        User user = message.getFrom();                      // пользователь, отправивший сообщение
        String responseId = user.getId().toString();        // id пользователя, отправившего сообщение
        System.out.println(message.getText());              // печатает написанное боту сообщение

        // Получение фотографий из сообщения
        PhotoSize photoSize = message.getPhoto().get(0);  // получение фотографий (первой из списка) из сообщения
        final String fileId = photoSize.getFileId();
        try {
            org.telegram.telegrambots.meta.api.objects.File file = sendApiMethod(new GetFile(fileId));
            final String imageUrl = "https://api.telegram.org/file/bot" + this.botToken + "/" + file.getFilePath();
            saveImage(imageUrl, "./src/main/java/ru/synergy/received_image.png");
            System.out.println("image is saved");
        } catch (TelegramApiException | IOException e) {
            throw new RuntimeException(e);
        }


        // Конструирование отправляемого фото
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(message.getChatId()); //присвоение сообщению id чата, в котором боту было отправлено сообщение
        InputFile newFile = new InputFile();
        newFile.setMedia(new File("./src/main/java/ru/synergy/img.png"));
        sendPhoto.setPhoto(newFile);
        sendPhoto.setCaption("This is sky picture"); // подпись к фото

        // Конструирование отправляемого отдельного сообщения
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText("Your message: " + "Date: " + responseDate + ", user id: " + responseId);

        try {
            execute(sendPhoto);          // отправка фото
            execute(sendMessage);        // отправка сообщения
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveImage(String url, String fileName) throws IOException {
        try (InputStream inputStream = new URL(url).openStream();
             OutputStream outputStream = new FileOutputStream(fileName)) {
            byte[] b = new byte[2048];  // будем записывать патчами по 2Кбайт
            int length;
            while ((length = inputStream.read(b)) != -1) { // пока имеются данные для считывания
                outputStream.write(b, 0, length);
            }
        }
    }

}
