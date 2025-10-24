package com.rossPalletScanApp.service;

import net.sourceforge.tess4j.*;
import net.sourceforge.tess4j.util.ImageHelper;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

import com.rossPalletScanApp.dto.ImagePreprocessor;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

import javax.imageio.ImageIO;

@Service
public class OcrService {

	private final Tesseract tesseract;
	private final int minConfidence;

	@Autowired
	public OcrService(Tesseract tesseract, @Value("${app.ocr.min-confidence:60}") int minConfidence) {
		this.tesseract = tesseract;
		this.minConfidence = minConfidence;
	}

	public OcrResult extract(InputStream imageStream) throws TesseractException, IOException {
		File preprocessed = ImagePreprocessor.preprocessToTempFile(imageStream);
		try {
			BufferedImage image = ImageIO.read(preprocessed);
			List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

			int avgConf = 0;
			if (words != null && !words.isEmpty()) {
				double sum = 0.0;
				for (Word w : words)
					sum += w.getConfidence();
				avgConf = (int) Math.round(sum / words.size());
			}

			String text = tesseract.doOCR(preprocessed);
			return new OcrResult(text, avgConf);
		} finally {
			preprocessed.delete();
		}
	}

	public int minConfidence() {
		return minConfidence;
	}

	// Java 8 POJO (not record)
	public static class OcrResult {
		private final String text;
		private final int confidence;

		public OcrResult(String text, int confidence) {
			this.text = text;
			this.confidence = confidence;
		}

		public String getText() {
			return text;
		}

		public int getConfidence() {
			return confidence;
		}
	}
}
