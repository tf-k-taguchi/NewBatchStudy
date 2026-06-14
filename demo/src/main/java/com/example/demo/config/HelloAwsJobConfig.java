package com.example.demo.config;

import com.example.demo.batch.tasklet.HelloAwsTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * HelloAwsBatchJob設定クラス
 * 
 * 【責務】
 * - JobとStepの設計図を定義
 * - 実処理（Tasklet）は別クラスに委譲
 * 
 * 【追跡性】
 * - Job名: HelloAwsBatchJob
 * - Step名: HelloAwsStep
 * - Tasklet実装: {@link HelloAwsTasklet}
 * 
 * 【IDEでの追跡方法】
 * - HelloAwsTaskletをCmd+クリック → 実処理クラスへジャンプ
 */
@Configuration
public class HelloAwsJobConfig {
    
    /**
     * HelloAwsBatchJob定義
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param helloAwsStep 実行するStep
     * @return Job定義
     */
    @Bean
    public Job helloAwsBatchJob(
            JobRepository jobRepository,
            Step helloAwsStep) {
        return new JobBuilder("HelloAwsBatchJob", jobRepository)
                .start(helloAwsStep)
                .build();
    }
    
    /**
     * HelloAwsStep定義
     * 
     * @param jobRepository Spring BatchのJobリポジトリ
     * @param transactionManager トランザクションマネージャー
     * @param helloAwsTasklet 実処理を行うTasklet（別クラス）
     * @return Step定義
     */
    @Bean
    public Step helloAwsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            HelloAwsTasklet helloAwsTasklet) {
        return new StepBuilder("HelloAwsStep", jobRepository)
                .tasklet(helloAwsTasklet, transactionManager)
                .build();
    }
}
