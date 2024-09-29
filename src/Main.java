import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    private static final String BASE_DIRECTORY = "public";
    private static final String LOG_DIRECTORY = "log";
    public static void main(String[] args) throws IOException {
        File logDir = new File(LOG_DIRECTORY);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Server started at http://localhost:8080");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            handleClientRequest(clientSocket);
        }
    }
    private static void handleClientRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        OutputStream outputStream = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        logRequest(requestLine);

        if (requestLine == null) {
            return;
        }

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 3 || !requestParts[0].equals("GET")) {
            return;
        }

        String requestedPath = requestParts[1];
        if (requestedPath.equals("/")) {
            requestedPath = "/index.html";
        }

        String filePath = BASE_DIRECTORY + requestedPath;
        File file = new File(filePath);

        if (file.exists() && !file.isDirectory()) {
            String contentType = getContentType(filePath);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + file.length());
            out.println("");
            out.flush();

            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                Files.copy(file.toPath(), bufferedOutputStream);
            }

            logMessage("Served file: " + requestedPath);

        } else {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Type: text/html");
            out.println("");
            out.println("<html><body><h1>404 Not Found</h1></body></html>");
            logMessage("File not found: " + requestedPath);
        }

        out.close();
        in.close();
        clientSocket.close();
    }

    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html") || filePath.endsWith(".htm")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".png")) {
            return "image/png";
        } else if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }
    private static void logRequest(String requestLine) {
        if (requestLine != null) {
            logMessage("Received request: " + requestLine);
        }
    }

    private static void logMessage(String message) {
        try {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String logFileName = LOG_DIRECTORY + "/" + date + ".log";
            FileWriter fw = new FileWriter(logFileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            out.println("[" + timestamp + "] " + message);
            out.close();
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}
