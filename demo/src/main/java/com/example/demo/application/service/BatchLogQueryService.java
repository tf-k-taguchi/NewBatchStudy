package com.example.demo.application.service;

import com.example.demo.domain.entity.BatchLogEntity;
import com.example.demo.domain.repository.BatchLogRepository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * バッチログ照会サービス
 * Application層のクエリサービス
 * 
 * 責務:
 * - バッチログの照会
 * - データ確認用のログ出力
 */
@Service
@Transactional(readOnly = true)
public class BatchLogQueryService {
    
    private final BatchLogger logger;
    private final BatchLogRepository batchLogRepository;
    
    public BatchLogQueryService(
            BatchLogger logger,
            BatchLogRepository batchLogRepository) {
        this.logger = logger;
        this.batchLogRepository = batchLogRepository;
    }
    
    /**
     * 全バッチログを取得して表示
     */
    public void displayAllBatchLogs() {
        logger.info("========================================");
        logger.info("バッチログ一覧を取得します");
        logger.info("========================================");
        
        List<BatchLogEntity> logs = batchLogRepository.findAll();
        
        if (logs.isEmpty()) {
            logger.info("バッチログが存在しません");
        } else {
            logger.info("バッチログ件数: %d件", logs.size());
            logger.info("----------------------------------------");
            for (BatchLogEntity log : logs) {
                logger.info("ID: %d | BatchName: %s | Message: %s | Status: %s | CreatedAt: %s",
                        log.getId(),
                        log.getBatchName(),
                        log.getMessage(),
                        log.getStatus(),
                        log.getCreatedAt());
            }
        }
        
        logger.info("========================================");
    }
    
    /**
     * 特定バッチ名のログを取得して表示
     */
    public void displayBatchLogsByName(String batchName) {
        logger.info("========================================");
        logger.info("バッチログを取得します: %s", batchName);
        logger.info("========================================");
        
        List<BatchLogEntity> logs = batchLogRepository.findByBatchName(batchName);
        
        if (logs.isEmpty()) {
            logger.info("該当するバッチログが存在しません");
        } else {
            logger.info("バッチログ件数: %d件", logs.size());
            logger.info("----------------------------------------");
            for (BatchLogEntity log : logs) {
                logger.info("ID: %d | Message: %s | Status: %s | CreatedAt: %s",
                        log.getId(),
                        log.getMessage(),
                        log.getStatus(),
                        log.getCreatedAt());
            }
        }
        
        logger.info("========================================");
    }
    
    /**
     * バッチログ件数を取得
     */
    public int countAllBatchLogs() {
        List<BatchLogEntity> logs = batchLogRepository.findAll();
        int count = logs.size();
        logger.info("現在のバッチログ件数: %d件", count);
        return count;
    }
}
