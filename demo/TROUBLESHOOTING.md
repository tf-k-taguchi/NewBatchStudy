# トラブルシューティングガイド

このドキュメントでは、HelloAwsBatchJobの実行時によくある問題と解決方法をまとめています。

## 🔴 起動時のエラー

### エラー: "メイン・クラスcom.example.demo.DemoApplicationを検出およびロードできませんでした"

```
エラー: メイン・クラスcom.example.demo.DemoApplicationを検出およびロードできませんでした
原因: java.lang.ClassNotFoundException: com.example.demo.DemoApplication
```

**原因**: クラスファイルがコンパイルされていない

**解決策**:

```bash
./mvnw clean compile
```

実行後、再度VSCodeのデバッグ起動（F5）を試してください。

---

### エラー: "BUILD FAILURE"

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**原因1**: Java 21がインストールされていない

**解決策**:
1. Javaバージョンを確認：
   ```bash
   java -version
   ```
2. Java 21以外の場合、[Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)をインストール

**原因2**: Mavenキャッシュの破損

**解決策**:
```bash
./mvnw clean
rm -rf ~/.m2/repository
./mvnw compile
```

---

### エラー: "Port 8080 already in use"

```
Web server failed to start. Port 8080 was already in use.
```

**原因**: ポート8080が既に使用中

**解決策1**: 使用中のプロセスを停止

```bash
# macOS/Linux
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID番号> /F
```

**解決策2**: 別のポートを使用

`src/main/resources/application.yml`に以下を追加：

```yaml
server:
  port: 8081
```

---

## 🔴 データベース関連のエラー

### エラー: "Table 'BATCH_LOG' not found"

```
org.h2.jdbc.JdbcSQLSyntaxErrorException: テーブル "BATCH_LOG" が見つかりません
```

**原因**: schema.sqlが実行されていない

**解決策**:

`src/main/resources/application.yml`を確認：

```yaml
spring:
  sql:
    init:
      mode: always  # この設定があることを確認
      schema-locations: classpath:schema.sql
```

設定後、アプリケーションを再起動してください。

---

### エラー: H2コンソールに接続できない

**症状**: `http://localhost:8080/h2-console`にアクセスできない

**原因1**: アプリケーションが起動していない

**解決策**: バッチジョブを起動してから、H2コンソールにアクセスしてください。

**原因2**: H2コンソールが無効化されている

**解決策**: `src/main/resources/application.yml`を確認：

```yaml
spring:
  h2:
    console:
      enabled: true  # trueになっていることを確認
```

---

## 🔴 VSCode関連のエラー

### デバッグ設定が表示されない

**症状**: 「実行とデバッグ」に「HelloAwsBatchJob起動」が表示されない

**解決策1**: VSCodeを再起動

**解決策2**: ワークスペースを再読み込み
1. `Ctrl+Shift+P` (Windows/Linux) または `Cmd+Shift+P` (Mac)
2. "Developer: Reload Window"を選択

**解決策3**: `.vscode/launch.json`の確認

ファイルが存在し、以下の内容が含まれていることを確認：

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "HelloAwsBatchJob起動",
            "request": "launch",
            "mainClass": "com.example.demo.DemoApplication",
            "projectName": "demo"
        }
    ]
}
```

---

### Java Extension Packがインストールされていない

**症状**: Javaファイルが正しく認識されない、デバッグができない

**解決策**:
1. VSCodeの拡張機能タブを開く（`Ctrl+Shift+X` / `Cmd+Shift+X`）
2. "Extension Pack for Java"を検索
3. インストールボタンをクリック
4. VSCodeを再起動

---

## 🔴 MyBatis関連のエラー

### エラー: "Invalid bound statement (not found)"

```
org.apache.ibatis.binding.BindingException: Invalid bound statement (not found): 
com.example.demo.infrastructure.mapper.BatchLogMapper.insert
```

**原因**: MyBatisマッパーXMLが読み込まれていない

**解決策**:

1. `src/main/resources/mybatis/mapper/BatchLogMapper.xml`が存在することを確認

2. `src/main/resources/application.yml`の設定を確認：

```yaml
mybatis:
  mapper-locations: classpath:mybatis/mapper/*.xml
```

3. 再コンパイル：

```bash
./mvnw clean compile
```

---

## 🔴 Spring Batch関連のエラー

### エラー: "A job instance already exists"

```
org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException: 
A job instance already exists and is complete
```

**原因**: 同じパラメータでジョブを再実行しようとしている

**解決策**: 

`src/main/java/com/example/demo/presentation/runner/BatchJobRunner.java`で、
毎回異なるパラメータを使用しています（タイムスタンプ）ので、通常は発生しません。

もし発生した場合は、H2データベースをリセット：

```bash
# アプリケーションを停止
# 再起動すると、インメモリDBがリセットされます
```

---

## 🔴 依存関係のエラー

### エラー: "Could not resolve dependencies"

```
[ERROR] Failed to execute goal on project demo: Could not resolve dependencies
```

**原因**: Maven Central Repositoryへの接続問題

**解決策**:

1. インターネット接続を確認

2. プロキシ設定が必要な場合、`~/.m2/settings.xml`を作成：

```xml
<settings>
  <proxies>
    <proxy>
      <id>example-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>proxy.example.com</host>
      <port>8080</port>
    </proxy>
  </proxies>
</settings>
```

3. 依存関係を再取得：

```bash
./mvnw dependency:purge-local-repository
./mvnw clean install
```

---

## 🔴 ログ関連の問題

### カスタムログが表示されない

**症状**: `[Console]`で始まるログが表示されない

**原因**: BatchLoggerが正しくDIされていない

**解決策**:

1. `ConsoleBatchLogger`に`@Component`アノテーションがあることを確認

2. アプリケーションを再起動

---

## 📞 サポート

上記の解決策で問題が解決しない場合：

1. **ログを確認**: VSCodeの統合ターミナルに表示されるエラーメッセージ全体を確認
2. **環境情報を収集**:
   ```bash
   java -version
   ./mvnw -version
   ```
3. **クリーンビルド**:
   ```bash
   ./mvnw clean
   rm -rf target
   ./mvnw compile
   ```

---

## ✅ 正常動作の確認

以下のコマンドで正常に動作することを確認できます：

```bash
# 1. コンパイル
./mvnw clean compile

# 2. 実行
./mvnw spring-boot:run
```

**期待される出力**:

```
========================================
バッチジョブを起動します
========================================

[Console] ... - INFO - Starting HelloAwsBatchJob
[Console] ... - INFO - SQS Mock: メッセージ受信 - Mock SQS Message 1
[Console] ... - INFO - バッチログをDBに保存しました - Status: SUCCESS

========================================
バッチジョブが完了しました
========================================

[INFO] BUILD SUCCESS
```

この出力が表示されれば、正常に動作しています。
