package bot.data;

import bot.model.Candle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches daily BTCUSDT candles from Binance's public REST API and caches
 * them to a CSV file so backtests run against a frozen, reproducible dataset.
 */
public class CandleLoader {

    // Binance.com blocks US-region IPs (HTTP 451); Binance.US mirrors the
    // same REST shape and isn't geo-restricted for this sandbox/Codespace.
    private static final String BINANCE_URL = "https://api.binance.us/api/v3/klines";
    // 2017-08-17T00:00:00Z, the day BTCUSDT started trading on Binance.com.
    // Binance.US only has BTCUSDT data from ~2019-09-23 onward; requesting
    // from this earlier date just means the first page naturally starts at
    // whatever the earliest available candle is.
    private static final long HISTORY_START_MS = 1502928000000L;
    private static final int PAGE_LIMIT = 1000;
    private static final Path CSV_PATH = Path.of("data", "btc_daily.csv");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<Candle> loadOrFetch() throws IOException, InterruptedException {
        if (Files.exists(CSV_PATH)) {
            System.out.println("Found " + CSV_PATH + " — loading cached dataset instead of hitting the API.");
            return loadFromCsv();
        }
        List<Candle> candles = fetchAll();
        writeCsv(candles);
        return candles;
    }

    /**
     * Binance caps each response at 1000 candles, so we page through history
     * by repeatedly requesting from a startTime and advancing it to just
     * after the last candle we received. A page shorter than PAGE_LIMIT
     * means we've reached the present and can stop.
     */
    private List<Candle> fetchAll() throws IOException, InterruptedException {
        List<Candle> all = new ArrayList<>();
        long startTime = HISTORY_START_MS;
        long now = System.currentTimeMillis();

        while (startTime < now) {
            List<Candle> page = fetchPage(startTime);
            if (page.isEmpty()) {
                break;
            }
            all.addAll(page);

            long lastOpenTime = page.get(page.size() - 1).timestamp();
            startTime = lastOpenTime + 1;

            if (page.size() < PAGE_LIMIT) {
                break;
            }
            Thread.sleep(250); // be polite to the public API's rate limit
        }
        return all;
    }

    private List<Candle> fetchPage(long startTime) throws IOException, InterruptedException {
        String url = String.format(
                "%s?symbol=BTCUSDT&interval=1d&startTime=%d&limit=%d",
                BINANCE_URL, startTime, PAGE_LIMIT);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Binance API error " + response.statusCode() + ": " + response.body());
        }
        return parseKlines(response.body());
    }

    /**
     * A kline response looks like:
     *   [[1502928000000,"4261.48000000","4485.39000000",...], [ ...next candle... ]]
     * Every value we need is either a bare long or a quoted decimal number —
     * no nested objects, no escaped characters — so a full JSON parser is
     * overkill. We strip the outer [ ], split the remaining text on the
     * "],[" boundary between rows, then split each row on commas.
     */
    private List<Candle> parseKlines(String json) {
        List<Candle> candles = new ArrayList<>();
        String trimmed = json.trim();
        if (trimmed.equals("[]") || trimmed.isEmpty()) {
            return candles;
        }

        trimmed = trimmed.substring(1, trimmed.length() - 1); // drop outer [ ]
        String[] rows = trimmed.split("\\]\\s*,\\s*\\[");

        for (String row : rows) {
            String cleaned = row.replace("[", "").replace("]", "");
            String[] fields = cleaned.split(",");

            long openTime = Long.parseLong(fields[0].trim());
            double open = parseField(fields[1]);
            double high = parseField(fields[2]);
            double low = parseField(fields[3]);
            double close = parseField(fields[4]);
            double volume = parseField(fields[5]);

            candles.add(new Candle(openTime, open, high, low, close, volume));
        }
        return candles;
    }

    private double parseField(String field) {
        return Double.parseDouble(field.trim().replace("\"", ""));
    }

    private void writeCsv(List<Candle> candles) throws IOException {
        Files.createDirectories(CSV_PATH.getParent());
        StringBuilder sb = new StringBuilder("timestamp,open,high,low,close,volume\n");
        for (Candle c : candles) {
            sb.append(c.timestamp()).append(',')
              .append(c.open()).append(',')
              .append(c.high()).append(',')
              .append(c.low()).append(',')
              .append(c.close()).append(',')
              .append(c.volume()).append('\n');
        }
        Files.writeString(CSV_PATH, sb.toString(), StandardCharsets.UTF_8);
    }

    private List<Candle> loadFromCsv() throws IOException {
        List<Candle> candles = new ArrayList<>();
        for (String line : Files.readAllLines(CSV_PATH)) {
            if (line.isBlank() || line.startsWith("timestamp")) {
                continue;
            }
            String[] p = line.split(",");
            candles.add(new Candle(
                    Long.parseLong(p[0]),
                    Double.parseDouble(p[1]),
                    Double.parseDouble(p[2]),
                    Double.parseDouble(p[3]),
                    Double.parseDouble(p[4]),
                    Double.parseDouble(p[5])));
        }
        return candles;
    }

    public static void main(String[] args) throws Exception {
        List<Candle> candles = new CandleLoader().loadOrFetch();

        if (candles.isEmpty()) {
            System.out.println("No candles loaded.");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
        String first = fmt.format(Instant.ofEpochMilli(candles.get(0).timestamp()));
        String last = fmt.format(Instant.ofEpochMilli(candles.get(candles.size() - 1).timestamp()));

        System.out.printf("Saved %d daily candles (%s to %s) to %s%n",
                candles.size(), first, last, CSV_PATH);
    }
}
