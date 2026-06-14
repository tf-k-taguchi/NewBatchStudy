package com.example.demo.infrastructure.common.logging;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * コンソール出力型BatchLogger実装
 * 依存関係トラブル回避のためのシンプルな実装
 */
@Component
public class ConsoleBatchLogger implements BatchLogger {
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void info(String message) {
        log("INFO", message);
    }
    
    @Override
    public void info(String message, Object... args) {
        log("INFO", String.format(message, args));
    }
    
    @Override
    public void warn(String message) {
        log("WARN", message);
    }
    
    @Override
    public void warn(String message, Object... args) {
        log("WARN", String.format(message, args));
    }
    
    @Override
    public void warnWithException(String message, Throwable throwable) {
        log("WARN", message + " - " + throwable.getMessage());
        throwable.printStackTrace();
    }
    
    @Override
    public void error(String message) {
        log("ERROR", message);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        log("ERROR", message + " - " + throwable.getMessage());
        throwable.printStackTrace();
    }
    
    @Override
    public void debug(String message) {
        log("DEBUG", message);
    }
    
    /**
     * ログ出力の共通処理
     * @param level ログレベル
     * @param message メッセージ
     */
    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println(String.format("[Console] %s - %s - %s", timestamp, level, message));
    }
}
