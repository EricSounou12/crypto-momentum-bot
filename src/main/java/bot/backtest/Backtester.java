package bot.backtest;

import bot.model.Candle;
import bot.strategy.MomentumStrategy;
import bot.strategy.MomentumStrategy.Signal;
import java.util.List;
import java.util.ArrayList;
 
public class Backtester{
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
    this.btcHeld = 0;         //  btc held
    this.isLong = false;  // are you long or not true or false
    this.trades = new ArrayList<>();
    this.equityCurve = new ArrayList<>();
    this.strategy = strategy;
       
}   
}