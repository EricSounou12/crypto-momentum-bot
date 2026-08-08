package bot.data;
import bot.model.Candle;
import java.util.ArrayList;
import java.util.List;

/*
LiveCandleWindow.java takes in the last 40 days of btc closing prices and creates a rolling
window to trim old prices and passes it onto main to ensure the strat has at least 28 days of look back data
*/

public class LiveCandleWindow {
    private final List<Candle> candles;
    private final int maxSize;

    
    public void loadWithHistory(List<Candle> fullHistory){
        List<Candle> last40Days = fullHistory.subList(fullHistory.size() - maxSize, fullHistory.size());

        for (int i = 0; i < last40Days.size(); i++) {
            addCandle(last40Days.get(i));
        
        }

    }

    //  maxsize ensures the array list for rolling window stays x int big
    public LiveCandleWindow(int maxSize){
        this.maxSize =  maxSize;
        this.candles =  new ArrayList<>();
    }

    public void addCandle(Candle  newCandle){
        candles.add(newCandle);

        if(candles.size() > maxSize){
            candles.remove(0);
        }
    }

    public List<Candle> getCandles(){
        return candles;
    }



}