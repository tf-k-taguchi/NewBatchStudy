package com.example.demo.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * PerformanceJobListener（パフォーマンス計測用）
 * 
 * 【責務】
 * - Job全体の実行時間を計測
 * - 開始時刻・終了時刻・実行時間をログ出力
 * 
 * 【使用目的】
 * TaskletモデルとChunkモデルの性能比較
 * 
 * 【計測内容】
 * - 開始時刻
 * - 終了時刻
 * - 実行時間（ミリ秒・秒）
 * - ステータス（COMPLETED/FAILED等）
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.TaskletPerformanceJobConfig}
 * - 使用元Config: {@link com.example.demo.config.ChunkPerformanceJobConfig}
 */
@Component
public class PerformanceJobListener implements JobExecutionListener {
    
    private long startTime;
    
    /**
     * Job開始前の処理
     * 
     * @param jobExecution Job実行情報
     */
    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        String jobName = jobExecution.getJobInstance().getJobName();
        System.out.println("\n========================================");
        System.out.println("Job開始: " + jobName);
        System.out.println("開始時刻: " + new java.util.Date(startTime));
        System.out.println("========================================\n");
    }
    
    /**
     * Job終了後の処理
     * 
     * @param jobExecution Job実行情報
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        String jobName = jobExecution.getJobInstance().getJobName();
        
        System.out.println("\n========================================");
        System.out.println("Job終了: " + jobName);
        System.out.println("終了時刻: " + new java.util.Date(endTime));
        System.out.println("実行時間: " + duration + " ms (" + (duration / 1000.0) + " 秒)");
        System.out.println("ステータス: " + jobExecution.getStatus());
        System.out.println("========================================\n");
    }
}
