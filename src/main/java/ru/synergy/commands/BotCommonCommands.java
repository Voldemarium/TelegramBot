package ru.synergy.commands;

import ru.synergy.functions.FilterOperation;

import java.lang.reflect.Method;

public class BotCommonCommands {
    @AppBotCommand(name = "/hello", description = "when request hello", showInHelp = true)
    String hello() {
        return "Hello, User!";
    }

    @AppBotCommand(name = "/bye", description = "when request bye", showInHelp = true)
    String bye() {
        return "Good bye, User!";
    }

    @AppBotCommand(name = "/help", description = "when request help", showInHelp = true, showInKeyboard = true)
    String help() {
        return " available commands: \n " +
                getCommandsForHelp(BotCommonCommands.class, ' ', ' ') +
                " keyboard buttons: \n " +
                getCommandsForHelp(FilterOperation.class, '<', '>');
    }

    private String getCommandsForHelp(Class<?> clazz, char ch1, char ch2) {
        StringBuilder builder = new StringBuilder();
        Method[] methods = clazz.getDeclaredMethods();
        int maxLengthCommand = getMaxLengthCommand(methods); //максимальная длина команды
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {  // из методов, помеченных аннотациями @AppBotCommand
                AppBotCommand command = method.getAnnotation(AppBotCommand.class); // достаем их реализацию аннотации
                if (command.showInHelp()) {
                    builder.append(ch1).append(command.name()).append(ch2);
                    builder.append(" ".repeat(maxLengthCommand - command.name().length()));
                    builder.append(" - ")
                            .append(command.description())
                            .append("\n");
                }
            }
        }
        return builder.toString();
    }

    private int getMaxLengthCommand(Method[] methods) {
        int maxLengthCommand = 0;
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {  // из методов, помеченных аннотациями @AppBotCommand
                AppBotCommand command = method.getAnnotation(AppBotCommand.class); // достаем их реализацию аннотации
                if (command.showInHelp()) {
                    if (command.name().length() > maxLengthCommand) {
                        maxLengthCommand = command.name().length();
                    }
                }
            }
        }
        return maxLengthCommand;
    }
}