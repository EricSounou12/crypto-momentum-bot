package bot;

import bot.data.CandleLoader;
import bot.model.Candle;
import bot.strategy.MomentumStrategy;
import bot.backtest.Backtester;
import bot.backtest.Report;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        CandleLoader loader = new CandleLoader();
        List<Candle> candles = loader.loadCandles();
        System.out.println("Loaded " + candles.size() + " candles.");

        // we are looking about every 28 daays and rebalancing the portfolio every 28 days
        MomentumStrategy strategy = new MomentumStrategy(28);
        // we are backttesting the strat with x dollars using data from candles and using the strat
        Backtester backtester = new Backtester(candles, 100000, strategy);
        backtester.run();

        // just returns the return rate of our strat
        Report report = new Report(backtester);
        System.out.println("------------------------------------------");
        System.out.println("Total Return: " + report.totalReturn());
        System.out.println("Win Rate: " + report.winRate());
        System.out.println("Max Drawdown: " + report.maxDrawdown());
        System.out.println("------------------------------------------");
    }
}
