package ru.synergy;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class Bot extends TelegramLongPollingBot {
    public Bot(String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return "asdhgfVbot";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage(); // возвращает написанное боту сообщение
        System.out.println(message.getText()); // печатает написанное боту сообщение
    }
}
