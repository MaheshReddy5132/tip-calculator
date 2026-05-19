import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TipCalculatorServer {
    private static final int PORT = 8000;
    private static final Path FRONTEND_DIR = resolveFrontendDir();

    private static Path resolveFrontendDir() {
        try {
            Path codePath = Paths.get(TipCalculatorServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(codePath)) {
                codePath = codePath.getParent();
            }
            return codePath.resolveSibling("frontend").toAbsolutePath().normalize();
        } catch (URISyntaxException e) {
            return Paths.get(System.getProperty("user.dir")).resolve("frontend").toAbsolutePath().normalize();
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/calculate", new CalculateHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(null);

        System.out.println("Tip calculator backend running at http://localhost:" + PORT);
        System.out.println("Open http://localhost:" + PORT + " in your browser.");
        System.out.println("Frontend directory: " + FRONTEND_DIR);
        server.start();
    }

    static class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            double totalBill;
            double tipPercent;
            int people;

            try {
                totalBill = parseJsonDouble(requestBody, "bill");
                tipPercent = parseJsonDouble(requestBody, "tip");
                people = parseJsonInt(requestBody, "people");
            } catch (IllegalArgumentException ex) {
                String response = "{\"error\": \"Invalid request data\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }

            TipCalculator calculator = new TipCalculator(totalBill, tipPercent, people);
            double tipAmount = calculator.calculateTipAmount();
            double totalWithTip = calculator.calculateTotalWithTip();
            double eachPays = calculator.calculateEachPays();

            String jsonResponse = String.format(
                    "{\"tipAmount\": %.2f, \"totalWithTip\": %.2f, \"eachPays\": %.2f}",
                    tipAmount, totalWithTip, eachPays);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private double parseJsonDouble(String body, String key) {
            String value = parseJsonValue(body, key);
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }

        private int parseJsonInt(String body, String key) {
            String value = parseJsonValue(body, key);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }

        private String parseJsonValue(String body, String key) {
            String search = "\"" + key + "\"";
            int index = body.indexOf(search);
            if (index < 0) {
                throw new IllegalArgumentException();
            }
            int colon = body.indexOf(':', index);
            if (colon < 0) {
                throw new IllegalArgumentException();
            }
            int start = colon + 1;
            while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
                start++;
            }
            int end = start;
            while (end < body.length() && ",}".indexOf(body.charAt(end)) == -1) {
                end++;
            }
            return body.substring(start, end).trim();
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path filePath = FRONTEND_DIR.resolve(requestPath.substring(1)).normalize();
            if (!filePath.startsWith(FRONTEND_DIR) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String contentType = URLConnection.guessContentTypeFromName(filePath.toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }
}
