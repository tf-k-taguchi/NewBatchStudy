package com.example.demo.domain.repository;

import com.example.demo.domain.model.SqsMessage;

/**
 * SQSリポジトリインターフェース
 * AWS SQSとの連携を抽象化
 */
public interface SqsRepository {
    
    /**
     * SQSキューからメッセージを受信
     * @return 受信メッセージ（SqsMessage形式）
     */
    SqsMessage receiveMessage();
    
    /**
     * SQSキューにメッセージを送信
     * @param message 送信メッセージ
     */
    void sendMessage(String message);
    
    /**
     * キュー内のメッセージ数を取得
     * @return メッセージ数
     */
    int getApproximateNumberOfMessages();
    
    /**
     * メッセージを削除（Acknowledge）
     * @param receiptHandle メッセージのレシートハンドル
     */
    void deleteMessage(String receiptHandle);
    
    /**
     * メッセージのVisibility Timeoutを延長
     * @param receiptHandle メッセージのレシートハンドル
     * @param visibilityTimeout 延長する秒数
     */
    void changeMessageVisibility(String receiptHandle, int visibilityTimeout);
    
    /**
     * DLQ（Dead Letter Queue）にメッセージを送信
     * @param originalMessage 元のメッセージ
     * @param errorDetails エラー詳細
     */
    void sendToDLQ(String originalMessage, String errorDetails);
}
