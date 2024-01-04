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
import java.util.*;

public class Bot extends TelegramLongPollingBot {
    final String botToken;
    Set<User> users = new LinkedHashSet<>();                //список пользователей
    HashMap<Long, Message> messages = new HashMap<>();       // для хранения предыдущих сообщений
    List <BotEventListener> eventListeners = new ArrayList<>(); //  список слушателей

    public Bot(String botToken) {
        super(botToken);
        this.botToken = botToken;
    }

    public void addEventListener(BotEventListener listener) {
        this.eventListeners.add(listener);
    }

    public void notifyEventListeners(BotEvent event) {
        for(BotEventListener listener : this.eventListeners) {
            listener.processEvent(event);
        }
    }

    @Override
    public String getBotUsername() {
        return "asdhgfVbot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        //1. Прием сообщения
        Message message = update.getMessage();              // возвращает написанное боту сообщение
        this.users.add(message.getFrom());                       //добавляем в список пользователей
        this.notifyEventListeners(new BotEvent(this));  //передаем событие слушателю

        // Для текстовой команды
        if (message.hasText()) {
            try {
                SendMessage responseTextMessage = runCommonCommand(message); // для общих команд
                if (responseTextMessage != null) {
                    execute(responseTextMessage);    // отправляем ответное сообщение
                    return;                          // завершаем цикл
                }
                if (!messages.isEmpty()) {           // если есть сохраненные фото из предыдущего сообщения
                    SendMediaGroup responseMediaMessage = runPhotoFilter(message);
                    if (responseMediaMessage != null) {
                        execute(responseMediaMessage);  //отправляем ответное сообщение с фото с наложенным фильтром
                        return;                         //завершаем цикл
                    }
                } else {
                    execute(getSendTextMessage(message.getChatId(), "send photo to use filter"));
                    return;                                   // завершаем цикл
                }
            } catch (InvocationTargetException | IllegalAccessException | TelegramApiException e) {
                e.printStackTrace();
            }
        }

        // Для присланных фото
        if (message.hasPhoto()) {
            try {
                SendMessage responseTextMessage = runPhotoMessage(message);
                if (responseTextMessage != null) {
                    execute(responseTextMessage);    // отправляем ответное текстовое сообщение
                    return;                          // завершаем цикл
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
                        return getSendTextMessage(message.getChatId(), responseText);
                    }
                }
            }
        }
        return null;
    }

    private SendMessage runPhotoMessage(Message message) {
        List<File> files = getFilesByMessage(message);
        if (files.isEmpty()) {
            return null;
        }
        this.messages.put(message.getChatId(), message);   //Сохраняем сообщение
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(); //создаем объект клавиатуры
        // создаем список - ряды кнопок
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>(getKeyboardRows(FilterOperation.class));
        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true); // клавиатура будет показываться только для одного сообщения
        SendMessage sendMessage = getSendTextMessage(message.getChatId(), "change filter");
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        return sendMessage;
    }

    private SendMediaGroup runPhotoFilter(Message newMessage) {  // принимает новое (следующее после фото сообщение)
        ImageOperation operation = ImageUtils.getOperation(newMessage.getText());
        if (operation == null) {
            return null;
        }
        Long chatId = newMessage.getChatId();
        Message photoMessage = messages.get(chatId); //извлекаем предыдущее сообщение с фото
        if (photoMessage != null) {
            //Сохранение присланных в сообщении фотографий, создание списка путей к сохраненным фото
            try {
                List<String> photoPaths = PhotoMessageUtils.savePhotos(getFilesByMessage(photoMessage), this.botToken);
                // Обработка полученных фотографий
                return preparePhotoMessage(photoPaths, operation, chatId); // объект для отправки нескольких медиафайлов
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private SendMessage getSendTextMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
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
        List<PhotoSize> photoSizes = message.getPhoto();  // получение фотографий из сообщения
        if (photoSizes == null) {
            return new ArrayList<>();
        }
        List<File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileId = photoSize.getFileId();
            try {
                File file = sendApiMethod(new GetFile(fileId));
//                String path = file.getFilePath();
                files.add(file);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return files;
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
