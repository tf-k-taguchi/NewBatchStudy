-- バッチログテーブル
CREATE TABLE IF NOT EXISTS batch_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_name VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    -- 新規追加カラム
    message_id VARCHAR(255),
    receipt_handle VARCHAR(255),
    error_details TEXT,
    retry_count INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- インデックス作成（H2用の構文）
CREATE INDEX IF NOT EXISTS idx_batch_name ON batch_log(batch_name);
CREATE INDEX IF NOT EXISTS idx_created_at ON batch_log(created_at);
-- 新規追加インデックス
CREATE INDEX IF NOT EXISTS idx_message_id ON batch_log(message_id);
CREATE INDEX IF NOT EXISTS idx_status ON batch_log(status);
CREATE INDEX IF NOT EXISTS idx_message_id_status ON batch_log(message_id, status);

-- パフォーマンス比較用ダミーデータテーブル
CREATE TABLE IF NOT EXISTS dummy_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- パフォーマンス向上のためのインデックス
CREATE INDEX IF NOT EXISTS idx_dummy_created_at ON dummy_record(created_at);
