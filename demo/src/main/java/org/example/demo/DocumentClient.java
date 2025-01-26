package org.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class DocumentClient extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8081;

    private String userId;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private TextArea textArea;
    private DocumentState localState;
    private boolean isLocalChange = false;

    @Override
    public void start(Stage primaryStage) {
        userId = UUID.randomUUID().toString();
        localState = new DocumentState();

        // Create UI
        VBox root = new VBox(10);
        MenuBar menuBar = createMenuBar(primaryStage);
        ToolBar toolBar = createToolBar();
        textArea = new TextArea();
        textArea.setWrapText(true);

        root.getChildren().addAll(menuBar, toolBar, textArea);

        // Set up text area listeners
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!isLocalChange) {
                handleTextChange(oldText, newText);
            }
        });

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Collaborative Document Editor");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Connect to server
        connectToServer();
    }

    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem loadItem = new MenuItem("Load");

        saveItem.setOnAction(e -> saveDocument(primaryStage));
        loadItem.setOnAction(e -> loadDocument(primaryStage));

        fileMenu.getItems().addAll(saveItem, loadItem);
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private ToolBar createToolBar() {
        ToolBar toolBar = new ToolBar();
        ToggleButton boldBtn = new ToggleButton("B");
        ToggleButton italicBtn = new ToggleButton("I");
        ToggleButton underlineBtn = new ToggleButton("U");

        boldBtn.setOnAction(e -> applyStyle(EditMessage.TextStyle.BOLD));
        italicBtn.setOnAction(e -> applyStyle(EditMessage.TextStyle.ITALIC));
        underlineBtn.setOnAction(e -> applyStyle(EditMessage.TextStyle.UNDERLINE));

        toolBar.getItems().addAll(boldBtn, italicBtn, underlineBtn);
        return toolBar;
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Start message listener thread
            new Thread(this::listenForMessages).start();

            // Send connect message
            sendMessage(new EditMessage(userId, EditMessage.MessageType.CONNECT, "", 0, null));
        } catch (IOException e) {
            showError("Could not connect to server");
        }
    }

    private void listenForMessages() {
        try {
            while (true) {
                EditMessage message = (EditMessage) in.readObject();
                Platform.runLater(() -> handleMessage(message));
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> showError("Lost connection to server"));
        }
    }

    private void handleMessage(EditMessage message) {
        switch (message.getType()) {
            case EDIT:
                localState.applyEdit(message);
                updateTextArea();
                break;
            case CURSOR_MOVE:
                updateUserCursor(message.getUserId(), message.getPosition());
                break;
        }
    }

    private void handleTextChange(String oldText, String newText) {
        int caretPosition = textArea.getCaretPosition();

        // More robust change detection
        if (!Objects.equals(oldText, newText)) {
            int start = findDifferenceStart(oldText, newText);
            int end = findDifferenceEnd(oldText, newText);

            String changedText = newText.substring(start, newText.length());
            EditMessage message = new EditMessage(
                    userId,
                    changedText.isEmpty() ? EditMessage.MessageType.EDIT : EditMessage.MessageType.EDIT,
                    changedText,
                    start,
                    EditMessage.TextStyle.NORMAL);

            sendMessage(message);
        }
    }

    private int findDifferenceEnd(String oldText, String newText) {
        int oldLen = oldText.length();
        int newLen = newText.length();

        for (int i = 1; i <= Math.min(oldLen, newLen); i++) {
            if (oldText.charAt(oldLen - i) != newText.charAt(newLen - i)) {
                return newLen - i + 1;
            }
        }
        return Math.min(oldLen, newLen);
    }

    private int findDifferenceStart(String oldText, String newText) {
        int minLength = Math.min(oldText.length(), newText.length());
        for (int i = 0; i < minLength; i++) {
            if (oldText.charAt(i) != newText.charAt(i)) {
                return i;
            }
        }
        return minLength;
    }

    private void sendMessage(EditMessage message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            showError("Failed to send message to server");
        }
    }

    private void updateTextArea() {
        String content = localState.getContent();
        if (!textArea.getText().equals(content)) {
            isLocalChange = true;
            textArea.setText(content);
            isLocalChange = false;
        }
    }

    private void updateUserCursor(String userId, int position) {
        // Implementation for showing other users' cursors
        // This would involve using TextArea's overlays or a custom control
    }

    private void applyStyle(EditMessage.TextStyle style) {
        int start = textArea.getSelection().getStart();
        int end = textArea.getSelection().getEnd();
        if (start != end) {
            EditMessage message = new EditMessage(userId, EditMessage.MessageType.STYLE_CHANGE, "", start, style);
            sendMessage(message);
        }
    }

    private void saveDocument(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Document");
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textArea.getText());
            } catch (IOException e) {
                showError("Failed to save document");
            }
        }
    }

    private void loadDocument(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Document");
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                isLocalChange = true;
                textArea.setText(content.toString());
                isLocalChange = false;
            } catch (IOException e) {
                showError("Failed to load document");
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                sendMessage(new EditMessage(userId, EditMessage.MessageType.DISCONNECT, "", 0, null));
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}