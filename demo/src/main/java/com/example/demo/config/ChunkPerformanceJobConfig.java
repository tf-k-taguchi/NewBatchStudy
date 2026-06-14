package com.example.demo.config;

import com.example.demo.batch.chunk.ChunkPerformanceItemReader;
import com.example.demo.batch.chunk.ChunkPerformanceItemWriter;
import com.example.demo.batch.listener.PerformanceJobListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ChunkPerformanceJob設定クラス
 * 
 * 【責務】
 * - Chunkモデルのパフォーマンス比較Job設計図を定義
 * - 実処理（Reader/Writer）は別クラスに委譲
 * 
 * 【Chunk処理の設定】
 * このクラスはtaguchiStudy.mdの「chunk処理」を実装するための設定です。
 * Spring BatchのChunkモデルを使用して、大量データを効率的に処理します。
 * 
 * 【処理方式】
 * バルクInsert（高速パターン）
 * - Reader: 1件ずつデータを読み込む
 * - Writer: チャンクサイズ分（10,000件）をまとめてバルクINSERT
 * 
 * 【目的】
 * Taskletモデルとの性能差を実証
 * - Tasklet（1件ずつINSERT）: 約60秒
 * - Chunk（バルクINSERT）: 約10秒
 * → 約6倍高速！
 * 
 * 【Chunk処理を選ぶ理由】
 * taguchiStudy.mdの「chunk処理を選ぶべきシーン」に該当：
 * - キューに入ったデータの処理を行う（大量データ処理）
 * - 1回で処理するデータ数を決めることができる（チャンクサイズ）
 * - データを区切ることで、途中で失敗しても最初から処理し直す必要がない
 * 
 * 【メモリ対策】
 * taguchiStudy.mdの「メモリ不足(OOM)対策」：
 * - チャンク単位でコミットすることで、メモリを解放
 * - 100万件のデータを一度にメモリに載せない
 * 
 * 【追跡性】
 * - Job名: ChunkPerformanceJob
 * - Step名: ChunkPerformanceStep
 * - Reader実装: {@link ChunkPerformanceItemReader}
 * - Writer実装: {@link ChunkPerformanceItemWriter}
 * - Listener: {@link PerformanceJobListener}
 * 
 * 【IDEでの追跡方法】
 * - ChunkPerformanceItemReaderをCmd+クリック → Reader実装クラスへジャンプ
 * - ChunkPerformanceItemWriterをCmd+クリック → Writer実装クラスへジャンプ
 * - PerformanceJobListenerをCmd+クリック → リスナークラスへジャンプ
 */
@Configuration
public class ChunkPerformanceJobConfig {
    
    @Value("${batch.performance.chunk-size:10000}")
    private int chunkSize;
    
    /**
     * ChunkPerformanceJob定義
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param chunkPerformanceStep 実行するStep
     * @param performanceJobListener 実行時間計測Listener
     * @return Job定義
     */
    @Bean
    public Job chunkPerformanceJob(
            JobRepository jobRepository,
            Step chunkPerformanceStep,
            PerformanceJobListener performanceJobListener) {
        return new JobBuilder("ChunkPerformanceJob", jobRepository)
                .listener(performanceJobListener)
                .start(chunkPerformanceStep)
                .build();
    }
    
    /**
     * ChunkPerformanceStep定義
     * 
     * 【Chunk処理の構成】
     * Reader → Processor → Writer
     * ↑        ↑(省略)     ↑
     * 1件ずつ              チャンク分まとめて
     * 
     * 【チャンクサイズ】
     * 10,000件
     * - この件数ごとにトランザクションがコミットされる
     * - メモリ使用量とパフォーマンスのバランスを考慮
     * 
     * 【処理フロー】
     * 1. Readerが10,000件読み込む（read()を10,000回呼び出し）
     * 2. Writerが10,000件をまとめてバルクINSERT
     * 3. トランザクションコミット
     * 4. メモリ解放
     * 5. 次のチャンクへ（1に戻る）
     * 
     * 【トランザクション管理】
     * taguchiStudy.mdの「データを区切ることができる」に該当：
     * - 0~10,000件: 成功 → コミット
     * - 10,000~20,000件: 成功 → コミット
     * - 20,000~30,000件: 失敗 → ロールバック（この範囲のみ）
     * → 失敗した範囲だけリトライすればOK
     * 
     * 【Processorの省略】
     * 今回はデータ加工が不要なため、Processorは省略しています。
     * 実際のプロジェクトでは、Processorでデータ変換やバリデーションを行います。
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param transactionManager トランザクションマネージャー
     * @param reader データ読み込み（1件ずつ）
     * @param writer データ書き込み（チャンク分まとめて）
     * @return Step定義
     */
    @Bean
    public Step chunkPerformanceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ChunkPerformanceItemReader reader,
            ChunkPerformanceItemWriter writer) {
        
        System.out.println("========================================");
        System.out.println("Chunkモデル設定");
        System.out.println("チャンクサイズ: " + chunkSize + "件");
        System.out.println("処理方式: バルクInsert（高速パターン）");
        System.out.println("========================================\n");
        
        return new StepBuilder("ChunkPerformanceStep", jobRepository)
                .<String, String>chunk(chunkSize, transactionManager)
                .reader(reader)
                .writer(writer)
                .build();
    }
}
