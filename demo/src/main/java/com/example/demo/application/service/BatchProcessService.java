package com.example.demo.application.service;

import com.example.demo.domain.entity.BatchLogEntity;
import com.example.demo.domain.exception.BatchProcessException;
import com.example.demo.domain.model.BatchErrorType;
import com.example.demo.domain.model.SqsMessage;
import com.example.demo.domain.repository.BatchLogRepository;
import com.example.demo.domain.repository.S3Repository;
import com.example.demo.domain.repository.SqsRepository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * バッチ処理サービス
 * Application層のサービス
 * 
 * 責務:
 * - ビジネスロジックの実行
 * - トランザクション境界の制御
 * - 複数のRepositoryを組み合わせた処理の調整
 */
@Service
public class BatchProcessService {
    
    private final BatchLogger logger;
    private final SqsRepository sqsRepository;
    private final S3Repository s3Repository;
    private final BatchLogRepository batchLogRepository;
    
    private static final String BATCH_NAME = "HelloAwsBatchJob";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_INTERVAL_MS = 1000;
    private static final int VISIBILITY_TIMEOUT_SECONDS = 300; // 5分
    private static final long VISIBILITY_EXTEND_THRESHOLD_MS = 60000; // 1分
    
    public BatchProcessService(
            BatchLogger logger,
            SqsRepository sqsRepository,
            S3Repository s3Repository,
            BatchLogRepository batchLogRepository) {
        this.logger = logger;
        this.sqsRepository = sqsRepository;
        this.s3Repository = s3Repository;
        this.batchLogRepository = batchLogRepository;
    }
    
    /**
     * キューにメッセージがあるかチェック
     */
    public boolean hasMessagesInQueue() {
        try {
            int messageCount = sqsRepository.getApproximateNumberOfMessages();
            return messageCount > 0;
        } catch (Exception ex) {
            logger.warn("キューのメッセージ数取得に失敗しました: %s", ex.getMessage());
            return false;
        }
    }
    
    /**
     * バッチ処理のメインロジック
     * 【トランザクション境界】
     * @Transactionalアノテーションにより、このメソッド全体が1つのトランザクションとして管理されます。
     * エラー発生時は自動的にロールバックされ、データの整合性が保たれます。
     *
     * 【冪等性の担保】
     * 重複実行チェック（ステップ2）により、同じメッセージを二重処理しないことを保証します。
     * これはtaguchiStudy.mdの「データの重複登録」対策に該当します。
     */
    @Transactional
    public void executeBatchProcess() {
        logger.info("========================================");
        logger.info("Starting %s", BATCH_NAME);
        logger.info("========================================");
        
        SqsMessage sqsMessage = null;
        
        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ1: SQSからメッセージ受信（リトライ付き）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】キューからメッセージを取得する（Produce側の処理）
            // 【対策】taguchiStudy.mdの「ネットワーク障害」対策として最大3回リトライ
            // 【設計】0510処理フロー.mdの「堅牢な実行構造」に該当
            sqsMessage = receiveMessageWithRetry();
            
            if (sqsMessage == null) {
                // メッセージが存在しない場合は正常終了
                handleNoMessage();
                return;
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ2: 重複実行チェック（冪等性の担保）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】同じメッセージを二重処理しないことを保証
            // 【対策】taguchiStudy.mdの「データの重複登録」対策
            // 【設計】0510処理フロー.mdの「整合性と検証（冪等性の担保）」に該当
            // 【実装】DBのbatch_logテーブルでmessage_idとstatus='PROCESSING'を検索
            if (isMessageProcessing(sqsMessage.getMessageId())) {
                handleDuplicateProcessing(sqsMessage);
                return;
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ3: 処理中ステータスをDBに記録
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】処理開始を記録し、他のインスタンスからの重複実行を防ぐ
            // 【設計】0510処理フロー.mdの「ドメイン・モデリング」に該当
            // 【実装】BatchLogEntityを生成してstatus='PROCESSING'で保存
            recordProcessingStatus(sqsMessage);
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ4: メッセージ処理（ビジネスロジック）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】実際のビジネスロジックを実行
            // 【設計】0510処理フロー.mdの「現状分析（ビジネスロジックの抽出）」に該当
            // 【実装】メッセージ内容を加工して結果を生成
            long startTime = System.currentTimeMillis();
            String processedData = processMessage(sqsMessage);
            long processingTime = System.currentTimeMillis() - startTime;
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ5: Visibility Timeoutの延長（必要に応じて）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】処理時間が長い場合、メッセージが他のインスタンスで再処理されるのを防ぐ
            // 【対策】taguchiStudy.mdの「タイムアウト」対策
            // 【実装】処理時間が1分を超えた場合、Visibility Timeoutを5分に延長
            extendVisibilityIfNeeded(sqsMessage, processingTime);
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ6: S3へアップロード
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】処理結果を永続化
            // 【設計】0510処理フロー.mdの「境界設計とデータ変換」に該当
            // 【実装】S3Repositoryを通じてインフラ層の詳細を隠蔽
            uploadResultToS3(processedData);
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ7: 成功ステータスをDBに記録
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】処理完了を記録し、監視・トラブルシューティングに活用
            // 【実装】BatchLogEntityを生成してstatus='SUCCESS'で保存
            recordSuccessStatus(sqsMessage);
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ8: メッセージ削除（Acknowledge）
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】処理完了したメッセージをキューから削除
            // 【重要】この処理が成功して初めて、メッセージが完全に処理されたことになる
            // 【設計】削除に失敗した場合、Visibility Timeout後に再処理される（冪等性により安全）
            deleteMessage(sqsMessage);
            
            logger.info("========================================");
            logger.info("%s completed successfully", BATCH_NAME);
            logger.info("========================================");
            
        } catch (Exception ex) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // エラーハンドリング
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】エラー種別を判定し、適切な対応を行う
            // 【対策】taguchiStudy.mdの「異常データ」「APIレート制限」等の対策
            // 【設計】0510処理フロー.mdの「例外隔離（Skip & DLQ）」に該当
            handleProcessingError(sqsMessage, ex);
            throw ex;
        }
    }
    
    /**
     * メッセージなし時の処理
     */
    private void handleNoMessage() {
        logger.info("キューにメッセージがありません。処理を終了します。");
        BatchLogEntity entity = new BatchLogEntity(BATCH_NAME, "NO_MESSAGE", "SUCCESS");
        entity.setStartedAt(LocalDateTime.now());
        entity.setCompletedAt(LocalDateTime.now());
        batchLogRepository.insert(entity);
        logger.info("バッチログをDBに保存しました - Status: SUCCESS");
    }
    
    /**
     * 重複処理時の処理
     */
    private void handleDuplicateProcessing(SqsMessage sqsMessage) {
        logger.warn("メッセージは既に処理中です。処理をスキップします - MessageId: %s", 
                sqsMessage.getMessageId());
        
        // Visibility Timeoutを延長して他のインスタンスでの再処理を防ぐ
        sqsRepository.changeMessageVisibility(
                sqsMessage.getReceiptHandle(), 
                VISIBILITY_TIMEOUT_SECONDS
        );
    }
    
    /**
     * リトライ機能付きメッセージ受信
     */
    private SqsMessage receiveMessageWithRetry() {
        for (int i = 1; i <= MAX_RETRY; i++) {
            try {
                logger.info("メッセージ受信試行 %d/%d", i, MAX_RETRY);
                return sqsRepository.receiveMessage();
            } catch (Exception ex) {
                logger.warn("メッセージ受信失敗 (試行 %d/%d): %s", i, MAX_RETRY, ex.getMessage());
                if (i == MAX_RETRY) {
                    throw new BatchProcessException(
                            "SQSメッセージ受信に失敗しました", 
                            ex, 
                            BatchErrorType.SYSTEM_ERROR
                    );
                }
                sleep(RETRY_INTERVAL_MS);
            }
        }
        return null;
    }
    
    /**
     * メッセージが処理中かチェック
     */
    private boolean isMessageProcessing(String messageId) {
        BatchLogEntity processingLog = batchLogRepository.findProcessingByMessageId(messageId);
        return processingLog != null;
    }
    
    /**
     * メッセージ処理ロジック
     * ビジネスロジックの中核
     */
    private String processMessage(SqsMessage sqsMessage) {
        logger.info("Processing message: %s", sqsMessage.getContent());
        logger.info("メッセージ処理中...");
        
        // ビジネスロジック実装箇所
        String result = String.format("Processed: %s at %s", 
                sqsMessage.getContent(), LocalDateTime.now());
        
        logger.info("メッセージ処理完了");
        return result;
    }
    
    /**
     * 処理中ステータスの記録
     */
    private void recordProcessingStatus(SqsMessage sqsMessage) {
        BatchLogEntity entity = new BatchLogEntity(BATCH_NAME, sqsMessage.getContent(), "PROCESSING");
        entity.setMessageId(sqsMessage.getMessageId());
        entity.setReceiptHandle(sqsMessage.getReceiptHandle());
        entity.setStartedAt(LocalDateTime.now());
        
        batchLogRepository.insert(entity);
        logger.info("バッチログをDBに保存しました - Status: PROCESSING, MessageId: %s", 
                sqsMessage.getMessageId());
    }
    
    /**
     * Visibility Timeoutの延長
     */
    private void extendVisibilityIfNeeded(SqsMessage sqsMessage, long processingTimeMs) {
        if (processingTimeMs > VISIBILITY_EXTEND_THRESHOLD_MS) {
            logger.info("処理時間が長いため、Visibility Timeoutを延長します - 処理時間: %dms", 
                    processingTimeMs);
            sqsRepository.changeMessageVisibility(
                    sqsMessage.getReceiptHandle(), 
                    VISIBILITY_TIMEOUT_SECONDS
            );
        }
    }
    
    /**
     * S3へのアップロード
     */
    private void uploadResultToS3(String content) {
        String s3Key = String.format("batch-result/%s/%d.txt", 
                BATCH_NAME, System.currentTimeMillis());
        s3Repository.uploadFile(s3Key, content);
    }
    
    /**
     * 成功ステータスの記録
     */
    private void recordSuccessStatus(SqsMessage sqsMessage) {
        BatchLogEntity entity = new BatchLogEntity(BATCH_NAME, sqsMessage.getContent(), "SUCCESS");
        entity.setMessageId(sqsMessage.getMessageId());
        entity.setReceiptHandle(sqsMessage.getReceiptHandle());
        entity.setStartedAt(sqsMessage.getReceivedAt());
        entity.setCompletedAt(LocalDateTime.now());
        
        batchLogRepository.insert(entity);
        logger.info("バッチログをDBに保存しました - Status: SUCCESS, MessageId: %s", 
                sqsMessage.getMessageId());
    }
    
    /**
     * メッセージ削除
     */
    private void deleteMessage(SqsMessage sqsMessage) {
        sqsRepository.deleteMessage(sqsMessage.getReceiptHandle());
        logger.info("SQSメッセージを削除しました - MessageId: %s", sqsMessage.getMessageId());
    }
    
    /**
     * エラーハンドリング
     *
     * 【目的】
     * エラー種別を判定し、適切な対応を行います。
     * これは0510処理フロー.mdの「例外隔離（Skip & DLQ）」に該当します。
     *
     * 【エラー種別と対応】
     * 1. RETRYABLE（リトライ可能）
     *    - 例: ネットワークエラー、タイムアウト
     *    - 対応: メッセージを削除せず、Visibility Timeout後に自動再処理
     *    - 対策: taguchiStudy.mdの「ネットワーク障害」対策
     *
     * 2. NON_RETRYABLE（リトライ不可能）
     *    - 例: データ形式エラー、ビジネスロジックエラー
     *    - 対応: DLQに送信してメッセージを削除
     *    - 対策: taguchiStudy.mdの「異常データ」対策
     *
     * 3. SYSTEM_ERROR（システムエラー）
     *    - 例: DB接続エラー、予期しない例外
     *    - 対応: DLQに送信してメッセージを削除
     *    - 対策: 運用チームへの通知が必要
     *
     * 【DLQ（Dead Letter Queue）】
     * 処理できなかったメッセージを隔離し、後で人手で確認・再処理できるようにします。
     * これにより、特定データの失敗が全体を止めないようにします。
     *
     * @param sqsMessage 処理中のSQSメッセージ（nullの場合は受信前エラー）
     * @param ex 発生した例外
     */
    private void handleProcessingError(SqsMessage sqsMessage, Exception ex) {
        if (sqsMessage == null) {
            logger.error("メッセージ受信前にエラーが発生しました", ex);
            return;
        }
        
        logger.error(String.format("メッセージ処理中にエラーが発生しました - MessageId: %s",
                sqsMessage.getMessageId()), ex);
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // エラー種別の判定
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // BatchErrorType.fromException()で例外の種類を判定
        BatchErrorType errorType = BatchErrorType.fromException(ex);
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 失敗ステータスをDBに記録
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        recordFailureStatus(sqsMessage, ex, errorType);
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // リトライ不可能なエラーまたはシステムエラーの場合、DLQへ送信
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // RETRYABLE以外のエラーは、何度リトライしても成功しないため、
        // DLQに送信してメッセージを削除します
        if (errorType != BatchErrorType.RETRYABLE) {
            sendToDLQ(sqsMessage, ex);
            deleteMessage(sqsMessage);
        }
        // RETRYABLEの場合は、メッセージを削除せずに終了
        // → Visibility Timeout後に自動的に再処理される
    }
    
    /**
     * 失敗ステータスの記録
     */
    private void recordFailureStatus(SqsMessage sqsMessage, Exception ex, BatchErrorType errorType) {
        BatchLogEntity entity = new BatchLogEntity(BATCH_NAME, sqsMessage.getContent(), "FAILED");
        entity.setMessageId(sqsMessage.getMessageId());
        entity.setReceiptHandle(sqsMessage.getReceiptHandle());
        entity.setErrorDetails(getErrorDetails(ex));
        entity.setStartedAt(sqsMessage.getReceivedAt());
        entity.setCompletedAt(LocalDateTime.now());
        
        batchLogRepository.insert(entity);
        logger.info("バッチログをDBに保存しました - Status: FAILED, MessageId: %s, ErrorType: %s", 
                sqsMessage.getMessageId(), errorType.getDescription());
    }
    
    /**
     * DLQへの送信
     */
    private void sendToDLQ(SqsMessage sqsMessage, Exception ex) {
        String errorDetails = getErrorDetails(ex);
        sqsRepository.sendToDLQ(sqsMessage.getContent(), errorDetails);
        logger.info("DLQへメッセージを送信しました - MessageId: %s", sqsMessage.getMessageId());
    }
    
    /**
     * エラー詳細の取得
     */
    private String getErrorDetails(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * スリープ処理
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("スリープ処理が中断されました", e);
        }
    }
}
