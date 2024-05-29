package com.neo.crypto_bot.service;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;

@Component
public class ImageFactory {

    public InputFile createImageWithText(String text, int fontSize, int backgroundNumber) throws IOException {

        String backgroundResource = String.format("/Images/background%d.jpg", backgroundNumber + 1);

        try (InputStream resourceStream = getClass().getResourceAsStream(backgroundResource);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            File tempFile = File.createTempFile("tempImage", ".jpg");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = resourceStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

//            System.out.println("Creating buffered image from stream");
//            BufferedImage bufferedImage = ImageIO.read(resourceStream);

            System.out.println("Creating Image plus from buffered image");
            Opener opener = new Opener();
            ImagePlus image = opener.openImage(tempFile.getAbsolutePath());

            //ImagePlus image = new ImagePlus("Background", new ColorProcessor(bufferedImage));

            System.out.println("Getting image processor");
            ImageProcessor ip = image.getProcessor();
//            System.out.println("Creating Graphics2D");
//            Graphics2D g2d = bufferedImage.createGraphics();

            System.out.println("Creating Font");
            Font font = new Font(Font.MONOSPACED, Font.BOLD, fontSize);

            System.out.println("Setting font");
            ip.setFont(font);
//            g2d.setFont(font);
            System.out.println("Setting font color");
            ip.setColor(Color.WHITE);
//            g2d.setColor(Color.WHITE);
            System.out.println("Get font metrics");
//            FontMetrics fontMetrics = g2d.getFontMetrics();
            FontMetrics fontMetrics = ip.getFontMetrics();
            int charWidth = fontMetrics.charWidth('A');

            String[] lines = text.split("\n");
            int x = 20;
            int y = fontMetrics.getHeight();
            for (String line : lines) {
                if (line.contains("(")) {
                    String[] subLines = line.split("\\(");
                    ip.drawString(subLines[0], x, y += ip.getFontMetrics().getHeight());
//                    g2d.drawString(subLines[0], x, y += fontMetrics.getHeight());
                    if (subLines[1].contains("-")) ip.setColor(Color.RED);
//                    if (subLines[1].contains("-")) g2d.setColor(Color.RED);
                    else {
                        ip.setColor(Color.GREEN);
//                        g2d.setColor(Color.GREEN);
                    }
                    ip.drawString("(" + subLines[1], x + charWidth * subLines[0].length(), y);
                    ip.setColor(Color.WHITE);
//                    g2d.drawString("(" + subLines[1], x + charWidth * subLines[0].length(), y);
//                    g2d.setColor(Color.WHITE);
                } else ip.drawString(line, 30, y += ip.getFontMetrics().getHeight() + 20);
//                g2d.drawString(line, 30, y += fontMetrics.getHeight() + 20);
            }
            ImageIO.write(ip.getBufferedImage(), "jpg", outputStream);
            System.out.println("Writing image to output stream");
            return new InputFile(new ByteArrayInputStream(outputStream.toByteArray()), "prices.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
