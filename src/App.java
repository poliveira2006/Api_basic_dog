import api.DogApi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class App {

    private static final int PORT = 8080;
    private static final Path WEB_ROOT =
            Paths.get(System.getProperty("user.dir"), "lib", "web").toAbsolutePath().normalize();

    private static final DogApi API = new DogApi();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/breeds", App::handleBreeds);
        server.createContext("/api/image",  App::handleImage);
        server.createContext("/",            App::handleStatic);

        server.setExecutor(null);
        server.start();

        System.out.println("Servidor em http://localhost:" + PORT);
        System.out.println("Servindo arquivos de " + WEB_ROOT);
    }

    /* ---------- API ---------- */

    /** GET /api/breeds → ["husky", "afghan hound", ...] */
    private static void handleBreeds(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "text/plain", "Método não permitido");
            return;
        }

        try {
            Map<String, List<String>> raw = API.allBreeds();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
                String breed = entry.getKey();
                List<String> subs = entry.getValue();

                if (subs == null || subs.isEmpty()) {
                    if (!first) json.append(',');
                    json.append('"').append(escape(breed)).append('"');
                    first = false;
                } else {
                    for (String sub : subs) {
                        if (!first) json.append(',');
                        json.append('"').append(escape(sub)).append(' ')
                            .append(escape(breed)).append('"');
                        first = false;
                    }
                }
            }
            json.append(']');

            send(ex, 200, "application/json; charset=utf-8", json.toString());

        } catch (Exception e) {
            send(ex, 500, "application/json; charset=utf-8",
                    "{\"error\":\"falha ao listar raças\"}");
        }
    }

    /** GET /api/image?breed=husky[&sub=siberian] → { "url": "..." } */
    private static void handleImage(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            send(ex, 405, "text/plain", "Método não permitido");
            return;
        }

        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
        String breed = query.get("breed");
        String sub   = query.get("sub");

        if (breed == null || breed.isBlank()) {
            send(ex, 400, "application/json; charset=utf-8",
                    "{\"error\":\"parâmetro breed ausente\"}");
            return;
        }

        try {
            // Se tiver sub-raça, a DogApi já aceita via allBreeds + getImage
            // Aqui usamos apenas o breed principal — o JS passa o "breed" correto.
            String url = API.getImage(breed);

            String body = "{\"url\":\"" + escape(url) + "\"}";
            send(ex, 200, "application/json; charset=utf-8", body);

        } catch (Exception e) {
            send(ex, 500, "application/json; charset=utf-8",
                    "{\"error\":\"falha ao buscar imagem\"}");
        }
    }

    /* ---------- estáticos ---------- */

    private static void handleStatic(HttpExchange ex) throws IOException {
        String rawPath = ex.getRequestURI().getPath();

        if (rawPath.equals("/") || rawPath.isEmpty()) {
            rawPath = "/index.html";
        }

        Path target = WEB_ROOT.resolve(rawPath.substring(1)).normalize();

        // impede path traversal (../)
        if (!target.startsWith(WEB_ROOT) || !Files.isRegularFile(target)) {
            send(ex, 404, "text/plain; charset=utf-8", "404");
            return;
        }

        byte[] bytes = Files.readAllBytes(target);
        String mime = mimeOf(target.getFileName().toString());

        ex.getResponseHeaders().set("Content-Type", mime);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String mimeOf(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css"))  return "text/css; charset=utf-8";
        if (n.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".svg"))  return "image/svg+xml";
        if (n.endsWith(".png"))  return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    /* ---------- utilitários ---------- */

    private static void send(HttpExchange ex, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;

        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            if (i < 0) {
                map.put(decode(pair), "");
            } else {
                map.put(decode(pair.substring(0, i)), decode(pair.substring(i + 1)));
            }
        }
        return map;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}