package com.example.demo.infrastructure.repository;

import com.example.demo.domain.repository.S3Repository;
import com.example.demo.infrastructure.common.logging.BatchLogger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * S3リポジトリのMock実装
 * ローカル開発用にメモリ上のMapで模擬
 */
@Component
@Profile("local")
public class S3RepositoryMock implements S3Repository {
    
    private final BatchLogger logger;
    private final Map<String, String> storage = new HashMap<>();
    
    public S3RepositoryMock(BatchLogger logger) {
        this.logger = logger;
    }
    
    @Override
    public void uploadFile(String key, String content) {
        storage.put(key, content);
        logger.info("S3 Mock: ファイルアップロード - Key: %s, Size: %d bytes", 
                    key, content.length());
    }
    
    @Override
    public String downloadFile(String key) {
        String content = storage.get(key);
        if (content != null) {
            logger.info("S3 Mock: ファイルダウンロード - Key: %s", key);
        } else {
            logger.warn("S3 Mock: ファイルが見つかりません - Key: %s", key);
        }
        return content;
    }
    
    @Override
    public void deleteFile(String key) {
        String removed = storage.remove(key);
        if (removed != null) {
            logger.info("S3 Mock: ファイル削除 - Key: %s", key);
        } else {
            logger.warn("S3 Mock: 削除対象ファイルが見つかりません - Key: %s", key);
        }
    }
}
