package Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class AiEventService {

    private static final String API_KEY = System.getenv("GROQ_SUMMARY_API");
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public String generate(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Missing GROQ API KEY");
        }

        // Build JSON safely
        ObjectNode root = mapper.createObjectNode();
        root.put("model", "llama-3.1-8b-instant");
        root.put("temperature", 0.4);

        ArrayNode messages = root.putArray("messages");

        ObjectNode sys = mapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content",
                "You are an assistant that generates neutral factual text about events unless explicitly asked for promotional text. " +
                        "Do NOT include names or dates unless they are explicitly provided in the prompt. " +
                        "Do NOT invent details. Keep it short and useful."
        );

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", prompt);

        messages.add(sys);
        messages.add(user);

        String json = mapper.writeValueAsString(root);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() >= 300) {
            throw new RuntimeException("Groq error " + res.statusCode() + ": " + res.body());
        }

        // Parse response safely
        JsonNode body = mapper.readTree(res.body());
        JsonNode content = body.at("/choices/0/message/content");

        if (content.isMissingNode()) {
            throw new RuntimeException("Could not parse AI response: choices[0].message.content missing");
        }

        return content.asText().trim();
    }
}