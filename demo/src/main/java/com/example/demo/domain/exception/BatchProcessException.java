package com.example.demo.domain.exception;

import com.example.demo.domain.model.BatchErrorType;

/**
 * バッチ処理例外
 */
public class BatchProcessException extends RuntimeException {
    
    private final BatchErrorType errorType;
    private final String messageId;
    
    public BatchProcessException(String message, BatchErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.messageId = null;
    }
    
    public BatchProcessException(String message, BatchErrorType errorType, String messageId) {
        super(message);
        this.errorType = errorType;
        this.messageId = messageId;
    }
    
    public BatchProcessException(String message, Throwable cause, BatchErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.messageId = null;
    }
    
    public BatchProcessException(String message, Throwable cause, BatchErrorType errorType, String messageId) {
        super(message, cause);
        this.errorType = errorType;
        this.messageId = messageId;
    }
    
    public BatchErrorType getErrorType() {
        return errorType;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public boolean isRetryable() {
        return errorType == BatchErrorType.RETRYABLE;
    }
}
