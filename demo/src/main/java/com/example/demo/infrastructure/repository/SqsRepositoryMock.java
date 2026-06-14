package com.example.demo.infrastructure.repository;

import com.example.demo.domain.model.SqsMessage;
import com.example.demo.domain.repository.SqsRepository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQSリポジトリのMock実装
 * ローカル開発用にメモリ上のキューで模擬
 */
@Component
@Profile("local")
public class SqsRepositoryMock implements SqsRepository {
    
    private final BatchLogger logger;
    private final Queue<String> messageQueue = new LinkedList<>();
    private final Map<String, String> receiptHandleMap = new ConcurrentHashMap<>();
    private final Queue<String> dlqQueue = new LinkedList<>();
    
    public SqsRepositoryMock(BatchLogger logger) {
        this.logger = logger;
        // 初期メッセージを投入
        messageQueue.offer("Mock SQS Message 1");
        messageQueue.offer("Mock SQS Message 2");
        messageQueue.offer("Mock SQS Message 3");
    }
    
    @Override
    public SqsMessage receiveMessage() {
        String content = messageQueue.poll();
        if (content == null) {
            logger.info("SQS Mock: キューにメッセージがありません");
            return null;
        }
        
        // メッセージIDとレシートハンドルを生成
        String messageId = UUID.randomUUID().toString();
        String receiptHandle = UUID.randomUUID().toString();
        
        // レシートハンドルとメッセージの紐付けを保存
        receiptHandleMap.put(receiptHandle, content);
        
        logger.info("SQS Mock: メッセージ受信 - MessageId: %s, Content: %s", messageId, content);
        
        return new SqsMessage(messageId, content, receiptHandle);
    }
    
    @Override
    public void sendMessage(String message) {
        messageQueue.offer(message);
        logger.info("SQS Mock: メッセージ送信 - %s", message);
    }
    
    @Override
    public int getApproximateNumberOfMessages() {
        int size = messageQueue.size();
        logger.info("SQS Mock: キュー内メッセージ数 - %d", size);
        return size;
    }
    
    @Override
    public void deleteMessage(String receiptHandle) {
        String message = receiptHandleMap.remove(receiptHandle);
        if (message != null) {
            logger.info("SQS Mock: メッセージ削除 - ReceiptHandle: %s", receiptHandle);
        } else {
            logger.warn("SQS Mock: 削除対象メッセージが見つかりません - ReceiptHandle: %s", receiptHandle);
        }
    }
    
    @Override
    public void changeMessageVisibility(String receiptHandle, int visibilityTimeout) {
        logger.info("SQS Mock: Visibility Timeout延長 - ReceiptHandle: %s, Timeout: %d秒", 
                receiptHandle, visibilityTimeout);
        // Mock環境では実際の延長処理は不要
    }
    
    @Override
    public void sendToDLQ(String originalMessage, String errorDetails) {
        String dlqMessage = String.format("DLQ: %s | Error: %s", originalMessage, errorDetails);
        dlqQueue.offer(dlqMessage);
        logger.info("SQS Mock: DLQへメッセージ送信 - %s", dlqMessage);
    }
    
    /**
     * DLQの内容を取得（テスト・デバッグ用）
     */
    public Queue<String> getDlqQueue() {
        return new LinkedList<>(dlqQueue);
    }
}
