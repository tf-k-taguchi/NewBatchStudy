package com.example.demo.domain.model;

/**
 * バッチ処理エラー種別
 *
 * 【例外隔離（Skip & DLQ）】
 * このEnumは0510処理フロー.mdの「例外隔離（Skip & DLQ）」に該当します。
 * エラーの種類を判定し、適切な対応（リトライ or DLQ送信）を決定します。
 *
 * 【エラー種別の分類】
 * taguchiStudy.mdの「バッチ処理ですごく大切なこと」に基づき、
 * 失敗が起きた際に運用に耐えられる前提で設計されています。
 *
 * 【設計思想】
 * - リトライすべきエラーとそうでないエラーを明確に区別
 * - 一時的なエラー（ネットワーク障害等）は自動リトライ
 * - 恒久的なエラー（データ形式エラー等）はDLQに隔離
 * - システムエラーは運用チームへの通知が必要
 *
 * 【使用箇所】
 * BatchProcessService.handleProcessingError()で使用され、
 * エラー種別に応じた適切な処理を実行します。
 */
public enum BatchErrorType {
    
    /**
     * リトライ可能なエラー
     * 例: 一時的なネットワークエラー、タイムアウト
     */
    RETRYABLE("リトライ可能"),
    
    /**
     * リトライ不可能なエラー
     * 例: データ形式エラー、ビジネスロジックエラー
     */
    NON_RETRYABLE("リトライ不可"),
    
    /**
     * システムエラー
     * 例: DB接続エラー、予期しない例外
     */
    SYSTEM_ERROR("システムエラー");
    
    private final String description;
    
    BatchErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 例外からエラー種別を判定
     *
     * 【判定ロジック】
     * 例外のクラス名とメッセージから、エラー種別を自動判定します。
     *
     * 【判定基準】
     * 1. RETRYABLE（リトライ可能）
     *    - タイムアウト系: taguchiStudy.mdの「タイムアウト」対策
     *    - ネットワーク系: taguchiStudy.mdの「ネットワーク障害」対策
     *    → 一時的なエラーなので、時間を置けば成功する可能性が高い
     *
     * 2. NON_RETRYABLE（リトライ不可能）
     *    - データ形式エラー: taguchiStudy.mdの「異常データ」対策
     *    - ビジネスロジックエラー: バリデーションエラー等
     *    → 何度リトライしても同じエラーになるため、DLQに隔離
     *
     * 3. SYSTEM_ERROR（システムエラー）
     *    - 上記以外の予期しないエラー
     *    → 運用チームへの通知が必要
     *
     * 【拡張ポイント】
     * 実際のプロジェクトでは、以下のような判定を追加します：
     * - APIレート制限エラー → RETRYABLE（待機後にリトライ）
     * - デッドロックエラー → RETRYABLE（再実行で解消される可能性）
     * - 権限エラー → NON_RETRYABLE（設定ミス）
     *
     * @param ex 発生した例外
     * @return エラー種別
     */
    public static BatchErrorType fromException(Exception ex) {
        String message = ex.getMessage();
        String className = ex.getClass().getSimpleName();
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // タイムアウト系 → RETRYABLE
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // taguchiStudy.mdの「タイムアウト」対策
        // データが多すぎて予定時間に処理が終わらない場合、リトライで成功する可能性
        if (className.contains("Timeout") || (message != null && message.contains("timeout"))) {
            return RETRYABLE;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ネットワーク系 → RETRYABLE
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // taguchiStudy.mdの「ネットワーク障害」対策
        // 通信の一時的な遮断で異常終了した場合、リトライで成功する可能性
        if (className.contains("Network") || className.contains("Connection")) {
            return RETRYABLE;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // データ形式エラー → NON_RETRYABLE
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // taguchiStudy.mdの「異常データ」対策
        // 文字列や空データなど想定外の値が混入している場合、リトライしても無駄
        if (className.contains("Parse") || className.contains("Format")) {
            return NON_RETRYABLE;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ビジネスロジックエラー → NON_RETRYABLE
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // バリデーションエラー等、ビジネスルール違反の場合、リトライしても無駄
        if (className.contains("IllegalArgument") || className.contains("Validation")) {
            return NON_RETRYABLE;
        }
        
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // その他はシステムエラー
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 予期しないエラーは、運用チームへの通知が必要
        return SYSTEM_ERROR;
    }
}
