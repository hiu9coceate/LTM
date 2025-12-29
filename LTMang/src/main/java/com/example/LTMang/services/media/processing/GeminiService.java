package com.example.LTMang.services.media.processing;

import com.example.LTMang.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class GeminiService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    public String askGemini(String base64Image, String userQuestion) {
        try {
            
            String systemPrompt = "hãy trả lời bằng tiếng việt."; 
            try {
                Path path = Path.of(AppConfig.PROMPT_FILE_PATH);
                if (Files.exists(path)) {
                    systemPrompt = Files.readString(path);
                } else {
                    System.err.println("⚠️ Không tìm thấy file prompt tại: " + AppConfig.PROMPT_FILE_PATH);
                }
            } catch (Exception e) { e.printStackTrace(); }

            String finalPrompt = systemPrompt + "\n\n" + (userQuestion != null ? "Câu hỏi: " + userQuestion : "Mô tả ảnh này.");
            JSONObject textPart = new JSONObject().put("text", finalPrompt);
            JSONObject imagePart = new JSONObject().put("inline_data", new JSONObject()
                    .put("mime_type", "image/jpeg")
                    .put("data", base64Image));
            
            JSONArray parts = new JSONArray().put(textPart).put(imagePart);
            JSONObject content = new JSONObject().put("parts", parts);
            JSONObject payload = new JSONObject().put("contents", new JSONArray().put(content));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?key=" + AppConfig.GEMINI_API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject resJson = new JSONObject(response.body());
                return resJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                return "Lỗi API (" + response.statusCode() + "): " + response.body();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi Server Java: " + e.getMessage();
        }
    }
}

