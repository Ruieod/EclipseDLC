package code.essence.common.logger.implement;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import code.essence.common.logger.Logger;

public class ConsoleLogger implements Logger {
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("essence");

    @Override
    public void log(Object message) {
        logger.info("[Essence] {}", message);
    }

    @Override
    public void minecraftLog(Text... components) {

    }
}
