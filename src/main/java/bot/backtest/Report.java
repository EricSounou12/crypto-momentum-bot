package bot.backtest;


import java.util.List;
import bot.model.Candle;

public class Report {
    private final Backtester backtester;
    

    /* refrence backtester data using repeort to access feilds like .getCash(), .getTrades(), .getStartingCash() for data */
    public Report(Backtester backtester) {
        this.backtester = backtester;
    }

    public double totalReturn(){
        double start = backtester.getStartingCash();
        List<Double> equityCurve = backtester.getEquityCurve();
        double last = equityCurve.get(equityCurve.size() - 1); // get the last price of the portfilio value for the day using equtiysurve to remove to get the size and last index
        
        return (last-start) / start;
    
    }

    public double winRate() {
        List<Trade> trades = backtester.getTrades();
        int wins = 0;


        for (int i = 0; i < trades.size(); i++) {

            Trade t = trades.get(i);
        if (t.pnlPercent() > 0) {
            wins++;
        }
    }

    return (double) wins / trades.size();
}


public double maxDrawdown() {
    List<Double> equityCurve = backtester.getEquityCurve();
    double peak = equityCurve.get(0);
    double worstDrawdown = 0;

    for (int i = 0; i < equityCurve.size(); i++) {
        double today = equityCurve.get(i);
        if(today > peak){
            peak = today;
        }
        double todaysDrawdown = (today-peak) / peak;
        if(todaysDrawdown<worstDrawdown){
                worstDrawdown = todaysDrawdown;
            }     
    }
    return worstDrawdown;
}

public double buyAndHoldReturn() {
    List<Candle> candles = backtester.getCandles();
    double firstPrice = candles.get(0).close();
    double lastPrice = candles.get(candles.size() - 1).close();

    double btcAmount = backtester.getStartingCash() / firstPrice;
    double  finalValue = btcAmount * lastPrice;

    return (finalValue - backtester.getStartingCash()) / backtester.getStartingCash();
}


}

