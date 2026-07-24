package com.fintrack.android.data.network

import com.fintrack.android.data.model.*
import retrofit2.http.*

/**
 * Every endpoint defined in the FinTrack Nextcloud app's appinfo/routes.php,
 * under 'api#...' — called with HTTP Basic Auth (Nextcloud login name + app
 * password obtained via Login Flow v2, see NextcloudAuthApi/SessionManager)
 * against {server}/index.php/apps/fintrack/api/. Basic-authenticated
 * requests to Nextcloud controllers aren't subject to the browser-session
 * CSRF check, so no separate CSRF token dance is needed here the way the
 * web app's own JS has to do it.
 */
interface FinTrackApi {

    // ── Accounts ──
    @GET("accounts")
    suspend fun getAccounts(): List<Account>

    @POST("accounts")
    suspend fun createAccount(@Body body: AccountRequest): Account

    @PUT("accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Int, @Body body: AccountRequest): Account

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Int): StatusResponse

    // ── Transactions ──
    @GET("transactions")
    suspend fun getTransactions(
        @Query("accountId") accountId: Int? = null,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0
    ): List<Transaction>

    @POST("transactions")
    suspend fun createTransaction(@Body body: TransactionRequest): Transaction

    @PUT("transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: Int, @Body body: TransactionRequest): Transaction

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int): StatusResponse

    @POST("transactions/import")
    suspend fun importTransactions(@Body body: TransactionsImportRequest): TransactionsImportResult

    // ── Transactions recycle bin ──
    @GET("transactions/trash")
    suspend fun getTrash(): List<Transaction>

    @POST("transactions/trash/{id}/restore")
    suspend fun restoreFromTrash(@Path("id") id: Int): Transaction

    @DELETE("transactions/trash/{id}")
    suspend fun destroyFromTrash(@Path("id") id: Int): StatusResponse

    @POST("transactions/trash/empty")
    suspend fun emptyTrash(): StatusResponse

    // ── Transfers ──
    @GET("transfers")
    suspend fun getTransfers(): List<Transfer>

    @POST("transfers")
    suspend fun createTransfer(@Body body: TransferRequest): Transfer

    @DELETE("transfers/{id}")
    suspend fun deleteTransfer(@Path("id") id: Int): StatusResponse

    // ── Budgets ──
    @GET("budgets")
    suspend fun getBudgets(): List<Budget>

    @POST("budgets")
    suspend fun createBudget(@Body body: BudgetRequest): Budget

    @PUT("budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Int, @Body body: BudgetRequest): Budget

    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Int): StatusResponse

    // ── Categories ──
    @GET("categories")
    suspend fun getCategories(): List<Category>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryRequest): Category

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body body: CategoryRequest): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): StatusResponse

    @GET("categories/export")
    suspend fun exportCategories(): CategoriesExport

    @POST("categories/import")
    suspend fun importCategories(@Body body: CategoriesImportRequest): CategoriesImportResult

    @POST("categories/create-defaults")
    suspend fun createDefaultCategories(): List<Category>

    // ── Currencies ──
    @GET("currencies")
    suspend fun getCurrencies(): List<Currency>

    @POST("currencies")
    suspend fun createCurrency(@Body body: CurrencyRequest): Currency

    @PUT("currencies/{id}")
    suspend fun updateCurrency(@Path("id") id: Int, @Body body: CurrencyRequest): Currency

    @DELETE("currencies/{id}")
    suspend fun deleteCurrency(@Path("id") id: Int): StatusResponse

    @GET("exchange-rate")
    suspend fun getExchangeRate(@Query("from") from: String, @Query("to") to: String): ExchangeRateResponse

    @POST("exchange-rate/test")
    suspend fun testExchangeRateApiKey(@Body body: ApiKeyTestRequest): ApiKeyTestResult

    // ── Recurring ──
    @GET("recurring")
    suspend fun getRecurring(): List<RecurringRule>

    @POST("recurring")
    suspend fun createRecurring(@Body body: RecurringRequest): RecurringRule

    @PUT("recurring/{id}")
    suspend fun updateRecurring(@Path("id") id: Int, @Body body: RecurringRequest): RecurringRule

    @DELETE("recurring/{id}")
    suspend fun deleteRecurring(@Path("id") id: Int): StatusResponse

    @POST("recurring/{id}/post")
    suspend fun postRecurringNow(@Path("id") id: Int): RecurringPostResult

    // ── Settings / tags / token / summary ──
    @GET("summary")
    suspend fun getSummary(): Summary

    @GET("settings")
    suspend fun getSettings(): Map<String, String>

    @POST("settings")
    suspend fun saveSettings(@Body body: Map<String, String>): StatusResponse

    @GET("tags")
    suspend fun getTags(): List<String>

    @POST("tags")
    suspend fun saveTags(@Body body: SaveTagsRequest): StatusResponse

    @POST("tags/rename")
    suspend fun renameTag(@Body body: RenameTagRequest): TagsResponse

    @GET("token")
    suspend fun getApiToken(): ApiToken

    @POST("token/regenerate")
    suspend fun regenerateApiToken(): ApiToken

    @POST("reset")
    suspend fun resetAllData(): ResetResult

    @POST("settings/restore")
    suspend fun restoreBackup(@Body body: Map<String, Any?>): StatusResponse

    // ── Auto-categorization rules (CSV import) ──
    @GET("category-rules")
    suspend fun getCategoryRules(): List<CategoryRule>

    @POST("category-rules")
    suspend fun saveCategoryRules(@Body body: CategoryRulesRequest): List<CategoryRule>

    // ── App Lock (PIN on top of Nextcloud auth) ──
    @GET("lock/status")
    suspend fun getLockStatus(): LockStatus

    @POST("lock/setup")
    suspend fun setupLock(@Body body: LockSetupRequest): LockStatus

    @POST("lock/disable")
    suspend fun disableLock(@Body body: LockDisableRequest): LockStatus

    @POST("lock/verify")
    suspend fun verifyLock(@Body body: LockVerifyRequest): StatusResponse

    @GET("lock/reset-question")
    suspend fun getLockResetQuestion(): LockResetQuestion

    @POST("lock/reset-verify")
    suspend fun verifyLockResetAnswer(@Body body: LockResetVerifyRequest): StatusResponse

    @POST("lock/request-admin-reset")
    suspend fun requestAdminLockReset(): StatusResponse
}
