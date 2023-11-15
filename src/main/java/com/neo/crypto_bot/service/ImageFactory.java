package com.neo.crypto_bot.service;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class ImageFactory {

    private final List<String> backgrounds = List.of("src/main/resources/Images/background1.jpg",
                                                     "src/main/resources/Images/background2.jpg");

    public InputFile createImageWithText(String text, int fontSize, int backgroundNumber) throws IOException {
        ImagePlus image = IJ.openImage(backgrounds.get(backgroundNumber));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageProcessor ip = image.getProcessor();

        Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);
        ip.setFont(font);
        ip.setColor(Color.WHITE);
        FontMetrics fontMetrics = ip.getFontMetrics();
        int charWidth = fontMetrics.charWidth('A');

        String[] lines = text.split("\n");
        int x = 20;
        int y = ip.getFontMetrics().getHeight();
        for (String line : lines) {
            if (line.contains("(")) {
                String[] subLines = line.split("\\(");
                ip.drawString(subLines[0], x, y += ip.getFontMetrics().getHeight());
                if (subLines[1].contains("-")) ip.setColor(Color.RED);
                else {
                    ip.setColor(Color.GREEN);
                }
                ip.drawString("(" + subLines[1], x + charWidth * subLines[0].length(), y);
                ip.setColor(Color.WHITE);
            } else ip.drawString(line, 30, y += ip.getFontMetrics().getHeight() + 20);
        }
        try {
            ImageIO.write(ip.getBufferedImage(), "jpg", outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new InputFile(new ByteArrayInputStream(outputStream.toByteArray()), "prices.jpg");
    }
}
