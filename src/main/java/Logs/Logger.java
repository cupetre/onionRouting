package Logs;

import java.text.SimpleDateFormat;

public class Logger {

    public static final String RESET = "\u001B[0m";
    public static final String ERROR = "\u001B[31m";
    public static final String WARN = "\u001B[32m";
    public static final String INFO = "\u001B[33m";
    public static final String SUCCESS = "\u001B[34m";
    public static final String DEBUG = "\u001B[35m";
    public static final String STATUS = "\u001B[36m";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String msg, LogLevel level) {
        String threadName = Thread.currentThread().getName();
        String timestamp = dateFormat.format(System.currentTimeMillis());
        String messagePrefix = "[" + timestamp + "][ " + threadName + "] " + level + ": ";

        switch (level) {
            case Info -> messagePrefix = INFO + messagePrefix + RESET;
            case Warn -> messagePrefix = WARN + messagePrefix + RESET;
            case Error -> messagePrefix = ERROR + messagePrefix + RESET;
            case Debug -> messagePrefix = DEBUG + messagePrefix + RESET;
            case Status -> messagePrefix = STATUS + messagePrefix + RESET;
            case Success -> messagePrefix = SUCCESS + messagePrefix + RESET;
        }

        System.out.println(messagePrefix + msg);
    }

    public static void log(String msg) {
        log(msg, LogLevel.Info);
    }
}