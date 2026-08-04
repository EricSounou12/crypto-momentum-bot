package bot.backtest;

import bot.model.Candle;
import bot.strategy.MomentumStrategy;
import bot.strategy.MomentumStrategy.Signal;
import java.util.List;
import java.util.ArrayList;

public class Backtester {
    private double entryPrice;
    private long entryTimestamp;
    private final List<Candle> candles;
    private double cash;
    private double btcHeld;
    private boolean isLong;
    private double startingCash;
    private List<Trade> trades;
    private List<Double> equityCurve;
    private final MomentumStrategy strategy;

    public Backtester(List<Candle> candles, double startingCash, MomentumStrategy strategy) {
        this.candles = candles;
        this.startingCash = startingCash;
        this.cash = startingCash;           // startingcash is the amount held at the start
        this.btcHeld = 0;                   // btc held
        this.isLong = false;                // are you long or not true or false
        this.trades = new ArrayList<>();
        this.equityCurve = new ArrayList<>();
        this.strategy = strategy;
    }

    /*
    public void run method job is run the simualtion and update feilds
    */
    public void run() {
        for (int i = 0; i < candles.size(); i++) {

            double portfolioValue;
            if (isLong) {
                portfolioValue = btcHeld * candles.get(i).close();
            } else {
                portfolioValue = cash;
            }
            equityCurve.add(portfolioValue);

            /* check if this is rebalnce day */
            if ((i + 1) % 5 == 0) {
                Signal todaySignal = strategy.signalAt(candles, i);

                // buys btc if conditons are we arent in a postion that is long and the signal is long
                if (!isLong && todaySignal == Signal.LONG) {
                    double buyPrice = candles.get(i).close(); // buying price is the candles clsing
                    double btcAmount = cash / buyPrice; // btc is calc by dividing cash by btc value. this will be used to figure how muc btc is available to buy

                    this.btcHeld = btcAmount * 0.999;
                    this.cash = 0;
                    this.isLong = true;
                    this.entryPrice = buyPrice;
                    this.entryTimestamp = candles.get(i).timestamp();
                }

                // trade logic used to enter a trade if the signal is flat and we are already long we should then sell
                if (isLong && todaySignal == Signal.FLAT) {
                    double sellPrice = candles.get(i).close();
                    double cashFromSale = btcHeld * sellPrice;
                    double actualCash = cashFromSale * 0.999;

                    this.cash = actualCash;
                    this.btcHeld = 0;
                    this.isLong = false;

                    double pnlPercent = (sellPrice - entryPrice) / entryPrice;
                    long exitTimestamp = candles.get(i).timestamp();
                    Trade completedTrade = new Trade(entryTimestamp, entryPrice, exitTimestamp, sellPrice, pnlPercent);
                    trades.add(completedTrade);
                }
            }
        }
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public double getCash() {
        return cash;
    }

    public double getStartingCash() {
        return startingCash;
    }
}