# Spring Batch リファクタリングガイド

## 📋 概要

このドキュメントは、Spring Batchプロジェクトのリファクタリング結果をまとめたものです。
**「IDE（VS Code等）で処理を追いやすく、どこに何があるか直感的に探せる構造」** を実現しました。

---

## 🎯 リファクタリングの目的

### 解決した課題
- ❌ **旧構造**: インライン実装（ラムダ式ベタ書き）で処理が追えない
- ✅ **新構造**: 独立したクラスファイルで「定義へジャンプ」が確実に機能

### 適用したルール

#### 1. インライン実装の完全禁止
- `@Configuration` クラス内でTasklet, Reader, Processor, Writerの中身を直接記述しない
- 実処理は必ず `implements Tasklet` 等を行った **独立したクラス（別ファイル）** として切り出し

#### 2. クラスとファイルの1対1対応
- 切り出した実処理クラスには `@Component` を付与
- 独立したファイル（例: `TaskletPerformanceTasklet.java`）として配置
- Configクラス側では、コンストラクタインジェクションで受け取り、Stepに組み込み
- **IDEの「定義へジャンプ」が確実に機能**

#### 3. ディレクトリ（パッケージ）の厳格な分離
- 用途ごとにパッケージを分け、ファイルがどこにあるか一目でわかるように配置

#### 4. 名前空間の統一
- Job、Step、Tasklet、Config の命名をリンク
- 例: `TaskletPerformanceJob` → 設定は `TaskletPerformanceJobConfig`、実処理は `TaskletPerformanceTasklet`

---

## 📁 推奨フォルダ構成ツリー

```
demo/src/main/java/com/example/demo/
│
├── config/                                    # ★ Job/Step設計図（@Configuration）
│   ├── HelloAwsJobConfig.java                # HelloAwsBatchJobの設計図
│   ├── TaskletPerformanceJobConfig.java      # TaskletPerformanceJobの設計図
│   └── ChunkPerformanceJobConfig.java        # ChunkPerformanceJobの設計図
│
├── batch/                                     # ★ バッチ処理の実装クラス
│   ├── tasklet/                              # Tasklet実装クラス
│   │   ├── HelloAwsTasklet.java              # HelloAwsBatchJobの実処理
│   │   └── TaskletPerformanceTasklet.java    # TaskletPerformanceJobの実処理
│   │
│   ├── chunk/                                # Chunk処理の実装クラス
│   │   ├── ChunkPerformanceItemReader.java   # ChunkPerformanceJobのReader実装
│   │   └── ChunkPerformanceItemWriter.java   # ChunkPerformanceJobのWriter実装
│   │
│   └── listener/                             # Listener実装クラス
│       └── PerformanceJobListener.java       # パフォーマンス計測Listener
│
├── presentation/                              # プレゼンテーション層
│   └── runner/                               # ★ バッチ起動係
│       └── BatchJobRunner.java               # アプリケーション起動時にJobを実行
│
├── application/                               # アプリケーション層
│   └── service/                              # ビジネスロジック
│       ├── BatchProcessService.java          # バッチ処理サービス
│       └── BatchLogQueryService.java         # バッチログ照会サービス
│
├── domain/                                    # ドメイン層
│   ├── entity/                               # エンティティ
│   │   └── BatchLogEntity.java
│   ├── model/                                # ドメインモデル
│   │   ├── SqsMessage.java
│   │   └── BatchErrorType.java
│   ├── repository/                           # リポジトリインターフェース
│   │   ├── BatchLogRepository.java
│   │   ├── S3Repository.java
│   │   └── SqsRepository.java
│   └── exception/                            # ドメイン例外
│       └── BatchProcessException.java
│
└── infrastructure/                            # インフラストラクチャ層
    ├── repository/                           # リポジトリ実装
    │   ├── BatchLogRepositoryImpl.java
    │   ├── S3RepositoryMock.java
    │   └── SqsRepositoryMock.java
    ├── mapper/                               # MyBatisマッパー
    │   └── BatchLogMapper.java
    └── common/                               # 共通機能
        └── logging/                          # ログ機能
            ├── BatchLogger.java
            └── ConsoleBatchLogger.java
```

---

## 🔍 IDEでの追跡方法

### 1. Jobから実処理を追跡する

#### ステップ1: Configクラスを開く
```java
// demo/src/main/java/com/example/demo/config/TaskletPerformanceJobConfig.java
@Configuration
public class TaskletPerformanceJobConfig {
    
    @Bean
    public Step taskletPerformanceStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TaskletPerformanceTasklet taskletPerformanceTasklet) {  // ← ここをCmd+クリック
        return new StepBuilder("TaskletPerformanceStep", jobRepository)
                .tasklet(taskletPerformanceTasklet, transactionManager)
                .build();
    }
}
```

#### ステップ2: 実処理クラスへジャンプ
- `TaskletPerformanceTasklet` をCmd+クリック（Mac）またはCtrl+クリック（Windows）
- → `demo/src/main/java/com/example/demo/batch/tasklet/TaskletPerformanceTasklet.java` へジャンプ

#### ステップ3: 実処理を確認
```java
// demo/src/main/java/com/example/demo/batch/tasklet/TaskletPerformanceTasklet.java
@Component
public class TaskletPerformanceTasklet implements Tasklet {
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) 
            throws Exception {
        // ← ここに実処理が記述されている
        // 1件ずつInsertする処理
    }
}
```

### 2. 逆引き: 実処理からConfigを追跡する

#### 方法1: JavaDocの@linkを利用
```java
/**
 * TaskletPerformanceTasklet（単一Insert実装）
 * 
 * 【追跡性】
 * - 使用元Config: {@link com.example.demo.config.TaskletPerformanceJobConfig}
 *                  ↑ ここをCmd+クリックでConfigへジャンプ
 */
@Component
public class TaskletPerformanceTasklet implements Tasklet {
    // ...
}
```

#### 方法2: IDEの「参照を検索」機能
- クラス名を右クリック → 「参照を検索」
- → どのConfigクラスで使用されているか一覧表示

---

## 📦 各パッケージの責務

### 1. `config/` - Job/Step設計図
**責務**: JobとStepの構成を定義する設計図

| ファイル | 責務 | 関連する実処理クラス |
|---------|------|---------------------|
| `HelloAwsJobConfig.java` | HelloAwsBatchJobの設計図 | `HelloAwsTasklet` |
| `TaskletPerformanceJobConfig.java` | TaskletPerformanceJobの設計図 | `TaskletPerformanceTasklet`, `PerformanceJobListener` |
| `ChunkPerformanceJobConfig.java` | ChunkPerformanceJobの設計図 | `ChunkPerformanceItemReader`, `ChunkPerformanceItemWriter`, `PerformanceJobListener` |

**特徴**:
- ✅ インライン実装なし（ラムダ式ベタ書き禁止）
- ✅ 実処理クラスをコンストラクタインジェクションで受け取る
- ✅ IDEの「定義へジャンプ」が確実に機能

### 2. `batch/tasklet/` - Tasklet実装クラス
**責務**: Taskletモデルの実処理を実装

| ファイル | 責務 | 使用元Config |
|---------|------|-------------|
| `HelloAwsTasklet.java` | SQSメッセージ処理の実処理 | `HelloAwsJobConfig` |
| `TaskletPerformanceTasklet.java` | 1件ずつInsertする実処理（性能測定用） | `TaskletPerformanceJobConfig` |

**特徴**:
- ✅ `implements Tasklet` で独立したクラス
- ✅ `@Component` でSpring管理Bean
- ✅ 1ファイル1クラスで追跡性が高い

### 3. `batch/chunk/` - Chunk処理実装クラス
**責務**: Chunkモデルの実処理を実装（Reader/Writer）

| ファイル | 責務 | 使用元Config |
|---------|------|-------------|
| `ChunkPerformanceItemReader.java` | 1件ずつデータを読み込む実処理 | `ChunkPerformanceJobConfig` |
| `ChunkPerformanceItemWriter.java` | チャンク分をまとめてバルクInsertする実処理 | `ChunkPerformanceJobConfig` |

**特徴**:
- ✅ `implements ItemReader<T>` / `implements ItemWriter<T>` で独立したクラス
- ✅ `@Component` でSpring管理Bean
- ✅ Reader/Writerが別ファイルで責務が明確

### 4. `batch/listener/` - Listener実装クラス
**責務**: Job/Stepのライフサイクルイベントを処理

| ファイル | 責務 | 使用元Config |
|---------|------|-------------|
| `PerformanceJobListener.java` | Job実行時間を計測してログ出力 | `TaskletPerformanceJobConfig`, `ChunkPerformanceJobConfig` |

**特徴**:
- ✅ `implements JobExecutionListener` で独立したクラス
- ✅ `@Component` でSpring管理Bean
- ✅ 複数のJobで再利用可能

### 5. `presentation/runner/` - バッチ起動係
**責務**: アプリケーション起動時にバッチジョブを実行

| ファイル | 責務 |
|---------|------|
| `BatchJobRunner.java` | コマンドライン引数に応じてJobを実行 |

**特徴**:
- ✅ `implements CommandLineRunner` でアプリケーション起動時に実行
- ✅ コマンドライン引数で実行するJobを切り替え可能
- ✅ 開発時は個別実行、性能測定時は比較実行が可能

---

## 🔗 命名規則の統一

### パターン1: Taskletモデル

| 要素 | 命名 | ファイルパス |
|------|------|-------------|
| Job名 | `TaskletPerformanceJob` | - |
| Config | `TaskletPerformanceJobConfig` | `config/TaskletPerformanceJobConfig.java` |
| Step名 | `TaskletPerformanceStep` | - |
| Tasklet実装 | `TaskletPerformanceTasklet` | `batch/tasklet/TaskletPerformanceTasklet.java` |

### パターン2: Chunkモデル

| 要素 | 命名 | ファイルパス |
|------|------|-------------|
| Job名 | `ChunkPerformanceJob` | - |
| Config | `ChunkPerformanceJobConfig` | `config/ChunkPerformanceJobConfig.java` |
| Step名 | `ChunkPerformanceStep` | - |
| Reader実装 | `ChunkPerformanceItemReader` | `batch/chunk/ChunkPerformanceItemReader.java` |
| Writer実装 | `ChunkPerformanceItemWriter` | `batch/chunk/ChunkPerformanceItemWriter.java` |

### パターン3: 汎用Tasklet

| 要素 | 命名 | ファイルパス |
|------|------|-------------|
| Job名 | `HelloAwsBatchJob` | - |
| Config | `HelloAwsJobConfig` | `config/HelloAwsJobConfig.java` |
| Step名 | `HelloAwsStep` | - |
| Tasklet実装 | `HelloAwsTasklet` | `batch/tasklet/HelloAwsTasklet.java` |

**命名ルール**:
- Job名の接頭辞を全ての関連クラスで統一
- Config: `{Job名}Config`
- Tasklet: `{Job名}Tasklet` または `{機能名}Tasklet`
- Reader: `{Job名}ItemReader`
- Writer: `{Job名}ItemWriter`

---

## 🚀 実行方法

### 1. HelloAwsBatchJob（SQS処理）のみ実行
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=hello"
```

### 2. Taskletモデル（性能測定）のみ実行
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=tasklet"
```

### 3. Chunkモデル（性能測定）のみ実行
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=chunk"
```

### 4. 性能比較（TaskletとChunk両方実行）
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=compare"
```

### 5. 全て実行
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--job=all"
```

### 6. 引数なし（デフォルト: HelloAwsBatchJobのみ実行）
```bash
mvn spring-boot:run
```

---

## ✅ リファクタリング前後の比較

### ❌ リファクタリング前（アンチパターン）

```java
@Configuration
public class BadJobConfig {
    
    @Bean
    public Step badStep(JobRepository jobRepository, 
                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("badStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // ← インライン実装（ラムダ式ベタ書き）
                    // ← IDEで「定義へジャンプ」できない
                    // ← 処理が追えない
                    System.out.println("処理実行");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
```

**問題点**:
- ❌ 実処理がConfigクラス内に埋め込まれている
- ❌ IDEの「定義へジャンプ」が機能しない
- ❌ 処理が追えない
- ❌ テストが書きにくい
- ❌ 再利用できない

### ✅ リファクタリング後（推奨パターン）

#### Config（設計図）
```java
// config/GoodJobConfig.java
@Configuration
public class GoodJobConfig {
    
    @Bean
    public Step goodStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            GoodTasklet goodTasklet) {  // ← 独立したクラスをインジェクション
        return new StepBuilder("goodStep", jobRepository)
                .tasklet(goodTasklet, transactionManager)  // ← Cmd+クリックで実処理へジャンプ可能
                .build();
    }
}
```

#### Tasklet（実処理）
```java
// batch/tasklet/GoodTasklet.java
@Component
public class GoodTasklet implements Tasklet {
    
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) 
            throws Exception {
        // ← 実処理がここに記述されている
        // ← IDEで「定義へジャンプ」できる
        // ← 処理が追える
        System.out.println("処理実行");
        return RepeatStatus.FINISHED;
    }
}
```

**改善点**:
- ✅ 実処理が独立したクラスファイルに分離
- ✅ IDEの「定義へジャンプ」が確実に機能
- ✅ 処理が追える
- ✅ テストが書きやすい
- ✅ 再利用できる

---

## 📝 まとめ

### リファクタリングで実現したこと

1. **追跡性の向上**
   - IDEの「定義へジャンプ」が確実に機能
   - どこに何があるか一目でわかる

2. **保守性の向上**
   - 1ファイル1クラスで責務が明確
   - テストが書きやすい

3. **再利用性の向上**
   - 独立したクラスなので他のJobでも再利用可能
   - Listenerなどは複数のJobで共有

4. **可読性の向上**
   - パッケージ構造が用途別に整理
   - 命名規則が統一されている

### 今後の開発で守るべきルール

1. ✅ **インライン実装（ラムダ式ベタ書き）は絶対に禁止**
2. ✅ **実処理は必ず独立したクラス（別ファイル）として切り出す**
3. ✅ **実処理クラスには `@Component` を付与**
4. ✅ **Configクラスではコンストラクタインジェクションで受け取る**
5. ✅ **パッケージ構造を厳格に守る**
6. ✅ **命名規則を統一する**

---

## 🔧 トラブルシューティング

### Q1: IDEで「定義へジャンプ」できない
**A**: 以下を確認してください
- 実処理クラスに `@Component` が付与されているか
- Configクラスでコンストラクタインジェクションしているか
- パッケージ名が正しいか

### Q2: Beanが見つからないエラー
**A**: 以下を確認してください
- 実処理クラスに `@Component` が付与されているか
- パッケージが `com.example.demo` 配下にあるか（ComponentScanの対象）
- クラス名とBean名が一致しているか

### Q3: 既存のJobが動かなくなった
**A**: 以下を確認してください
- 旧パッケージ（`presentation/config/`, `presentation/tasklet/` 等）のファイルを削除したか
- 新パッケージ（`config/`, `batch/tasklet/` 等）にファイルが正しく配置されているか
- import文が新しいパッケージを参照しているか

---

## 📚 参考資料

- [Spring Batch公式ドキュメント](https://spring.io/projects/spring-batch)
- [taguchiStudy.md](docs/taguchiStudy.md) - Tasklet vs Chunk の選択基準
- [0510処理フロー.md](docs/0510処理フロー.md) - レイヤー設計の詳細

---

**作成日**: 2026-05-10  
**バージョン**: 1.0.0
