package services.community;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AiSummaryService {

    private static final String API_KEY;
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    static {
        Properties props = new Properties();
        try (InputStream is = AiSummaryService.class.getResourceAsStream("/secrets.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("Could not load secrets.properties: " + e.getMessage());
        }
        API_KEY = props.getProperty("groq.api_key");
    }

    private final HttpClient http = HttpClient.newHttpClient();

    public String summarize(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Missing GROQ API KEY");
        }

        String json = """
        {
          "model": "llama-3.1-8b-instant",
          "messages": [
            {"role":"system","content":"You summarize community threads. Output ONLY the final summary text (1-2 sentences). Never include headings/labels/topic/key points, never echo the input. Never output usernames/firstnames or dates/timestamps. make a clear , friendly and not professional Summary, take in consideration that comments can contain Tunisian Text. COMMENTS_COUNT is authoritative: if COMMENTS_COUNT > 0, you MUST summarize and you MUST NOT say 'No discussion yet'. If COMMENTS_COUNT = 0, output exactly: No discussion yet."},
            {"role":"user","content": %s}
          ],
          "temperature": 0.2
        }
        """.formatted(toJsonString(prompt));

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

        return extractContent(res.body()).trim();
    }

    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    // ✅ robust JSON string parsing for choices[0].message.content
    private static String extractContent(String body) {
        String marker = "\"content\":";
        int idx = body.indexOf(marker);
        if (idx < 0) throw new RuntimeException("Could not parse AI response: content missing");

        // move to first quote after "content":
        int i = body.indexOf("\"", idx + marker.length());
        if (i < 0) throw new RuntimeException("Could not parse AI response: content quote missing");

        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int p = i + 1; p < body.length(); p++) {
            char ch = body.charAt(p);
            if (esc) {
                // handle escapes
                if (ch == 'n') out.append('\n');
                else if (ch == 't') out.append('\t');
                else out.append(ch);
                esc = false;
            } else {
                if (ch == '\\') esc = true;
                else if (ch == '"') break; // end of JSON string
                else out.append(ch);
            }
        }
        return out.toString();
    }
}
