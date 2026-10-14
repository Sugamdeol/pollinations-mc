package dev.sugamdeol.pollinations;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.map.MapPalette;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.logging.Logger;

/**
 * Minimal async client for the Pollinations API (https://gen.pollinations.ai).
 * Zero external dependencies: plain java.net.http.
 */
public class ApiClient {

    private final String baseUrl;
    private final String apiKey;
    private final String textModel;
    private final String imageModel;
    private final Logger log;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public ApiClient(String baseUrl, String apiKey, String textModel, String imageModel, Logger log) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://gen.pollinations.ai" : baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.textModel = textModel == null || textModel.isBlank() ? "openai/gpt-5.4-nano" : textModel;
        this.imageModel = imageModel == null || imageModel.isBlank() ? "black-forest-labs/flux.1-schnell" : imageModel;
        this.log = log;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Generate text via POST /v1/chat/completions. */
    public CompletableFuture<String> generateText(String prompt) {
        String body = "{\"model\":\"" + jsonEscape(textModel) + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}]}";
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        return send(rb, resp -> {
            String json = new String(resp.body(), StandardCharsets.UTF_8);
            String content = extractStringAfterKey(json, "\"content\":\"");
            if (content == null) {
                throw new IllegalStateException("Unexpected API response: " + truncate(json, 200));
            }
            return unescapeJson(content);
        });
    }

    /** Generate an image via GET /image/{prompt}?model=...&key=... and return it as a BufferedImage. */
    public CompletableFuture<BufferedImage> generateImage(String prompt) {
        String url = baseUrl + "/image/" + URLEncoder.encode(prompt, StandardCharsets.UTF_8)
                + "?model=" + URLEncoder.encode(imageModel, StandardCharsets.UTF_8)
                + "&width=512&height=512&nologo=true"
                + (apiKey.isBlank() ? "" : "&key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));

        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .GET();

        return send(rb, resp -> {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(resp.body()));
            if (img == null) {
                // Probably a JSON error body
                String err = new String(resp.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Image API error: " + truncate(err, 200));
            }
            return img;
        });
    }

    /** Resize to 128x128 for Minecraft maps, returns byte-indexed MapPalette colors. */
    public static byte[][] toMapColors(BufferedImage src) {
        BufferedImage dst = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, 128, 128, null);
        g.dispose();

        byte[][] colors = new byte[128][128];
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int rgb = dst.getRGB(x, y);
                colors[x][y] = MapPalette.matchColor(new java.awt.Color((rgb & 0xFFFFFF) | 0xFF000000, false));
            }
        }
        return colors;
    }

    // ---------- internals ----------

    private <T> CompletableFuture<T> send(HttpRequest.Builder rb, ThrowingBodyParser<T> parser) {
        CompletableFuture<T> future = new CompletableFuture<>();
        http.sendAsync(rb.build(), HttpResponse.BodyHandlers.ofByteArray()).whenComplete((resp, err) -> {
            if (err != null) {
                future.completeExceptionally(new IllegalStateException("Network error: " + err.getMessage()));
                return;
            }
            int status = resp.statusCode();
            if (status == 401) {
                future.completeExceptionally(new IllegalStateException("Invalid API key (401). Set api-key in config.yml."));
                return;
            }
            if (status == 402) {
                future.completeExceptionally(new IllegalStateException("Insufficient Pollen balance (402). Top up at enter.pollinations.ai."));
                return;
            }
            if (status >= 400) {
                String body = new String(resp.body(), StandardCharsets.UTF_8);
                future.completeExceptionally(new IllegalStateException("HTTP " + status + ": " + truncate(extractStringAfterKey(body, "\"message\":\""), 200)));
                return;
            }
            try {
                future.complete(parser.parse(resp));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private interface ThrowingBodyParser<T> {
        T parse(HttpResponse<byte[]> resp) throws Exception;
    }

    /** Tiny JSON helpers avoiding a dependency. */
    private static String extractStringAfterKey(String json, String key) {
        int i = json == null ? -1 : json.indexOf(key);
        if (i < 0) return null;
        int start = i + key.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\' && end + 1 < json.length()) end++;
            end++;
        }
        return json.substring(start, end);
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\").replace("\\t", "\t");
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(' '); else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\n", " ");
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}