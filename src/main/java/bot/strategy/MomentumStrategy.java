

//Declares which "folder group" this file belongs to. Java belongs to a package, and the package name has to match the folder path (src/main/java/bot/strategy/ → package bot.strategy). Think of it as the file's mailing address — it's how other files find and refer to this one.
package bot.strategy;
//Says "I want to use the Candle class in this file, and it lives over in the bot.model package. in candle.java
import bot.model.Candle;
import java.util.List;

public class MomentumStrategy {
    // FIXED TERMS ENUM ALLOWS ME TO CREATE LONG AND FLAT FOR BTC
    public enum Signal { LONG, FLAT }

    private final int lookbackDays;

    public MomentumStrategy(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public Signal signalAt(List<Candle> candles, int i) {
        if(i <  lookbackDays) {
            return Signal.FLAT;
        }
        double todayClose = candles.get(i).close();
        double pastClose = candles.get(i- lookbackDays).close();
    
    if (todayClose > pastClose) {
        return Signal.LONG;
    }
        else{
            return Signal.FLAT;
    }

}}