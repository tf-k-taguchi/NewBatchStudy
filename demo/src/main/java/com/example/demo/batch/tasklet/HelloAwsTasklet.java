package com.example.demo.batch.tasklet;

import com.example.demo.application.service.BatchProcessService;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * HelloAwsTasklet
 * 
 * 【責務】
 * - Spring Batchフレームワークとの接点（execute()メソッド）
 * - StepContributionの制御（ExitStatusの設定等）
 * - ビジネスロジックはBatchProcessServiceに委譲
 * 
 * 【レイヤー設計】
 * このクラスは0510処理フロー.mdの「現状分析（関心事の抽出）」に基づき、
 * Spring Batchフレームワークとビジネスロジックを完全に分離しています。
 * 
 * 【Taskletを選ぶ理由】
 * taguchiStudy.mdの「Taskletを選ぶべきシーン」に該当：
 * - キューにデータを入れる（Produce側の処理）
 * - データ件数に関係なく、1回の処理で終わる作業
 * - SQSからメッセージを1件取得して処理する
 * 
 * 【処理フロー】
 * 1. キューにメッセージがあるかチェック
 * 2. メッセージがあればBatchProcessServiceに処理を委譲
 * 3. 処理結果に応じてExitStatusを設定
 * 4. 例外発生時はFAILEDステータスを設定して再スロー
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.HelloAwsJobConfig}
 * - 委譲先Service: {@link BatchProcessService}
 */
@Component
public class HelloAwsTasklet implements Tasklet {
    
    private final BatchLogger logger;
    private final BatchProcessService batchProcessService;
    

    /**
     * コンストラクタインジェクション
     * 
     * @param logger バッチロガー
     * @param batchProcessService ビジネスロジックを実行するサービス
     */
    public HelloAwsTasklet(
            BatchLogger logger,
            BatchProcessService batchProcessService) {
        this.logger = logger;
        this.batchProcessService = batchProcessService;
    }
    
    /**
     * Taskletの実行メソッド
     * 
     * 【Spring Batchの制御】
     * このメソッドはSpring Batchフレームワークから呼び出されます。
     * StepContributionを通じて、Stepの実行結果をフレームワークに通知します。
     * 
     * 【処理フロー】
     * 1. キューにメッセージがあるかチェック
     *    → ない場合: 正常終了（COMPLETED）
     *    → ある場合: ステップ2へ
     * 
     * 2. BatchProcessServiceにビジネスロジックを委譲
     *    → 0510処理フロー.mdの「ユースケース実装」に該当
     *    → インフラストラクチャの詳細を知らない
     * 
     * 3. 正常終了時: ExitStatus.COMPLETEDを設定
     * 
     * 4. 例外発生時: ExitStatus.FAILEDを設定して再スロー
     *    → Spring Batchが自動的にロールバックを実行
     * 
     * 【戻り値】
     * RepeatStatus.FINISHED: このTaskletは1回だけ実行される
     * （RepeatStatus.CONTINUABLEを返すと繰り返し実行される）
     * 
     * @param contribution Spring BatchのStep実行結果を格納
     * @param chunkContext Spring BatchのChunk実行コンテキスト
     * @return RepeatStatus.FINISHED（1回だけ実行）
     * @throws Exception 処理中に発生した例外
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {
        
        logger.info("Tasklet開始: HelloAwsStep");
        
        try {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ1: キューにメッセージがあるかチェック
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【目的】無駄な処理を避けるため、事前にメッセージの有無を確認
            // 【実装】SQSのApproximateNumberOfMessagesを取得
            if (!batchProcessService.hasMessagesInQueue()) {
                logger.info("キューにメッセージがありません。処理をスキップします。");
                contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
                return RepeatStatus.FINISHED;
            }
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ2: Application層のServiceにビジネスロジックを委譲
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【設計】0510処理フロー.mdの「現状分析（関心事の抽出）」に該当
            // 【責務分離】Taskletはフレームワーク制御のみ、ビジネスロジックはServiceへ
            batchProcessService.executeBatchProcess();
            
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ3: 正常終了時のステータス設定
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED);
            logger.info("Tasklet正常終了: HelloAwsStep");
            
            return RepeatStatus.FINISHED;
            
        } catch (Exception ex) {
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // ステップ4: エラー時のステータス設定
            // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            // 【重要】ExitStatus.FAILEDを設定することで、Spring Batchが
            // 自動的にトランザクションをロールバックします
            contribution.setExitStatus(org.springframework.batch.core.ExitStatus.FAILED);
            logger.error("Tasklet異常終了: HelloAwsStep", ex);
            
            // 例外を再スローしてSpring Batchに処理を委ねる
            throw ex;
        }
    }
}
