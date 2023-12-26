package ru.synergy.utils;

import ru.synergy.functions.ImageOperation;

import java.awt.image.BufferedImage;

public class RgbMaster {
    private final BufferedImage image;
    private final int width;
    private final int height;
    private final boolean hasAlphaChannel;
    private final int[] pixels;

    public RgbMaster(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.hasAlphaChannel = image.getAlphaRaster() != null;
        this.pixels = image.getRGB(0, 0, this.width, this.height, null, 0, this.width);
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public BufferedImage changeImage(ImageOperation operation) throws Exception {
        int[] newPixels = this.pixels.clone();
        BufferedImage newImage = getImage();
        for (int i = 0; i < newPixels.length; i++) {
            float[] pixel = ImageUtils.rgbIntToArray(newPixels[i]);
            float[] newPixel = operation.execute(pixel);
            newPixels[i] = ImageUtils.rgbArrayToInt(newPixel);
        }
        newImage.setRGB(0, 0, this.width, this.height, newPixels, 0, this.width);
        return newImage;
    }
}