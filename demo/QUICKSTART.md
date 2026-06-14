# クイックスタートガイド

このガイドでは、HelloAwsBatchJobを最速で起動する手順を説明します。

## 📋 前提条件チェック

以下がインストールされていることを確認してください：

- ✅ Java 21以上
- ✅ VSCode
- ✅ VSCode拡張機能「Extension Pack for Java」

### Java バージョン確認

ターミナルで以下を実行：

```bash
java -version
```

出力例：
```
java version "21.0.3" 2024-04-16 LTS
```

## 🚀 起動手順（3ステップ）

### ステップ1: コンパイル（必須）

VSCodeのターミナルで以下を実行：

```bash
./mvnw clean compile
```

**実行時間**: 約10〜30秒

**成功メッセージ**:
```
[INFO] BUILD SUCCESS
[INFO] Compiling 14 source files with javac
```

⚠️ **重要**: この手順を実行しないと、VSCodeのデバッグ起動時に以下のエラーが発生します：
```
エラー: メイン・クラスcom.example.demo.DemoApplicationを検出およびロードできませんでした
原因: java.lang.ClassNotFoundException: com.example.demo.DemoApplication
```

### ステップ2: VSCodeでデバッグ設定を選択

1. VSCodeの左サイドバーで「実行とデバッグ」アイコンをクリック
   - または、キーボードショートカット: `Ctrl+Shift+D` (Windows/Linux) / `Cmd+Shift+D` (Mac)

2. 上部のドロップダウンメニューから **「HelloAwsBatchJob起動」** を選択

![デバッグ設定選択](https://via.placeholder.com/600x100/4CAF50/FFFFFF?text=HelloAwsBatchJob%E8%B5%B7%E5%8B%95%E3%82%92%E9%81%B8%E6%8A%9E)

### ステップ3: 起動ボタンをクリック

緑色の再生ボタン（▶️）をクリック、または `F5` キーを押す

## ✅ 成功確認

### コンソール出力を確認

VSCodeの統合ターミナルに以下のようなログが表示されれば成功です：

```
========================================
バッチジョブを起動します
========================================

[Console] 2026-02-14 13:41:12 - INFO - Starting HelloAwsBatchJob
[Console] 2026-02-14 13:41:12 - INFO - SQS Mock: メッセージ受信 - Mock SQS Message 1
[Console] 2026-02-14 13:41:12 - INFO - Processing message: Mock SQS Message 1
[Console] 2026-02-14 13:41:12 - INFO - バッチログをDBに保存しました - Status: SUCCESS

========================================
バッチジョブが完了しました
========================================
```

### 処理内容の確認

バッチジョブは以下の処理を実行します：

1. ✅ **SQS Mock**からメッセージを受信（3件のメッセージが用意されています）
2. ✅ **メッセージ処理**を実行
3. ✅ **H2データベース**にバッチログを保存
4. ✅ **S3 Mock**に処理結果をアップロード

## 🔍 データベースの確認（オプション）

バッチ実行後、H2コンソールでデータを確認できます：

### 手順

1. バッチが起動している状態で、ブラウザで以下にアクセス：
   ```
   http://localhost:8080/h2-console
   ```

2. 接続情報を入力：
   - **JDBC URL**: `jdbc:h2:mem:batch_db`
   - **User Name**: `sa`
   - **Password**: （空白）

3. 「Connect」をクリック

4. 以下のSQLを実行：
   ```sql
   SELECT * FROM batch_log ORDER BY created_at DESC;
   ```

### 期待される結果

| ID | BATCH_NAME | MESSAGE | STATUS | CREATED_AT |
|----|------------|---------|--------|------------|
| 2 | HelloAwsBatchJob | Mock SQS Message 1 | SUCCESS | 2026-02-14 13:41:12 |
| 1 | HelloAwsBatchJob | Mock SQS Message 1 | PROCESSING | 2026-02-14 13:41:12 |

## 🐛 デバッグモードで実行

コードの動作を詳しく確認したい場合：

### 手順

1. `src/main/java/com/example/demo/presentation/tasklet/HelloAwsTasklet.java` を開く

2. 行番号の左側をクリックしてブレークポイントを設定
   - 推奨: 44行目付近の `execute` メソッド先頭

3. ドロップダウンから **「HelloAwsBatchJob起動（デバッグ）」** を選択

4. `F5` で起動

5. ブレークポイントで停止したら：
   - `F10`: 次の行へ
   - `F11`: メソッド内部へ
   - `F5`: 続行

## ❓ トラブルシューティング

### エラー: "BUILD FAILURE"

**原因**: 依存関係のダウンロード失敗

**解決策**:
```bash
./mvnw clean
./mvnw install
```

### エラー: "Port 8080 already in use"

**原因**: ポート8080が既に使用中

**解決策**: `src/main/resources/application.yml` に以下を追加：
```yaml
server:
  port: 8081
```

### エラー: "Java version mismatch"

**原因**: Java 21がインストールされていない

**解決策**: 
1. [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) をダウンロード
2. インストール後、VSCodeを再起動

### デバッグ設定が表示されない

**原因**: `.vscode/launch.json` が読み込まれていない

**解決策**:
1. VSCodeを再起動
2. または、`Ctrl+Shift+P` → "Reload Window" を実行

## 📚 次のステップ

起動が成功したら、以下のドキュメントを参照してください：

- **詳細な使い方**: [README.md](README.md)
- **アーキテクチャ設計**: [docs/詳細.md](docs/詳細.md)
- **実装方針**: [docs/実装.md](docs/実装.md)

## 💡 ヒント

### 複数回実行する場合

バッチジョブは起動するたびに自動的に実行されます。
再度実行したい場合は、単純に再度 `F5` を押すだけです。

### SQSメッセージを変更する場合

`src/main/java/com/example/demo/infrastructure/repository/SqsRepositoryMock.java` の
コンストラクタで初期メッセージを変更できます：

```java
public SqsRepositoryMock(BatchLogger logger) {
    this.logger = logger;
    // ここでメッセージを変更
    messageQueue.offer("カスタムメッセージ1");
    messageQueue.offer("カスタムメッセージ2");
}
```

---

**🎉 おめでとうございます！**

HelloAwsBatchJobが正常に起動できました。
Spring Batchの基本的な動作を理解できたら、実際のAWS連携実装にチャレンジしてみましょう！
