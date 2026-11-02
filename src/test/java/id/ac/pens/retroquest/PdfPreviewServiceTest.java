package id.ac.pens.retroquest;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.pens.retroquest.service.PdfPreviewService;
import java.io.IOException;
import java.nio.file.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfPreviewServiceTest {
  @TempDir Path temp;

  private Path document() throws Exception {
    Path path = temp.resolve("Tugas OOP #1 + revisi.pdf");
    try (var pdf = new PDDocument()) {
      pdf.addPage(new PDPage());
      pdf.addPage(new PDPage());
      pdf.save(path.toFile());
    }
    return path;
  }

  @Test
  void rendersOriginalWithSpacesAndSpecialCharacters() throws Exception {
    Path path = document();
    byte[] before = Files.readAllBytes(path);
    var service = new PdfPreviewService();
    var first = service.render(path, 0);
    assertEquals(2, first.count());
    assertEquals(0, first.index());
    assertTrue(first.image().getWidth() > 0);
    assertTrue(first.image().getHeight() > 0);
    assertEquals(1, service.render(path, 1).index());
    assertArrayEquals(
        before, Files.readAllBytes(path), "Melihat PDF tidak boleh mengubah berkas asli");
  }

  @Test
  void missingFileHasReadableError() {
    var error =
        assertThrows(
            IOException.class,
            () -> new PdfPreviewService().render(temp.resolve("missing.pdf"), 0));
    assertTrue(error.getMessage().contains("tidak ditemukan"));
  }

  @Test
  void invalidPageRejected() throws Exception {
    Path path = document();
    assertThrows(IOException.class, () -> new PdfPreviewService().render(path, 2));
    assertThrows(IOException.class, () -> new PdfPreviewService().render(path, -1));
  }

  @Test
  void corruptedPdfRejected() throws Exception {
    Path path = temp.resolve("broken.pdf");
    Files.writeString(path, "This is not a PDF");
    assertThrows(IOException.class, () -> new PdfPreviewService().render(path, 0));
  }

  @Test
  void extremePageSizeIsBounded() throws Exception {
    Path path = temp.resolve("large-page.pdf");
    try (var pdf = new PDDocument()) {
      pdf.addPage(new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(10000, 10000)));
      pdf.save(path.toFile());
    }
    var image = new PdfPreviewService().render(path, 0).image();
    assertTrue(image.getWidth() <= 2401);
    assertTrue(image.getHeight() <= 2401);
  }
}
