package bot;

import bot.data.CandleLoader;
import bot.model.Candle;
import bot.strategy.MomentumStrategy;
import bot.backtest.Backtester;
import bot.backtest.Report;
import bot.data.LivePriceFethcer;
import bot.data.LiveCandleWindow;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneOffset;
import bot.strategy.MomentumStrategy.Signal;


public class Main {
    public static void main(String[] args) throws Exception {
        CandleLoader loader = new CandleLoader();
        List<Candle> candles = loader.loadCandles();
        System.out.println("Loaded " + candles.size() + " candles.");

        LiveCandleWindow window = new LiveCandleWindow(40);
        window.loadWithHistory(candles);

LivePriceFethcer fetcher = new LivePriceFethcer();

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
        System.out.println("Buy & Hold Return: " + report.buyAndHoldReturn());
        System.out.println("------------------------------------------");

        long splitTime = LocalDate.of(2024, 1, 1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();

        int splitIndex = -1;
        for (int i = 0; i < candles.size(); i++) {
            if (candles.get(i).timestamp() >= splitTime) {
                splitIndex = i;
                break;
            }
        }

        List<Candle> inSample = candles.subList(0, splitIndex);
        List<Candle> outOfSample = candles.subList(splitIndex, candles.size());

        Backtester inSampleBacktester = new Backtester(inSample, 100000, strategy);
        inSampleBacktester.run();
        Report inSampleReport = new Report(inSampleBacktester);

        Backtester outOfSampleBacktester = new Backtester(outOfSample, 100000, strategy);
        outOfSampleBacktester.run();
        Report outOfSampleReport = new Report(outOfSampleBacktester);

        System.out.println("=== IN-SAMPLE (2019-2023) ===");
        System.out.println("Total Return: " + inSampleReport.totalReturn());
        System.out.println("Buy & Hold Return: " + inSampleReport.buyAndHoldReturn());
        System.out.println("Max Drawdown: " + inSampleReport.maxDrawdown());

        System.out.println("=== OUT-OF-SAMPLE (2024-2026) ===");
        System.out.println("Total Return: " + outOfSampleReport.totalReturn());
        System.out.println("Buy & Hold Return: " + outOfSampleReport.buyAndHoldReturn());
        System.out.println("Max Drawdown: " + outOfSampleReport.maxDrawdown());

        while(true){

            Candle latest = fetcher.fetchLatCandle();
            window.addCandle(latest);

            List<Candle> currCandles = window.getCandles();
            int latestIndex = currCandles.size() - 1;

            Signal signal = strategy.signalAt(currCandles, latestIndex);
            System.out.println("Latest signal: " + signal);

            Thread.sleep(10000); 




        }
    }
}
