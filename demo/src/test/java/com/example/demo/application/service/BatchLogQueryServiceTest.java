package com.example.demo.application.service;

import com.example.demo.domain.entity.BatchLogEntity;
import com.example.demo.domain.repository.BatchLogRepository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BatchLogQueryServiceの単体テスト
 * 
 * テスト対象: BatchLogQueryService
 * テスト方針:
 * - 正常系: バッチログの取得と表示
 * - 境界値: ログが0件の場合、複数件の場合
 * - 異常系: nullや空文字のバッチ名
 */
@SpringBootTest(classes = {BatchLogQueryService.class})
class BatchLogQueryServiceTest {

    @Autowired
    private BatchLogQueryService batchLogQueryService;

    @MockitoBean
    private BatchLogger logger;

    @MockitoBean
    private BatchLogRepository batchLogRepository;

    @AfterEach
    void tearDown() {
        // モックの余計な呼び出しがないことを検証
        // 注意: loggerは多数の呼び出しがあるため、個別に検証する
        verifyNoMoreInteractions(batchLogRepository);
    }

    /**
     * 正常系: 全バッチログを取得して表示（複数件）
     * 
     * 条件:
     * - バッチログが3件存在
     * 
     * 期待値:
     * - 3件のログが取得される
     * - 各ログの情報がログ出力される
     */
    @Test
    @DisplayName("displayAllBatchLogs_複数件のログが存在_全件表示される")
    void testDisplayAllBatchLogs_whenMultipleLogs_displaysAll() {
        // Given: 3件のバッチログが存在
        List<BatchLogEntity> logs = createTestLogs(3);
        when(batchLogRepository.findAll()).thenReturn(logs);

        // When: 全バッチログを表示
        batchLogQueryService.displayAllBatchLogs();

        // Then: リポジトリから全件取得され、ログ出力される
        verify(batchLogRepository).findAll();
        verify(logger, times(3)).info("========================================");
        verify(logger).info("バッチログ一覧を取得します");
        verify(logger).info(eq("バッチログ件数: %d件"), eq(3));
        verify(logger).info("----------------------------------------");

        // 各ログの情報が出力される
        for (BatchLogEntity log : logs) {
            verify(logger).info(
                eq("ID: %d | BatchName: %s | Message: %s | Status: %s | CreatedAt: %s"),
                eq(log.getId()),
                eq(log.getBatchName()),
                eq(log.getMessage()),
                eq(log.getStatus()),
                eq(log.getCreatedAt())
            );
        }
    }

    /**
     * 境界値: 全バッチログを取得（0件）
     * 
     * 条件:
     * - バッチログが0件
     * 
     * 期待値:
     * - 「バッチログが存在しません」というメッセージが出力される
     */
    @Test
    @DisplayName("displayAllBatchLogs_ログが0件_存在しないメッセージを表示")
    void testDisplayAllBatchLogs_whenNoLogs_displaysNoLogsMessage() {
        // Given: バッチログが0件
        when(batchLogRepository.findAll()).thenReturn(Collections.emptyList());

        // When: 全バッチログを表示
        batchLogQueryService.displayAllBatchLogs();

        // Then: 存在しないメッセージが出力される
        verify(batchLogRepository).findAll();
        verify(logger, times(3)).info("========================================");
        verify(logger).info("バッチログ一覧を取得します");
        verify(logger).info("バッチログが存在しません");
    }

    /**
     * 境界値: 全バッチログを取得（1件）
     * 
     * 条件:
     * - バッチログが1件のみ
     * 
     * 期待値:
     * - 1件のログが表示される
     */
    @Test
    @DisplayName("displayAllBatchLogs_ログが1件_1件表示される")
    void testDisplayAllBatchLogs_whenSingleLog_displaysSingle() {
        // Given: バッチログが1件
        List<BatchLogEntity> logs = createTestLogs(1);
        when(batchLogRepository.findAll()).thenReturn(logs);

        // When: 全バッチログを表示
        batchLogQueryService.displayAllBatchLogs();

        // Then: 1件のログが表示される
        verify(batchLogRepository).findAll();
        verify(logger, times(3)).info("========================================");
        verify(logger).info("バッチログ一覧を取得します");
        verify(logger).info(eq("バッチログ件数: %d件"), eq(1));
        verify(logger).info("----------------------------------------");
        verify(logger).info(
            eq("ID: %d | BatchName: %s | Message: %s | Status: %s | CreatedAt: %s"),
            anyLong(),
            anyString(),
            anyString(),
            anyString(),
            any(LocalDateTime.class)
        );
    }

    /**
     * 正常系: 特定バッチ名のログを取得して表示（複数件）
     * 
     * 条件:
     * - 指定したバッチ名のログが2件存在
     * 
     * 期待値:
     * - 2件のログが取得される
     * - 各ログの情報がログ出力される
     */
    @Test
    @DisplayName("displayBatchLogsByName_指定バッチ名のログが複数件_全件表示される")
    void testDisplayBatchLogsByName_whenMultipleLogs_displaysAll() {
        // Given: 指定バッチ名のログが2件存在
        String batchName = "TestBatchJob";
        List<BatchLogEntity> logs = createTestLogsWithName(batchName, 2);
        when(batchLogRepository.findByBatchName(batchName)).thenReturn(logs);

        // When: 特定バッチ名のログを表示
        batchLogQueryService.displayBatchLogsByName(batchName);

        // Then: リポジトリから該当ログが取得され、ログ出力される
        verify(batchLogRepository).findByBatchName(batchName);
        verify(logger, times(3)).info("========================================");
        verify(logger).info(eq("バッチログを取得します: %s"), eq(batchName));
        verify(logger).info(eq("バッチログ件数: %d件"), eq(2));
        verify(logger).info("----------------------------------------");

        // 各ログの情報が出力される
        for (BatchLogEntity log : logs) {
            verify(logger).info(
                eq("ID: %d | Message: %s | Status: %s | CreatedAt: %s"),
                eq(log.getId()),
                eq(log.getMessage()),
                eq(log.getStatus()),
                eq(log.getCreatedAt())
            );
        }
    }

    /**
     * 境界値: 特定バッチ名のログを取得（0件）
     * 
     * 条件:
     * - 指定したバッチ名のログが0件
     * 
     * 期待値:
     * - 「該当するバッチログが存在しません」というメッセージが出力される
     */
    @Test
    @DisplayName("displayBatchLogsByName_該当ログが0件_存在しないメッセージを表示")
    void testDisplayBatchLogsByName_whenNoLogs_displaysNoLogsMessage() {
        // Given: 該当するログが0件
        String batchName = "NonExistentBatch";
        when(batchLogRepository.findByBatchName(batchName)).thenReturn(Collections.emptyList());

        // When: 特定バッチ名のログを表示
        batchLogQueryService.displayBatchLogsByName(batchName);

        // Then: 存在しないメッセージが出力される
        verify(batchLogRepository).findByBatchName(batchName);
        verify(logger, times(3)).info("========================================");
        verify(logger).info(eq("バッチログを取得します: %s"), eq(batchName));
        verify(logger).info("該当するバッチログが存在しません");
    }

    /**
     * 境界値: 特定バッチ名のログを取得（1件）
     * 
     * 条件:
     * - 指定したバッチ名のログが1件のみ
     * 
     * 期待値:
     * - 1件のログが表示される
     */
    @Test
    @DisplayName("displayBatchLogsByName_該当ログが1件_1件表示される")
    void testDisplayBatchLogsByName_whenSingleLog_displaysSingle() {
        // Given: 該当するログが1件
        String batchName = "SingleBatch";
        List<BatchLogEntity> logs = createTestLogsWithName(batchName, 1);
        when(batchLogRepository.findByBatchName(batchName)).thenReturn(logs);

        // When: 特定バッチ名のログを表示
        batchLogQueryService.displayBatchLogsByName(batchName);

        // Then: 1件のログが表示される
        verify(batchLogRepository).findByBatchName(batchName);
        verify(logger).info(eq("バッチログ件数: %d件"), eq(1));
        verify(logger).info(
            eq("ID: %d | Message: %s | Status: %s | CreatedAt: %s"),
            anyLong(),
            anyString(),
            anyString(),
            any(LocalDateTime.class)
        );
        verify(logger).info(eq("バッチログを取得します: %s"), eq(batchName));
        verify(logger).info("----------------------------------------");
        verify(logger, times(3)).info("========================================");
    }

    /**
     * 境界値: null のバッチ名でログを取得
     * 
     * 条件:
     * - バッチ名にnullを指定
     * 
     * 期待値:
     * - リポジトリにnullが渡される
     * - 空のリストが返却される想定
     */
    @Test
    @DisplayName("displayBatchLogsByName_バッチ名がnull_リポジトリに渡される")
    void testDisplayBatchLogsByName_whenNullBatchName_passesToRepository() {
        // Given: バッチ名がnull
        when(batchLogRepository.findByBatchName(null)).thenReturn(Collections.emptyList());

        // When: nullのバッチ名でログを表示
        batchLogQueryService.displayBatchLogsByName(null);

        // Then: リポジトリにnullが渡される
        verify(batchLogRepository).findByBatchName(null);
        verify(logger).info(eq("バッチログを取得します: %s"), eq((String) null));
        verify(logger).info("該当するバッチログが存在しません");
        verify(logger, times(3)).info("========================================");
    }

    /**
     * 境界値: 空文字のバッチ名でログを取得
     * 
     * 条件:
     * - バッチ名に空文字を指定
     * 
     * 期待値:
     * - リポジトリに空文字が渡される
     */
    @Test
    @DisplayName("displayBatchLogsByName_バッチ名が空文字_リポジトリに渡される")
    void testDisplayBatchLogsByName_whenEmptyBatchName_passesToRepository() {
        // Given: バッチ名が空文字
        String emptyBatchName = "";
        when(batchLogRepository.findByBatchName(emptyBatchName)).thenReturn(Collections.emptyList());

        // When: 空文字のバッチ名でログを表示
        batchLogQueryService.displayBatchLogsByName(emptyBatchName);

        // Then: リポジトリに空文字が渡される
        verify(batchLogRepository).findByBatchName(emptyBatchName);
        verify(logger).info(eq("バッチログを取得します: %s"), eq(emptyBatchName));
        verify(logger).info("該当するバッチログが存在しません");
        verify(logger, times(3)).info("========================================");
    }

    /**
     * 正常系: バッチログ件数を取得（複数件）
     * 
     * 条件:
     * - バッチログが5件存在
     * 
     * 期待値:
     * - 5が返却される
     * - 件数がログ出力される
     */
    @Test
    @DisplayName("countAllBatchLogs_複数件のログが存在_件数を返す")
    void testCountAllBatchLogs_whenMultipleLogs_returnsCount() {
        // Given: バッチログが5件存在
        List<BatchLogEntity> logs = createTestLogs(5);
        when(batchLogRepository.findAll()).thenReturn(logs);

        // When: バッチログ件数を取得
        int count = batchLogQueryService.countAllBatchLogs();

        // Then: 5が返却される
        assertEquals(5, count);
        verify(batchLogRepository).findAll();
        verify(logger).info(eq("現在のバッチログ件数: %d件"), eq(5));
    }

    /**
     * 境界値: バッチログ件数を取得（0件）
     * 
     * 条件:
     * - バッチログが0件
     * 
     * 期待値:
     * - 0が返却される
     */
    @Test
    @DisplayName("countAllBatchLogs_ログが0件_0を返す")
    void testCountAllBatchLogs_whenNoLogs_returnsZero() {
        // Given: バッチログが0件
        when(batchLogRepository.findAll()).thenReturn(Collections.emptyList());

        // When: バッチログ件数を取得
        int count = batchLogQueryService.countAllBatchLogs();

        // Then: 0が返却される
        assertEquals(0, count);
        verify(batchLogRepository).findAll();
        verify(logger).info(eq("現在のバッチログ件数: %d件"), eq(0));
    }

    /**
     * 境界値: バッチログ件数を取得（1件）
     * 
     * 条件:
     * - バッチログが1件のみ
     * 
     * 期待値:
     * - 1が返却される
     */
    @Test
    @DisplayName("countAllBatchLogs_ログが1件_1を返す")
    void testCountAllBatchLogs_whenSingleLog_returnsOne() {
        // Given: バッチログが1件
        List<BatchLogEntity> logs = createTestLogs(1);
        when(batchLogRepository.findAll()).thenReturn(logs);

        // When: バッチログ件数を取得
        int count = batchLogQueryService.countAllBatchLogs();

        // Then: 1が返却される
        assertEquals(1, count);
        verify(batchLogRepository).findAll();
        verify(logger).info(eq("現在のバッチログ件数: %d件"), eq(1));
    }

    /**
     * 境界値: バッチログ件数を取得（大量データ）
     * 
     * 条件:
     * - バッチログが1000件存在
     * 
     * 期待値:
     * - 1000が返却される
     */
    @Test
    @DisplayName("countAllBatchLogs_大量データ_正確な件数を返す")
    void testCountAllBatchLogs_whenLargeDataset_returnsAccurateCount() {
        // Given: バッチログが1000件存在
        List<BatchLogEntity> logs = createTestLogs(1000);
        when(batchLogRepository.findAll()).thenReturn(logs);

        // When: バッチログ件数を取得
        int count = batchLogQueryService.countAllBatchLogs();

        // Then: 1000が返却される
        assertEquals(1000, count);
        verify(batchLogRepository).findAll();
        verify(logger).info(eq("現在のバッチログ件数: %d件"), eq(1000));
    }

    // ========================================
    // テストデータ作成用ヘルパーメソッド
    // ========================================

    /**
     * テスト用のバッチログリストを作成
     * 
     * @param count 作成する件数
     * @return バッチログのリスト
     */
    private List<BatchLogEntity> createTestLogs(int count) {
        List<BatchLogEntity> logs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BatchLogEntity log = new BatchLogEntity();
            log.setId((long) i);
            log.setBatchName("TestBatch" + i);
            log.setMessage("Test message " + i);
            log.setStatus("SUCCESS");
            log.setCreatedAt(LocalDateTime.now().minusHours(i));
            logs.add(log);
        }
        return logs;
    }

    /**
     * 指定したバッチ名でテスト用のバッチログリストを作成
     * 
     * @param batchName バッチ名
     * @param count 作成する件数
     * @return バッチログのリスト
     */
    private List<BatchLogEntity> createTestLogsWithName(String batchName, int count) {
        List<BatchLogEntity> logs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BatchLogEntity log = new BatchLogEntity();
            log.setId((long) i);
            log.setBatchName(batchName);
            log.setMessage("Test message " + i);
            log.setStatus("SUCCESS");
            log.setCreatedAt(LocalDateTime.now().minusHours(i));
            logs.add(log);
        }
        return logs;
    }
}
