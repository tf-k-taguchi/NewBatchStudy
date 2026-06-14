package com.example.demo.application.service;

import com.example.demo.domain.entity.BatchLogEntity;
import com.example.demo.domain.exception.BatchProcessException;
import com.example.demo.domain.model.BatchErrorType;
import com.example.demo.domain.model.SqsMessage;
import com.example.demo.domain.repository.BatchLogRepository;
import com.example.demo.domain.repository.S3Repository;
import com.example.demo.domain.repository.SqsRepository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BatchProcessServiceの単体テスト
 * 
 * テスト対象: BatchProcessService
 * テスト方針:
 * - 正常系: メッセージ受信から処理完了までの一連の流れ
 * - 異常系: 各種エラーケース（メッセージ受信失敗、重複処理、処理中エラー）
 * - 境界値: メッセージなし、リトライ上限、Visibility Timeout延長
 */
@SpringBootTest(classes = {BatchProcessService.class})
class BatchProcessServiceTest {

    @Autowired
    private BatchProcessService batchProcessService;

    @MockitoBean
    private BatchLogger logger;

    @MockitoBean
    private SqsRepository sqsRepository;

    @MockitoBean
    private S3Repository s3Repository;

    @MockitoBean
    private BatchLogRepository batchLogRepository;

    @AfterEach
    void tearDown() {
        // モックの余計な呼び出しがないことを検証
        // 注意: loggerは多数の呼び出しがあるため、個別に検証する
        verifyNoMoreInteractions(sqsRepository, s3Repository, batchLogRepository);
    }

    /**
     * 正常系: メッセージがキューに存在する場合
     * 
     * 条件:
     * - キューにメッセージが1件以上存在
     * 
     * 期待値:
     * - trueが返却される
     */
    @Test
    @DisplayName("hasMessagesInQueue_キューにメッセージが存在する場合_trueを返す")
    void testHasMessagesInQueue_whenMessagesExist_returnsTrue() {
        // Given: キューに3件のメッセージが存在
        when(sqsRepository.getApproximateNumberOfMessages()).thenReturn(3);

        // When: メッセージ存在チェックを実行
        boolean result = batchProcessService.hasMessagesInQueue();

        // Then: trueが返却される
        assertTrue(result);
        verify(sqsRepository).getApproximateNumberOfMessages();
    }

    /**
     * 正常系: メッセージがキューに存在しない場合
     * 
     * 条件:
     * - キューにメッセージが0件
     * 
     * 期待値:
     * - falseが返却される
     */
    @Test
    @DisplayName("hasMessagesInQueue_キューにメッセージが存在しない場合_falseを返す")
    void testHasMessagesInQueue_whenNoMessages_returnsFalse() {
        // Given: キューにメッセージが0件
        when(sqsRepository.getApproximateNumberOfMessages()).thenReturn(0);

        // When: メッセージ存在チェックを実行
        boolean result = batchProcessService.hasMessagesInQueue();

        // Then: falseが返却される
        assertFalse(result);
        verify(sqsRepository).getApproximateNumberOfMessages();
    }

    /**
     * 異常系: キューのメッセージ数取得時に例外が発生
     * 
     * 条件:
     * - getApproximateNumberOfMessagesで例外発生
     * 
     * 期待値:
     * - falseが返却される（エラーを握りつぶす）
     * - 警告ログが出力される
     */
    @Test
    @DisplayName("hasMessagesInQueue_メッセージ数取得失敗時_falseを返す")
    void testHasMessagesInQueue_whenExceptionOccurs_returnsFalse() {
        // Given: メッセージ数取得時に例外が発生
        RuntimeException exception = new RuntimeException("SQS接続エラー");
        when(sqsRepository.getApproximateNumberOfMessages()).thenThrow(exception);

        // When: メッセージ存在チェックを実行
        boolean result = batchProcessService.hasMessagesInQueue();

        // Then: falseが返却され、警告ログが出力される
        assertFalse(result);
        verify(sqsRepository).getApproximateNumberOfMessages();
        verify(logger).warn(eq("キューのメッセージ数取得に失敗しました: %s"), eq("SQS接続エラー"));
    }

    /**
     * 正常系: バッチ処理が正常に完了
     * 
     * 条件:
     * - メッセージ受信成功
     * - 重複処理なし
     * - 処理時間が短い（Visibility Timeout延長なし）
     * 
     * 期待値:
     * - 処理中ステータスがDBに記録される
     * - S3へアップロードされる
     * - 成功ステータスがDBに記録される
     * - メッセージが削除される
     */
    @Test
    @DisplayName("executeBatchProcess_正常系_処理が成功する")
    void testExecuteBatchProcess_whenNormalFlow_succeeds() {
        // Given: 正常なメッセージを受信
        SqsMessage sqsMessage = new SqsMessage("msg-001", "test-content", "receipt-001");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);
        when(batchLogRepository.findProcessingByMessageId("msg-001")).thenReturn(null);

        // When: バッチ処理を実行
        batchProcessService.executeBatchProcess();

        // Then: 各処理が正しく実行される
        // 1. メッセージ受信
        verify(sqsRepository).receiveMessage();
        verify(logger).info(eq("メッセージ受信試行 %d/%d"), eq(1), eq(3));

        // 2. 重複チェック
        verify(batchLogRepository).findProcessingByMessageId("msg-001");

        // 3. 処理中ステータス記録
        ArgumentCaptor<BatchLogEntity> processingCaptor = ArgumentCaptor.forClass(BatchLogEntity.class);
        verify(batchLogRepository, times(2)).insert(processingCaptor.capture());
        BatchLogEntity processingLog = processingCaptor.getAllValues().get(0);
        assertEquals("HelloAwsBatchJob", processingLog.getBatchName());
        assertEquals("test-content", processingLog.getMessage());
        assertEquals("PROCESSING", processingLog.getStatus());
        assertEquals("msg-001", processingLog.getMessageId());
        assertEquals("receipt-001", processingLog.getReceiptHandle());

        // 4. S3アップロード
        ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Repository).uploadFile(s3KeyCaptor.capture(), contentCaptor.capture());
        assertTrue(s3KeyCaptor.getValue().startsWith("batch-result/HelloAwsBatchJob/"));
        assertTrue(contentCaptor.getValue().contains("Processed: test-content"));

        // 5. 成功ステータス記録
        BatchLogEntity successLog = processingCaptor.getAllValues().get(1);
        assertEquals("HelloAwsBatchJob", successLog.getBatchName());
        assertEquals("test-content", successLog.getMessage());
        assertEquals("SUCCESS", successLog.getStatus());
        assertEquals("msg-001", successLog.getMessageId());

        // 6. メッセージ削除
        verify(sqsRepository).deleteMessage("receipt-001");

        // 7. ログ出力確認
        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
    }

    /**
     * 正常系: メッセージがない場合
     * 
     * 条件:
     * - receiveMessageがnullを返す
     * 
     * 期待値:
     * - NO_MESSAGEステータスがDBに記録される
     * - 処理が正常終了する
     */
    @Test
    @DisplayName("executeBatchProcess_メッセージなし_NO_MESSAGEステータスで終了")
    void testExecuteBatchProcess_whenNoMessage_recordsNoMessageStatus() {
        // Given: メッセージが存在しない
        when(sqsRepository.receiveMessage()).thenReturn(null);

        // When: バッチ処理を実行
        batchProcessService.executeBatchProcess();

        // Then: NO_MESSAGEステータスが記録される
        verify(sqsRepository).receiveMessage();
        verify(logger).info(eq("メッセージ受信試行 %d/%d"), eq(1), eq(3));
        verify(logger).info("キューにメッセージがありません。処理を終了します。");

        ArgumentCaptor<BatchLogEntity> captor = ArgumentCaptor.forClass(BatchLogEntity.class);
        verify(batchLogRepository).insert(captor.capture());
        BatchLogEntity log = captor.getValue();
        assertEquals("HelloAwsBatchJob", log.getBatchName());
        assertEquals("NO_MESSAGE", log.getMessage());
        assertEquals("SUCCESS", log.getStatus());
        assertNotNull(log.getStartedAt());
        assertNotNull(log.getCompletedAt());

        verify(logger).info("バッチログをDBに保存しました - Status: SUCCESS");
        verify(logger, atLeastOnce()).info(anyString());
    }

    /**
     * 異常系: メッセージ受信時にリトライ上限まで失敗
     * 
     * 条件:
     * - receiveMessageが3回連続で例外をスロー
     * 
     * 期待値:
     * - BatchProcessExceptionがスローされる
     * - エラータイプはSYSTEM_ERROR
     */
    @Test
    @DisplayName("executeBatchProcess_メッセージ受信失敗_リトライ後に例外をスロー")
    void testExecuteBatchProcess_whenReceiveMessageFailsAfterRetry_throwsException() {
        // Given: メッセージ受信が3回失敗
        RuntimeException cause = new RuntimeException("SQS接続エラー");
        when(sqsRepository.receiveMessage()).thenThrow(cause);

        // When & Then: BatchProcessExceptionがスローされる
        BatchProcessException exception = assertThrows(BatchProcessException.class, () -> {
            batchProcessService.executeBatchProcess();
        });

        assertEquals("SQSメッセージ受信に失敗しました", exception.getMessage());
        assertEquals(BatchErrorType.SYSTEM_ERROR, exception.getErrorType());
        assertSame(cause, exception.getCause());

        // リトライが3回実行されたことを確認
        verify(sqsRepository, times(3)).receiveMessage();
        verify(logger, times(3)).info(eq("メッセージ受信試行 %d/%d"), anyInt(), eq(3));
        verify(logger, times(3)).warn(eq("メッセージ受信失敗 (試行 %d/%d): %s"), anyInt(), eq(3), eq("SQS接続エラー"));
        verify(logger, atLeastOnce()).info(anyString());
    }

    /**
     * 異常系: 重複処理の検出
     * 
     * 条件:
     * - 同じメッセージIDが既に処理中
     * 
     * 期待値:
     * - 処理がスキップされる
     * - Visibility Timeoutが延長される
     */
    @Test
    @DisplayName("executeBatchProcess_重複処理検出_処理をスキップしVisibilityTimeoutを延長")
    void testExecuteBatchProcess_whenDuplicateProcessing_skipsAndExtendsVisibility() {
        // Given: メッセージ受信成功、ただし既に処理中
        SqsMessage sqsMessage = new SqsMessage("msg-002", "duplicate-content", "receipt-002");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);

        BatchLogEntity processingLog = new BatchLogEntity("HelloAwsBatchJob", "duplicate-content", "PROCESSING");
        processingLog.setMessageId("msg-002");
        when(batchLogRepository.findProcessingByMessageId("msg-002")).thenReturn(processingLog);

        // When: バッチ処理を実行
        batchProcessService.executeBatchProcess();

        // Then: 処理がスキップされ、Visibility Timeoutが延長される
        verify(sqsRepository).receiveMessage();
        verify(batchLogRepository).findProcessingByMessageId("msg-002");
        verify(logger).warn(eq("メッセージは既に処理中です。処理をスキップします - MessageId: %s"), eq("msg-002"));
        verify(sqsRepository).changeMessageVisibility("receipt-002", 300);
        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
    }

    /**
     * 異常系: 処理中に例外が発生（リトライ可能エラー）
     * 
     * 条件:
     * - S3アップロード時にタイムアウト例外が発生
     * 
     * 期待値:
     * - 失敗ステータスがDBに記録される
     * - DLQへは送信されない（リトライ可能なため）
     * - メッセージは削除されない
     * - 例外が再スローされる
     */
    @Test
    @DisplayName("executeBatchProcess_リトライ可能エラー_DLQに送信せずメッセージを残す")
    void testExecuteBatchProcess_whenRetryableError_doesNotSendToDLQ() {
        // Given: S3アップロード時にタイムアウト例外
        SqsMessage sqsMessage = new SqsMessage("msg-003", "timeout-content", "receipt-003");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);
        when(batchLogRepository.findProcessingByMessageId("msg-003")).thenReturn(null);

        RuntimeException timeoutException = new RuntimeException("Connection timeout");
        doThrow(timeoutException).when(s3Repository).uploadFile(anyString(), anyString());

        // When & Then: 例外がスローされる
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchProcessService.executeBatchProcess();
        });

        assertEquals("Connection timeout", exception.getMessage());

        // Then: 失敗ステータスが記録され、DLQには送信されない
        verify(sqsRepository).receiveMessage();
        verify(batchLogRepository).findProcessingByMessageId("msg-003");
        verify(batchLogRepository, times(2)).insert(any(BatchLogEntity.class));
        verify(s3Repository).uploadFile(anyString(), anyString());

        // DLQへの送信とメッセージ削除は行われない（リトライ可能エラーのため）
        verify(sqsRepository, never()).sendToDLQ(anyString(), anyString());
        verify(sqsRepository, never()).deleteMessage(anyString());

        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
        verify(logger).error(eq(String.format("メッセージ処理中にエラーが発生しました - MessageId: %s", "msg-003")), eq(timeoutException));
    }

    /**
     * 異常系: 処理中に例外が発生（リトライ不可能エラー）
     * 
     * 条件:
     * - S3アップロード時にフォーマット例外が発生
     * 
     * 期待値:
     * - 失敗ステータスがDBに記録される
     * - DLQへ送信される
     * - メッセージが削除される
     * - 例外が再スローされる
     */
    @Test
    @DisplayName("executeBatchProcess_リトライ不可能エラー_DLQに送信しメッセージを削除")
    void testExecuteBatchProcess_whenNonRetryableError_sendsToDLQAndDeletesMessage() {
        // Given: S3アップロード時にフォーマット例外
        SqsMessage sqsMessage = new SqsMessage("msg-004", "invalid-format", "receipt-004");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);
        when(batchLogRepository.findProcessingByMessageId("msg-004")).thenReturn(null);

        RuntimeException formatException = new IllegalArgumentException("Invalid format");
        doThrow(formatException).when(s3Repository).uploadFile(anyString(), anyString());

        // When & Then: 例外がスローされる
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            batchProcessService.executeBatchProcess();
        });

        assertEquals("Invalid format", exception.getMessage());

        // Then: 失敗ステータスが記録され、DLQに送信され、メッセージが削除される
        verify(sqsRepository).receiveMessage();
        verify(batchLogRepository).findProcessingByMessageId("msg-004");
        verify(batchLogRepository, times(2)).insert(any(BatchLogEntity.class));
        verify(s3Repository).uploadFile(anyString(), anyString());

        // DLQへの送信とメッセージ削除が行われる
        ArgumentCaptor<String> errorDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(sqsRepository).sendToDLQ(eq("invalid-format"), errorDetailsCaptor.capture());
        assertTrue(errorDetailsCaptor.getValue().contains("Invalid format"));
        verify(sqsRepository).deleteMessage("receipt-004");

        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
        verify(logger).error(eq(String.format("メッセージ処理中にエラーが発生しました - MessageId: %s", "msg-004")), eq(formatException));
        verify(logger).info(eq("DLQへメッセージを送信しました - MessageId: %s"), eq("msg-004"));
        verify(logger).info(eq("SQSメッセージを削除しました - MessageId: %s"), eq("msg-004"));
    }

    /**
     * 異常系: 処理中に例外が発生（システムエラー）
     * 
     * 条件:
     * - S3アップロード時にシステム例外が発生
     * 
     * 期待値:
     * - 失敗ステータスがDBに記録される
     * - DLQへ送信される
     * - メッセージが削除される
     */
    @Test
    @DisplayName("executeBatchProcess_システムエラー_DLQに送信しメッセージを削除")
    void testExecuteBatchProcess_whenSystemError_sendsToDLQAndDeletesMessage() {
        // Given: S3アップロード時にシステム例外
        SqsMessage sqsMessage = new SqsMessage("msg-005", "system-error", "receipt-005");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);
        when(batchLogRepository.findProcessingByMessageId("msg-005")).thenReturn(null);

        RuntimeException systemException = new RuntimeException("Unexpected system error");
        doThrow(systemException).when(s3Repository).uploadFile(anyString(), anyString());

        // When & Then: 例外がスローされる
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            batchProcessService.executeBatchProcess();
        });

        assertEquals("Unexpected system error", exception.getMessage());

        // Then: DLQへの送信とメッセージ削除が行われる
        verify(sqsRepository).receiveMessage();
        verify(batchLogRepository).findProcessingByMessageId("msg-005");
        verify(batchLogRepository, times(2)).insert(any(BatchLogEntity.class));
        verify(s3Repository).uploadFile(anyString(), anyString());
        verify(sqsRepository).sendToDLQ(eq("system-error"), anyString());
        verify(sqsRepository).deleteMessage("receipt-005");

        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
        verify(logger).error(eq(String.format("メッセージ処理中にエラーが発生しました - MessageId: %s", "msg-005")), eq(systemException));
    }

    /**
     * 境界値: 処理時間が短い場合のVisibility Timeout延長なし
     *
     * 条件:
     * - 処理時間が60秒未満（VISIBILITY_EXTEND_THRESHOLD_MS以下）
     *
     * 期待値:
     * - Visibility Timeoutが延長されない
     * - 正常に処理が完了する
     *
     * 注意: 実際の処理時間を60秒以上にするのは現実的でないため、
     *       延長されないケースをテストする
     */
    @Test
    @DisplayName("executeBatchProcess_処理時間が短い場合_VisibilityTimeoutを延長しない")
    void testExecuteBatchProcess_whenShortProcessing_doesNotExtendVisibility() throws Exception {
        // Given: 正常なメッセージを受信
        SqsMessage sqsMessage = new SqsMessage("msg-006", "short-process", "receipt-006");
        when(sqsRepository.receiveMessage()).thenReturn(sqsMessage);
        when(batchLogRepository.findProcessingByMessageId("msg-006")).thenReturn(null);

        // When: バッチ処理を実行（処理時間は短い）
        batchProcessService.executeBatchProcess();

        // Then: Visibility Timeoutは延長されず、正常に完了する
        verify(sqsRepository).receiveMessage();
        verify(batchLogRepository).findProcessingByMessageId("msg-006");
        verify(batchLogRepository, times(2)).insert(any(BatchLogEntity.class));
        verify(s3Repository).uploadFile(anyString(), anyString());
        // changeMessageVisibilityは呼ばれない
        verify(sqsRepository, never()).changeMessageVisibility(anyString(), anyInt());
        verify(sqsRepository).deleteMessage("receipt-006");

        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
    }

    /**
     * 異常系: メッセージ受信前にエラーが発生
     * 
     * 条件:
     * - メッセージ受信前（sqsMessage == null）にエラー
     * 
     * 期待値:
     * - エラーログが出力される
     * - DLQ送信やメッセージ削除は行われない
     */
    @Test
    @DisplayName("executeBatchProcess_メッセージ受信前エラー_エラーログのみ出力")
    void testExecuteBatchProcess_whenErrorBeforeReceivingMessage_logsErrorOnly() {
        // Given: メッセージ受信時に例外（リトライ後）
        RuntimeException exception = new RuntimeException("Initial error");
        when(sqsRepository.receiveMessage()).thenThrow(exception);

        // When & Then: 例外がスローされる
        assertThrows(BatchProcessException.class, () -> {
            batchProcessService.executeBatchProcess();
        });

        // Then: エラーログのみ出力され、DLQ送信等は行われない
        verify(sqsRepository, times(3)).receiveMessage();
        verify(sqsRepository, never()).sendToDLQ(anyString(), anyString());
        verify(sqsRepository, never()).deleteMessage(anyString());

        verify(logger, atLeastOnce()).info(anyString());
        verify(logger, atLeastOnce()).info(anyString(), any());
        verify(logger, atLeastOnce()).warn(anyString(), anyInt(), anyInt(), anyString());
    }
}
