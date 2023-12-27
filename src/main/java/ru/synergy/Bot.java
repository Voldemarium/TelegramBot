package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.synergy.functions.FilterOperation;
import ru.synergy.utils.PhotoMessageUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    final String botToken;
    List<String> photoPaths;
    String extension;
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
            this.photoPaths = PhotoMessageUtils.savePhotos(getFilesByMessage(message), this.botToken, this.extension);
            // Отправка ответного сообщения пользователю c текстом команд для выбора фильтра
            sendText(chatId, PhotoMessageUtils.getTextFilterNames(filterNames));
        } else if (photoPaths == null) {
            sendText(chatId, "your message is incorrect, send photos");
        } else if (message.hasText() && !this.photoPaths.isEmpty()) {
            String text = message.getText();
            boolean correctFilter = PhotoMessageUtils.isCorrectFilter(text, filterNames);
            if (correctFilter) {
                // Обработка полученных фотографий и отправка пользователю
                for (String path : this.photoPaths) {
                    PhotoMessageUtils.processingImage(path, text.toLowerCase(), this.extension);
                    // Конструирование отправляемого фото для отправки
                    SendPhoto sendPhoto = preparePhotoMessage(path, message.getChatId());
                    try {
                        execute(sendPhoto); // Отправка ответного сообщения пользователю c отредактированным фото
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                sendText(chatId, "your filter name is incorrect");
            }
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
            e.printStackTrace();
        }
    }

    private SendPhoto preparePhotoMessage(String localPath, Long chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setReplyMarkup(getKeyboard(FilterOperation.class)); //создание и настройка кнопок и добавление в сообщение
        sendPhoto.setChatId(chatId); //присвоение сообщению id чата, в котором боту было отправлено сообщение
        InputFile newFile = new InputFile();
        newFile.setMedia(new File(localPath));
        sendPhoto.setPhoto(newFile);
        return sendPhoto;
    }

    private List<org.telegram.telegrambots.meta.api.objects.File> getFilesByMessage(Message message) {
        this.extension = null;  //обнуление расширения файла
        List<PhotoSize> photoSizes = message.getPhoto();  // получение фотографий (первой из списка) из сообщения
        List<org.telegram.telegrambots.meta.api.objects.File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileId = photoSize.getFileId();
            try {
                org.telegram.telegrambots.meta.api.objects.File file = sendApiMethod(new GetFile(fileId));
                String path = file.getFilePath();
                if (extension == null) {
                    this.extension = path.substring(path.indexOf('.') + 1); //присвоение расширения файла
                }
                files.add(file);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    private ReplyKeyboardMarkup getKeyboard(Class<?> someClass) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(); //создаем объект клавиатуры
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();     // создаем список - ряды кнопок

        Method[] methods = someClass.getMethods();
        int columnCount = 3;   // зададим кол-во колонок в клавиатуре
        // находим число строк, округляя в большую сторону результат от деления (число методов / число колонок)
        int rowsCount =  (int) Math.ceil((double) methods.length / columnCount);
        int indexButton = 0;
        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            KeyboardRow keyboardButtons = new KeyboardRow();
            for (int columnIndex = 0; columnIndex < columnCount && indexButton < methods.length; columnIndex++) {
                //добавим кнопки в ряды кнопок клавиатуры
                Method method = methods[indexButton];
                KeyboardButton button = new KeyboardButton(method.getName());
                keyboardButtons.add(button);
                indexButton++;
            }
            keyboardRows.add(keyboardButtons);
        }
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true); // клавиатура будет показываться только для одного сообщения
        return replyKeyboardMarkup;
    }
}
