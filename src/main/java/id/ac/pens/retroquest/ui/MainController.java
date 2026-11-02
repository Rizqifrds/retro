package id.ac.pens.retroquest.ui;

import static id.ac.pens.retroquest.service.AiService.JSON;

import id.ac.pens.retroquest.dao.*;
import id.ac.pens.retroquest.dao.JdbcDao.Row;
import id.ac.pens.retroquest.model.*;
import id.ac.pens.retroquest.service.*;
import java.awt.Desktop;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

public final class MainController {
  @FXML private BorderPane root;
  private Stage stage;
  private Database db;
  private AuthService auth;
  private QuestService service;
  private User user;
  private Row room;
  private VBox content;
  private Label status = new Label("Siap");
  private ComboBox<Row> rooms;
  private String page = "Dashboard";
  private Timeline refreshTimer;
  private boolean busy;
  private int requestId;
  private final Map<String, Button> navigation = new LinkedHashMap<>();
  private final Map<String, Integer> seenExp = new HashMap<>();
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.forLanguageTag("id-ID"));

  @FunctionalInterface
  private interface Job<T> {
    T run() throws Exception;
  }

  public void start(Stage stage, Database db) {
    this.stage = stage;
    this.db = db;
    auth = new AuthService(db);
    service = new QuestService(db);
    service.observe(
        () ->
            Platform.runLater(
                () -> status.setText("Perubahan tersimpan · progres kelas diperbarui")));
    root.setCenter(label("Menyiapkan guild…", "heading"));
    async(
        "Menyiapkan database",
        () -> {
          db.initialize();
          return true;
        },
        ok -> login());
    stage.setOnCloseRequest(
        e -> {
          if (refreshTimer != null) refreshTimer.stop();
        });
  }

  private <T> void async(String message, Job<T> job, Consumer<T> success) {
    if (busy) return;
    busy = true;
    root.setDisable(true);
    status.setText(message + "…");
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return job.run();
          }
        };
    task.setOnSucceeded(
        e -> {
          busy = false;
          root.setDisable(false);
          status.setText("Siap · " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
          try {
            success.accept(task.getValue());
          } catch (Exception ex) {
            error(ex);
          }
        });
    task.setOnFailed(
        e -> {
          busy = false;
          root.setDisable(false);
          status.setText("Operasi belum berhasil");
          error(task.getException());
        });
    Thread thread = new Thread(task, "retroquest-worker");
    thread.setDaemon(true);
    thread.start();
  }

  private void safe(Runnable action) {
    try {
      action.run();
    } catch (Exception e) {
      error(e);
    }
  }

  private void error(Throwable e) {
    var a = new Alert(Alert.AlertType.ERROR);
    a.initOwner(stage);
    a.setHeaderText("Operasi belum berhasil");
    a.setContentText(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
    a.showAndWait();
  }

  private void info(String text) {
    var a = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
    a.initOwner(stage);
    a.setHeaderText("RetroQuest");
    a.showAndWait();
  }

  private static Label label(String text, String style) {
    var l = new Label(text);
    l.setWrapText(true);
    l.getStyleClass().add(style);
    return l;
  }

  private Button button(String text, String style, Runnable action) {
    var b = new Button(text);
    if (!style.isBlank()) b.getStyleClass().add(style);
    b.setOnAction(e -> safe(action));
    return b;
  }

  private static VBox column(Node... nodes) {
    return new VBox(12, nodes);
  }

  private static HBox row(Node... nodes) {
    var h = new HBox(14, nodes);
    h.setAlignment(Pos.CENTER_LEFT);
    return h;
  }

  private static Region spacer() {
    var r = new Region();
    HBox.setHgrow(r, Priority.ALWAYS);
    return r;
  }

  private static VBox card(Node... nodes) {
    var b = column(nodes);
    b.getStyleClass().add("card");
    return b;
  }

  private static ScrollPane scroll(Node node) {
    var s = new ScrollPane(node);
    s.setFitToWidth(true);
    s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    return s;
  }

  private static TextField field(String prompt, String value) {
    var f = new TextField(value);
    f.setPromptText(prompt);
    return f;
  }

  private static TextArea area(String text, int rows) {
    var a = new TextArea(text);
    a.setWrapText(true);
    a.setPrefRowCount(rows);
    return a;
  }

  private static VBox input(String text, Node field) {
    return new VBox(6, label(text, "field-label"), field);
  }

  private void login() {
    user = null;
    room = null;
    if (refreshTimer != null) refreshTimer.stop();
    root.setTop(null);
    root.setLeft(null);
    root.setBottom(null);
    var intro =
        column(
            label("R Q  /  ACADEMIC ADVENTURE", "eyebrow"),
            label("Belajar. Berpetualang.\nNaik level.", "login-title"),
            label(
                "Ubah setiap praktikum menjadi sebuah misi.\n"
                    + "Bangun kemampuanmu, selesaikan quest,\n"
                    + "dan tumbuh bersama guild kelasmu.",
                "muted"),
            AvatarFactory.create("Code Warrior", 350, 10),
            label("JAVA  ·  OOP  ·  YOUR NEXT LEVEL", "eyebrow"));
    intro.setAlignment(Pos.CENTER_LEFT);
    intro.setPadding(new Insets(50));
    intro.setPrefWidth(600);
    intro.getStyleClass().add("login-intro");
    var email = field("nama@email.com", "");
    var password = new PasswordField();
    password.setPromptText("Minimal 8 karakter");
    var name = field("Nama lengkap", "");
    var role = new ComboBox<String>();
    role.getItems().addAll("Mahasiswa / Player", "Dosen / Game Master");
    role.getSelectionModel().selectFirst();
    role.setMaxWidth(Double.MAX_VALUE);
    var form =
        column(
            label("Selamat datang, adventurer.", "heading"),
            label("Masuk untuk melanjutkan perjalanan belajarmu.", "muted"),
            input("EMAIL", email),
            input("PASSWORD", password));
    var submit =
        button(
            "Masuk ke guild  →",
            "primary",
            () ->
                async(
                    "Memeriksa akun",
                    () -> auth.login(email.getText(), password.getText()),
                    u -> {
                      user = u;
                      shell();
                    }));
    submit.setMaxWidth(Double.MAX_VALUE);
    submit.setDefaultButton(true);
    var registration = column(input("NAMA LENGKAP", name), input("PERAN", role));
    registration.setManaged(false);
    registration.setVisible(false);
    var toggle =
        button(
            "Belum punya akun? Daftar",
            "link-button",
            () -> {
              boolean visible = !registration.isVisible();
              registration.setVisible(visible);
              registration.setManaged(visible);
              submit.setText(visible ? "Buat akun" : "Masuk ke guild  →");
              submit.setOnAction(
                  e ->
                      safe(
                          () ->
                              async(
                                  visible ? "Membuat akun" : "Memeriksa akun",
                                  () ->
                                      visible
                                          ? auth.register(
                                              name.getText(),
                                              email.getText(),
                                              password.getText(),
                                              role.getSelectionModel().getSelectedIndex() == 1)
                                          : auth.login(email.getText(), password.getText()),
                                  u -> {
                                    user = u;
                                    shell();
                                  })));
            });
    form.getChildren()
        .addAll(
            registration,
            submit,
            toggle,
            new Separator(),
            label("COBA ALUR LENGKAP · DATA DEMO LOKAL", "eyebrow"),
            row(
                button("Demo mahasiswa", "secondary", () -> demo(false)),
                button("Demo dosen", "secondary", () -> demo(true))),
            label(
                "Akun demo memakai data contoh dan tersimpan terpisah sebagai akun berlabel Demo.",
                "muted"));
    form.setMaxWidth(470);
    form.setPadding(new Insets(35));
    var holder = new StackPane(form);
    HBox.setHgrow(holder, Priority.ALWAYS);
    var layout = new HBox(intro, holder);
    root.setCenter(layout);
  }

  private void demo(boolean teacher) {
    async(
        "Menyiapkan demo",
        () -> {
          DemoData.seed(db);
          return auth.login(
              teacher ? "dosen@retroquest.demo" : "mahasiswa@retroquest.demo", DemoData.PASSWORD);
        },
        u -> {
          user = u;
          shell();
        });
  }

  private void shell() {
    var sidebar =
        column(
            label("RETRO\nQUEST_", "brand"),
            label("YOUR ACADEMIC ADVENTURE", "tiny"),
            new Separator());
    sidebar.getStyleClass().add("sidebar");
    sidebar.setPrefWidth(222);
    navigation.clear();
    for (String name :
        List.of(
            "Dashboard",
            "Quest Board",
            "Materi",
            user.isLecturer() ? "Verification Center" : "Riwayat Tugas",
            "Leaderboard",
            "Profil")) {
      var b =
          button(
              name,
              "nav",
              () -> {
                page = name;
                refresh();
              });
      b.setMaxWidth(Double.MAX_VALUE);
      navigation.put(name, b);
      sidebar.getChildren().add(b);
    }
    var vspace = new Region();
    VBox.setVgrow(vspace, Priority.ALWAYS);
    sidebar
        .getChildren()
        .addAll(
            vspace,
            card(
                label(user.name(), "body-strong"),
                label(user.isLecturer() ? "GAME MASTER" : "PLAYER", "eyebrow")),
            button("Keluar", "secondary", this::login));
    root.setLeft(sidebar);
    rooms = new ComboBox<>();
    rooms.setPrefWidth(330);
    rooms.setConverter(
        new StringConverter<>() {
          public String toString(Row r) {
            return r == null ? "Pilih guild" : r.text("name");
          }

          public Row fromString(String s) {
            return null;
          }
        });
    rooms
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              room = newValue;
              refresh();
            });
    var bar =
        row(
            label("GUILD ROOM", "eyebrow"),
            rooms,
            spacer(),
            button(
                user.isLecturer() ? "+ Buat kelas" : "+ Gabung kelas",
                "primary",
                this::roomDialog));
    bar.getStyleClass().add("topbar");
    root.setTop(bar);
    var footer = row(status, spacer(), label("RETROQUEST 1.0  /  PBO PENS", "tiny"));
    footer.getStyleClass().add("footer");
    root.setBottom(footer);
    page = "Dashboard";
    loadRooms();
    refreshTimer =
        new Timeline(
            new KeyFrame(
                Duration.seconds(15),
                e -> {
                  if (!busy
                      && stage.isShowing()
                      && !(stage.getScene().getFocusOwner() instanceof TextInputControl)
                      && Window.getWindows().stream().noneMatch(w -> w != stage && w.isShowing()))
                    refresh();
                }));
    refreshTimer.setCycleCount(Timeline.INDEFINITE);
    refreshTimer.play();
  }

  private void loadRooms() {
    async(
        "Memuat kelas",
        () -> service.classes(user),
        list -> {
          rooms.getItems().setAll(list);
          room = list.isEmpty() ? null : list.getFirst();
          rooms.setValue(room);
          refresh();
        });
  }

  private void roomDialog() {
    var title = field(user.isLecturer() ? "Nama kelas" : "Kode undangan", "");
    var type = new ComboBox<String>();
    type.getItems().addAll("Code Warrior", "Byte Mage", "Logic Rogue");
    type.getSelectionModel().selectFirst();
    var d =
        dialog(
            user.isLecturer() ? "Buat guild baru" : "Gabung guild",
            column(
                input(user.isLecturer() ? "NAMA KELAS" : "KODE UNDANGAN", title),
                user.isLecturer()
                    ? label("Kode undangan dibuat otomatis.", "muted")
                    : input("ARKETIPE", type)),
            "Simpan");
    intercept(
        d,
        () -> {
          String value = title.getText(), archetype = type.getValue();
          dialogTask(
              d,
              "Menyimpan kelas",
              () -> {
                if (user.isLecturer()) service.createClass(user, value);
                else service.join(user, value, archetype);
                return true;
              },
              ok -> {
                d.close();
                loadRooms();
              });
        });
    d.showAndWait();
  }

  private record Snapshot(
      List<Row> quests,
      List<Row> submissions,
      List<Row> leaders,
      List<Row> modules,
      List<Row> badges) {}

  private void refresh() {
    if (busy || user == null) return;
    navigation.forEach(
        (name, b) -> {
          b.getStyleClass().remove("active");
          if (name.equals(page)) b.getStyleClass().add("active");
        });
    content = column();
    content.getStyleClass().add("page");
    root.setCenter(scroll(content));
    if (room == null) {
      content
          .getChildren()
          .addAll(
              label("Petualanganmu dimulai di sini.", "page-title"),
              card(
                  label("Belum ada guild", "heading"),
                  label(
                      user.isLecturer()
                          ? "Buat kelas pertamamu untuk menyiapkan materi dan quest."
                          : "Minta kode undangan kepada dosen, lalu gabung ke kelasmu.",
                      "muted"),
                  button(
                      user.isLecturer() ? "Buat kelas" : "Gabung kelas",
                      "primary",
                      this::roomDialog)));
      return;
    }
    long classId = room.id("id");
    int token = ++requestId;
    async(
        "Memuat " + page,
        () ->
            new Snapshot(
                service.quests(user, classId),
                service.submissions(user, classId),
                service.leaderboard(user, classId),
                user.isLecturer() ? service.modules(user, classId) : List.of(),
                service.badges(user, classId)),
        s -> {
          if (token != requestId) return;
          render(s);
        });
  }

  private void render(Snapshot s) {
    Row player = me(s);
    if (player != null) {
      String key = user.id() + ":" + room.id("id");
      int exp = player.number("exp_total");
      Integer previous = seenExp.put(key, exp);
      if (previous != null && new Progress(exp).level() > new Progress(previous).level()) {
        var p = new Progress(exp);
        status.setText("LEVEL UP! Level " + p.level() + " · Tier " + p.tier() + " · " + p.title());
        var glow = new FadeTransition(Duration.millis(350), content);
        glow.setFromValue(0.35);
        glow.setToValue(1);
        glow.setCycleCount(2);
        glow.setAutoReverse(true);
        glow.setOnFinished(e -> content.setOpacity(1));
        glow.play();
      }
    }
    String description =
        switch (page) {
          case "Quest Board" -> "Pilih misi, pelajari rubrik, lalu tunjukkan kemampuanmu.";
          case "Materi" -> "Sedikit demi sedikit. Satu konsep, satu langkah lebih dekat.";
          case "Verification Center" -> "Keputusan akhir ada di tangan Game Master.";
          case "Riwayat Tugas" -> "Setiap versi adalah bagian dari proses belajarmu.";
          case "Leaderboard" -> "Tumbuh bersama. Rayakan progres setiap anggota guild.";
          case "Profil" -> "Identitas petualang dan pencapaianmu dalam guild ini.";
          default ->
              "Selamat datang kembali, "
                  + user.name().split(" ")[0]
                  + ". Petualangan belajarmu berlanjut.";
        };
    content
        .getChildren()
        .addAll(
            label("GUILD / " + room.text("name").toUpperCase(Locale.ROOT), "eyebrow"),
            row(
                label(page, "page-title"),
                spacer(),
                label("KODE GUILD  " + room.text("join_code"), "chip")),
            label(description, "muted"));
    switch (page) {
      case "Quest Board" -> questBoard(s);
      case "Materi" -> materials(s);
      case "Verification Center", "Riwayat Tugas" -> history(s);
      case "Leaderboard" -> leaderboard(s);
      case "Profil" -> profile(s);
      default -> dashboard(s);
    }
  }

  private Row me(Snapshot s) {
    return s.leaders().stream()
        .filter(r -> r.id("student_id") == user.id())
        .findFirst()
        .orElse(null);
  }

  private void dashboard(Snapshot s) {
    Row me = me(s);
    int exp = me == null ? 0 : me.number("exp_total");
    var progress = new Progress(exp);
    var welcome =
        column(
            label(
                user.isLecturer() ? "GAME MASTER'S JOURNAL" : "THE NEXT CHAPTER IS YOURS",
                "eyebrow"),
            label(
                user.isLecturer()
                    ? "Bimbing guild-mu\nmenuju level berikutnya."
                    : "Quest kecil hari ini.\nSkill besar esok hari.",
                "hero-title"),
            label(
                user.isLecturer()
                    ? "Tinjau tugas, berikan umpan balik, dan apresiasi setiap progres."
                    : "Selesaikan misi, dapatkan feedback, dan kumpulkan EXP setelah verifikasi"
                        + " dosen.",
                "muted"),
            button(
                user.isLecturer() ? "Buka Verification Center  →" : "Jelajahi Quest Board  →",
                "primary",
                () -> {
                  page = user.isLecturer() ? "Verification Center" : "Quest Board";
                  refresh();
                }));
    HBox.setHgrow(welcome, Priority.ALWAYS);
    var hero =
        row(welcome, AvatarFactory.create(me == null ? "Byte Mage" : me.text("archetype"), exp, 9));
    hero.getStyleClass().add("hero");
    content.getChildren().add(hero);
    long pending =
        s.submissions().stream().filter(r -> r.text("status").equals("REVIEW_PENDING")).count();
    var metrics =
        row(
            metric(
                user.isLecturer() ? "ANGGOTA GUILD" : "TOTAL EXP",
                user.isLecturer() ? "" + s.leaders().size() : "" + exp,
                "Progres dalam kelas ini"),
            metric(
                "QUEST TERBIT",
                "" + s.quests().stream().filter(r -> r.text("status").equals("PUBLISHED")).count(),
                "Misi untuk dikerjakan"),
            metric("MENUNGGU TINJAUAN", "" + pending, "EXP diberikan setelah ACC"),
            metric(
                user.isLecturer() ? "DRAF QUEST" : "LEVEL / TIER",
                user.isLecturer()
                    ? "" + s.quests().stream().filter(r -> r.text("status").equals("DRAFT")).count()
                    : progress.level() + " / T" + progress.tier(),
                user.isLecturer() ? "Siap untuk ditinjau" : progress.title()));
    content.getChildren().add(metrics);
    content
        .getChildren()
        .add(
            row(
                label("Misi di guild-mu", "heading"),
                spacer(),
                button(
                    "Lihat semua →",
                    "link-button",
                    () -> {
                      page = "Quest Board";
                      refresh();
                    })));
    if (s.quests().isEmpty())
      content
          .getChildren()
          .add(empty("Belum ada quest. Game Master dapat menyiapkan materi dan misi pertama."));
    s.quests().stream().limit(3).forEach(q -> content.getChildren().add(questTile(q, s)));
  }

  private VBox metric(String title, String value, String caption) {
    var b = card(label(title, "eyebrow"), label(value, "metric"), label(caption, "muted"));
    HBox.setHgrow(b, Priority.ALWAYS);
    b.setMaxWidth(Double.MAX_VALUE);
    return b;
  }

  private VBox empty(String text) {
    return card(label(text, "muted"));
  }

  private void questBoard(Snapshot s) {
    if (user.isLecturer())
      content
          .getChildren()
          .add(
              row(
                  button("+ Buat quest", "primary", () -> questEditor(null, s.modules())),
                  button("+ Tambah materi", "secondary", this::moduleEditor)));
    var search = field("Cari judul quest…", "");
    var filter = new ComboBox<String>();
    filter.getItems().addAll("Semua", "PUBLISHED", "DRAFT");
    filter.getSelectionModel().selectFirst();
    if (!user.isLecturer()) filter.getItems().remove("DRAFT");
    var list = column();
    Runnable draw =
        () -> {
          list.getChildren().clear();
          s.quests().stream()
              .filter(
                  q ->
                      q.text("title")
                          .toLowerCase(Locale.ROOT)
                          .contains(search.getText().toLowerCase(Locale.ROOT)))
              .filter(
                  q ->
                      filter.getValue().equals("Semua")
                          || q.text("status").equals(filter.getValue()))
              .forEach(q -> list.getChildren().add(questTile(q, s)));
          if (list.getChildren().isEmpty())
            list.getChildren().add(empty("Tidak ada quest yang sesuai."));
        };
    search.textProperty().addListener((o, a, b) -> draw.run());
    filter.setOnAction(e -> draw.run());
    HBox.setHgrow(search, Priority.ALWAYS);
    content.getChildren().addAll(row(search, filter), list);
    draw.run();
  }

  private VBox questTile(Row q, Snapshot s) {
    String state = q.text("status");
    if (!user.isLecturer())
      state =
          s.submissions().stream()
              .filter(r -> r.id("quest_id") == q.id("id"))
              .findFirst()
              .map(r -> r.text("status"))
              .orElse("BELUM DIKIRIM");
    var top = row(label(q.text("module_title"), "eyebrow"), spacer(), label(state, "chip"));
    var bottom =
        row(
            label("Tenggat " + DATE.format(q.time("deadline")), "muted"),
            spacer(),
            label("+" + q.number("exp_reward") + " EXP", "reward"),
            button(
                user.isLecturer() && q.text("status").equals("DRAFT")
                    ? "Tinjau draf →"
                    : "Buka quest →",
                "secondary",
                () -> {
                  if (user.isLecturer() && q.text("status").equals("DRAFT"))
                    questEditor(q, s.modules());
                  else questDetail(q);
                }));
    return card(
        top,
        label(q.text("title"), "heading"),
        label(shorten(q.text("description"), 180), "muted"),
        bottom);
  }

  private static String shorten(String text, int length) {
    return text.length() > length ? text.substring(0, length) + "…" : text;
  }

  private void questDetail(Row row) {
    async(
        "Memuat quest",
        () -> service.detail(user, row.id("id")),
        q -> {
          var box =
              column(
                  label(q.text("title"), "page-title"),
                  label(
                      "+"
                          + q.number("exp_reward")
                          + " EXP  ·  "
                          + DATE.format(q.time("deadline"))
                          + "  ·  "
                          + q.text("allowed_types"),
                      "reward"),
                  label(q.text("description"), "body"),
                  label("Rubrik penilaian", "heading"));
          try {
            var rubric = JSON.readValue(q.text("rubric_json"), Rubric.class);
            rubric
                .criteria()
                .forEach(
                    c ->
                        box.getChildren()
                            .add(
                                card(
                                    row(
                                        label(c.name(), "body-strong"),
                                        spacer(),
                                        label(c.weight() + "%", "reward")),
                                    label(c.guide(), "muted"))));
          } catch (Exception e) {
            error(e);
            return;
          }
          box.getChildren()
              .add(button("Pelajari kartu NPC", "secondary", () -> cards(q.text("content_json"))));
          if (!user.isLecturer())
            box.getChildren()
                .add(
                    button(
                        "Unggah tugas / revisi",
                        "primary",
                        () -> upload(q.id("id"), q.text("allowed_types"))));
          var d = dialog("Quest briefing", box, "Tutup");
          d.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
          d.showAndWait();
        });
  }

  private void upload(long questId, String allowed) {
    var chooser = new FileChooser();
    chooser.setTitle("Pilih tugas · maksimal 20 MB");
    chooser
        .getExtensionFilters()
        .add(
            new FileChooser.ExtensionFilter(
                "Berkas quest",
                QuestService.parseTypes(allowed).stream().map(t -> "*." + t).toList()));
    var file = chooser.showOpenDialog(stage);
    if (file == null) return;
    async(
        "Menyimpan tugas",
        () -> service.submit(user, questId, file.toPath()),
        id -> {
          info(
              "Tugas tersimpan. EXP menunggu persetujuan dosen. Evaluasi AI opsional dapat"
                  + " dijalankan dari Riwayat Tugas.");
          page = "Riwayat Tugas";
          refresh();
        });
  }

  private void materials(Snapshot s) {
    if (user.isLecturer()) {
      content.getChildren().add(button("+ Unggah / tulis materi", "primary", this::moduleEditor));
      for (var m : s.modules())
        content
            .getChildren()
            .add(
                card(
                    row(
                        label(m.text("title"), "heading"),
                        spacer(),
                        label(m.text("status"), "chip")),
                    label(shorten(m.text("extracted_text"), 180), "muted"),
                    row(
                        button("Buka kartu NPC", "secondary", () -> cards(m.text("content_json"))),
                        button("Perbaiki draf materi", "secondary", () -> editModule(m)))));
      if (s.modules().isEmpty())
        content
            .getChildren()
            .add(empty("Unggah PDF/DOCX atau tulis materi pertama secara manual."));
    } else {
      var unique = new LinkedHashMap<Long, Row>();
      s.quests().forEach(q -> unique.putIfAbsent(q.id("module_id"), q));
      if (unique.isEmpty())
        content.getChildren().add(empty("Materi akan tersedia setelah dosen menerbitkan quest."));
      unique
          .values()
          .forEach(
              q ->
                  content
                      .getChildren()
                      .add(
                          card(
                              label(q.text("module_title"), "heading"),
                              label(
                                  "Bicaralah dengan NPC untuk mempelajari konsep sebelum memulai"
                                      + " quest.",
                                  "muted"),
                              button(
                                  "Mulai belajar →",
                                  "primary",
                                  () ->
                                      async(
                                          "Membuka materi",
                                          () -> service.detail(user, q.id("id")),
                                          detail -> cards(detail.text("content_json")))))));
    }
  }

  private void cards(String json) {
    try {
      var items =
          JSON.readValue(
              json,
              JSON.getTypeFactory().constructCollectionType(List.class, AiService.Card.class));
      @SuppressWarnings("unchecked")
      List<AiService.Card> list = (List<AiService.Card>) items;
      if (list.isEmpty()) {
        info("Belum ada kartu belajar.");
        return;
      }
      int[] index = {0};
      var title = label("", "heading");
      var body = label("", "body");
      var counter = label("", "eyebrow");
      var prev = button("← Sebelumnya", "secondary", () -> {});
      var next = button("Berikutnya →", "primary", () -> {});
      Runnable draw =
          () -> {
            title.setText(list.get(index[0]).title());
            body.setText(list.get(index[0]).body());
            counter.setText("KARTU " + (index[0] + 1) + " / " + list.size());
            prev.setDisable(index[0] == 0);
            next.setDisable(index[0] == list.size() - 1);
          };
      prev.setOnAction(
          e -> {
            index[0]--;
            draw.run();
          });
      next.setOnAction(
          e -> {
            index[0]++;
            draw.run();
          });
      draw.run();
      var box =
          column(
              row(
                  AvatarFactory.create("Byte Mage", 700, 5),
                  column(
                      label("ARCHIVIST · NPC MENTOR", "eyebrow"),
                      label("Ada ilmu baru untuk petualanganmu.", "muted"))),
              card(counter, title, body),
              row(prev, spacer(), next));
      var d = dialog("Retro Micro-Learning", box, "Tutup");
      d.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
      d.showAndWait();
    } catch (Exception e) {
      error(e);
    }
  }

  private void history(Snapshot s) {
    var list = column();
    var onlyPending = new CheckBox("Tampilkan hanya yang menunggu tinjauan");
    Runnable draw =
        () -> {
          list.getChildren().clear();
          for (var sub : s.submissions())
            if (!onlyPending.isSelected() || sub.text("status").equals("REVIEW_PENDING"))
              list.getChildren()
                  .add(
                      card(
                          row(
                              label(sub.text("quest_title"), "heading"),
                              spacer(),
                              label(sub.text("status"), "chip")),
                          label(
                              sub.text("student_name")
                                  + " · Versi "
                                  + sub.number("version")
                                  + " · "
                                  + DATE.format(sub.time("submitted_at")),
                              "muted"),
                          label(
                              sub.has("final_score")
                                  ? "Nilai akhir: "
                                      + sub.number("final_score")
                                      + " / 100 · "
                                      + sub.text("final_feedback")
                                  : sub.text("final_feedback"),
                              "body"),
                          button(
                              user.isLecturer()
                                  ? "Tinjau berkas & nilai →"
                                  : "Detail & umpan balik →",
                              "secondary",
                              () -> submissionDialog(sub))));
          if (list.getChildren().isEmpty())
            list.getChildren().add(empty("Belum ada pengumpulan untuk ditampilkan."));
        };
    onlyPending.setOnAction(e -> draw.run());
    content.getChildren().addAll(onlyPending, list);
    draw.run();
  }

  private record ReviewData(Row quest, List<Row> evaluations, Path file, String preview) {}

  private void submissionDialog(Row s) {
    async(
        "Membaca pengumpulan",
        () -> {
          var q = service.detail(user, s.id("quest_id"));
          Path file = service.submissionFile(user, s.id("id"));
          String preview;
          try {
            preview = new DocumentService().extract(file);
          } catch (Exception e) {
            preview =
                "Ekstraksi otomatis tidak tersedia: "
                    + e.getMessage()
                    + "\nGunakan tombol Buka berkas asli untuk pemeriksaan manual.";
          }
          return new ReviewData(q, service.evaluations(user, s.id("id")), file, preview);
        },
        data -> {
          var box =
              column(
                  label(s.text("quest_title"), "heading"),
                  label(
                      s.text("student_name")
                          + " · Versi "
                          + s.number("version")
                          + " · "
                          + s.text("status"),
                      "eyebrow"),
                  label(data.quest().text("description"), "body"));
          try {
            var rubric = JSON.readValue(data.quest().text("rubric_json"), Rubric.class);
            for (var criterion : rubric.criteria())
              box.getChildren()
                  .add(
                      label(
                          criterion.name() + " (" + criterion.weight() + "%): " + criterion.guide(),
                          "muted"));
          } catch (Exception e) {
            error(e);
            return;
          }
          var preview = area(shorten(data.preview(), 100000), 8);
          preview.setEditable(false);
          box.getChildren()
              .addAll(
                  input("PRATINJAU TEKS · KODE TIDAK DIEKSEKUSI", preview),
                  button(
                      "Buka berkas asli",
                      "secondary",
                      () -> {
                        try {
                          Path original = data.file().toAbsolutePath().normalize();
                          if (!Files.isRegularFile(original) || !Files.isReadable(original)) {
                            throw new IllegalArgumentException(
                                "Berkas asli tidak ditemukan atau tidak dapat dibaca: " + original);
                          }
                          if (FilesService.extension(original).equals("pdf")) {
                            new PdfViewer().open(box.getScene().getWindow(), original);
                          } else {
                            Desktop.getDesktop().open(original.toFile());
                          }
                        } catch (Exception e) {
                          error(e);
                        }
                      }));
          if (data.evaluations().isEmpty())
            box.getChildren()
                .add(
                    label(
                        "Belum ada rekomendasi AI. Dosen dapat langsung menilai secara manual.",
                        "muted"));
          for (var ai : data.evaluations())
            box.getChildren()
                .add(
                    card(
                        label(
                            "AI · "
                                + ai.text("status")
                                + (ai.has("score_draft")
                                    ? " · Rekomendasi " + ai.number("score_draft") + "/100"
                                    : ""),
                            "eyebrow"),
                        label(ai.text("feedback") + ai.text("error_message"), "muted")));
          var score = field("0-100", s.has("final_score") ? s.text("final_score") : "");
          var feedback = area(s.text("final_feedback"), 3);
          box.getChildren()
              .addAll(input("NILAI AKHIR DOSEN", score), input("CATATAN / PERBAIKAN", feedback));
          score.setEditable(user.isLecturer());
          feedback.setEditable(user.isLecturer());
          var d = dialog("Verification Center · versi " + s.number("version"), box, "Tutup");
          d.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
          if (s.text("status").equals("REVIEW_PENDING")) {
            box.getChildren()
                .add(
                    button(
                        "Minta rekomendasi AI (opsional)",
                        "secondary",
                        () -> {
                          var consent =
                              new Alert(
                                  Alert.AlertType.CONFIRMATION,
                                  "Teks tugas, instruksi, dan rubrik akan dikirim ke endpoint AI"
                                      + " yang Anda konfigurasi. Lanjutkan?",
                                  ButtonType.YES,
                                  ButtonType.NO);
                          consent.initOwner(d.getOwner());
                          if (consent.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
                          d.close();
                          async(
                              "Mengevaluasi dengan AI",
                              () -> service.evaluate(user, s.id("id")),
                              message -> {
                                info(message);
                                refresh();
                              });
                        }));
            if (user.isLecturer())
              box.getChildren()
                  .add(
                      row(
                          button(
                              "Minta revisi",
                              "secondary",
                              () -> {
                                if (feedback.getText().isBlank())
                                  throw new IllegalArgumentException("Catatan revisi wajib diisi.");
                                String notes = feedback.getText();
                                d.close();
                                async(
                                    "Menyimpan revisi",
                                    () -> {
                                      service.review(user, s.id("id"), null, notes, true);
                                      return true;
                                    },
                                    ok -> refresh());
                              }),
                          button(
                              "Setujui & berikan EXP",
                              "primary",
                              () -> {
                                int value = Integer.parseInt(score.getText().strip());
                                if (value < 0 || value > 100 || feedback.getText().isBlank())
                                  throw new IllegalArgumentException(
                                      "Isi nilai 0-100 dan catatan dosen.");
                                String notes = feedback.getText();
                                d.close();
                                async(
                                    "Menyimpan penilaian",
                                    () -> {
                                      service.review(user, s.id("id"), value, notes, false);
                                      return true;
                                    },
                                    ok -> {
                                      info(
                                          "Penilaian tersimpan. EXP, level, tier, dan achievement"
                                              + " kelas telah diperbarui.");
                                      refresh();
                                    });
                              })));
          }
          d.showAndWait();
        });
  }

  private void leaderboard(Snapshot s) {
    content
        .getChildren()
        .add(
            label(
                "Peringkat berdasarkan EXP terverifikasi. Diperbarui otomatis setiap 15 detik.",
                "muted"));
    if (s.leaders().isEmpty())
      content.getChildren().add(empty("Belum ada anggota. Bagikan kode guild kepada mahasiswa."));
    int rank = 0;
    for (var player : s.leaders()) {
      var p = new Progress(player.number("exp_total"));
      var text =
          column(
              label(
                  player.text("name") + (player.id("student_id") == user.id() ? "  (kamu)" : ""),
                  "heading"),
              label(
                  player.text("archetype")
                      + " · Level "
                      + p.level()
                      + " / Tier "
                      + p.tier()
                      + " · "
                      + player.number("completed")
                      + " quest selesai",
                  "muted"));
      HBox.setHgrow(text, Priority.ALWAYS);
      content
          .getChildren()
          .add(
              card(
                  row(
                      label(String.format("%02d", ++rank), "rank"),
                      AvatarFactory.create(player.text("archetype"), p.exp(), 3),
                      text,
                      column(
                          label(p.exp() + " EXP", "reward"),
                          label(
                              player.text("latest_status").isBlank()
                                  ? "Belum mengirim"
                                  : player.text("latest_status"),
                              "tiny")))));
    }
  }

  private void profile(Snapshot s) {
    var me = me(s);
    if (me == null) {
      content
          .getChildren()
          .add(
              card(
                  label(user.name(), "heading"),
                  label(user.email(), "muted"),
                  label("Game Master · Pengelola " + room.text("name"), "reward")));
      return;
    }
    var p = new Progress(me.number("exp_total"));
    var bar = new ProgressBar((p.exp() % 100) / 100.0);
    bar.setMaxWidth(Double.MAX_VALUE);
    content
        .getChildren()
        .add(
            card(
                row(
                    AvatarFactory.create(me.text("archetype"), p.exp(), 8),
                    column(
                        label(user.name(), "page-title"),
                        label(p.title(), "reward"),
                        label(
                            "Level " + p.level() + " · Tier " + p.tier() + " · " + p.exp() + " EXP",
                            "body"))),
                bar,
                label(
                    (100 - p.exp() % 100)
                        + " EXP menuju level berikutnya. Progres dihitung khusus kelas ini.",
                    "muted")));
    var archetype = new ComboBox<String>();
    archetype.getItems().addAll("Code Warrior", "Byte Mage", "Logic Rogue");
    archetype.setValue(me.text("archetype"));
    content
        .getChildren()
        .add(
            card(
                label("Pilih arketipemu", "heading"),
                row(
                    archetype,
                    button(
                        "Simpan avatar",
                        "primary",
                        () ->
                            async(
                                "Mengubah avatar",
                                () -> {
                                  service.avatar(user, room.id("id"), archetype.getValue());
                                  return true;
                                },
                                ok -> refresh())))));
    content.getChildren().add(label("Achievement dalam guild", "heading"));
    if (s.badges().isEmpty())
      content
          .getChildren()
          .add(empty("Selesaikan quest terverifikasi pertamamu untuk membuka lencana."));
    for (var badge : s.badges())
      content
          .getChildren()
          .add(
              card(
                  label("★  " + badge.text("badge_code"), "reward"),
                  label("Diperoleh " + DATE.format(badge.time("earned_at")), "muted")));
  }

  private Dialog<ButtonType> dialog(String title, Node body, String action) {
    var d = new Dialog<ButtonType>();
    d.initOwner(stage);
    d.setTitle(title);
    d.setResizable(true);
    d.getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/ui/retroquest.css").toExternalForm());
    d.getDialogPane()
        .getButtonTypes()
        .addAll(new ButtonType(action, ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
    var padded = new VBox(body);
    padded.setPadding(new Insets(15));
    d.getDialogPane().setContent(scroll(padded));
    d.getDialogPane().setPrefWidth(780);
    d.getDialogPane()
        .setPrefHeight(Math.min(740, Screen.getPrimary().getVisualBounds().getHeight() - 100));
    return d;
  }

  private void intercept(Dialog<ButtonType> d, Runnable action) {
    var ok =
        d.getDialogPane().getButtonTypes().stream()
            .filter(b -> b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
            .findFirst()
            .orElseThrow();
    d.getDialogPane()
        .lookupButton(ok)
        .addEventFilter(
            javafx.event.ActionEvent.ACTION,
            e -> {
              e.consume();
              safe(action);
            });
  }

  private static String cardsText(List<AiService.Card> cards) {
    return String.join("\n---\n", cards.stream().map(c -> c.title() + "\n" + c.body()).toList());
  }

  private static List<AiService.Card> parseCards(String text) {
    var list = new ArrayList<AiService.Card>();
    for (String block : text.split("(?m)^---\\s*$")) {
      String[] parts = block.strip().split("\\R", 2);
      if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank())
        throw new IllegalArgumentException(
            "Setiap kartu harus memuat judul pada baris pertama dan isi pada baris berikutnya.");
      list.add(new AiService.Card(parts[0].strip(), parts[1].strip()));
    }
    return list;
  }

  private void editModule(Row module) {
    try {
      List<AiService.Card> existing =
          JSON.readValue(
              module.text("content_json"),
              JSON.getTypeFactory().constructCollectionType(List.class, AiService.Card.class));
      var title = field("Judul modul", module.text("title"));
      var text = area(module.text("extracted_text"), 7);
      var cards = area(cardsText(existing), 7);
      var d =
          dialog(
              "Perbaiki draf materi",
              column(
                  label(
                      "Materi hanya dapat diperbaiki selama belum ada quest terbit yang"
                          + " menggunakannya.",
                      "muted"),
                  input("JUDUL", title),
                  input("TEKS SUMBER", text),
                  input("KARTU: JUDUL + ISI, PISAHKAN DENGAN BARIS ---", cards)),
              "Simpan");
      intercept(
          d,
          () -> {
            String name = title.getText(), source = text.getText();
            var list = parseCards(cards.getText());
            dialogTask(
                d,
                "Menyimpan draf",
                () -> {
                  service.updateModule(user, module.id("id"), name, source, list);
                  return true;
                },
                ok -> {
                  d.close();
                  refresh();
                });
          });
      d.showAndWait();
    } catch (Exception e) {
      error(e);
    }
  }

  private void moduleEditor() {
    var title = field("Judul materi", "");
    var sourceText = area("", 7);
    var cards = area("Konsep pertama\nTuliskan penjelasan singkat di sini.", 6);
    Path[] source = {null};
    AiService.Curriculum[] suggestion = {null};
    var message = label("PDF/DOCX maksimal 20 MB. Materi juga dapat ditulis manual.", "muted");
    var generateQuest = new CheckBox("Simpan juga draf quest dari rekomendasi AI");
    var box =
        column(
            input("JUDUL MODUL", title),
            message,
            input("TEKS SUMBER (DAPAT DIPERBAIKI)", sourceText),
            input("KARTU: JUDUL + ISI · PEMISAH ANTARKARTU: ---", cards),
            generateQuest);
    var d = dialog("Workshop materi", box, "Simpan draf");
    var upload =
        button(
            "Pilih PDF / DOCX",
            "secondary",
            () -> {
              var chooser = new FileChooser();
              chooser
                  .getExtensionFilters()
                  .add(new FileChooser.ExtensionFilter("Materi", "*.pdf", "*.docx"));
              var file = chooser.showOpenDialog(stage);
              if (file == null) return;
              source[0] = file.toPath();
              title.setText(file.getName().replaceFirst("\\.[^.]+$", ""));
              dialogTask(
                  d,
                  "Mengekstrak dokumen",
                  () -> {
                    try {
                      return new DocumentService().extract(file.toPath());
                    } catch (Exception e) {
                      return "";
                    }
                  },
                  text -> {
                    sourceText.setText(text);
                    message.setText(
                        text.isBlank()
                            ? "Ekstraksi gagal atau OCR belum tersedia. Berkas tetap dapat"
                                + " disimpan; isi teks dan kartu secara manual."
                            : "Teks berhasil diekstrak. Tinjau dan perbaiki sebelum menyimpan.");
                  });
            });
    var ai =
        button(
            "Susun draf dengan AI",
            "secondary",
            () -> {
              if (sourceText.getText().isBlank())
                throw new IllegalArgumentException("Isi atau ekstrak teks sumber dahulu.");
              var confirm =
                  new Alert(
                      Alert.AlertType.CONFIRMATION,
                      "Teks sumber akan dikirim ke endpoint AI pada konfigurasi Anda. Lanjutkan?",
                      ButtonType.YES,
                      ButtonType.NO);
              confirm.initOwner(stage);
              if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
              String text = sourceText.getText();
              dialogTask(
                  d,
                  "Menyusun draf AI",
                  () -> new AiService().curriculum(text),
                  result -> {
                    suggestion[0] = result;
                    cards.setText(cardsText(result.cards()));
                    generateQuest.setSelected(true);
                    message.setText(
                        "Draf AI tersedia. Tinjau kartu di bawah; quest disimpan sebagai DRAFT"
                            + " untuk ditinjau kembali.");
                  });
            });
    box.getChildren().add(1, row(upload, ai));
    intercept(
        d,
        () -> {
          String name = title.getText(), text = sourceText.getText();
          var items = parseCards(cards.getText());
          if (name.isBlank()) throw new IllegalArgumentException("Isi judul modul.");
          var proposed = generateQuest.isSelected() ? suggestion[0] : null;
          Path file = source[0];
          dialogTask(
              d,
              "Menyimpan materi",
              () -> {
                long module = service.saveModule(user, room.id("id"), name, text, items, file);
                if (proposed != null)
                  service.saveQuest(
                      user,
                      0,
                      module,
                      proposed.title(),
                      proposed.instructions(),
                      LocalDateTime.now().plusDays(7),
                      "pdf,docx,java",
                      100,
                      proposed.rubric(),
                      false);
                return module;
              },
              id -> {
                d.close();
                page = "Materi";
                refresh();
              });
        });
    d.showAndWait();
  }

  private <T> void dialogTask(Dialog<?> d, String message, Job<T> work, Consumer<T> success) {
    d.getDialogPane().setDisable(true);
    status.setText(message + "…");
    Task<T> task =
        new Task<>() {
          @Override
          protected T call() throws Exception {
            return work.run();
          }
        };
    task.setOnSucceeded(
        e -> {
          d.getDialogPane().setDisable(false);
          status.setText("Siap");
          safe(() -> success.accept(task.getValue()));
        });
    task.setOnFailed(
        e -> {
          d.getDialogPane().setDisable(false);
          status.setText("Lanjutkan manual atau coba ulang");
          error(task.getException());
        });
    Thread thread = new Thread(task, "retroquest-document");
    thread.setDaemon(true);
    thread.start();
  }

  private void questEditor(Row q, List<Row> modules) {
    if (modules.isEmpty()) {
      info("Tambahkan materi dan kartu belajar dahulu melalui menu Materi.");
      return;
    }
    var module = new ComboBox<Row>();
    module.getItems().setAll(modules);
    module.setConverter(
        new StringConverter<>() {
          public String toString(Row r) {
            return r == null ? "" : r.text("title");
          }

          public Row fromString(String text) {
            return null;
          }
        });
    module.setMaxWidth(Double.MAX_VALUE);
    module.getSelectionModel().selectFirst();
    if (q != null) {
      modules.stream()
          .filter(m -> m.id("id") == q.id("module_id"))
          .findFirst()
          .ifPresent(module::setValue);
      module.setDisable(true);
    }
    var title = field("Judul misi", q == null ? "" : q.text("title"));
    var desc = area(q == null ? "" : q.text("description"), 5);
    var deadline =
        new DatePicker(q == null ? LocalDate.now().plusDays(7) : q.time("deadline").toLocalDate());
    var time =
        field(
            "HH:mm",
            q == null
                ? "23:59"
                : q.time("deadline").toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
    var reward = field("100", q == null ? "100" : q.text("exp_reward"));
    var types = field("pdf,docx,java", q == null ? "pdf,docx,java" : q.text("allowed_types"));
    Rubric rubric = Rubric.standard();
    try {
      if (q != null) rubric = JSON.readValue(q.text("rubric_json"), Rubric.class);
    } catch (Exception e) {
      error(e);
      return;
    }
    String initial =
        String.join(
            "\n",
            rubric.criteria().stream()
                .map(c -> c.name() + " | " + c.weight() + " | " + c.guide())
                .toList());
    var rubricField = area(initial, 5);
    var publish =
        new CheckBox("Saya sudah meninjau materi, instruksi, dan rubrik. Terbitkan quest.");
    var box =
        column(
            input("MODUL SUMBER", module),
            input("JUDUL QUEST", title),
            input("INSTRUKSI", desc),
            row(
                input("TANGGAL TENGGAT", deadline),
                input("JAM", time),
                input("REWARD EXP", reward)),
            input("FORMAT BERKAS (PISAHKAN KOMA)", types),
            input("RUBRIK · SATU BARIS PER KRITERIA: NAMA | BOBOT | PANDUAN SKOR", rubricField),
            label(
                "Total bobot harus tepat 100%. Quest terbit tidak dapat diubah agar rubrik dan"
                    + " reward tetap konsisten.",
                "muted"),
            publish);
    var d = dialog(q == null ? "Rancang quest baru" : "Tinjau draf quest", box, "Simpan quest");
    intercept(
        d,
        () -> {
          var criteria = new ArrayList<Rubric.Criterion>();
          for (String line : rubricField.getText().split("\\R")) {
            if (line.isBlank()) continue;
            var parts = line.split("\\|", 3);
            if (parts.length != 3)
              throw new IllegalArgumentException("Format rubrik: nama | bobot | panduan skor");
            criteria.add(
                new Rubric.Criterion(
                    parts[0].strip(), Integer.parseInt(parts[1].strip()), parts[2].strip()));
          }
          var r = new Rubric(criteria);
          LocalDateTime due =
              LocalDateTime.of(
                  Objects.requireNonNull(deadline.getValue(), "Pilih tanggal tenggat"),
                  LocalTime.parse(time.getText().strip()));
          int exp = Integer.parseInt(reward.getText().strip());
          String name = title.getText(), instructions = desc.getText(), formats = types.getText();
          long moduleId = module.getValue().id("id");
          boolean published = publish.isSelected();
          dialogTask(
              d,
              "Menyimpan quest",
              () ->
                  service.saveQuest(
                      user,
                      q == null ? 0 : q.id("id"),
                      moduleId,
                      name,
                      instructions,
                      due,
                      formats,
                      exp,
                      r,
                      published),
              id -> {
                d.close();
                page = "Quest Board";
                refresh();
              });
        });
    d.showAndWait();
  }
}
