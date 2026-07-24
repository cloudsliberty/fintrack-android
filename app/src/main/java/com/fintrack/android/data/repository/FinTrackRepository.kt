package com.fintrack.android.data.repository

import android.content.Context
import com.fintrack.android.data.OfflineCache
import com.fintrack.android.data.SyncStatusManager
import com.fintrack.android.data.model.*
import com.fintrack.android.data.network.ApiClient
import com.fintrack.android.data.network.FinTrackApi
import com.fintrack.android.data.sync.PendingOpType
import com.fintrack.android.data.sync.PendingSyncQueue
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Thin wrapper over FinTrackApi that (a) resolves the current session's
 * Retrofit client, (b) runs every call on Dispatchers.IO, and (c) turns
 * exceptions into a Result so every ViewModel handles errors the same way
 * instead of each screen needing its own try/catch boilerplate.
 */
class FinTrackRepository(private val context: Context) {

    private val gson = Gson()
    private val accountsListType = object : TypeToken<List<Account>>() {}.type
    private val transactionListType = object : TypeToken<List<Transaction>>() {}.type

    /** Cache keys shared with [peekCache] so screens can seed their initial state from whatever was last cached, before the network call resolves — avoids a blank/loading screen on every visit. */
    companion object {
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_CATEGORIES = "categories"
        const val KEY_TAGS = "tags"
        const val KEY_CURRENCIES = "currencies"
        const val KEY_BUDGETS = "budgets"
        const val KEY_RECURRING = "recurring"
        const val KEY_TRANSFERS = "transfers"
        const val KEY_SUMMARY = "summary"
        fun transactionsKey(accountId: Int?, type: String?, category: String?, from: String?, to: String?) =
            "transactions:$accountId:$type:$category:$from:$to"
    }

    /** Synchronous, no-network peek at whatever is currently cached under [key] — use to seed a screen's initial UI state immediately, before kicking off the real (network-first) load. */
    fun <T> peekCache(key: String, type: java.lang.reflect.Type): T? = OfflineCache.get(context, key, type)
    val accountsType: java.lang.reflect.Type get() = accountsListType
    val transactionsType: java.lang.reflect.Type get() = transactionListType

    private fun api(): FinTrackApi = ApiClient.finTrack(context)
        ?: throw IllegalStateException("Not logged in")

    private suspend fun <T> call(block: suspend (FinTrackApi) -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block(api()))
        } catch (e: HttpException) {
            val message = e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: e.message()
            Result.failure(Exception(message))
        } catch (e: IOException) {
            Result.failure(Exception("Network error — check your connection and server address."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Same as [call], but for cacheable GET data: every successful response is written to
     * [OfflineCache] under [cacheKey], and every failed one falls back to whatever was last cached
     * there (still returned as [Result.success]) so the screen keeps showing usable data offline.
     * [SyncStatusManager] is nudged around the call so the app-wide status pill can reflect it.
     */
    private suspend fun <T> cachedCall(cacheKey: String, type: java.lang.reflect.Type, block: suspend (FinTrackApi) -> T): Result<T> =
        withContext(Dispatchers.IO) {
            SyncStatusManager.begin()
            try {
                val result = block(api())
                OfflineCache.put(context, cacheKey, result)
                SyncStatusManager.end(usedCacheFallback = false)
                Result.success(result)
            } catch (e: Exception) {
                val cached = OfflineCache.get<T>(context, cacheKey, type)
                if (cached != null) {
                    SyncStatusManager.end(usedCacheFallback = true)
                    Result.success(cached)
                } else {
                    SyncStatusManager.end(usedCacheFallback = false)
                    when (e) {
                        is HttpException -> Result.failure(Exception(e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: e.message()))
                        is IOException -> Result.failure(Exception("Network error — check your connection and server address."))
                        else -> Result.failure(e)
                    }
                }
            }
        }

    /**
     * For writes (create/update account or transaction): tries the network first. If that fails
     * specifically because the device is offline (IOException — no route to the server, timeout,
     * etc; NOT a validation/HTTP error from the server, which still surfaces normally), the write
     * is queued in [PendingSyncQueue] for [SyncWorker] to replay later, and [patchCache] is used to
     * make the change visible locally right away so the screen doesn't look like it silently failed.
     */
    private suspend fun <Req, Resp> offlineFirstWrite(
        opType: PendingOpType,
        body: Req,
        targetId: Int?,
        summary: String,
        network: suspend (FinTrackApi) -> Resp,
        localPlaceholder: (localId: Int) -> Resp,
        patchCache: (Resp) -> Unit
    ): Result<Resp> = withContext(Dispatchers.IO) {
        try {
            Result.success(network(api()))
        } catch (e: IOException) {
            val localId = targetId ?: -(System.currentTimeMillis() % 1_000_000_000L).toInt()
            val placeholder = localPlaceholder(localId)
            PendingSyncQueue.enqueue(context, opType, gson.toJson(body), targetId, if (targetId == null) localId else null, summary)
            patchCache(placeholder)
            SyncStatusManager.setPendingCount(PendingSyncQueue.count(context))
            Result.success(placeholder)
        } catch (e: HttpException) {
            Result.failure(Exception(e.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: e.message()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Accounts ──
    suspend fun getAccounts() = cachedCall("accounts", accountsListType) { it.getAccounts() }
    suspend fun createAccount(body: AccountRequest) = offlineFirstWrite(
        opType = PendingOpType.CREATE_ACCOUNT, body = body, targetId = null,
        summary = "Create account \"${body.name}\"",
        network = { it.createAccount(body) },
        localPlaceholder = { localId ->
            Account(id = localId, name = body.name, type = body.type, currency = body.currency,
                description = body.description ?: "", icon = body.icon ?: "", color = body.color ?: "#4f8ef7",
                active = body.active, created = System.currentTimeMillis())
        },
        patchCache = { placeholder ->
            val cached = OfflineCache.get<List<Account>>(context, "accounts", accountsListType) ?: emptyList()
            OfflineCache.put(context, "accounts", cached + placeholder)
        }
    )
    suspend fun updateAccount(id: Int, body: AccountRequest) = offlineFirstWrite(
        opType = PendingOpType.UPDATE_ACCOUNT, body = body, targetId = id,
        summary = "Update account \"${body.name}\"",
        network = { it.updateAccount(id, body) },
        localPlaceholder = {
            Account(id = id, name = body.name, type = body.type, currency = body.currency,
                description = body.description ?: "", icon = body.icon ?: "", color = body.color ?: "#4f8ef7",
                active = body.active, created = System.currentTimeMillis())
        },
        patchCache = { placeholder ->
            val cached = OfflineCache.get<List<Account>>(context, "accounts", accountsListType) ?: emptyList()
            OfflineCache.put(context, "accounts", cached.map { if (it.id == id) placeholder else it })
        }
    )
    suspend fun deleteAccount(id: Int) = call { it.deleteAccount(id) }

    // ── Transactions ──
    suspend fun getTransactions(
        accountId: Int? = null, type: String? = null, category: String? = null,
        from: String? = null, to: String? = null, limit: Int = 500, offset: Int = 0
    ) = cachedCall("transactions:$accountId:$type:$category:$from:$to", transactionListType) {
        it.getTransactions(accountId, type, category, from, to, limit, offset)
    }
    suspend fun createTransaction(body: TransactionRequest) = offlineFirstWrite(
        opType = PendingOpType.CREATE_TRANSACTION, body = body, targetId = null,
        summary = "Create transaction \"${body.description ?: body.category ?: "Untitled"}\"",
        network = { it.createTransaction(body) },
        localPlaceholder = { localId -> transactionFromRequest(localId, body) },
        patchCache = { placeholder -> patchAllTransactionCaches { it + placeholder } }
    )
    suspend fun updateTransaction(id: Int, body: TransactionRequest) = offlineFirstWrite(
        opType = PendingOpType.UPDATE_TRANSACTION, body = body, targetId = id,
        summary = "Update transaction \"${body.description ?: body.category ?: "Untitled"}\"",
        network = { it.updateTransaction(id, body) },
        localPlaceholder = { transactionFromRequest(id, body) },
        patchCache = { placeholder -> patchAllTransactionCaches { list -> list.map { if (it.id == id) placeholder else it } } }
    )
    suspend fun deleteTransaction(id: Int) = call { it.deleteTransaction(id) }
    suspend fun importTransactions(body: TransactionsImportRequest) = call { it.importTransactions(body) }

    private fun transactionFromRequest(id: Int, body: TransactionRequest) = Transaction(
        id = id, accountId = body.accountId, type = body.type, amount = body.amount, currency = body.currency,
        conversionRate = body.conversionRate, description = body.description ?: "", category = body.category ?: "",
        tags = body.tags, notes = body.notes ?: "", date = body.date, source = "manual", created = System.currentTimeMillis()
    )

    /** Every cached "transactions:*" filter variant gets patched, since we don't know which one the visible screen is using. */
    private fun patchAllTransactionCaches(transform: (List<Transaction>) -> List<Transaction>) {
        OfflineCache.keysWithPrefix(context, "transactions:").forEach { key ->
            val cached = OfflineCache.get<List<Transaction>>(context, key, transactionListType) ?: emptyList()
            OfflineCache.put(context, key, transform(cached))
        }
    }

    // ── Trash ──
    suspend fun getTrash() = call { it.getTrash() }
    suspend fun restoreFromTrash(id: Int) = call { it.restoreFromTrash(id) }
    suspend fun destroyFromTrash(id: Int) = call { it.destroyFromTrash(id) }
    suspend fun emptyTrash() = call { it.emptyTrash() }

    // ── Transfers ──
    suspend fun getTransfers() = cachedCall("transfers", object : TypeToken<List<Transfer>>() {}.type) { it.getTransfers() }
    suspend fun createTransfer(body: TransferRequest) = call { it.createTransfer(body) }
    suspend fun deleteTransfer(id: Int) = call { it.deleteTransfer(id) }

    // ── Budgets ──
    suspend fun getBudgets() = cachedCall("budgets", object : TypeToken<List<Budget>>() {}.type) { it.getBudgets() }
    suspend fun createBudget(body: BudgetRequest) = call { it.createBudget(body) }
    suspend fun updateBudget(id: Int, body: BudgetRequest) = call { it.updateBudget(id, body) }
    suspend fun deleteBudget(id: Int) = call { it.deleteBudget(id) }

    // ── Categories ──
    suspend fun getCategories() = cachedCall("categories", object : TypeToken<List<Category>>() {}.type) { it.getCategories() }
    suspend fun createCategory(body: CategoryRequest) = call { it.createCategory(body) }
    suspend fun updateCategory(id: Int, body: CategoryRequest) = call { it.updateCategory(id, body) }
    suspend fun deleteCategory(id: Int) = call { it.deleteCategory(id) }
    suspend fun exportCategories() = call { it.exportCategories() }
    suspend fun importCategories(body: CategoriesImportRequest) = call { it.importCategories(body) }
    suspend fun createDefaultCategories() = call { it.createDefaultCategories() }

    // ── Currencies ──
    suspend fun getCurrencies() = cachedCall("currencies", object : TypeToken<List<Currency>>() {}.type) { it.getCurrencies() }
    suspend fun createCurrency(body: CurrencyRequest) = call { it.createCurrency(body) }
    suspend fun updateCurrency(id: Int, body: CurrencyRequest) = call { it.updateCurrency(id, body) }
    suspend fun deleteCurrency(id: Int) = call { it.deleteCurrency(id) }
    suspend fun getExchangeRate(from: String, to: String) = call { it.getExchangeRate(from, to) }
    suspend fun testExchangeRateApiKey(body: ApiKeyTestRequest) = call { it.testExchangeRateApiKey(body) }

    // ── Recurring ──
    suspend fun getRecurring() = cachedCall("recurring", object : TypeToken<List<RecurringRule>>() {}.type) { it.getRecurring() }
    suspend fun createRecurring(body: RecurringRequest) = call { it.createRecurring(body) }
    suspend fun updateRecurring(id: Int, body: RecurringRequest) = call { it.updateRecurring(id, body) }
    suspend fun deleteRecurring(id: Int) = call { it.deleteRecurring(id) }
    suspend fun postRecurringNow(id: Int) = call { it.postRecurringNow(id) }

    // ── Settings / tags / token / summary ──
    suspend fun getSummary() = cachedCall("summary", object : TypeToken<Summary>() {}.type) { it.getSummary() }
    suspend fun getSettings() = call { it.getSettings() }
    suspend fun saveSettings(body: Map<String, String>) = call { it.saveSettings(body) }
    suspend fun getTags() = cachedCall("tags", object : TypeToken<List<String>>() {}.type) { it.getTags() }
    suspend fun saveTags(body: SaveTagsRequest) = call { it.saveTags(body) }
    suspend fun renameTag(body: RenameTagRequest) = call { it.renameTag(body) }
    suspend fun getApiToken() = call { it.getApiToken() }
    suspend fun regenerateApiToken() = call { it.regenerateApiToken() }
    suspend fun resetAllData() = call { it.resetAllData() }
    suspend fun restoreBackup(body: Map<String, Any?>) = call { it.restoreBackup(body) }

    // ── Category rules ──
    suspend fun getCategoryRules() = call { it.getCategoryRules() }
    suspend fun saveCategoryRules(body: CategoryRulesRequest) = call { it.saveCategoryRules(body) }

    // ── App Lock ──
    suspend fun getLockStatus() = call { it.getLockStatus() }
    suspend fun setupLock(body: LockSetupRequest) = call { it.setupLock(body) }
    suspend fun disableLock(body: LockDisableRequest) = call { it.disableLock(body) }
    suspend fun verifyLock(body: LockVerifyRequest) = call { it.verifyLock(body) }
    suspend fun getLockResetQuestion() = call { it.getLockResetQuestion() }
    suspend fun verifyLockResetAnswer(body: LockResetVerifyRequest) = call { it.verifyLockResetAnswer(body) }
    suspend fun requestAdminLockReset() = call { it.requestAdminLockReset() }
}
