package bot.data;

import bot.model.Candle;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads BTC/USDT daily candles.
 * - If data/btc_daily.csv already exists, reads from that (fast, reproducible).
 * - Otherwise, fetches from Binance.US public API (no key needed) and caches to CSV.
 */
public class CandleLoader {

    private static final String CSV_PATH = "data/btc_daily.csv";
    private static final String BASE_URL = "https://api.binance.us/api/v3/klines";

    public List<Candle> loadCandles() throws IOException, InterruptedException {
        File csvFile = new File(CSV_PATH);

        if (csvFile.exists()) {
            return readFromCsv(csvFile);
        } else {
            List<Candle> candles = fetchFromBinance();
            writeToCsv(candles);
            return candles;
        }
    }

    // ---------- Reading cached CSV ----------

    private List<Candle> readFromCsv(File file) throws IOException {
        List<Candle> candles = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        // line 0 is the header, so skip it
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            long timestamp = Long.parseLong(parts[0]);
            double open = Double.parseDouble(parts[1]);
            double high = Double.parseDouble(parts[2]);
            double low = Double.parseDouble(parts[3]);
            double close = Double.parseDouble(parts[4]);
            double volume = Double.parseDouble(parts[5]);

            candles.add(new Candle(timestamp, open, high, low, close, volume));
        }
        return candles;
    }

    // ---------- Fetching fresh from Binance ----------

    private List<Candle> fetchFromBinance() throws IOException, InterruptedException {
        List<Candle> allCandles = new ArrayList<>();
        HttpClient client = HttpClient.newHttpClient();

        // Start from 2019-09-23 (Binance.US BTCUSDT history start)
        long startTime = LocalDate.of(2019, 9, 23)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();

        long now = Instant.now().toEpochMilli();

        while (startTime < now) {
            String url = BASE_URL
                    + "?symbol=BTCUSDT&interval=1d&limit=1000&startTime=" + startTime;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            List<Candle> page = parseKlinesJson(body);
            if (page.isEmpty()) {
                break; // no more data
            }

            allCandles.addAll(page);

            // advance startTime to just after the last candle we got
            long lastTimestamp = page.get(page.size() - 1).timestamp();
            startTime = lastTimestamp + 1;

            if (page.size() < 1000) {
                break; // short page means we've reached the end
            }
        }

        return allCandles;
    }

    /**
     * Binance returns something like:
     * [[1569196800000,"10125.30","10175.00","9950.00","10100.50","1234.5678", ... ], [ ... ], ...]
     * We only care about the first 6 fields of each inner array:
     * open time, open, high, low, close, volume.
     */
    private List<Candle> parseKlinesJson(String json) {
        List<Candle> candles = new ArrayList<>();

        // strip the outer [ and ]
        String trimmed = json.trim();
        trimmed = trimmed.substring(1, trimmed.length() - 1);

        // split into individual candle arrays on "],["
        String[] rawCandles = trimmed.split("\\],\\[");

        for (String raw : rawCandles) {
            // clean up leftover brackets and quotes
            String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
            String[] fields = cleaned.split(",");

            long timestamp = Long.parseLong(fields[0]);
            double open = Double.parseDouble(fields[1]);
            double high = Double.parseDouble(fields[2]);
            double low = Double.parseDouble(fields[3]);
            double close = Double.parseDouble(fields[4]);
            double volume = Double.parseDouble(fields[5]);

            candles.add(new Candle(timestamp, open, high, low, close, volume));
        }

        return candles;
    }

    // ---------- Writing to CSV cache ----------

    private void writeToCsv(List<Candle> candles) throws IOException {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_PATH))) {
            writer.write("timestamp,open,high,low,close,volume");
            writer.newLine();

            for (Candle c : candles) {
                writer.write(c.timestamp() + "," + c.open() + "," + c.high() + ","
                        + c.low() + "," + c.close() + "," + c.volume());
                writer.newLine();
            }
        }
    }

    // Standalone runner, so you can regenerate the CSV any time with:
    // mvn compile exec:java -Dexec.mainClass=bot.data.CandleLoader
    public static void main(String[] args) throws Exception {
        CandleLoader loader = new CandleLoader();
        List<Candle> candles = loader.loadCandles();
        System.out.println("Loaded " + candles.size() + " candles.");
        if (!candles.isEmpty()) {
            System.out.println("First: " + candles.get(0));
            System.out.println("Last: " + candles.get(candles.size() - 1));
        }
    }
}