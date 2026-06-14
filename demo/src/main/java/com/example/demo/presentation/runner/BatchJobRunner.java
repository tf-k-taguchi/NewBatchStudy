package com.example.demo.presentation.runner;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * BatchJobRunner（バッチジョブ実行ランナー）
 * 
 * 【責務】
 * - アプリケーション起動時にバッチジョブを実行
 * - コマンドライン引数に応じて実行するジョブを切り替え
 * 
 * 【実行方法】
 * 1. HelloAwsBatchJob（SQS処理）のみ実行:
 *    mvn spring-boot:run -Dspring-boot.run.arguments="--job=hello"
 * 
 * 2. Taskletモデル（性能測定）のみ実行:
 *    mvn spring-boot:run -Dspring-boot.run.arguments="--job=tasklet"
 * 
 * 3. Chunkモデル（性能測定）のみ実行:
 *    mvn spring-boot:run -Dspring-boot.run.arguments="--job=chunk"
 * 
 * 4. 性能比較（TaskletとChunk両方実行）:
 *    mvn spring-boot:run -Dspring-boot.run.arguments="--job=compare"
 * 
 * 5. 全て実行:
 *    mvn spring-boot:run -Dspring-boot.run.arguments="--job=all"
 * 
 * 6. 引数なし（デフォルト: HelloAwsBatchJobのみ実行）:
 *    mvn spring-boot:run
 * 
 * 【設計思想】
 * - 開発時は個別に実行できるようにする
 * - 性能測定時は比較モードで実行
 * - デフォルトは本番想定のHelloAwsBatchJob
 * 
 * 【追跡性】
 * - 実行Job: {@link com.example.demo.config.HelloAwsJobConfig}
 * - 実行Job: {@link com.example.demo.config.TaskletPerformanceJobConfig}
 * - 実行Job: {@link com.example.demo.config.ChunkPerformanceJobConfig}
 */
@Component
public class BatchJobRunner implements CommandLineRunner {
    
    private final JobLauncher jobLauncher;
    private final Job helloAwsBatchJob;
    private final Job taskletPerformanceJob;
    private final Job chunkPerformanceJob;
    
    /**
     * コンストラクタインジェクション
     * 
     * @param jobLauncher Spring BatchのJobLauncher
     * @param helloAwsBatchJob HelloAwsBatchJob
     * @param taskletPerformanceJob TaskletPerformanceJob
     * @param chunkPerformanceJob ChunkPerformanceJob
     */
    public BatchJobRunner(
            JobLauncher jobLauncher,
            @Qualifier("helloAwsBatchJob") Job helloAwsBatchJob,
            @Qualifier("taskletPerformanceJob") Job taskletPerformanceJob,
            @Qualifier("chunkPerformanceJob") Job chunkPerformanceJob) {
        this.jobLauncher = jobLauncher;
        this.helloAwsBatchJob = helloAwsBatchJob;
        this.taskletPerformanceJob = taskletPerformanceJob;
        this.chunkPerformanceJob = chunkPerformanceJob;
    }
    
    /**
     * アプリケーション起動時に実行されるメソッド
     * 
     * @param args コマンドライン引数
     * @throws Exception 実行エラー
     */
    @Override
    public void run(String... args) throws Exception {
        // コマンドライン引数から実行モード取得
        String jobMode = getJobMode(args);
        
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║   Spring Batch ジョブ実行ランナー                     ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        
        switch (jobMode) {
            case "hello":
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // HelloAwsBatchJob（SQS処理）のみ実行
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: HelloAwsBatchJob（SQS処理）\n");
                System.out.println("【説明】");
                System.out.println("- SQSからメッセージを受信して処理");
                System.out.println("- Taskletモデルを使用");
                System.out.println("- 冪等性チェック、リトライ、DLQ送信を実装\n");
                jobLauncher.run(helloAwsBatchJob, jobParameters);
                break;
                
            case "tasklet":
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // Taskletモデル（性能測定）のみ実行
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: Taskletモデル（性能測定）\n");
                System.out.println("【説明】");
                System.out.println("- 1件ずつINSERTする処理");
                System.out.println("- Chunkモデルとの性能比較用");
                System.out.println("- 処理時間を計測します\n");
                jobLauncher.run(taskletPerformanceJob, jobParameters);
                break;
                
            case "chunk":
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // Chunkモデル（性能測定）のみ実行
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: Chunkモデル（性能測定）\n");
                System.out.println("【説明】");
                System.out.println("- バルクINSERTで高速処理");
                System.out.println("- Taskletモデルとの性能比較用");
                System.out.println("- 処理時間を計測します\n");
                jobLauncher.run(chunkPerformanceJob, jobParameters);
                break;
                
            case "compare":
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // 性能比較モード（TaskletとChunk両方実行）
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: 性能比較（Tasklet vs Chunk）\n");
                System.out.println("【説明】");
                System.out.println("- TaskletとChunkの両方を実行して性能を比較");
                System.out.println("- 処理時間の差を確認できます\n");
                
                // Taskletモデル実行
                System.out.println("\n【1/2】Taskletモデル実行開始");
                jobLauncher.run(taskletPerformanceJob, jobParameters);
                
                // 少し待機
                Thread.sleep(2000);
                
                // Chunkモデル実行
                System.out.println("\n【2/2】Chunkモデル実行開始");
                JobParameters jobParametersChunk = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(chunkPerformanceJob, jobParametersChunk);
                
                System.out.println("\n╔════════════════════════════════════════════════════════╗");
                System.out.println("║   パフォーマンス比較完了                               ║");
                System.out.println("║   上記のログから実行時間を比較してください             ║");
                System.out.println("╚════════════════════════════════════════════════════════╝\n");
                break;
                
            case "all":
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // 全てのジョブを実行
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: 全てのジョブを実行\n");
                
                // HelloAwsBatchJob実行
                System.out.println("\n【1/3】HelloAwsBatchJob実行開始");
                jobLauncher.run(helloAwsBatchJob, jobParameters);
                Thread.sleep(2000);
                
                // Taskletモデル実行
                System.out.println("\n【2/3】Taskletモデル実行開始");
                JobParameters jobParametersTasklet = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(taskletPerformanceJob, jobParametersTasklet);
                Thread.sleep(2000);
                
                // Chunkモデル実行
                System.out.println("\n【3/3】Chunkモデル実行開始");
                JobParameters jobParametersChunkAll = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();
                jobLauncher.run(chunkPerformanceJob, jobParametersChunkAll);
                
                System.out.println("\n╔════════════════════════════════════════════════════════╗");
                System.out.println("║   全てのジョブ実行完了                                 ║");
                System.out.println("╚════════════════════════════════════════════════════════╝\n");
                break;
                
            default:
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                // デフォルト: HelloAwsBatchJobのみ実行
                // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                System.out.println("実行モード: HelloAwsBatchJob（デフォルト）\n");
                System.out.println("【説明】");
                System.out.println("- 引数なしの場合、HelloAwsBatchJobを実行");
                System.out.println("- 他のジョブを実行する場合は --job=<モード> を指定\n");
                System.out.println("【利用可能なモード】");
                System.out.println("  --job=hello    : HelloAwsBatchJob（SQS処理）");
                System.out.println("  --job=tasklet  : Taskletモデル（性能測定）");
                System.out.println("  --job=chunk    : Chunkモデル（性能測定）");
                System.out.println("  --job=compare  : 性能比較（TaskletとChunk）");
                System.out.println("  --job=all      : 全てのジョブを実行\n");
                jobLauncher.run(helloAwsBatchJob, jobParameters);
                break;
        }
    }
    
    /**
     * コマンドライン引数からジョブモード取得
     * 
     * 【サポートするモード】
     * - hello: HelloAwsBatchJob（SQS処理）
     * - tasklet: Taskletモデル（性能測定）
     * - chunk: Chunkモデル（性能測定）
     * - compare: 性能比較（TaskletとChunk）
     * - all: 全てのジョブを実行
     * - デフォルト: HelloAwsBatchJob
     * 
     * @param args コマンドライン引数
     * @return ジョブモード
     */
    private String getJobMode(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--job=")) {
                return arg.substring(6).toLowerCase();
            }
        }
        return "default";  // デフォルトはHelloAwsBatchJob
    }
}
