package com.example.demo.domain.model;

import java.time.LocalDateTime;

/**
 * SQSメッセージDTO（Data Transfer Object）
 *
 * 【境界設計とデータ変換】
 * このクラスは0510処理フロー.mdの「境界設計とデータ変換（DTO）」に該当します。
 * 外部システム（SQS）との入出力専用のデータ構造です。
 *
 * 【責務】
 * SQSから受信したメッセージの情報を保持
 * - メッセージID: 一意識別子（冪等性チェックに使用）
 * - メッセージ内容: 実際のビジネスデータ
 * - レシートハンドル: メッセージ削除・Visibility Timeout延長に使用
 * - 受信時刻: 処理時間の計測に使用
 *
 * 【設計思想】
 * - イミュータブル（不変）: finalフィールドで変更不可
 * - インフラ層の詳細を隠蔽: SQS SDKの詳細をドメイン層に漏らさない
 * - レイヤー間の境界を明確化
 *
 * 【使用箇所】
 * 1. SqsRepository（インフラ層）: SQS SDKからこのDTOに変換
 * 2. BatchProcessService（アプリケーション層）: このDTOを使用してビジネスロジックを実行
 * 3. BatchLogEntity（ドメイン層）: このDTOから必要な情報を抽出
 *
 * 【冪等性の担保】
 * messageIdを使用して、同じメッセージを二重処理しないことを保証します。
 */
public class SqsMessage {
    
    private final String messageId;
    private final String content;
    private final String receiptHandle;
    private final LocalDateTime receivedAt;
    
    public SqsMessage(String messageId, String content, String receiptHandle) {
        this.messageId = messageId;
        this.content = content;
        this.receiptHandle = receiptHandle;
        this.receivedAt = LocalDateTime.now();
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getReceiptHandle() {
        return receiptHandle;
    }
    
    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
    
    @Override
    public String toString() {
        return "SqsMessage{" +
                "messageId='" + messageId + '\'' +
                ", content='" + content + '\'' +
                ", receiptHandle='" + receiptHandle + '\'' +
                ", receivedAt=" + receivedAt +
                '}';
    }
}
