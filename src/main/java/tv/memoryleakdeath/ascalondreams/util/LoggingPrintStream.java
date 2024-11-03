package tv.memoryleakdeath.ascalondreams.util;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LoggingPrintStream extends OutputStream {
    private Logger logger;
    private StringBuilder sb = new StringBuilder();
    private Level level;

    public LoggingPrintStream(Level level, Class<?> loggingClass) {
        logger = LoggerFactory.getLogger(loggingClass);
        this.level = level;
    }

    @Override
    public void write(int b) throws IOException {
        char c = (char) b;
        if (c == '\r' || c == '\n') {
            if (sb.length() > 0) {
                logger.atLevel(level).log(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }
    }

}
