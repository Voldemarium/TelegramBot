package ru.synergy;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.synergy.utils.PhotoMessageUtils;

public class Main {
    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        Bot bot = new Bot("5586574249:AAEUyz6yeb6clUdKphmaUKwie9IGmxxMAZo");

        //чистим папку images
        PhotoMessageUtils.cleaningDirectory();

        BotSession botSession = api.registerBot(bot); //регистрируем сессию с ботом, после этого сессия с ботом
        // запускается, можно писать ему сообщения, которые эта программа будет обрабатывать

        AdminPanel adminPanel = new AdminPanel(api, bot, botSession);
        adminPanel.setVisible(true);



    }

}