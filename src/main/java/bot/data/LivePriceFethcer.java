package bot.data;

import bot.model.Candle;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


// public class that allows us to fetch data from the binace to mdoel trades 
public class LivePriceFethcer {
    public Candle fetchLatCandle() throws Exception {
    String url = "https://api.binance.us/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=1";

    HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return parseCandle(response.body());
    }

    // Binance returns something like [[1622825400000,"36622.72","36700.00","36500.00","36610.50","1234.56", ...]]
    // we only care about the first 6 fields of the single inner array: open time, open, high, low, close, volume.
    private Candle parseCandle(String json) {
        String trimmed = json.trim();
        trimmed = trimmed.substring(1, trimmed.length() - 1); // strip outer [ ]
        trimmed = trimmed.substring(1, trimmed.length() - 1); // strip inner [ ]

        String cleaned = trimmed.replace("\"", "");
        String[] fields = cleaned.split(",");

        long timestamp = Long.parseLong(fields[0]);
        double open = Double.parseDouble(fields[1]);
        double high = Double.parseDouble(fields[2]);
        double low = Double.parseDouble(fields[3]);
        double close = Double.parseDouble(fields[4]);
        double volume = Double.parseDouble(fields[5]);

        return new Candle(timestamp, open, high, low, close, volume);
    }
}