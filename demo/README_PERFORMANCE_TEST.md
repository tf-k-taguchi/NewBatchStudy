# Tasklet vs Chunk パフォーマンス比較テスト

## 📋 実装完了

TaskletモデルとChunkモデルのパフォーマンス比較実装が完了しました。

### 実装したファイル

#### 新規作成（7ファイル）
1. ✅ `src/main/java/com/example/demo/presentation/listener/PerformanceJobListener.java`
2. ✅ `src/main/java/com/example/demo/presentation/tasklet/SingleInsertTasklet.java`
3. ✅ `src/main/java/com/example/demo/presentation/config/TaskletPerformanceJobConfig.java`
4. ✅ `src/main/java/com/example/demo/presentation/reader/DummyDataItemReader.java`
5. ✅ `src/main/java/com/example/demo/presentation/writer/DummyDataBatchItemWriter.java`
6. ✅ `src/main/java/com/example/demo/presentation/config/ChunkPerformanceJobConfig.java`
7. ✅ 新規ディレクトリ: `listener/`, `reader/`, `writer/`

#### 改修（3ファイル）
1. ✅ `src/main/resources/schema.sql` - dummy_recordテーブル追加
2. ✅ `src/main/resources/application.yml` - バッチ設定追加
3. ✅ `src/main/java/com/example/demo/presentation/runner/BatchJobRunner.java` - 実行モード切り替え対応

---

## 🚀 実行方法

### 前提条件

```bash
cd demo
```

### 1. Taskletモデルのみ実行（低速パターン）

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=tasklet"
```

**期待される動作**:
- 100万件のデータを1件ずつInsert
- 予想実行時間: 60〜120秒
- 進捗ログが10%ごとに表示される

### 2. Chunkモデルのみ実行（高速パターン）

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=chunk"
```

**期待される動作**:
- 100万件のデータを1万件ごとにバルクInsert
- 予想実行時間: 5〜15秒
- 進捗ログがチャンクごとに表示される

### 3. 両方実行（比較モード）

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=both"
```

または

```bash
mvn spring-boot:run
```

**期待される動作**:
- Taskletモデル → Chunkモデルの順に実行
- 両方の実行時間が表示され、比較できる
- 予想性能差: Chunkが8〜12倍高速

---

## 📊 テスト手順

### Step 1: 小規模データテスト（1,000件）

まずは小規模データで動作確認を行います。

1. `application.yml` を編集:
```yaml
batch:
  performance:
    data-count: 1000      # 1,000件に変更
    chunk-size: 100       # チャンクサイズも調整
```

2. 実行:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=both"
```

3. 確認事項:
- [ ] エラーなく実行完了
- [ ] Taskletの実行時間: _______ 秒
- [ ] Chunkの実行時間: _______ 秒
- [ ] 性能差: 約 _______ 倍

### Step 2: 中規模データテスト（10,000件）

1. `application.yml` を編集:
```yaml
batch:
  performance:
    data-count: 10000     # 10,000件に変更
    chunk-size: 1000      # チャンクサイズも調整
```

2. 実行:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=both"
```

3. 確認事項:
- [ ] エラーなく実行完了
- [ ] Taskletの実行時間: _______ 秒
- [ ] Chunkの実行時間: _______ 秒
- [ ] 性能差: 約 _______ 倍

### Step 3: 大規模データテスト（100,000件）

1. `application.yml` を編集:
```yaml
batch:
  performance:
    data-count: 100000    # 100,000件に変更
    chunk-size: 10000     # チャンクサイズも調整
```

2. 実行:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=both"
```

3. 確認事項:
- [ ] エラーなく実行完了
- [ ] Taskletの実行時間: _______ 秒
- [ ] Chunkの実行時間: _______ 秒
- [ ] 性能差: 約 _______ 倍

### Step 4: 本番データテスト（1,000,000件）

⚠️ **注意**: Taskletモデルは非常に時間がかかります（60〜120秒）

1. `application.yml` を編集:
```yaml
batch:
  performance:
    data-count: 1000000   # 1,000,000件（デフォルト）
    chunk-size: 10000     # チャンクサイズ
```

2. 実行:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=both"
```

3. 確認事項:
- [ ] エラーなく実行完了
- [ ] Taskletの実行時間: _______ 秒
- [ ] Chunkの実行時間: _______ 秒
- [ ] 性能差: 約 _______ 倍

---

## 📈 期待される結果

### パフォーマンス予測

| データ量 | Tasklet実行時間 | Chunk実行時間 | 性能差 |
|---------|----------------|--------------|--------|
| 1,000件 | 1〜2秒 | 0.5〜1秒 | 2倍 |
| 10,000件 | 6〜12秒 | 1〜2秒 | 6倍 |
| 100,000件 | 60〜120秒 | 5〜10秒 | 10倍 |
| 1,000,000件 | 600〜1200秒 | 50〜100秒 | 10〜12倍 |

### 性能差の要因

1. **DB往復回数**
   - Tasklet: データ件数分（100万回）
   - Chunk: チャンク数分（100回）

2. **トランザクション回数**
   - Tasklet: データ件数分（100万回）
   - Chunk: チャンク数分（100回）

3. **SQLパース回数**
   - Tasklet: データ件数分（100万回）
   - Chunk: バッチ更新により最適化

---

## 🔍 データ確認方法

### H2コンソールでデータ確認

1. アプリケーション起動中に以下にアクセス:
```
http://localhost:8080/h2-console
```

2. 接続情報:
- JDBC URL: `jdbc:h2:mem:batch_db`
- User Name: `sa`
- Password: (空欄)

3. データ確認SQL:
```sql
-- 総件数確認
SELECT COUNT(*) FROM dummy_record;

-- Taskletデータ確認
SELECT * FROM dummy_record WHERE data_value LIKE 'TaskletData%' LIMIT 10;

-- Chunkデータ確認
SELECT * FROM dummy_record WHERE data_value LIKE 'ChunkData%' LIMIT 10;

-- 実行履歴確認
SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC;

-- Step実行詳細確認
SELECT 
    sje.STEP_NAME,
    sje.READ_COUNT,
    sje.WRITE_COUNT,
    sje.COMMIT_COUNT,
    sje.START_TIME,
    sje.END_TIME
FROM BATCH_STEP_EXECUTION sje
ORDER BY sje.START_TIME DESC;
```

---

## 🛠️ トラブルシューティング

### メモリ不足エラーが発生した場合

```bash
export MAVEN_OPTS="-Xms512m -Xmx2g"
mvn spring-boot:run
```

### 実行時間が異常に長い場合

- CPU/メモリ使用状況を確認
- 他のプロセスがリソースを使用していないか確認
- データ量を減らしてテスト

### Bean定義エラーが発生した場合

```bash
# クリーンビルド
mvn clean install
mvn spring-boot:run
```

---

## 📝 実行結果記録テンプレート

### テスト環境

- OS: _______________________
- Java バージョン: _______________________
- メモリ: _______________________
- CPU: _______________________

### 実行結果

#### 1,000件テスト
- Tasklet実行時間: _______ 秒
- Chunk実行時間: _______ 秒
- 性能差: _______ 倍

#### 10,000件テスト
- Tasklet実行時間: _______ 秒
- Chunk実行時間: _______ 秒
- 性能差: _______ 倍

#### 100,000件テスト
- Tasklet実行時間: _______ 秒
- Chunk実行時間: _______ 秒
- 性能差: _______ 倍

#### 1,000,000件テスト
- Tasklet実行時間: _______ 秒
- Chunk実行時間: _______ 秒
- 性能差: _______ 倍

### 考察

_______________________
_______________________
_______________________

---

## 🎓 学習ポイント

### 1. アーキテクチャの違い

**Taskletモデル**:
- シンプルな実装
- 細かい制御が可能
- 大量データには不向き

**Chunkモデル**:
- Spring Batchの推奨パターン
- トランザクション管理が自動
- 大量データ処理に最適

### 2. 実務での選択基準

| 条件 | 推奨モデル |
|------|-----------|
| 大量データ処理 | **Chunk** |
| 複雑な制御フロー | Tasklet |
| トランザクション境界の細かい制御 | Tasklet |
| 標準的なETL処理 | **Chunk** |

### 3. パフォーマンスチューニング

- チャンクサイズの調整（推奨: 1,000〜10,000）
- コネクションプール設定
- バルクInsertの活用
- トランザクション境界の最適化

---

## 📚 参考資料

- [Spring Batch公式ドキュメント](https://docs.spring.io/spring-batch/docs/current/reference/html/)
- [Chunk-oriented Processing](https://docs.spring.io/spring-batch/docs/current/reference/html/step.html#chunkOrientedProcessing)
- [実装計画書](../plans/Tasklet_vs_Chunk_パフォーマンス比較実装計画.md)
- [アーキテクチャ図](../plans/パフォーマンス比較_アーキテクチャ図.md)
- [実装チェックリスト](../plans/実装チェックリスト_パフォーマンス比較.md)

---

## ✨ まとめ

この実装により、以下を実証できます：

1. **アーキテクチャの違いによる性能差**
   - Tasklet（1件ずつ）vs Chunk（バルク）
   - 約8〜12倍の性能差

2. **Spring Batchのベストプラクティス**
   - 大量データ処理にはChunkモデル
   - バルク処理の重要性

3. **実務での選択基準**
   - データ量に応じたモデル選択
   - パフォーマンス要件の考慮

まずは小規模データ（1,000件）からテストを開始してください！
