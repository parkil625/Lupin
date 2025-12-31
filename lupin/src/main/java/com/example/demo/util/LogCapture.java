package com.example.demo.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class LogCapture {
    private static final int MAX_LOGS = 1000;
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] " + message;
        System.out.println(logEntry);

        synchronized (logs) {
            logs.add(logEntry);
            if (logs.size() > MAX_LOGS) {
                logs.remove(0);
            }
        }
    }

    public void error(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = "[" + timestamp + "] ERROR: " + message;
        System.err.println(logEntry);

        synchronized (logs) {
            logs.add(logEntry);
            if (logs.size() > MAX_LOGS) {
                logs.remove(0);
            }
        }
    }

    public void error(String message, Throwable e) {
        error(message + " - " + e.getClass().getName() + ": " + e.getMessage());
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            for (int i = 0; i < Math.min(5, e.getStackTrace().length); i++) {
                error("  at " + e.getStackTrace()[i].toString());
            }
        }
    }

    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public List<String> getLastLogs(int count) {
        synchronized (logs) {
            int size = logs.size();
            int fromIndex = Math.max(0, size - count);
            return new ArrayList<>(logs.subList(fromIndex, size));
        }
    }

    public void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }
}
