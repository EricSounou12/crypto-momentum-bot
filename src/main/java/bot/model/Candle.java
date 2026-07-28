// Candle needs to store 6 pieces of data (timestamp, open, high, low, close, volume), and it should be immutable (never allowed to change once created)

package bot.model;

public record Candle(long timestamp, double open, double high, double low, double close, double volume) {
    
}