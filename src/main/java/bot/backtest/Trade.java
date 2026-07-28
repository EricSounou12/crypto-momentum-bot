package bot.backtest;

public record Trade(long entryTimestamp, double entryPrice, long exitTimestamp, double exitPrice, double pnlPercent) {

}