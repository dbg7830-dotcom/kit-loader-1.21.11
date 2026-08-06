package com.kitmod.util;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
public class HttpUtil {
    public static final String WORKER_BASE = "https://kitstore.chinpatakdamdampro.workers.dev";
    public static final String RAW_BASE = "https://raw.githubusercontent.com/vulgarmc/kitmod-community/main";
    public static CompletableFuture<String> get(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection c = open(url);
                c.setRequestMethod("GET"); c.setConnectTimeout(8000); c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent", "KitSaverPlus/1.0");
                return c.getResponseCode() < 300 ? read(c.getInputStream()) : null;
            } catch (Exception e) { return null; }
        });
    }
    public static CompletableFuture<String> postJson(String url, String json) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpURLConnection c = open(url);
                c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(20000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("User-Agent", "KitSaverPlus/1.0");
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                c.setRequestProperty("Content-Length", String.valueOf(body.length));
                try (OutputStream os = c.getOutputStream()) { os.write(body); }
                int code = c.getResponseCode();
                InputStream is = code < 300 ? c.getInputStream() : c.getErrorStream();
                return is != null ? read(is) : String.valueOf(code);
            } catch (Exception e) { return "error: " + e.getMessage(); }
        });
    }
    private static HttpURLConnection open(String url) throws Exception {
        return (HttpURLConnection) URI.create(url).toURL().openConnection();
    }
    private static String read(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        }
    }
}
