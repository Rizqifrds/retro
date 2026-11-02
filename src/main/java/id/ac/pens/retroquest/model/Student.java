package id.ac.pens.retroquest.model;

public final class Student extends User {
  public Student(long id, String name, String email) {
    super(id, name, email);
  }

  @Override
  public String role() {
    return "STUDENT";
  }
}
