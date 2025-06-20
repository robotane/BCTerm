package fr.univreunion.bcterm.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Logger {

    private static class SimpleMessageFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return record.getMessage() + System.lineSeparator();
        }
    }

    public static java.util.logging.Logger getLogger(Class<?> clazz) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(clazz.getName());
        setupLogger(logger);
        return logger;
    }

    public static java.util.logging.Logger getLogger(String name) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
        setupLogger(logger);
        return logger;
    }

    private static void setupLogger(java.util.logging.Logger logger) {
        logger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleMessageFormatter());
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }
}