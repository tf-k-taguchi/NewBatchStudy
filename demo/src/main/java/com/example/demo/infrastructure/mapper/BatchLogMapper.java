package com.example.demo.infrastructure.mapper;

import com.example.demo.domain.entity.BatchLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * バッチログマッパー
 * MyBatisマッパーインターフェース（XML設定使用）
 */
@Mapper
public interface BatchLogMapper {
    
    /**
     * バッチログを挿入
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
    List<BatchLogEntity> findByBatchName(@Param("batchName") String batchName);
    
    /**
     * メッセージIDで処理中のレコードを検索
     * @param messageId メッセージID
     * @return 処理中のバッチログ（存在しない場合はnull）
     */
    BatchLogEntity findProcessingByMessageId(@Param("messageId") String messageId);
    
    /**
     * メッセージIDで検索
     * @param messageId メッセージID
     * @return バッチログリスト
     */
    List<BatchLogEntity> findByMessageId(@Param("messageId") String messageId);
}
