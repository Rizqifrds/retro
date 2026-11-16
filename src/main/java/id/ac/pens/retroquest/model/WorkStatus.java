package id.ac.pens.retroquest.model;

public enum WorkStatus {
  NOT_STARTED("Belum dimulai"),
  IN_PROGRESS("Sedang dikerjakan"),
  COMPLETED("Selesai");

  private final String label;

  WorkStatus(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
