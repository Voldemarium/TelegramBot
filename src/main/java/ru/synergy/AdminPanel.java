package ru.synergy;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JFrame {
    TelegramBotsApi api;
    Bot bot;
    BotSession botSession;
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

    public AdminPanel(TelegramBotsApi api, Bot bot, BotSession botSession) {
        super("telegramBot");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.api = api;
        this.bot = bot;
        this.setSize(300, 300);
        this.setLocation((int) (dimension.getWidth() / 2 - 150), (int) (dimension.getHeight() / 2 - 150));
        this.botSession = botSession;

        // контейнер для объектов
        Container container = super.getContentPane();
        container.setLayout(new GridLayout(2, 2, 10, 10)); //как располагать кнопки друг отдруга

        //текстовые окошки
        JLabel labelUsers = new JLabel("list of users:", SwingConstants.CENTER); //надпись
        container.add(labelUsers);
        JTextArea textArea = new JTextArea(5, 20); // текстовая область 5 строк, длина 20 символов
        textArea.setLineWrap(true);                             // с переходом на новую строку
        container.add(textArea);

        //кнопки
        JButton jButtonStart = new JButton("start a bot");
        container.add(jButtonStart);
        JButton jButtonStop = new JButton("stop a bot");
        container.add(jButtonStop);

        // Добавление слушателя в класс Bot для извлечения списка пользователей в текстовое окошко панели
        bot.addEventListener(event -> {
            StringBuilder builder = new StringBuilder();
            for (User user : bot.users) {
                builder.append(user.getId()).append(", ").append(user.getFirstName()).append("; \n");
            }
            textArea.setText(builder.toString());
        });

        // Подключение слушателей событий для кнопок
        jButtonStart.addActionListener(event -> {
            if (!botSession.isRunning()) {
                botSession.start(); //регистрируем сессию с ботом, после этого сессия с ботом
                               // запускается, можно писать ему сообщения, которые он будет обрабатывать
            }
            System.out.println("bot session started");
        });

        jButtonStop.addActionListener(event -> {
            if (botSession != null) {
                if (botSession.isRunning()) {
                    botSession.stop();  //останавливаем сессию с ботом,
                    System.out.println("bot session stopped");
                }
            } else {
                System.out.println("bot session is not going");
            }
        });
    }
}