# バッチジョブ実行ガイド

このドキュメントでは、各バッチジョブの個別実行方法を説明します。

## 📋 目次

1. [実行可能なジョブ一覧](#実行可能なジョブ一覧)
2. [実行方法](#実行方法)
3. [各ジョブの詳細](#各ジョブの詳細)
4. [トラブルシューティング](#トラブルシューティング)

---

## 実行可能なジョブ一覧

| ジョブ名 | 実行コマンド | 説明 | 用途 |
|---------|------------|------|------|
| **HelloAwsBatchJob** | `--job=hello` | SQSメッセージ処理 | 本番想定の処理 |
| **TaskletPerformanceJob** | `--job=tasklet` | Taskletモデル（性能測定） | 性能測定・学習用 |
| **ChunkPerformanceJob** | `--job=chunk` | Chunkモデル（性能測定） | 性能測定・学習用 |
| **性能比較** | `--job=compare` | TaskletとChunkの比較 | 性能差の確認 |
| **全て実行** | `--job=all` | 全ジョブを順次実行 | 動作確認 |
| **デフォルト** | 引数なし | HelloAwsBatchJob実行 | 通常実行 |

---

## 実行方法

### 基本コマンド

```bash
# Maven経由で実行
mvn spring-boot:run -Dspring-boot.run.arguments="--job=<モード>"

# または、JARファイルから実行
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar --job=<モード>
```

### 具体例

#### 1. HelloAwsBatchJob（SQS処理）のみ実行

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=hello"
```

**実行内容:**
- SQSからメッセージを受信
- メッセージを処理
- S3にアップロード
- 処理結果をDBに記録

**対応ドキュメント:**
- `demo/docs/0510処理フロー.md`
- `demo/docs/taguchiStudy.md`（Taskletを選ぶべきシーン）

---

#### 2. Taskletモデル（性能測定）のみ実行

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=tasklet"
```

**実行内容:**
- ダミーデータを1件ずつINSERT
- 処理時間を計測
- 合計処理件数を表示

**特徴:**
- 1件ずつ処理するため、処理時間が長い
- Chunkモデルとの性能比較用

---

#### 3. Chunkモデル（性能測定）のみ実行

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=chunk"
```

**実行内容:**
- ダミーデータをバルクINSERT
- 処理時間を計測
- 合計処理件数を表示

**特徴:**
- チャンクサイズ（10,000件）ごとにまとめて処理
- Taskletモデルより約6倍高速

**対応ドキュメント:**
- `demo/docs/taguchiStudy.md`（chunk処理を選ぶべきシーン）

---

#### 4. 性能比較（TaskletとChunk両方実行）

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=compare"
```

**実行内容:**
1. Taskletモデルを実行
2. 2秒待機
3. Chunkモデルを実行
4. 処理時間を比較

**用途:**
- TaskletとChunkの性能差を確認
- 学習・デモ用

---

#### 5. 全てのジョブを実行

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=all"
```

**実行内容:**
1. HelloAwsBatchJob実行
2. TaskletPerformanceJob実行
3. ChunkPerformanceJob実行

**用途:**
- 全機能の動作確認
- 統合テスト

---

#### 6. デフォルト実行（引数なし）

```bash
mvn spring-boot:run
```

**実行内容:**
- HelloAwsBatchJobのみ実行
- 本番想定の動作

---

## 各ジョブの詳細

### HelloAwsBatchJob（SQS処理）

**処理フロー:**
```
1. キューにメッセージがあるかチェック
2. メッセージ受信（リトライ付き）
3. 重複実行チェック（冪等性）
4. 処理中ステータスをDBに記録
5. メッセージ処理（ビジネスロジック）
6. Visibility Timeout延長（必要に応じて）
7. S3へアップロード
8. 成功ステータスをDBに記録
9. メッセージ削除（Acknowledge）
```

**エラーハンドリング:**
- RETRYABLE: メッセージを削除せず、自動再処理
- NON_RETRYABLE: DLQに送信してメッセージ削除
- SYSTEM_ERROR: DLQに送信してメッセージ削除

**設定ファイル:**
- `demo/src/main/java/com/example/demo/presentation/config/HelloAwsJobConfig.java`
- `demo/src/main/java/com/example/demo/presentation/tasklet/HelloAwsTasklet.java`
- `demo/src/main/java/com/example/demo/application/service/BatchProcessService.java`

---

### TaskletPerformanceJob（性能測定）

**処理フロー:**
```
1. ダミーデータを生成
2. 1件ずつINSERT
3. 処理時間を計測
```

**設定:**
- 処理件数: `application.yml`の`batch.performance.data-count`
- デフォルト: 10,000,000件

**設定ファイル:**
- `demo/src/main/java/com/example/demo/presentation/config/TaskletPerformanceJobConfig.java`
- `demo/src/main/java/com/example/demo/presentation/tasklet/SingleInsertTasklet.java`

---

### ChunkPerformanceJob（性能測定）

**処理フロー:**
```
1. Reader: 1件ずつデータを読み込む
2. Writer: チャンクサイズ分をまとめてバルクINSERT
3. トランザクションコミット
4. メモリ解放
5. 次のチャンクへ
```

**設定:**
- 処理件数: `application.yml`の`batch.performance.data-count`
- チャンクサイズ: `application.yml`の`batch.performance.chunk-size`
- デフォルト: 10,000,000件、チャンクサイズ100,000件

**設定ファイル:**
- `demo/src/main/java/com/example/demo/presentation/config/ChunkPerformanceJobConfig.java`
- `demo/src/main/java/com/example/demo/presentation/reader/DummyDataItemReader.java`
- `demo/src/main/java/com/example/demo/presentation/writer/DummyDataBatchItemWriter.java`

---

## トラブルシューティング

### エラー: "Job instance already exists"

**原因:**
同じJobParametersで複数回実行しようとした。

**解決方法:**
Spring Batchは同じJobParametersでの重複実行を防ぎます。
時間をパラメータに含めているため、通常は発生しませんが、
もし発生した場合は以下を実行してください：

```bash
# H2データベースをリセット
mvn clean
mvn spring-boot:run -Dspring-boot.run.arguments="--job=<モード>"
```

---

### エラー: "No qualifying bean of type 'Job'"

**原因:**
指定したジョブ名が存在しない。

**解決方法:**
正しいジョブ名を指定してください：
- `hello`
- `tasklet`
- `chunk`
- `compare`
- `all`

---

### 処理が遅い

**Taskletモデルの場合:**
- 1件ずつ処理するため、処理時間が長いのは正常です
- Chunkモデルを使用することで高速化できます

**Chunkモデルの場合:**
- チャンクサイズを調整してください（`application.yml`）
- メモリとパフォーマンスのバランスを考慮

---

### メモリ不足（OOM）エラー

**原因:**
処理件数が多すぎる、またはチャンクサイズが大きすぎる。

**解決方法:**
1. 処理件数を減らす（`application.yml`の`batch.performance.data-count`）
2. チャンクサイズを小さくする（`application.yml`の`batch.performance.chunk-size`）
3. JVMヒープサイズを増やす

```bash
# ヒープサイズを2GBに設定
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g" -Dspring-boot.run.arguments="--job=chunk"
```

---

## 設定変更

### 処理件数の変更

`demo/src/main/resources/application.yml`を編集：

```yaml
batch:
  performance:
    data-count: 1000000      # 処理件数を100万件に変更
    chunk-size: 10000        # チャンクサイズを1万件に変更
```

---

## 参考ドキュメント

- **処理フロー詳細**: `demo/docs/0510処理フロー.md`
- **TaskletとChunkの使い分け**: `demo/docs/taguchiStudy.md`
- **性能測定結果**: `demo/docs/性能測定.md`
- **パフォーマンステスト**: `demo/README_PERFORMANCE_TEST.md`
- **SQS処理リファクタリング**: `demo/README_SQS_REFACTORING.md`

---

## まとめ

| 目的 | 実行コマンド |
|------|------------|
| 本番想定の処理 | `mvn spring-boot:run -Dspring-boot.run.arguments="--job=hello"` |
| Taskletの学習 | `mvn spring-boot:run -Dspring-boot.run.arguments="--job=tasklet"` |
| Chunkの学習 | `mvn spring-boot:run -Dspring-boot.run.arguments="--job=chunk"` |
| 性能比較 | `mvn spring-boot:run -Dspring-boot.run.arguments="--job=compare"` |
| 全機能確認 | `mvn spring-boot:run -Dspring-boot.run.arguments="--job=all"` |
| デフォルト実行 | `mvn spring-boot:run` |
