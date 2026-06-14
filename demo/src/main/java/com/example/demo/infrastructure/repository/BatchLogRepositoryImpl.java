package com.example.demo.infrastructure.repository;

import com.example.demo.domain.entity.BatchLogEntity;
import com.example.demo.domain.repository.BatchLogRepository;
import com.example.demo.infrastructure.mapper.BatchLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * バッチログリポジトリ実装
 * MyBatisマッパーを使用したDB操作
 */
@Repository
public class BatchLogRepositoryImpl implements BatchLogRepository {
    
    private final BatchLogMapper mapper;
    
    public BatchLogRepositoryImpl(BatchLogMapper mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public void insert(BatchLogEntity entity) {
        mapper.insert(entity);
    }
    
    @Override
    public List<BatchLogEntity> findAll() {
        return mapper.findAll();
    }
    
    @Override
    public List<BatchLogEntity> findByBatchName(String batchName) {
        return mapper.findByBatchName(batchName);
    }
    
    @Override
    public BatchLogEntity findProcessingByMessageId(String messageId) {
        return mapper.findProcessingByMessageId(messageId);
    }
    
    @Override
    public List<BatchLogEntity> findByMessageId(String messageId) {
        return mapper.findByMessageId(messageId);
    }
}
