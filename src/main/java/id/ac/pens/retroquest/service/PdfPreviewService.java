package id.ac.pens.retroquest.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;

/** Reads original PDF bytes without launching a browser or modifying the document. */
public final class PdfPreviewService {
  public record Page(int index, int count, BufferedImage image) {}

  public Page render(Path path, int index) throws IOException {
    Path file = path.toAbsolutePath().normalize();
    if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
      throw new IOException("Berkas PDF tidak ditemukan atau tidak dapat dibaca: " + file);
    }
    try (var document = Loader.loadPDF(file.toFile())) {
      int count = document.getNumberOfPages();
      if (index < 0 || index >= count) {
        throw new IOException("Halaman PDF tidak tersedia: " + (index + 1));
      }
      var bounds = document.getPage(index).getCropBox();
      double longest = Math.max(bounds.getWidth(), bounds.getHeight());
      float dpi = (float) Math.min(144, 2400 * 72 / Math.max(1, longest));
      var renderer = new PDFRenderer(document);
      renderer.setSubsamplingAllowed(true);
      return new Page(index, count, renderer.renderImageWithDPI(index, dpi));
    }
  }
}
