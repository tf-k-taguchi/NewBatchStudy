package com.example.demo.domain.entity;

import java.time.LocalDateTime;

/**
 * バッチログエンティティ
 *
 * 【ドメインモデル】
 * このクラスは0510処理フロー.mdの「ドメイン・モデリング（純粋Entityの定義）」に該当します。
 * フレームワークやDBの都合に依存しない、業務ルールのみを持つEntityです。
 *
 * 【責務】
 * バッチ処理の実行履歴を管理するドメインモデル
 * - 処理状態の記録（PROCESSING, SUCCESS, FAILED）
 * - 冪等性チェックのための情報保持（messageId）
 * - エラー情報の保持（errorDetails）
 * - 処理時間の記録（startedAt, completedAt）
 *
 * 【設計思想】
 * - インフラストラクチャ（MyBatis、JPA等）から独立
 * - ビジネスロジックに集中
 * - テスタビリティを重視
 *
 * 【冪等性の担保】
 * taguchiStudy.mdの「データの重複登録」対策として、
 * messageIdとstatusを組み合わせて重複実行をチェックします。
 *
 * 【改善ポイント】
 * 現在はデータクラスですが、本来は以下のような振る舞いを持つべきです：
 * - isProcessing(): 処理中かどうかを判定
 * - markAsSuccess(): 成功ステータスに変更
 * - markAsFailed(): 失敗ステータスに変更
 * - canRetry(): リトライ可能かどうかを判定
 */
public class BatchLogEntity {
    
    private Long id;
    private String batchName;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    
    // 新規追加フィールド
    private String messageId;          // SQSメッセージID
    private String receiptHandle;      // SQSレシートハンドル
    private String errorDetails;       // エラー詳細
    private Integer retryCount;        // リトライ回数
    private LocalDateTime startedAt;   // 処理開始時刻
    private LocalDateTime completedAt; // 処理完了時刻
    
    public BatchLogEntity() {
    }
    
    public BatchLogEntity(String batchName, String message, String status) {
        this.batchName = batchName;
        this.message = message;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBatchName() {
        return batchName;
    }
    
    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // 新規追加のGetters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getReceiptHandle() {
        return receiptHandle;
    }
    
    public void setReceiptHandle(String receiptHandle) {
        this.receiptHandle = receiptHandle;
    }
    
    public String getErrorDetails() {
        return errorDetails;
    }
    
    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return "BatchLogEntity{" +
                "id=" + id +
                ", batchName='" + batchName + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", messageId='" + messageId + '\'' +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
