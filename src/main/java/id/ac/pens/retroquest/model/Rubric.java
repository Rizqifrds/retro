package id.ac.pens.retroquest.model;

import java.util.List;

public record Rubric(List<Criterion> criteria) {
  public record Criterion(String name, int weight, String guide) {}

  public Rubric {
    if (criteria == null
        || criteria.isEmpty()
        || criteria.stream()
            .anyMatch(
                c ->
                    c.name() == null
                        || c.name().isBlank()
                        || c.guide() == null
                        || c.guide().isBlank()
                        || c.weight() <= 0)
        || criteria.stream().mapToInt(Criterion::weight).sum() != 100)
      throw new IllegalArgumentException(
          "Rubrik harus lengkap, bobot positif, dan total tepat 100%.");
    criteria = List.copyOf(criteria);
  }

  public static Rubric standard() {
    return new Rubric(
        List.of(
            new Criterion(
                "Ketepatan solusi", 40, "Kesesuaian solusi dengan seluruh instruksi tugas"),
            new Criterion(
                "Penerapan OOP", 35, "Enkapsulasi, abstraksi, dan pemisahan tanggung jawab"),
            new Criterion(
                "Dokumentasi dan kerapian", 25, "Penjelasan, keterbacaan, dan bukti hasil")));
  }
}
