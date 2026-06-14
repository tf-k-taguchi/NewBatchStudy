package com.example.demo.config;

import com.example.demo.batch.listener.PerformanceJobListener;
import com.example.demo.batch.tasklet.TaskletPerformanceTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * TaskletPerformanceJob設定クラス
 * 
 * 【責務】
 * - Taskletモデルのパフォーマンス比較Job設計図を定義
 * - 実処理（Tasklet）は別クラスに委譲
 * 
 * 【処理方式】
 * 1件ずつInsert（低速パターン）
 * 
 * 【目的】
 * Chunkモデルとの性能差を実証
 * 
 * 【追跡性】
 * - Job名: TaskletPerformanceJob
 * - Step名: TaskletPerformanceStep
 * - Tasklet実装: {@link TaskletPerformanceTasklet}
 * - Listener: {@link PerformanceJobListener}
 * 
 * 【IDEでの追跡方法】
 * - TaskletPerformanceTaskletをCmd+クリック → 実処理クラスへジャンプ
 * - PerformanceJobListenerをCmd+クリック → リスナークラスへジャンプ
 */
@Configuration
public class TaskletPerformanceJobConfig {
    
    /**
     * TaskletPerformanceJob定義
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param taskletPerformanceStep 実行するStep
     * @param performanceJobListener 実行時間計測Listener
     * @return Job定義
     */
    @Bean
    public Job taskletPerformanceJob(
            JobRepository jobRepository,
            Step taskletPerformanceStep,
            PerformanceJobListener performanceJobListener) {
        return new JobBuilder("TaskletPerformanceJob", jobRepository)
        // ジョブやステップの前後に特定の処理(ログ出力,リソースのクリーンアップ,メールの通知)を挿入するためのインタフェース
                .listener(performanceJobListener)
        //start()はステップの起点
        //1ジョブに対して1step以上存在してないとエラーが起きる
                .start(taskletPerformanceStep)
        //next()　今回はないが前の処理が完了した後に実行するものを指定する
                .build();
    }
    
    /**
     * TaskletPerformanceStep定義
     * これはStepファイルで定義をすればよかった
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param transactionManager トランザクションマネージャー
     * @param taskletPerformanceTasklet 実処理を行うTasklet（別クラス）
     * @return Step定義
     */
    @Bean
    public Step taskletPerformanceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TaskletPerformanceTasklet taskletPerformanceTasklet) {
        return new StepBuilder("TaskletPerformanceStep", jobRepository)
                .tasklet(taskletPerformanceTasklet, transactionManager)
                .build();
                //build()メソッドを呼び出した瞬間,Spring Batchは裏側でStepインタフェース
                //のルールを完璧に守ってexecuteやgetNameを実装済みのクラスTaskletStepクラス
                // などを自動的に生成してくれる
    }
}
