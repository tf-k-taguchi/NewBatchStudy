# HelloAwsBatchJob - Spring Batch デモプロジェクト

## プロジェクト概要

Spring Batch詳細設計書に基づき、依存関係トラブルに強い堅牢なバッチ処理システムを実装したデモプロジェクトです。
AWS連携（SQS、S3）をMock化し、ローカル環境で完全動作する段階的実装を実現しています。

## 技術スタック

- **Java**: 21
- **Spring Boot**: 3.4.2
- **Spring Batch**: 5.x (Spring Boot 3.4.2に含まれる)
- **MyBatis**: 3.0.3
- **H2 Database**: インメモリデータベース（ローカル開発用）
- **カスタムLogger**: Lombok依存を回避した独自実装

## セットアップ

### 前提条件
- Java 21以上
- Maven 3.6以上
- VSCode + Java Extension Pack（推奨）

### 依存関係のインストール

```bash
./mvnw clean install
```

## 実行方法

### 方法1: VSCodeのデバッグ機能を使用（推奨）

1. VSCodeでプロジェクトを開く
2. 左サイドバーの「実行とデバッグ」アイコンをクリック（または `Ctrl+Shift+D` / `Cmd+Shift+D`）
3. 上部のドロップダウンから **「HelloAwsBatchJob起動」** を選択
4. 緑色の再生ボタン（▶️）をクリック、または `F5` キーを押す

**デバッグモードで実行する場合:**
- ドロップダウンから **「HelloAwsBatchJob起動（デバッグ）」** を選択
- ブレークポイントを設定してステップ実行が可能

### 方法2: Mavenコマンドで実行

```bash
./mvnw spring-boot:run
```

### 方法3: JARファイルをビルドして実行

```bash
# ビルド
./mvnw clean package

# 実行
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## 実行結果の確認

### コンソール出力例

バッチジョブが正常に実行されると、以下のようなログが出力されます：

```
========================================
バッチジョブを起動します
========================================

[Console] 2026-02-14 13:41:12 - INFO - ========================================
[Console] 2026-02-14 13:41:12 - INFO - Starting HelloAwsBatchJob
[Console] 2026-02-14 13:41:12 - INFO - ========================================
[Console] 2026-02-14 13:41:12 - INFO - メッセージ受信試行 1/3
[Console] 2026-02-14 13:41:12 - INFO - SQS Mock: メッセージ受信 - Mock SQS Message 1
[Console] 2026-02-14 13:41:12 - INFO - Processing message: Mock SQS Message 1
[Console] 2026-02-14 13:41:12 - INFO - メッセージ処理中...
[Console] 2026-02-14 13:41:12 - INFO - メッセージ処理完了
[Console] 2026-02-14 13:41:12 - INFO - バッチログをDBに保存しました - Status: PROCESSING
[Console] 2026-02-14 13:41:12 - INFO - S3 Mock: ファイルアップロード - Key: batch-result/HelloAwsBatchJob/1771044072688.txt, Size: 59 bytes
[Console] 2026-02-14 13:41:12 - INFO - バッチログをDBに保存しました - Status: SUCCESS
[Console] 2026-02-14 13:41:12 - INFO - ========================================
[Console] 2026-02-14 13:41:12 - INFO - HelloAwsBatchJob completed successfully
[Console] 2026-02-14 13:41:12 - INFO - ========================================

========================================
バッチジョブが完了しました
========================================
```

### H2データベースコンソールで確認

バッチ実行中にH2コンソールでデータを確認できます：

1. バッチを起動した状態で、ブラウザで以下にアクセス：
   ```
   http://localhost:8080/h2-console
   ```

2. 接続情報を入力：
   - **JDBC URL**: `jdbc:h2:mem:batch_db`
   - **User Name**: `sa`
   - **Password**: （空白のまま）

3. 「Connect」をクリック

4. SQLを実行してバッチログを確認：
   ```sql
   SELECT * FROM batch_log ORDER BY created_at DESC;
   ```

## アーキテクチャ

### DDD レイヤードアーキテクチャ

```
com.example.demo
├── infrastructure/common/logging/     # 共通基盤層
│   ├── BatchLogger.java              # ロガーインターフェース
│   └── ConsoleBatchLogger.java       # コンソール実装
├── domain/                           # ドメイン層
│   ├── entity/
│   │   └── BatchLogEntity.java       # バッチログエンティティ
│   └── repository/
│       ├── BatchLogRepository.java   # バッチログリポジトリIF
│       ├── SqsRepository.java        # SQSリポジトリIF
│       └── S3Repository.java         # S3リポジトリIF
├── infrastructure/                   # インフラ層
│   ├── mapper/
│   │   └── BatchLogMapper.java       # MyBatisマッパー
│   └── repository/
│       ├── BatchLogRepositoryImpl.java  # バッチログ実装
│       ├── SqsRepositoryMock.java       # SQS Mock実装
│       └── S3RepositoryMock.java        # S3 Mock実装
└── presentation/                     # プレゼンテーション層
    ├── config/
    │   └── HelloAwsJobConfig.java    # Job設定
    ├── tasklet/
    │   └── HelloAwsTasklet.java      # メインTasklet
    └── runner/
        └── BatchJobRunner.java       # 起動ランナー
```

## 主要機能

### 1. カスタムロガー
Lombokの`@Slf4j`依存を回避するため、独自のBatchLoggerインターフェースを実装。

### 2. Mock実装
- **SqsRepositoryMock**: メモリ上のキューでSQSを模擬（初期メッセージ3件）
- **S3RepositoryMock**: メモリ上のMapでS3を模擬

### 3. バッチ処理フロー
1. SQSからメッセージ受信（Mock）
2. メッセージ処理
3. DBへバッチログ保存（MyBatis）
4. S3へ結果アップロード（Mock）
5. 完了ログ保存

### 4. リトライ機構
最大3回のリトライ機能を実装（1秒間隔）

## デバッグ方法

### ブレークポイントの設定

1. VSCodeで以下のファイルを開く：
   - `src/main/java/com/example/demo/presentation/tasklet/HelloAwsTasklet.java`

2. 行番号の左側をクリックしてブレークポイントを設定
   - 推奨箇所: `execute`メソッドの先頭（44行目付近）

3. 「HelloAwsBatchJob起動（デバッグ）」で実行

4. ブレークポイントで停止したら、以下の操作が可能：
   - **F10**: ステップオーバー（次の行へ）
   - **F11**: ステップイン（メソッド内部へ）
   - **Shift+F11**: ステップアウト（メソッドから抜ける）
   - **F5**: 続行（次のブレークポイントまで）

### 変数の確認

デバッグ中、左サイドバーの「変数」セクションで以下を確認できます：
- `message`: 受信したSQSメッセージ
- `processedData`: 処理後のデータ
- `s3Key`: S3アップロード先のキー

## データベース

### テーブル構造

```sql
CREATE TABLE batch_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_name VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- インデックス
CREATE INDEX idx_batch_name ON batch_log(batch_name);
CREATE INDEX idx_created_at ON batch_log(created_at);
```

### ステータス値
- **PROCESSING**: 処理中
- **SUCCESS**: 成功
- **FAILED**: 失敗

## 設定ファイル

### application.yml
主要な設定項目：

```yaml
spring:
  profiles:
    active: local  # プロファイル
  
  datasource:
    url: jdbc:h2:mem:batch_db  # H2インメモリDB
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  batch:
    job:
      enabled: false  # 自動起動無効化（手動実行のため）

mybatis:
  mapper-locations: classpath:mybatis/mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

## トラブルシューティング

### ビルドエラーが発生する場合

```bash
# Mavenキャッシュをクリア
./mvnw clean

# 依存関係を再取得
./mvnw dependency:purge-local-repository
./mvnw clean install
```

### H2コンソールに接続できない場合

- バッチジョブが実行中であることを確認
- ブラウザのキャッシュをクリア
- JDBC URLが正しいか確認: `jdbc:h2:mem:batch_db`

### ポート8080が使用中の場合

`application.yml`に以下を追加：

```yaml
server:
  port: 8081  # 別のポート番号に変更
```

## 実装のポイント

### 1. 依存関係トラブル対策
- Lombokの`@Slf4j`を使用せず、カスタムロガーをDI
- Spring Boot 3.4.2の安定版を使用

### 2. プロファイル戦略
```java
@Component
@Profile("local")
public class SqsRepositoryMock implements SqsRepository {
    // ローカル開発用Mock実装
}
```

### 3. MyBatis XML設定
アノテーションではなくXMLファイルでSQL定義：
- `src/main/resources/mybatis/mapper/BatchLogMapper.xml`

### 4. トランザクション管理
Spring Batchのトランザクション管理を活用し、DB操作の整合性を保証

## 今後の拡張

### 本番環境対応
1. AWS SDK依存関係追加
2. 本番用リポジトリ実装（`@Profile("prod")`）
3. 環境変数による設定外部化
4. CloudWatch Logsへのログ出力

### 機能拡張
1. Chunk処理への移行（大量データ対応）
2. 並列処理の実装
3. ジョブパラメータによる動的制御
4. エラー通知機能（メール、Slack等）

## 参考ドキュメント

- [docs/詳細.md](docs/詳細.md) - Spring Batch詳細設計書
- [docs/実装.md](docs/実装.md) - 実装方針書

## ライセンス

このプロジェクトはデモ目的で作成されています。
