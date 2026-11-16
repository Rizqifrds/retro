package id.ac.pens.retroquest.model;

import java.time.LocalDateTime;

public enum QuestPriority {
  URGENT("Mendesak"),
  HIGH("Tinggi"),
  NORMAL("Normal"),
  OVERDUE("Lewat tenggat");

  private final String label;

  QuestPriority(String label) {
    this.label = label;
  }

  public static QuestPriority fromDeadline(LocalDateTime deadline, LocalDateTime now) {
    if (!deadline.isAfter(now)) return OVERDUE;
    if (!deadline.isAfter(now.plusHours(48))) return URGENT;
    if (!deadline.isAfter(now.plusDays(7))) return HIGH;
    return NORMAL;
  }

  @Override
  public String toString() {
    return label;
  }
}
