# 役割
あなたはSpring Batch、MyBatis、およびドメイン駆動設計（DDD）に精通したシニア・ソフトウェアアーキテクトです。
私の指示に従い、`docs/Spring_Batch詳細設計書_複数バッチ分析.md` の内容に準拠した、**依存関係トラブルに強い堅牢なバッチ処理のスケルトンコード** を作成してください。

# 重要なお願い（制約事項）
Roocodeの暴走を抑制し、確実に動作するコードにするため、以下のルールを厳守してください。

1.  **段階的実装:** いきなりロジック（メソッドの中身）を書かないでください。まずは「クラスファイル」「メソッド定義（シグネチャ）」「DDL」のみを作成し、依存関係（DI）の確認を行います。
2.  **AWSのモック化:** S3とSQSへのアクセスは必ずインターフェース（Repositoryパターン）を介してください。今回はローカル動作確認用として、**メモリ上やログ出力だけで動作する「Mock実装クラス」** を作成してください（`@Profile("local")` を付与）。
3.  **カスタムロガーの実装（重要）:** * 標準のSLF4J/Logback依存関係エラーを回避するため、**独自のロガーインターフェースと実装クラス**を作成してください。
    * 既存の `@Slf4j` アノテーションは使用せず、DIしたカスタムロガーを使用してください。
4.  **命名規則の徹底:**
    * Entityクラス → `~Entity.java`
    * Service/Repositoryインターフェース → `~Service.java`, `~Repository.java`
    * 実装クラス → `~Impl.java` (Mockは `~Mock.java`)
    * Mapperインターフェース → `~Mapper.java`
5.  **技術スタック:** MyBatis (Mapper I/F必須), Java標準機能によるMock, カスタムLogger

---

# 今回作成するバッチの仕様
* **Job名:** `HelloAwsBatchJob`
* **処理フロー:**
    1.  **SQS (Mock):** キューからメッセージを受信する。
    2.  **DB (MyBatis):** 受信ログをDBにINSERTする。
    3.  **Logic:** カスタムロガーで "Processing..." と出力。例外発生時のリトライ（3回）を確認する。
    4.  **S3 (Mock):** 処理結果をアップロード（したつもりでログ出力）。

---

# 指示：フェーズ1 【構造定義・インターフェース設計】

**ロジックの中身（if文など）はまだ書かないでください。**
以下のDDDパッケージ構成案に従い、必要なファイルを「空の実装」または「インターフェース定義」の状態で作ってください。

## 作成すべきパッケージ構成とファイル

### 1. 共通基盤層 (Infrastructure/Common)
依存関係エラーを避けるためのカスタムロガーです。
```text
com.example.batch.infrastructure.common.logging
├── BatchLogger.java (インターフェース: info(msg), error(msg, e) 等を定義)
└── ConsoleBatchLogger.java (実装: System.out.println等を使い、時刻付きでコンソール出力する)

ドメイン層 (Domain)
com.example.batch.domain
├── model
│   └── BatchLogEntity.java (DB用Entity)
└── repository
    ├── BatchLogRepository.java (DB用インターフェース)
    ├── SqsRepository.java (SQS用インターフェース)
    └── S3Repository.java (S3用インターフェース)


インフラ層 (Infrastructure)
com.example.batch.infrastructure
├── mapper
│   └── BatchLogMapper.java (MyBatis Mapper: @Mapper)
├── repository
│   ├── BatchLogRepositoryImpl.java (BatchLogRepository実装)
│   ├── SqsRepositoryMock.java (SqsRepository実装: @Profile("local"))
│   └── S3RepositoryMock.java (S3Repository実装: @Profile("local"))
└── persistence
    └── (XMLはまだ不要)

プレゼンテーション層 (Presentation)
com.example.batch.presentation.tasklet
└── HelloAwsTasklet.java (SqsRepository, S3Repository, BatchLogRepository, BatchLogger をDIする)

com.example.batch.presentation.config
└── HelloAwsJobConfig.java (Job/Step定義)


### ログ出力実装のポイント（エラー回避策）

1.  **`BatchLogger` インターフェース:**
    * 開発中は `ConsoleBatchLogger` (System.out.println) を使います。これでLogbackのエラーで起動しない問題を確実に回避できます。
    * 将来的にエラーが解決したら、`Slf4jBatchLogger` という実装を作って差し替えるだけで、全てのコードを修正せずに済みます。

2.  **アノテーション `@Slf4j` の禁止:**
    * Lombokの `@Slf4j` は便利ですが、クラスパスの設定ミスで「シンボルが見つかりません」になりがちです。今回はこれを禁止し、DIで解決することで安全性を高めました。