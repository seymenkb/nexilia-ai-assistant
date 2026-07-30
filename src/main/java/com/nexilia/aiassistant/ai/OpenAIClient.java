package com.nexilia.aiassistant.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nexilia.aiassistant.NexiliaAIAssistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * OpenAI Chat Completions API'sine dogrudan HTTP istegi atar.
 * Harici kutuphane gerektirmez (java.net.http + sunucuda hazir gelen Gson).
 */
public class OpenAIClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final NexiliaAIAssistant plugin;
    private final HttpClient httpClient;

    public OpenAIClient(NexiliaAIAssistant plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Async olarak GPT'ye istek atar. Cagiran taraf zaten async thread'de olmali (komut icinde).
     *
     * @param systemPrompt sistem talimati (format kurallari)
     * @param userContent  soru + ilgili dosya baglamlari
     * @return AI'nin ham metin cevabi
     */
    public CompletableFuture<String> ask(String systemPrompt, String userContent) {
        String apiKey = plugin.cfg().getString("openai-api-key", "");
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-xxxx")) {
            return CompletableFuture.failedFuture(new IllegalStateException("api-key-missing"));
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", plugin.cfg().getString("model", "gpt-4o-mini"));
        body.addProperty("max_tokens", plugin.cfg().getInt("max-tokens", 500));
        body.addProperty("temperature", plugin.cfg().getDouble("temperature", 0.2));

        JsonArray messages = new JsonArray();
        messages.add(chatMessage("system", systemPrompt));
        messages.add(chatMessage("user", userContent));
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse);
    }

    private JsonObject chatMessage(String role, String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("role", role);
        obj.addProperty("content", content);
        return obj;
    }

    private String parseResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            plugin.getLogger().log(Level.WARNING, "[NexAI] OpenAI API hatasi (" + response.statusCode() + "): " + response.body());
            throw new RuntimeException("openai-http-" + response.statusCode());
        }
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[NexAI] OpenAI cevabi parse edilemedi.", e);
            throw new RuntimeException("openai-parse-error", e);
        }
    }
}
