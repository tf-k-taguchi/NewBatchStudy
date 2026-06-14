package com.example.demo.batch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * TaskletPerformanceTasklet（単一Insert実装）
 * 
 * 【責務】
 * - Taskletモデルのパフォーマンス測定用実処理
 * - 1件ずつJdbcTemplate.updateを実行してInsert
 * 
 * 【目的】
 * 意図的にDBへのI/Oオーバーヘッドを発生させる
 * 
 * 【処理方式】
 * 1件ずつInsert（低速パターン）
 * 
 * 【注意】
 * これはアンチパターンです。Chunkモデルとの性能差を明確にするための実装です。
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.TaskletPerformanceJobConfig}
 * - 比較対象: {@link com.example.demo.batch.chunk.ChunkPerformanceItemWriter}
 */
@Component
public class TaskletPerformanceTasklet implements Tasklet {
    
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${batch.performance.data-count:1000000}")
    private int dataCount;
    
    /**
     * コンストラクタインジェクション
     * 
     * @param jdbcTemplate JDBCテンプレート
     */
    public TaskletPerformanceTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Tasklet実行メソッド
     * 
     * 【処理内容】
     * 1. 指定件数分ループ
     * 2. 1件ずつダミーデータを生成
     * 3. 1件ずつDBへInsert（アンチパターン）
     * 4. 進捗ログ出力（10%ごと）
     * 
     * @param contribution Spring BatchのStep実行結果を格納
     * @param chunkContext Spring BatchのChunk実行コンテキスト
     * @return RepeatStatus.FINISHED（1回だけ実行）
     * @throws Exception 処理中に発生した例外
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) 
            throws Exception {
        
        System.out.println("========================================");
        System.out.println("Taskletモデル処理開始");
        System.out.println("処理件数: " + dataCount + "件");
        System.out.println("処理方式: 1件ずつInsert（低速パターン）");
        System.out.println("========================================\n");
        
        // SQL文（1件ずつInsert）
        String sql = "INSERT INTO dummy_record (data_value, created_at) VALUES (?, ?)";
        
        // 進捗表示用
        int progressInterval = dataCount / 10; // 10%ごとに表示
        
        // ========================================
        // メインループ: 100万回処理
        // ※意図的に1件ずつInsertしてI/Oオーバーヘッドを発生させる
        // ========================================
        for (int i = 1; i <= dataCount; i++) {
            // ダミーデータ生成
            String dataValue = String.format("TaskletData-%010d", i);
            LocalDateTime createdAt = LocalDateTime.now();
            
            // 1件ずつDBへInsert（アンチパターン）
            jdbcTemplate.update(sql, dataValue, createdAt);
            
            // 進捗ログ出力（10%ごと）
            if (i % progressInterval == 0) {
                int progress = (i * 100) / dataCount;
                System.out.println(String.format(
                    "[Tasklet進捗] %d%% 完了 (%,d / %,d 件)", 
                    progress, i, dataCount
                ));
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("Taskletモデル処理完了");
        System.out.println("総処理件数: " + dataCount + "件");
        System.out.println("========================================\n");
        
        // StepContributionに処理件数を記録
        contribution.incrementWriteCount(dataCount);
        
        return RepeatStatus.FINISHED;
    }
}
