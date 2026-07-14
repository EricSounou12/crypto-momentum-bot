package bot.model;

/**
 * A single OHLCV candle.
 *
 * @param timestamp candle open time, in milliseconds since the Unix epoch
 * @param open      opening price
 * @param high      highest price during the period
 * @param low       lowest price during the period
 * @param close     closing price
 * @param volume    traded volume during the period
 */
public record Candle(long timestamp, double open, double high, double low, double close, double volume) {
}
