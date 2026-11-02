package id.ac.pens.retroquest.model;

public abstract sealed class User permits Lecturer, Student {
  private final long id;
  private final String name, email;

  protected User(long id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }

  public final long id() {
    return id;
  }

  public final String name() {
    return name;
  }

  public final String email() {
    return email;
  }

  public abstract String role();

  public final boolean isLecturer() {
    return this instanceof Lecturer;
  }
}
