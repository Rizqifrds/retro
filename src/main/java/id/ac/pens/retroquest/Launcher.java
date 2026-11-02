package id.ac.pens.retroquest;

public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    if (System.getProperty("prism.order") == null) System.setProperty("prism.order", "sw");
    App.main(args);
  }
}
