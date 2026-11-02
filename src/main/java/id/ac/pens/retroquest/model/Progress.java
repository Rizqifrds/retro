package id.ac.pens.retroquest.model;

public record Progress(int exp) {
  public Progress {
    if (exp < 0) throw new IllegalArgumentException("EXP tidak boleh negatif");
  }

  public int level() {
    return 1 + exp / 100;
  }

  public int tier() {
    int l = level();
    return l <= 3 ? 1 : l <= 7 ? 2 : l <= 10 ? 3 : 4;
  }

  public String title() {
    return switch (tier()) {
      case 1 -> "Novice Apprentice";
      case 2 -> "Adept Adventurer";
      case 3 -> "Elite Knight / Sorcerer";
      default -> "Legendary Grandmaster";
    };
  }
}
