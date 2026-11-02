package id.ac.pens.retroquest.service;

import java.nio.file.*;
import java.util.Set;

public final class DocumentService {
  public static final Set<String> TYPES =
      Set.of("pdf", "docx", "java", "txt", "png", "jpg", "jpeg");

  public String extract(Path path) throws Exception {
    FilesService.validate(path, TYPES);
    DocumentExtractor strategy =
        switch (FilesService.extension(path)) {
          case "pdf" -> new PdfExtractor();
          case "docx" -> new DocxExtractor();
          case "png", "jpg", "jpeg" -> new OcrService();
          default -> Files::readString;
        };
    String text = strategy.extract(path);
    if (text.isBlank() && FilesService.extension(path).equals("pdf"))
      text = new OcrService().extract(path);
    if (text.isBlank())
      throw new IllegalArgumentException("Teks tidak ditemukan. Lanjutkan pemeriksaan manual.");
    return text;
  }
}
