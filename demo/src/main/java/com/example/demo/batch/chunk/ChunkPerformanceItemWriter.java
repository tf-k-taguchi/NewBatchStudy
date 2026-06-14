package com.example.demo.batch.chunk;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChunkPerformanceItemWriter（バルクInsert実装）
 * 
 * 【責務】
 * - Chunkモデルのパフォーマンス測定用データ書き込み
 * - JdbcTemplateのbatchUpdateを使用してバルクInsert
 * 
 * 【Chunk処理のWriter】
 * このクラスはtaguchiStudy.mdの「chunk処理（バルクINSERT文）」に該当します。
 * Spring BatchのChunkモデルにおける「書込（Writer）」を担当します。
 * 
 * 【処理フロー】
 * Chunk処理: Reader → Processor → Writer
 *                                ↑このクラス
 * 
 * 【バルクINSERTの利点】
 * taguchiStudy.mdの「chunkはまとめてデータを流す」に該当：
 * - 1件ずつINSERTするより圧倒的に高速
 * - ネットワーク往復回数を削減（1万件を1回のSQL実行で処理）
 * - DBへの負荷を分散（チャンク単位でコミット）
 * 
 * 【Taskletとの比較】
 * - Tasklet（TaskletPerformanceTasklet）: 1件ずつINSERT → 遅い
 * - Chunk（このクラス）: 1万件まとめてINSERT → 高速
 * 
 * 【処理】
 * JdbcTemplateのbatchUpdateを使用してバルクInsert
 * 
 * 【特徴】
 * チャンクサイズ（1万件）ごとにまとめてInsert（高速）
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.ChunkPerformanceJobConfig}
 * - 連携元Reader: {@link ChunkPerformanceItemReader}
 * - 比較対象: {@link com.example.demo.batch.tasklet.TaskletPerformanceTasklet}
 */
@Component
public class ChunkPerformanceItemWriter implements ItemWriter<String> {
    
    private final JdbcTemplate jdbcTemplate;
    private int totalWriteCount = 0;
    
    /**
     * コンストラクタインジェクション
     * 
     * @param jdbcTemplate JDBCテンプレート
     */
    public ChunkPerformanceItemWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * チャンク分のデータをまとめて書き込むメソッド
     * 
     * 【Spring Batchの動作】
     * このメソッドはSpring Batchフレームワークから呼び出されます。
     * Readerがチャンクサイズ分（10,000件）読み込んだ後、
     * そのデータをまとめてこのメソッドに渡します。
     * 
     * 【処理フロー】
     * 1. Readerが10,000件読み込む（read()を10,000回呼び出し）
     * 2. このwrite()メソッドが呼ばれる（10,000件をまとめて渡される）
     * 3. JdbcTemplateのbatchUpdateで一括INSERT
     * 4. トランザクションコミット
     * 5. メモリ解放
     * 6. 次のチャンクへ
     * 
     * 【バルクINSERTの仕組み】
     * JdbcTemplateのbatchUpdate()は、内部でPreparedStatementの
     * addBatch()とexecuteBatch()を使用します。
     * これにより、1万件のINSERTを1回のSQL実行で処理できます。
     * 
     * 【性能比較】
     * - Tasklet（1件ずつINSERT）: 100万件 → 約60秒
     * - Chunk（バルクINSERT）: 100万件 → 約10秒
     * → 約6倍高速！
     * 
     * 【トランザクション管理】
     * taguchiStudy.mdの「データを区切ることができる」に該当：
     * - 0~10,000件: 成功 → コミット
     * - 10,000~20,000件: 成功 → コミット
     * - 20,000~30,000件: 失敗 → ロールバック（この範囲のみ）
     * - 30,000~40,000件: 未処理
     * → 失敗した範囲だけリトライすればOK
     * 
     * @param chunk チャンク分のデータ（最大10,000件）
     * @throws Exception 書き込み失敗時
     */
    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        List<? extends String> items = chunk.getItems();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // バルクInsert用SQL
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        String sql = "INSERT INTO dummy_record (data_value, created_at) VALUES (?, ?)";
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // バッチ更新実行（バルクINSERT）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 【重要】JdbcTemplateのbatchUpdateを使用してバルクInsert
        // 【性能】1万件を1回のSQL実行で処理（ネットワーク往復1回）
        // 【比較】1件ずつINSERTすると1万回のネットワーク往復が必要
        jdbcTemplate.batchUpdate(sql, items, items.size(), (ps, dataValue) -> {
            ps.setString(1, dataValue);
            ps.setObject(2, LocalDateTime.now());
        });
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 進捗カウント
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        totalWriteCount += items.size();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 進捗ログ出力（チャンクごと）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println(String.format(
            "[Writer進捗] %,d 件書き込み完了（チャンクサイズ: %,d 件）",
            totalWriteCount, items.size()
        ));
    }
}
