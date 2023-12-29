package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.synergy.commands.AppBotCommand;
import ru.synergy.commands.BotCommonCommands;
import ru.synergy.functions.FilterOperation;
import ru.synergy.functions.ImageOperation;
import ru.synergy.utils.ImageUtils;
import ru.synergy.utils.PhotoMessageUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    final String botToken;

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

        // Для текстовой команды
        if (message.hasText()) {
            try {
                SendMessage responseTextMessage = runCommonCommand(message);
                if (responseTextMessage != null) {   // Если была корректная текстовая команда
                    execute(responseTextMessage);    // отправляем ответное сообщение
                    return;                          // завершаем цикл
                }
            } catch (InvocationTargetException | IllegalAccessException | TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Для присланных фото
        if (message.hasPhoto()) {
            try {
                SendMediaGroup responseMediaMessage = runPhotoFilter(message);
                if (responseMediaMessage != null) {
                    execute(responseMediaMessage);    // отправляем ответное сообщение
                    return;                           // завершаем цикл )
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private SendMessage runCommonCommand(Message message) throws InvocationTargetException, IllegalAccessException {
        String text = message.getText();
        BotCommonCommands botCommonCommands = new BotCommonCommands();
        Method[] methods = botCommonCommands.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {  // из методов, помеченных аннотациями @AppBotCommand
                AppBotCommand command = method.getAnnotation(AppBotCommand.class); // достаем их реализацию аннотации
                if (command.name().equals(text)) {
                    method.setAccessible(true);  // даем доступ к методу
                    String responseText = (String) method.invoke(botCommonCommands);
                    if (responseText != null) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(message.getChatId());
                        sendMessage.setText(responseText);
                        return sendMessage;
                    }
                }
            }
        }
        return null;
    }

    private SendMediaGroup runPhotoFilter(Message message) {
        ImageOperation operation = ImageUtils.getOperation(message.getCaption());
        if (operation == null) {
            return null;
        }
        //Сохранение присланных в сообщении фотографий, создание списка путей к сохраненным фото
        try {
            List<String> photoPaths = PhotoMessageUtils.savePhotos(getFilesByMessage(message), this.botToken);
            Long chatId = message.getChatId();
            // Обработка полученных фотографий
            return preparePhotoMessage(photoPaths, operation, chatId); // объект для отправки нескольких медиафайлов
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private SendMediaGroup preparePhotoMessage(List<String> localPaths, ImageOperation operation, Long chatId) throws Exception {
        SendMediaGroup mediaGroup = new SendMediaGroup();    // объект для отправки нескольких медиафайлов
        ArrayList<InputMedia> medias = new ArrayList<>();
        for (String path : localPaths) {
            InputMedia inputMedia = new InputMediaPhoto(); // объект для вложения в него фото
            PhotoMessageUtils.processingImage(path, operation);
            inputMedia.setMedia(new java.io.File(path), path); // вложение одного фото
            medias.add(inputMedia);
        }
        mediaGroup.setMedias(medias);
        mediaGroup.setChatId(chatId);
        return mediaGroup;
    }

    private List<File> getFilesByMessage(Message message) {
        List<PhotoSize> photoSizes = message.getPhoto();  // получение фотографий (первой из списка) из сообщения
        List<File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileId = photoSize.getFileId();
            try {
                File file = sendApiMethod(new GetFile(fileId));
                String path = file.getFilePath();
                files.add(file);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    private ReplyKeyboardMarkup getKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(); //создаем объект клавиатуры
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>();     // создаем список - ряды кнопок
        allKeyboardRows.addAll(getKeyboardRows(BotCommonCommands.class));
        allKeyboardRows.addAll(getKeyboardRows(FilterOperation.class));

        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true); // клавиатура будет показываться только для одного сообщения
        return replyKeyboardMarkup;
    }

    private ArrayList<KeyboardRow> getKeyboardRows(Class<?> someClass) {
        Method[] methods = someClass.getDeclaredMethods();
        ArrayList<AppBotCommand> commands = new ArrayList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {  // из методов, помеченных аннотациями @AppBotCommand
                AppBotCommand command = method.getAnnotation(AppBotCommand.class); // достаем их реализацию аннотации
                commands.add(command);                                             // и добавляем ее в список commands
            }
        }
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();     // создаем список - ряды кнопок
        int columnCount = 3;   // зададим кол-во колонок в клавиатуре
        // находим число строк, округляя в большую сторону результат от деления (число методов / число колонок)
        int rowsCount = (int) Math.ceil((double) commands.size() / columnCount);
        int indexButton = 0;
        for (int rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
            KeyboardRow keyboardButtons = new KeyboardRow();
            for (int columnIndex = 0; columnIndex < columnCount && indexButton < commands.size(); columnIndex++) {
                //добавим кнопки в ряды кнопок клавиатуры
                AppBotCommand command = commands.get(indexButton);
                KeyboardButton button = new KeyboardButton(command.name());
                keyboardButtons.add(button);
                indexButton++;
            }
            keyboardRows.add(keyboardButtons);
        }
        return keyboardRows;
    }
}
