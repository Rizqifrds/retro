package id.ac.pens.retroquest.ui;

import id.ac.pens.retroquest.model.Progress;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

public final class AvatarFactory {
  private AvatarFactory() {}

  public static Canvas create(String archetype, int exp, int scale) {
    Canvas c = new Canvas(18 * scale, 20 * scale);
    var g = c.getGraphicsContext2D();
    int tier = new Progress(exp).tier();
    Color accent =
        Color.web(
            archetype.equals("Byte Mage")
                ? "#b09aff"
                : archetype.equals("Logic Rogue") ? "#ffb86b" : "#79e6c4");
    String[] sprite = {
      "000000077770000000",
      "000000777777000000",
      "000007777777700000",
      "000007222222700000",
      "000002242242000000",
      "000002222222000000",
      "000000222220000000",
      "000003333333300000",
      "000033313133330000",
      "000233313133320000",
      "000203333333020000",
      "000003333333000000",
      "000003331333000000",
      "000003330333000000",
      "000003330333000000",
      "000006660666000000",
      "000006660666000000"
    };
    g.setFill(Color.web("#101d2e"));
    g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 12, 12);
    for (int y = 0; y < sprite.length; y++)
      for (int x = 0; x < sprite[y].length(); x++) {
        char p = sprite[y].charAt(x);
        if (p == '0') continue;
        g.setFill(
            switch (p) {
              case '1' -> Color.web("#e4b65c");
              case '2' -> Color.web("#e9b08f");
              case '3' -> accent;
              case '4' -> Color.web("#111b30");
              case '6' -> Color.web("#52637d");
              default -> tier >= 3 ? Color.web("#ffd071") : accent.darker();
            });
        g.fillRect(x * scale, (y + 2) * scale, scale, scale);
      }
    if (archetype.equals("Byte Mage")) {
      g.setFill(accent);
      g.fillRect(13 * scale, 5 * scale, scale, 11 * scale);
      g.setFill(Color.web("#ffd071"));
      g.fillRect(12 * scale, 4 * scale, 3 * scale, 3 * scale);
    }
    if (archetype.equals("Code Warrior")) {
      g.setFill(Color.web("#dce8ef"));
      g.fillRect(14 * scale, 7 * scale, scale, 6 * scale);
      g.setFill(Color.web("#ffd071"));
      g.fillRect(13 * scale, 12 * scale, 3 * scale, scale);
    }
    if (archetype.equals("Logic Rogue")) {
      g.setFill(Color.web("#c9deed"));
      g.fillRect(14 * scale, 10 * scale, 2 * scale, scale);
    }
    if (tier >= 2) {
      g.setFill(Color.web("#ffd071"));
      g.fillRect(7 * scale, scale, 4 * scale, scale);
    }
    if (tier >= 4) {
      g.setStroke(Color.web("#ffd071"));
      g.setLineWidth(2);
      g.strokeRect(2, 2, c.getWidth() - 4, c.getHeight() - 4);
    }
    return c;
  }
}
