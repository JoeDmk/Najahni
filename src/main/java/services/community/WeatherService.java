package services.community;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherService {

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static class WeatherInfo {
        public final double tMin;
        public final double tMax;
        public final int rainProbMax;
        public final int weatherCode;

        public WeatherInfo(double tMin, double tMax, int rainProbMax, int weatherCode) {
            this.tMin = tMin;
            this.tMax = tMax;
            this.rainProbMax = rainProbMax;
            this.weatherCode = weatherCode;
        }
    }

    // Open-Meteo forecast = max ~16 days ahead
    public WeatherInfo getDaily(LocalDate date, double lat, double lon) throws Exception {

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode"
                + "&start_date=" + date
                + "&end_date=" + date
                + "&timezone=auto";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new RuntimeException("Weather API error: HTTP " + res.statusCode());
        }

        JsonNode root = mapper.readTree(res.body());
        JsonNode daily = root.get("daily");

        // index 0 car on a demandé 1 seule journée
        double tMax = daily.get("temperature_2m_max").get(0).asDouble();
        double tMin = daily.get("temperature_2m_min").get(0).asDouble();
        int rain = daily.get("precipitation_probability_max").get(0).asInt();
        int code = daily.get("weathercode").get(0).asInt();

        return new WeatherInfo(tMin, tMax, rain, code);
    }

    // Petite traduction du weathercode (version minimale)
    public static String labelFromCode(int code) {
        if (code == 0) return "☀️ Clear";
        if (code == 1 || code == 2) return "🌤 Partly cloudy";
        if (code == 3) return "☁️ Overcast";
        if (code >= 51 && code <= 67) return "🌧 Drizzle/Rain";
        if (code >= 71 && code <= 77) return "❄️ Snow";
        if (code >= 80 && code <= 82) return "🌦 Showers";
        if (code >= 95) return "⛈ Thunderstorm";
        return "🌡 Weather";
    }
}