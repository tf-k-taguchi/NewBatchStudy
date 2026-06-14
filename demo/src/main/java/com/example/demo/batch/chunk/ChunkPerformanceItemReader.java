package com.example.demo.batch.chunk;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ChunkPerformanceItemReader（ダミーデータ読み込み）
 * 
 * 【責務】
 * - Chunkモデルのパフォーマンス測定用データ読み込み
 * - 1件ずつダミーデータを生成して返す
 * 
 * 【Chunk処理のReader】
 * このクラスはtaguchiStudy.mdの「chunk処理」に該当します。
 * Spring BatchのChunkモデルにおける「読込（Reader）」を担当します。
 * 
 * 【処理フロー】
 * Chunk処理: Reader → Processor → Writer
 *            ↑このクラス
 * 
 * 【Chunk処理を選ぶ理由】
 * taguchiStudy.mdの「chunk処理を選ぶべきシーン」に該当：
 * - キューに入ったデータの処理を行う（大量データ処理）
 * - 1回で処理するデータ数を決めることができる（チャンクサイズ）
 * - データを区切ることで、途中で失敗しても最初から処理し直す必要がない
 * 
 * 【メモリ対策】
 * taguchiStudy.mdの「メモリ不足(OOM)対策」として実装：
 * - 一度に大量のデータを読み込まず、1件ずつ返す
 * - チャンク単位でメモリを解放しながら処理
 * 
 * 【処理】
 * オンメモリでダミーデータを生成して返す
 * 
 * 【特徴】
 * 100万件に達するまでデータを生成し続ける
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.ChunkPerformanceJobConfig}
 * - 連携先Writer: {@link ChunkPerformanceItemWriter}
 */
@Component
public class ChunkPerformanceItemReader implements ItemReader<String> {
    
    @Value("${batch.performance.data-count:1000000}")
    private int dataCount;
    
    private int currentCount = 0;
    
    /**
     * データを1件ずつ読み込むメソッド
     * 
     * 【Spring Batchの動作】
     * このメソッドはSpring Batchフレームワークから繰り返し呼び出されます。
     * nullを返すまで、チャンクサイズ分だけ連続して呼び出されます。
     * 
     * 【処理フロー】
     * 1. read()が呼ばれる → データ1件を返す
     * 2. read()が呼ばれる → データ1件を返す
     * 3. ... チャンクサイズ（10,000件）まで繰り返し
     * 4. Writerが呼ばれる → チャンク分をまとめて書き込み
     * 5. トランザクションコミット → メモリ解放
     * 6. 1に戻る（次のチャンク）
     * 
     * 【終了条件】
     * nullを返すと、Spring Batchは読み込み終了と判断します。
     * 
     * 【メモリ効率】
     * taguchiStudy.mdの「メモリ不足(OOM)対策」：
     * - 100万件のデータを一度にメモリに載せない
     * - 1件ずつ生成して返すことで、メモリ使用量を最小化
     * - チャンク単位でコミットされるため、メモリが解放される
     * 
     * @return 読み込んだデータ（nullの場合は読み込み終了）
     */
    @Override
    public String read() {
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 終了判定: 指定件数に達したら終了
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (currentCount >= dataCount) {
            return null;  // nullを返すと読み込み終了
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // カウントアップ
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        currentCount++;
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ダミーデータ生成（連番付き文字列）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 実際のプロジェクトでは、ここでDBやファイルからデータを読み込む
        String dummyData = String.format("ChunkData-%010d", currentCount);
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 進捗ログ出力（10万件ごと）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        if (currentCount % 100000 == 0) {
            System.out.println(String.format(
                "[Reader進捗] %,d 件読み込み完了", currentCount
            ));
        }
        
        return dummyData;
    }
}
