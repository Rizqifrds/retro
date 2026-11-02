package id.ac.pens.retroquest.service;

import id.ac.pens.retroquest.config.AppConfig;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class OcrService implements DocumentExtractor {
  @Override
  public String extract(Path file) throws Exception {
    if (!FilesService.extension(file).equals("pdf")) return image(file);
    StringBuilder text = new StringBuilder();
    try (var doc = Loader.loadPDF(file.toFile())) {
      if (doc.getNumberOfPages() > 40)
        throw new IllegalArgumentException(
            "PDF OCR dibatasi 40 halaman. Bagi berkas terlebih dahulu.");
      var renderer = new PDFRenderer(doc);
      for (int i = 0; i < doc.getNumberOfPages(); i++) {
        Path png = Files.createTempFile("retroquest-ocr-", ".png");
        try {
          ImageIO.write(renderer.renderImageWithDPI(i, 150), "png", png.toFile());
          text.append(image(png)).append('\n');
        } finally {
          Files.deleteIfExists(png);
        }
      }
    }
    return text.toString();
  }

  private String image(Path path) throws Exception {
    var config = AppConfig.get();
    Path out = Files.createTempFile("retroquest-tesseract-", ".txt");
    Path err = Files.createTempFile("retroquest-tesseract-", ".log");
    try {
      Process p =
          new ProcessBuilder(
                  config.value("ocr.command", "tesseract"),
                  path.toAbsolutePath().toString(),
                  "stdout",
                  "-l",
                  config.value("ocr.language", "eng"))
              .redirectOutput(out.toFile())
              .redirectError(err.toFile())
              .start();
      try {
        if (!p.waitFor(60, TimeUnit.SECONDS)) {
          p.destroyForcibly();
          throw new IllegalStateException("OCR melewati batas waktu.");
        }
      } catch (InterruptedException e) {
        p.destroyForcibly();
        Thread.currentThread().interrupt();
        throw e;
      }
      if (p.exitValue() != 0)
        throw new IllegalStateException("OCR gagal. Periksa Tesseract dan bahasa yang terpasang.");
      return Files.readString(out);
    } finally {
      Files.deleteIfExists(out);
      Files.deleteIfExists(err);
    }
  }
}
