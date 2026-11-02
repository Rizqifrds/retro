package id.ac.pens.retroquest.service;

import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfExtractor implements DocumentExtractor {
  @Override
  public String extract(Path path) throws Exception {
    try (var doc = Loader.loadPDF(path.toFile())) {
      return new PDFTextStripper().getText(doc);
    }
  }
}
