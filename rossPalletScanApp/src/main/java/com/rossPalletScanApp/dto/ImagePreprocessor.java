package com.rossPalletScanApp.dto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ImagePreprocessor {

    /**
     * Preprocess for OCR adaptively for white or carton labels
     */
    public static File preprocessToTempFile(InputStream in) throws IOException {
        BufferedImage src = ImageIO.read(in);
        if (src == null) throw new IOException("Invalid image");

        // 1) To grayscale
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g0 = gray.createGraphics();
        g0.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g0.drawImage(src, 0, 0, null);
        g0.dispose();

        // 2) Downscale if too wide
        int maxW = 1400;
        BufferedImage work = gray;
        if (gray.getWidth() > maxW) {
            double scale = (double) maxW / gray.getWidth();
            int newW = (int) Math.round(gray.getWidth() * scale);
            int newH = (int) Math.round(gray.getHeight() * scale);
            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g1 = scaled.createGraphics();
            g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g1.drawImage(gray, 0, 0, newW, newH, null);
            g1.dispose();
            work = scaled;
        }

        // 3) Detect if carton label (low average brightness)
        boolean isCarton = isCartonLabel(work);

        // 4) Contrast/brightness boost
        float contrast = isCarton ? 1.5f : 1.2f; // stronger for carton labels
        RescaleOp rescale = new RescaleOp(contrast, 0f, null);
        BufferedImage boosted = rescale.filter(work, null);

        // 5) Optional sharpen (skip for carton labels)
        BufferedImage finalImg = boosted;
        if (!isCarton) {
            float[] kernel = {
                    0f, -0.05f, 0f,
                    -0.05f, 1.20f, -0.05f,
                    0f, -0.05f, 0f
            };
            ConvolveOp sharpen = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
            finalImg = sharpen.filter(boosted, null);
        }

        // 6) Write to temp PNG
        File tmp = Files.createTempFile("ocr_", ".png").toFile();
        ImageIO.write(finalImg, "png", tmp);
        return tmp;
    }

    /** Simple heuristic: check average brightness to detect carton label */
    private static boolean isCartonLabel(BufferedImage img) {
        long sum = 0;
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; y += 5) { // sample every 5 pixels for speed
            for (int x = 0; x < w; x += 5) {
                int rgb = img.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                sum += gray;
            }
        }
        double avg = sum / ((w / 5.0) * (h / 5.0));
        return avg < 160; // threshold: low brightness => carton label
    }

    /**
     * Optional: Binary preprocessing if needed
     */
    public static File preprocessToBinaryTempFile(InputStream in) throws IOException {
        BufferedImage src = ImageIO.read(in);
        if (src == null) throw new IOException("Invalid image");

        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g0 = gray.createGraphics();
        g0.drawImage(src, 0, 0, null);
        g0.dispose();

        int maxW = 1400;
        if (gray.getWidth() > maxW) {
            double scale = (double) maxW / gray.getWidth();
            int newW = (int) Math.round(gray.getWidth() * scale);
            int newH = (int) Math.round(gray.getHeight() * scale);
            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g1 = scaled.createGraphics();
            g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g1.drawImage(gray, 0, 0, newW, newH, null);
            g1.dispose();
            gray = scaled;
        }

        int thr = otsuThreshold(gray);
        BufferedImage bin = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int rgb = gray.getRGB(x, y);
                int val = ((rgb >> 16) & 0xFF) >= thr ? 0xFFFFFF : 0x000000;
                bin.setRGB(x, y, (0xFF << 24) | val);
            }
        }

        File tmp = Files.createTempFile("ocr_bin_", ".png").toFile();
        ImageIO.write(bin, "png", tmp);
        return tmp;
    }

    // Otsu threshold
    private static int otsuThreshold(BufferedImage img) {
        int[] hist = new int[256];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int v = (img.getRGB(x, y) >> 16) & 0xFF;
                hist[v]++;
            }
        }
        int total = img.getWidth() * img.getHeight();
        float sum = 0;
        for (int t = 0; t < 256; t++) sum += t * hist[t];

        float sumB = 0;
        int wB = 0;
        int wF = 0;
        float maxVar = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            wF = total - wB;
            if (wF == 0) break;
            sumB += t * hist[t];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
            float between = (float) wB * (float) wF * (mB - mF) * (mB - mF);
            if (between > maxVar) {
                maxVar = between;
                threshold = t;
            }
        }
        return threshold;
    }
}
