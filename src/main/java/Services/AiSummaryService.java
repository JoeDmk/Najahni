package Services;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

public class AiSummaryService {

    private static final String API_KEY = System.getenv("GROQ_SUMMARY_API");
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient http = HttpClient.newHttpClient();

    public String summarize(String prompt) throws Exception {
        if (API_KEY == null || API_KEY.isBlank()) {
            throw new IllegalStateException("Missing GROQ API KEY");
        }

        String json = """
        {
          "model": "llama-3.1-8b-instant",
          "messages": [
            {"role":"system","content":"You are a summarization engine. Never output usernames or dates. Never quote verbatim."},
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
