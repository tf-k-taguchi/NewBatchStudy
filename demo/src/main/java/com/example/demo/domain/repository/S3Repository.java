package com.example.demo.domain.repository;

/**
 * S3リポジトリインターフェース
 * AWS S3との連携を抽象化
 */
public interface S3Repository {
    
    /**
     * S3にファイルをアップロード
     * @param key ファイルキー
     * @param content ファイル内容
     */
    void uploadFile(String key, String content);
    
    /**
     * S3からファイルをダウンロード
     * @param key ファイルキー
     * @return ファイル内容
     */
    String downloadFile(String key);
    
    /**
     * S3のファイルを削除
     * @param key ファイルキー
     */
    void deleteFile(String key);
}
