package id.ac.pens.retroquest.service;

import java.nio.file.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public final class DocxExtractor implements DocumentExtractor {
  @Override
  public String extract(Path path) throws Exception {
    try (var in = Files.newInputStream(path);
        var doc = new XWPFDocument(in);
        var extractor = new XWPFWordExtractor(doc)) {
      return extractor.getText();
    }
  }
}
