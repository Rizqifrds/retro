package id.ac.pens.retroquest.model;

public final class Lecturer extends User {
  public Lecturer(long id, String name, String email) {
    super(id, name, email);
  }

  @Override
  public String role() {
    return "LECTURER";
  }
}
