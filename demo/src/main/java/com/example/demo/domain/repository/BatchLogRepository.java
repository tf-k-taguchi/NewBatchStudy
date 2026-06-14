package com.example.demo.domain.repository;

import com.example.demo.domain.entity.BatchLogEntity;

import java.util.List;

/**
 * バッチログリポジトリインターフェース
 *
 * 【リポジトリ・インターフェース】
 * このインターフェースは0510処理フロー.mdの「ドメイン・モデリング（リポジトリ・インターフェース）」に該当します。
 * ドメイン層がインフラ層の詳細を知らなくても良いよう、データの永続化に関する抽象的なインターフェースを定義します。
 *
 * 【設計思想】
 * - ドメイン層に配置（domain.repository）
 * - 実装はインフラ層に配置（infrastructure.repository）
 * - 依存性逆転の原則（DIP）を適用
 * - テスタビリティを重視（モック化が容易）
 *
 * 【責務】
 * バッチログの永続化に関する操作を定義
 * - 挿入: insert()
 * - 検索: findAll(), findByBatchName(), findByMessageId()
 * - 冪等性チェック: findProcessingByMessageId()
 *
 * 【実装】
 * BatchLogRepositoryImpl（インフラ層）がこのインターフェースを実装し、
 * MyBatisを使用して実際のDB操作を行います。
 *
 * 【境界設計】
 * 0510処理フロー.mdの「境界設計とデータ変換」に該当：
 * - ドメイン層: BatchLogEntity（ドメインモデル）
 * - インフラ層: MyBatis Mapper（DB操作）
 * - この境界でデータ変換が行われる
 */
public interface BatchLogRepository {
    
    /**
     * バッチログを挿入
     *
     * 【用途】
     * - 処理開始時: status='PROCESSING'で記録
     * - 処理成功時: status='SUCCESS'で記録
     * - 処理失敗時: status='FAILED'で記録
     *
     * 【冪等性の担保】
     * messageIdとstatusを組み合わせて、重複実行をチェックします。
     *
     * @param entity バッチログエンティティ
     */
    void insert(BatchLogEntity entity);
    
    /**
     * 全バッチログを取得
     * @return バッチログリスト
     */
    List<BatchLogEntity> findAll();
    
    /**
     * バッチ名でバッチログを検索
     * @param batchName バッチ名
     * @return バッチログリスト
     */
    List<BatchLogEntity> findByBatchName(String batchName);
    
    /**
     * メッセージIDで処理中のレコードを検索
     *
     * 【冪等性チェック】
     * このメソッドは0510処理フロー.mdの「整合性と検証（冪等性の担保）」に該当します。
     * taguchiStudy.mdの「データの重複登録」対策として使用されます。
     *
     * 【使用箇所】
     * BatchProcessService.isMessageProcessing()で呼び出され、
     * 同じメッセージを二重処理しないことを保証します。
     *
     * 【検索条件】
     * - message_id = 指定されたメッセージID
     * - status = 'PROCESSING'
     *
     * 【戻り値】
     * - 存在する場合: 他のインスタンスまたは前回の処理がまだ実行中
     * - 存在しない場合: 未処理または処理完了済み
     *
     * @param messageId メッセージID
     * @return 処理中のバッチログ（存在しない場合はnull）
     */
    BatchLogEntity findProcessingByMessageId(String messageId);
    
    /**
     * メッセージIDで検索
     * @param messageId メッセージID
     * @return バッチログリスト
     */
    List<BatchLogEntity> findByMessageId(String messageId);
}
