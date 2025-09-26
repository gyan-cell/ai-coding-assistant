package com.blade.aicoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {
  private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
  private static final String MODEL_NAME = "deepseek-coder-v2:16b"; // Using latest for better compatibility
  private static final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  private static final ObjectMapper mapper = new ObjectMapper();

  private FloatingWindow floatingWindow;

  public OllamaClient(FloatingWindow floatingWindow) {
    this.floatingWindow = floatingWindow;
  }

  public String sendMessage(String message) throws Exception {
    // Prepare the request payload using Jackson
    String requestBody = mapper.writeValueAsString(new OllamaRequest(MODEL_NAME, message, false));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(OLLAMA_URL))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();

    HttpResponse<String> response = httpClient.send(request,
        HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException("Ollama API error: " + response.statusCode() +
          " - " + response.body());
    }

    // Parse response
    JsonNode jsonResponse = mapper.readTree(response.body());
    return jsonResponse.get("response").asText();
  }

  // Simple POJO for Ollama request
  private static class OllamaRequest {
    public String model;
    public String prompt;
    public boolean stream;

    public OllamaRequest(String model, String prompt, boolean stream) {
      this.model = model;
      this.prompt = prompt;
      this.stream = stream;
    }
  }
}
