import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    private static final int HTTP_PORT = 4000;
    private static final int WS_PORT = 3001;
    private static final Map<String, String> documents = new ConcurrentHashMap<>(); // In-memory storage
    private static final String DEFAULT_VALUE = "";

    public static void main(String[] args) {
        new Thread(Main::startHttpServer).start();
        new Thread(Main::startWebSocketServer).start();
    }

    // Basic HTTP Server
    public static void startHttpServer() {
        try (ServerSocket serverSocket = new ServerSocket(HTTP_PORT)) {
            System.out.println("HTTP Server running on http://localhost:" + HTTP_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleHttpRequest(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleHttpRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            // Read the request
            String requestLine = in.readLine();
            if (requestLine != null && requestLine.startsWith("GET")) {
                // Send a basic HTTP response
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("Hello, World!");
                out.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Basic WebSocket Server
    public static void startWebSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(WS_PORT)) {
            System.out.println("WebSocket Server running on ws://localhost:" + WS_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleWebSocket(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleWebSocket(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                // Simulate WebSocket behavior
                if (line.contains("get-document")) {
                    String documentId = line.split(":")[1];
                    String document = documents.getOrDefault(documentId, DEFAULT_VALUE);
                    out.println("load-document:" + document);
                    out.flush();
                } else if (line.contains("save-document")) {
                    String[] parts = line.split(":");
                    String documentId = parts[1];
                    String data = parts[2];
                    documents.put(documentId, data);
                    out.println("document-saved");
                    out.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
