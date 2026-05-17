package flashcard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FlashcardApp {

    // ── Storage ───────────────────────────────────────────────────
    private static final String DATA_FILE = "data.properties";
    private final Properties props = new Properties();

    // ── Runtime data ──────────────────────────────────────────────
    private final Map<String, String> users = new HashMap<>();
    private final Map<String, List<Flashcard>> userCards = new HashMap<>();

    // ── Session ───────────────────────────────────────────────────
    private String loggedInUser = null;
    private int currentIndex = 0;
    private boolean showingQuestion = true;

    // ── UI ────────────────────────────────────────────────────────
    private Stage primaryStage;
    private final Label cardLabel    = new Label();
    private final Label counterLabel = new Label();

    // ─────────────────────────────────────────────────────────────
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Flashcard App");
        loadData();
        showAuthScreen();
        stage.show();
    }

    // ══════════════════════════════════════════════════════════════
    //  SAVE & LOAD  (Properties file — no libraries needed)
    // ══════════════════════════════════════════════════════════════

    /**
     * Properties file format:
     *   user.alice = password123
     *   cards.alice.count = 2
     *   cards.alice.0.q = What is Java?
     *   cards.alice.0.a = A programming language
     *   cards.alice.1.q = What is JavaFX?
     *   cards.alice.1.a = A UI framework
     */
    private void saveData() {
        props.clear();

        // Save users
        for (Map.Entry<String, String> e : users.entrySet()) {
            props.setProperty("user." + e.getKey(), e.getValue());
        }

        // Save cards per user
        for (Map.Entry<String, List<Flashcard>> e : userCards.entrySet()) {
            String username = e.getKey();
            List<Flashcard> cards = e.getValue();
            props.setProperty("cards." + username + ".count",
                    String.valueOf(cards.size()));
            for (int i = 0; i < cards.size(); i++) {
                props.setProperty("cards." + username + "." + i + ".q",
                        cards.get(i).getQuestion());
                props.setProperty("cards." + username + "." + i + ".a",
                        cards.get(i).getAnswer());
            }
        }

        try (FileWriter fw = new FileWriter(DATA_FILE)) {
            props.store(fw, "Flashcard App Data");
        } catch (IOException ex) {
            showAlert("Save Error", "Could not save: " + ex.getMessage());
        }
    }

    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;

        try (FileReader fr = new FileReader(file)) {
            props.load(fr);
        } catch (IOException ex) {
            showAlert("Load Error", "Could not load: " + ex.getMessage());
            return;
        }

        // Load users
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("user.")) {
                String username = key.substring(5); // remove "user."
                users.put(username, props.getProperty(key));
            }
        }

        // Load cards per user
        for (String username : users.keySet()) {
            String countKey = "cards." + username + ".count";
            if (!props.containsKey(countKey)) continue;

            int count = Integer.parseInt(props.getProperty(countKey));
            List<Flashcard> cards = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String q = props.getProperty("cards." + username + "." + i + ".q", "");
                String a = props.getProperty("cards." + username + "." + i + ".a", "");
                if (!q.isEmpty()) cards.add(new Flashcard(q, a));
            }
            userCards.put(username, cards);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AUTH SCREEN
    // ══════════════════════════════════════════════════════════════
    private void showAuthScreen() {
        Label title = new Label("Flashcard App");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));

        Button signInBtn = styledButton("Sign In", "#5b8dee");
        Button signUpBtn = styledButton("Sign Up", "#27ae60");

        signInBtn.setPrefWidth(200);
        signUpBtn.setPrefWidth(200);

        signInBtn.setOnAction(e -> showSignInDialog());
        signUpBtn.setOnAction(e -> showSignUpDialog());

        VBox root = new VBox(20, title, signInBtn, signUpBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #f4f4f4;");

        primaryStage.setScene(new Scene(root, 400, 300));
    }

    // ── Sign Up ───────────────────────────────────────────────────
    private void showSignUpDialog() {
        Stage dialog = dialogStage("Sign Up");

        Label userLbl = new Label("Username:");
        TextField userField = new TextField();

        Label passLbl = new Label("Password:");
        PasswordField passField = new PasswordField();

        Label confirmLbl = new Label("Confirm Password:");
        PasswordField confirmField = new PasswordField();

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: red;");

        Button signUpBtn = styledButton("Create Account", "#27ae60");
        signUpBtn.setMaxWidth(Double.MAX_VALUE);

        signUpBtn.setOnAction(e -> {
            String user    = userField.getText().trim();
            String pass    = passField.getText();
            String confirm = confirmField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                errorLbl.setText("Username and password cannot be empty.");
            } else if (!pass.equals(confirm)) {
                errorLbl.setText("Passwords do not match.");
            } else if (users.containsKey(user)) {
                errorLbl.setText("Username already exists.");
            } else {
                users.put(user, pass);
                userCards.put(user, new ArrayList<>());
                saveData();
                dialog.close();
                showAlert("Account Created", "Account created! Please sign in.");
            }
        });

        VBox layout = new VBox(10,
                userLbl, userField,
                passLbl, passField,
                confirmLbl, confirmField,
                errorLbl, signUpBtn);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f4f4f4;");

        dialog.setScene(new Scene(layout, 320, 290));
        dialog.showAndWait();
    }

    // ── Sign In ───────────────────────────────────────────────────
    private void showSignInDialog() {
        Stage dialog = dialogStage("Sign In");

        Label userLbl = new Label("Username:");
        TextField userField = new TextField();

        Label passLbl = new Label("Password:");
        PasswordField passField = new PasswordField();

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: red;");

        Button signInBtn = styledButton("Sign In", "#5b8dee");
        signInBtn.setMaxWidth(Double.MAX_VALUE);

        signInBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();

            if (users.containsKey(user) && users.get(user).equals(pass)) {
                loggedInUser = user;
                currentIndex = 0;
                showingQuestion = true;
                dialog.close();
                showMainScreen();
            } else {
                errorLbl.setText("Invalid username or password.");
            }
        });

        VBox layout = new VBox(10,
                userLbl, userField,
                passLbl, passField,
                errorLbl, signInBtn);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f4f4f4;");

        dialog.setScene(new Scene(layout, 320, 230));
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    //  MAIN FLASHCARD SCREEN
    // ══════════════════════════════════════════════════════════════
    private void showMainScreen() {
        Label welcomeLbl = new Label("Logged in as: " + loggedInUser);
        welcomeLbl.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        Button signOutBtn = styledButton("Sign Out", "#e74c3c");
        signOutBtn.setOnAction(e -> signOut());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, welcomeLbl, spacer, signOutBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        cardLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        cardLabel.setWrapText(true);
        cardLabel.setAlignment(Pos.CENTER);
        cardLabel.setMaxWidth(Double.MAX_VALUE);
        cardLabel.setMaxHeight(Double.MAX_VALUE);
        cardLabel.setPadding(new Insets(20));

        StackPane cardPane = new StackPane(cardLabel);
        cardPane.setStyle(questionStyle());
        cardPane.setPrefHeight(180);
        VBox.setVgrow(cardPane, Priority.ALWAYS);

        counterLabel.setFont(Font.font("Arial", 13));
        counterLabel.setAlignment(Pos.CENTER);

        Button prevBtn    = styledButton("Prev",    "#5b8dee");
        Button flipBtn    = styledButton("Flip",    "#f0a500");
        Button nextBtn    = styledButton("Next",    "#5b8dee");
        Button shuffleBtn = styledButton("Shuffle", "#8e44ad");
        Button addBtn     = styledButton("Add",     "#27ae60");
        Button editBtn    = styledButton("Edit",    "#2980b9");
        Button deleteBtn  = styledButton("Delete",  "#e74c3c");

        prevBtn.setOnAction(e    -> navigate(-1));
        flipBtn.setOnAction(e    -> flip(cardPane));
        nextBtn.setOnAction(e    -> navigate(1));
        shuffleBtn.setOnAction(e -> shuffle());
        addBtn.setOnAction(e     -> addCard());
        editBtn.setOnAction(e    -> editCard());
        deleteBtn.setOnAction(e  -> deleteCard());

        HBox navRow    = centeredHBox(10, prevBtn, flipBtn, nextBtn);
        HBox actionRow = centeredHBox(10, shuffleBtn, addBtn, editBtn, deleteBtn);

        VBox root = new VBox(12, topBar, counterLabel, cardPane, navRow, actionRow);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f4f4f4;");

        primaryStage.setScene(new Scene(root, 540, 430));
        updateDisplay(cardPane);
    }

    // ══════════════════════════════════════════════════════════════
    //  FLASHCARD ACTIONS
    // ══════════════════════════════════════════════════════════════
    private void flip(StackPane cardPane) {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) return;
        showingQuestion = !showingQuestion;
        Flashcard c = cards.get(currentIndex);
        if (showingQuestion) {
            cardLabel.setText("Q: " + c.getQuestion());
            cardPane.setStyle(questionStyle());
        } else {
            cardLabel.setText("A: " + c.getAnswer());
            cardPane.setStyle(answerStyle());
        }
    }

    private void navigate(int dir) {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) return;
        currentIndex = (currentIndex + dir + cards.size()) % cards.size();
        showingQuestion = true;
        showMainScreen();
    }

    private void shuffle() {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) return;
        Collections.shuffle(cards);
        currentIndex = 0;
        showingQuestion = true;
        showMainScreen();
        showAlert("Shuffled!", "Cards have been shuffled.");
    }

    private void addCard() {
        String q = inputDialog("New Card - Step 1 of 2", "Enter the question:");
        if (q == null || q.isBlank()) return;
        String a = inputDialog("New Card - Step 2 of 2", "Enter the answer:");
        if (a == null || a.isBlank()) return;

        currentCards().add(new Flashcard(q.trim(), a.trim()));
        currentIndex = currentCards().size() - 1;
        showingQuestion = true;
        saveData();
        showMainScreen();
    }

    private void editCard() {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) {
            showAlert("No Cards", "Add a card first.");
            return;
        }
        Flashcard c = cards.get(currentIndex);

        String newQ = inputDialogWithDefault("Edit Card - Question",
                "Edit the question:", c.getQuestion());
        if (newQ == null) return;

        String newA = inputDialogWithDefault("Edit Card - Answer",
                "Edit the answer:", c.getAnswer());
        if (newA == null) return;

        if (!newQ.isBlank()) c.setQuestion(newQ.trim());
        if (!newA.isBlank()) c.setAnswer(newA.trim());

        showingQuestion = true;
        saveData();
        showMainScreen();
    }

    private void deleteCard() {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) {
            showAlert("No Cards", "Nothing to delete.");
            return;
        }
        Flashcard c = cards.get(currentIndex);

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this card?\n\nQ: " + c.getQuestion(),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Card");
        confirm.setHeaderText(null);
        confirm.initOwner(primaryStage);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                cards.remove(currentIndex);
                if (currentIndex >= cards.size()) {
                    currentIndex = Math.max(0, cards.size() - 1);
                }
                showingQuestion = true;
                saveData();
                showMainScreen();
            }
        });
    }

    private void signOut() {
        loggedInUser = null;
        currentIndex = 0;
        showingQuestion = true;
        showAuthScreen();
    }

    // ══════════════════════════════════════════════════════════════
    //  DISPLAY
    // ══════════════════════════════════════════════════════════════
    private void updateDisplay(StackPane cardPane) {
        List<Flashcard> cards = currentCards();
        if (cards.isEmpty()) {
            cardLabel.setText("No cards yet! Click Add.");
            counterLabel.setText("");
            cardPane.setStyle(questionStyle());
        } else {
            cardLabel.setText("Q: " + cards.get(currentIndex).getQuestion());
            counterLabel.setText("Card " + (currentIndex + 1) + " of " + cards.size());
            cardPane.setStyle(questionStyle());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════
    private List<Flashcard> currentCards() {
        return userCards.getOrDefault(loggedInUser, new ArrayList<>());
    }

    private String questionStyle() {
        return "-fx-background-color: #FFFADC; -fx-border-color: #aaa; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;";
    }

    private String answerStyle() {
        return "-fx-background-color: #DCF0FF; -fx-border-color: #aaa; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;";
    }

    private Button styledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-background-radius: 8; -fx-padding: 7 14;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private HBox centeredHBox(int spacing, Button... buttons) {
        HBox box = new HBox(spacing, buttons);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Stage dialogStage(String title) {
        Stage dialog = new Stage();
        dialog.setTitle(title);
        dialog.initOwner(primaryStage);
        return dialog;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    private String inputDialog(String title, String prompt) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(prompt);
        d.initOwner(primaryStage);
        return d.showAndWait().orElse(null);
    }

    private String inputDialogWithDefault(String title, String prompt, String defaultVal) {
        TextInputDialog d = new TextInputDialog(defaultVal);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(prompt);
        d.initOwner(primaryStage);
        return d.showAndWait().orElse(null);
    }
}