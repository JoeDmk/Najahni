package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * GoogleOAuthService handles the "Continue with Google" OAuth 2.0 flow
 * for this JavaFX desktop application.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  HOW IT WORKS — STEP BY STEP:
 * ═══════════════════════════════════════════════════════════════════
 *
 *  1. SETUP: You need a Google Cloud project with OAuth 2.0 credentials
 *     configured as a "Desktop app". Get CLIENT_ID and CLIENT_SECRET from:
 *     https://console.cloud.google.com/apis/credentials
 *
 *  2. FLOW:
 *     a) User clicks "Continuer avec Google" on the login page.
 *     b) We start a tiny local HTTP server on port 8888 to receive the callback.
 *     c) A WebView window opens showing Google's login page.
 *     d) User signs in with their Google account and grants permission.
 *     e) Google redirects to http://localhost:8888/callback?code=XXXXX
 *     f) Our local server catches the authorization code.
 *     g) We exchange the code for an access token via Google's token endpoint.
 *     h) We use the access token to fetch the user's profile (email, name, picture).
 *     i) We return a GoogleUserInfo object to the controller.
 *
 *  3. The controller then checks if a user with that email exists in the DB:
 *     - If YES → log them in directly.
 *     - If NO  → auto-create a new account with their Google info.
 *
 * ═══════════════════════════════════════════════════════════════════
 *  SETUP INSTRUCTIONS:
 * ═══════════════════════════════════════════════════════════════════
 *
 *  1. Go to https://console.cloud.google.com/
 *  2. Create a new project (or select existing one)
 *  3. Navigate to APIs & Services > Credentials
 *  4. Click "Create Credentials" > "OAuth client ID"
 *  5. Application type: "Web application"
 *  6. Add authorized redirect URI: http://localhost:8888/callback
 *  7. Copy the Client ID and Client Secret below
 *  8. Enable the "Google People API" or "Google+ API" in the API Library
 */
public class GoogleOAuthService {

    // Credentials loaded from secrets.properties
    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;

    static {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = GoogleOAuthService.class.getResourceAsStream("/secrets.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("Could not load secrets.properties: " + e.getMessage());
        }
        CLIENT_ID = props.getProperty("google.client_id", "REPLACE_WITH_CLIENT_ID");
        CLIENT_SECRET = props.getProperty("google.client_secret", "REPLACE_WITH_CLIENT_SECRET");
    }

    // OAuth 2.0 endpoints (standard Google URLs, don't change these)
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    // Local callback server configuration
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final int CALLBACK_PORT = 8888;

    // Scopes: we request basic profile info and email
    private static final String SCOPES = "openid email profile";

    /**
     * A simple POJO holding the Google user's profile information
     * returned after a successful OAuth flow.
     */
    public static class GoogleUserInfo {
        private final String googleId;      // Google's unique user ID
        private final String email;         // User's Gmail address
        private final String firstName;     // Given name
        private final String lastName;      // Family name
        private final String pictureUrl;    // Profile picture URL

        public GoogleUserInfo(String googleId, String email, String firstName, String lastName, String pictureUrl) {
            this.googleId = googleId;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.pictureUrl = pictureUrl;
        }

        public String getGoogleId()   { return googleId; }
        public String getEmail()      { return email; }
        public String getFirstName()  { return firstName; }
        public String getLastName()   { return lastName; }
        public String getPictureUrl() { return pictureUrl; }

        @Override
        public String toString() {
            return "GoogleUserInfo{email='" + email + "', name='" + firstName + " " + lastName + "'}";
        }
    }

    /**
     * MAIN ENTRY POINT: Launches the Google Sign-In flow.
     *
     * Opens a WebView popup where the user logs into their Google account.
     * Returns a CompletableFuture that resolves to GoogleUserInfo on success,
     * or null if the user cancelled or an error occurred.
     *
     * @param ownerStage the parent stage (so the popup is modal)
     * @return CompletableFuture<GoogleUserInfo> with the user's Google profile
     */
    public CompletableFuture<GoogleUserInfo> signIn(Stage ownerStage) {
        CompletableFuture<GoogleUserInfo> future = new CompletableFuture<>();

        Platform.runLater(() -> {
            try {
                // ── Step 1: Start a local HTTP server to catch Google's redirect ──
                HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
                Stage webStage = new Stage();

                server.createContext("/callback", exchange -> {
                    // ── Step 2: Extract the authorization code from the redirect URL ──
                    String query = exchange.getRequestURI().getQuery();
                    String code = extractParam(query, "code");

                    // Show a success message in the browser
                    String response = "<html><body style='font-family:Segoe UI;text-align:center;padding:60px;'>"
                            + "<h1 style='color:#00B894;'>&#10003; Connexion réussie!</h1>"
                            + "<p>Vous pouvez fermer cette fenêtre.</p></body></html>";
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();

                    // Shut down the server — we only need it once
                    server.stop(0);

                    if (code != null) {
                        // ── Step 3: Exchange the auth code for an access token ──
                        // ── Step 4: Fetch user info with that token ──
                        // This runs on a background thread to avoid blocking the UI
                        CompletableFuture.runAsync(() -> {
                            try {
                                String accessToken = exchangeCodeForToken(code);
                                GoogleUserInfo userInfo = fetchUserInfo(accessToken);
                                future.complete(userInfo);
                            } catch (Exception e) {
                                e.printStackTrace();
                                future.complete(null);
                            }
                        });
                    } else {
                        future.complete(null);
                    }

                    // Close the WebView window
                    Platform.runLater(webStage::close);
                });

                server.setExecutor(null);
                server.start();
                System.out.println("OAuth callback server started on port " + CALLBACK_PORT);

                // ── Build the Google authorization URL ──
                String authorizationUrl = AUTH_URL
                        + "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                        + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                        + "&response_type=code"
                        + "&scope=" + URLEncoder.encode(SCOPES, StandardCharsets.UTF_8)
                        + "&access_type=offline"
                        + "&prompt=select_account";

                // ── Open a WebView with Google's login page ──
                WebView webView = new WebView();
                WebEngine webEngine = webView.getEngine();
                webEngine.load(authorizationUrl);

                webStage.setTitle("Connexion avec Google");
                webStage.initModality(Modality.WINDOW_MODAL);
                webStage.initOwner(ownerStage);
                webStage.setScene(new Scene(webView, 500, 650));

                // If user closes the window manually, clean up
                webStage.setOnCloseRequest(event -> {
                    server.stop(0);
                    if (!future.isDone()) {
                        future.complete(null);
                    }
                });

                webStage.show();

            } catch (IOException e) {
                System.err.println("Failed to start OAuth server: " + e.getMessage());
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    /**
     * Step 3: Exchange the authorization code for an access token.
     *
     * Sends a POST request to Google's token endpoint with:
     * - code (from the redirect)
     * - client_id, client_secret (our credentials)
     * - redirect_uri, grant_type
     *
     * Returns the access token string.
     */
    private String exchangeCodeForToken(String code) throws IOException {
        String params = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpURLConnection conn = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Send the POST body
        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        // Read the response
        String responseBody = readResponse(conn);
        System.out.println("Token response: " + responseBody);

        // Parse JSON to extract access_token
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("access_token")) {
            return json.get("access_token").getAsString();
        } else {
            throw new IOException("Failed to get access token: " + responseBody);
        }
    }

    /**
     * Step 4: Fetch the user's profile from Google's UserInfo endpoint.
     *
     * Sends a GET request with the access token in the Authorization header.
     * Parses the JSON response to extract: id, email, given_name, family_name, picture.
     */
    private GoogleUserInfo fetchUserInfo(String accessToken) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(USER_INFO_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        String responseBody = readResponse(conn);
        System.out.println("User info response: " + responseBody);

        // Parse the JSON response
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

        String googleId   = json.has("id")           ? json.get("id").getAsString()           : "";
        String email      = json.has("email")        ? json.get("email").getAsString()        : "";
        String firstName  = json.has("given_name")   ? json.get("given_name").getAsString()   : "";
        String lastName   = json.has("family_name")  ? json.get("family_name").getAsString()  : "";
        String pictureUrl = json.has("picture")      ? json.get("picture").getAsString()      : "";

        return new GoogleUserInfo(googleId, email, firstName, lastName, pictureUrl);
    }

    // ─────────────── Utility helpers ───────────────

    /**
     * Read the full response body from an HttpURLConnection.
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Extract a query parameter value from a URL query string.
     * Example: extractParam("code=abc123&scope=email", "code") → "abc123"
     */
    private String extractParam(String query, String param) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(param)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
