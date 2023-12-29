package ru.synergy.utils;

import ru.synergy.commands.AppBotCommand;
import ru.synergy.functions.FilterOperation;
import ru.synergy.functions.ImageOperation;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.lang.reflect.Method;

public class ImageUtils {
    private final String formatName;

    public ImageUtils(String formatName) {
        this.formatName = formatName;
    }

    static public BufferedImage getImage(String path) throws IOException {
        return ImageIO.read(new File(path));
    }

    public void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, this.formatName, new File(path));
    }

    static public float[] rgbIntToArray(int pixel) {
        Color color = new Color(pixel);
        return color.getRGBColorComponents(null);
    }

    static public int rgbArrayToInt(float[] array) throws Exception {
        Color color = null;
        if (array.length == 3) {
            color = new Color(array[0], array[1], array[2]);
        } else if (array.length == 4) {
            color = new Color(array[0], array[1], array[2], array[3]);
        }
        if (color != null) {
            return color.getRGB();
        }
        throw new Exception("Invalid color");
    }

    public static ImageOperation getOperation(String operationName) {
        Method[] methods = FilterOperation.class.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {  // из методов, помеченных аннотациями @AppBotCommand
                AppBotCommand command = method.getAnnotation(AppBotCommand.class); // достаем их реализацию аннотации
                if (command.name().equals(operationName))
                    // из метода получаем лямбду ImageOperation
                    return f -> (float[]) method.invoke(new FilterOperation(), f);
            }
        }
        return null;
    }
}