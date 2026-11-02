package id.ac.pens.retroquest;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.pens.retroquest.service.*;
import java.nio.file.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class DocumentTest {
  @TempDir Path temp;

  @Test
  void extractPdf() throws Exception {
    Path p = temp.resolve("module.pdf");
    try (var doc = new PDDocument()) {
      var page = new PDPage();
      doc.addPage(page);
      try (var content = new PDPageContentStream(doc, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        content.newLineAtOffset(50, 700);
        content.showText("Encapsulation with private attributes");
        content.endText();
      }
      doc.save(p.toFile());
    }
    assertTrue(new DocumentService().extract(p).contains("Encapsulation"));
  }

  @Test
  void extractDocx() throws Exception {
    Path p = temp.resolve("module.docx");
    try (var doc = new XWPFDocument();
        var out = Files.newOutputStream(p)) {
      doc.createParagraph().createRun().setText("Polimorfisme Java");
      doc.write(out);
    }
    assertTrue(new DocumentService().extract(p).contains("Polimorfisme"));
  }

  @Test
  void sourceCodeReadStatically() throws Exception {
    Path p = temp.resolve("Unsafe.java");
    Files.writeString(p, "class Unsafe { static { System.exit(1); } }");
    assertTrue(new DocumentService().extract(p).contains("System.exit"));
  }

  @Test
  void aiResponseValidation() {
    assertThrows(IllegalArgumentException.class, () -> new AiService.Evaluation(101, "Bad"));
    assertThrows(IllegalArgumentException.class, () -> new AiService.Evaluation(80, ""));
  }
}
