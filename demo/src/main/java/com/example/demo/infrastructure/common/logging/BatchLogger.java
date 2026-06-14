package com.example.demo.infrastructure.common.logging;

/**
 * バッチ処理用カスタムロガーインターフェース
 * Lombokの@Slf4jアノテーション依存を回避するための独自実装
 */
public interface BatchLogger {
    
    /**
     * INFOレベルのログを出力
     * @param message ログメッセージ
     */
    void info(String message);
    
    /**
     * INFOレベルのログを出力（パラメータ付き）
     * @param message ログメッセージ
     * @param args メッセージパラメータ
     */
    void info(String message, Object... args);
    
    /**
     * WARNレベルのログを出力
     * @param message ログメッセージ
     */
    void warn(String message);
    
    /**
     * WARNレベルのログを出力（パラメータ付き）
     * @param message ログメッセージ
     * @param args メッセージパラメータ
     */
    void warn(String message, Object... args);
    
    /**
     * WARNレベルのログを出力（例外付き）
     * @param message ログメッセージ
     * @param throwable 例外オブジェクト
     */
    void warnWithException(String message, Throwable throwable);
    
    /**
     * ERRORレベルのログを出力
     * @param message ログメッセージ
     */
    void error(String message);
    
    /**
     * ERRORレベルのログを出力（例外付き）
     * @param message ログメッセージ
     * @param throwable 例外オブジェクト
     */
    void error(String message, Throwable throwable);
    
    /**
     * DEBUGレベルのログを出力
     * @param message ログメッセージ
     */
    void debug(String message);
}
