import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class Application {

    private static final String BASE_URL = "https://api.weather.yandex.ru/v2/forecast";

    private static double LATITUDE ;
    private static double LONGITUDE;
    private static int LIMIT;
    private static String KEY;

    static {
        Properties props = new Properties();
        ClassLoader classLoader = Application.class.getClassLoader();
        try (InputStream iStream = classLoader.getResourceAsStream("application.properties")) {
            props.load(iStream);
            KEY = props.getProperty("yandex.weather.api.key");
            LATITUDE = Double.parseDouble(props.getProperty("yandex.weather.api.latitude"));
            LONGITUDE = Double.parseDouble(props.getProperty("yandex.weather.api.longitude"));
            LIMIT = Integer.parseInt(props.getProperty("yandex.weather.api.limit"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String fullUrl = String.format("%s?lat=%s&lon=%s&limit=%s", BASE_URL, LATITUDE, LONGITUDE, LIMIT);

        String jsonResponse = sendRequestWithParams(fullUrl, KEY);
        System.out.println("Полный JSON-ответ от сервиса:");
        System.out.println(jsonResponse);

        JsonNode rootNode = getRootNode(jsonResponse);

        int currentTemp = getCurrentTemperature(rootNode);
        System.out.println("Текущая температура: " + currentTemp + "°C");

        double avgTemp = getAverageTemperatureForNDays(rootNode, LIMIT);
        System.out.printf("Средняя температура по прогнозу за %d дней: %.2f°C%n", LIMIT, avgTemp);
    }

    public static String sendRequestWithParams(String fullUrl, String key) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(fullUrl))
                    .header("X-Yandex-Weather-Key", key)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JsonNode getRootNode(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getCurrentTemperature(JsonNode rootNode) {
        JsonNode factNode = rootNode.path("fact");
        JsonNode tempNode = factNode.path("temp");
        return tempNode.asInt();
    }

    public static double getAverageTemperatureForNDays(JsonNode rootNode, int n) {
        JsonNode forecastsNode = rootNode.path("forecasts");
        double tempAvgSum = 0;
        for (JsonNode forecast : forecastsNode) {
            JsonNode partsNode = forecast.path("parts");
            JsonNode dayNode = partsNode.path("day");
            JsonNode tempAvgNode = dayNode.path("temp_avg");
            tempAvgSum += tempAvgNode.asDouble();
        }
        return tempAvgSum / n;
    }
}
