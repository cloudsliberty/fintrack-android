package com.fintrack.android.data.model

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════
//  Core entities — field names/types mirror each PHP Service's
//  mapRow()/toArray(), so Gson can deserialize API responses directly.
// ═══════════════════════════════════════════════════════════════

data class Account(
    val id: Int = 0,
    val name: String = "",
    val type: String = "asset", // asset | expense | revenue | liability
    val currency: String = "USD",
    val description: String = "",
    val icon: String = "",
    val color: String = "#4f8ef7",
    val active: Boolean = true,
    val created: Long = 0
)

/** The four ledger account types FinTrack's Nextcloud backend accepts. */
val ACCOUNT_TYPES = listOf("asset", "expense", "revenue", "liability")

data class Transaction(
    val id: Int = 0,
    val accountId: Int = 0,
    val type: String = "expense", // income | expense
    val amount: Double = 0.0,
    val currency: String = "USD",
    val conversionRate: Double? = null,
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val date: String = "", // yyyy-MM-dd
    val source: String = "manual",
    val recurringId: Int? = null,
    val created: Long = 0,
    val deletedAt: Long? = null
)

data class Transfer(
    val id: Int = 0,
    val fromAccountId: Int = 0,
    val toAccountId: Int = 0,
    val fromAmount: Double = 0.0,
    val toAmount: Double = 0.0,
    val fromCurrency: String = "USD",
    val toCurrency: String = "USD",
    val conversionRate: Double = 1.0,
    val description: String = "",
    val date: String = "",
    val created: Long = 0
)

data class Budget(
    val id: Int = 0,
    val name: String = "",
    @SerializedName("limit") val limitAmount: Double = 0.0,
    val currency: String = "USD",
    val period: String = "monthly", // weekly | monthly | yearly
    val category: String = "",
    val active: Boolean = true,
    val startDate: String = "",
    val created: Long = 0
)

data class Category(
    val id: Int = 0,
    val name: String = "",
    val type: String = "expense", // income | expense
    val icon: String = "",
    val color: String = "#4f8ef7"
)

data class Currency(
    val id: Int = 0,
    val code: String = "USD",
    val name: String = "",
    val symbol: String = "$",
    val rate: Double = 1.0
)

data class RecurringRule(
    val id: Int = 0,
    val name: String = "",
    val type: String = "expense",
    val accountId: Int = 0,
    val amount: Double = 0.0,
    val currency: String = "USD",
    val frequency: String = "monthly", // daily | weekly | monthly | yearly
    val nextDate: String = "",
    val endDate: String? = null,
    val lastPosted: String? = null,
    val category: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val created: Long = 0
)

// ═══════════════════════════════════════════════════════════════
//  Settings / summary / lock / misc
// ═══════════════════════════════════════════════════════════════

data class Summary(
    val baseCurrency: String = "USD",
    val totalAccounts: Int = 0,
    val totalTransactions: Int = 0,
    val currencies: List<Currency> = emptyList()
)

data class ApiToken(
    val token: String = ""
)

data class LockStatus(
    val enabled: Boolean = false,
    val timeoutMinutes: Int = 10,
    val lockedUntil: Long? = null,
    val pendingAdminReset: Boolean = false
)

data class LockResetQuestion(
    val question: String? = null
)

data class StatusResponse(
    val status: String = "",
    val error: String? = null
)

data class TagsResponse(
    val tags: List<String>? = null,
    val status: String? = null,
    val error: String? = null
)

data class CategoriesExport(
    val categories: List<Category> = emptyList(),
    val tags: List<String> = emptyList()
)

data class CategoriesImportRequest(
    val categories: List<Category> = emptyList(),
    val tags: List<String> = emptyList()
)

data class CategoriesImportResult(
    val importedCategories: Int = 0,
    val importedTags: Int = 0
)

data class TransactionImportRow(
    val date: String,
    val type: String? = null,
    val amount: Double? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val conversionRate: Double? = null,
    @SerializedName("unique-key(for-updating)") val uniqueKey: String? = null
)

data class TransactionsImportRequest(
    val accountId: Int,
    val transactions: List<TransactionImportRow>
)

data class TransactionsImportResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val autoCategorized: Int = 0,
    val errors: List<Map<String, String>> = emptyList()
)

data class CategoryRule(
    val pattern: String = "",
    val category: String = ""
)

data class CategoryRulesRequest(
    val rules: List<CategoryRule>
)

data class ExchangeRateResponse(
    val from: String = "",
    val to: String = "",
    val rate: Double = 1.0,
    val error: String? = null
)

data class ApiKeyTestRequest(val apiKey: String? = null)
data class ApiKeyTestResult(val ok: Boolean = false, val message: String? = null)

data class RecurringPostResult(
    val transaction: Transaction? = null,
    val nextDate: String = "",
    val expired: Boolean = false
)

/** Result of POST /api/reset — a pre-reset backup is written to Nextcloud Files first. */
data class ResetResult(
    val status: String = "",
    val backupPath: String? = null,
    val backupError: String? = null
)

// ═══════════════════════════════════════════════════════════════
//  Request bodies (only the fields each endpoint actually reads)
// ═══════════════════════════════════════════════════════════════

data class AccountRequest(
    val name: String,
    val type: String,
    val currency: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val active: Boolean = true
)

data class TransactionRequest(
    val accountId: Int,
    val type: String,
    val amount: Double,
    val currency: String,
    val conversionRate: Double? = null,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val date: String
)

data class TransferRequest(
    val fromAccountId: Int,
    val toAccountId: Int,
    val fromAmount: Double,
    val toAmount: Double,
    val fromCurrency: String,
    val toCurrency: String,
    val conversionRate: Double,
    val description: String? = null,
    val date: String
)

data class BudgetRequest(
    val name: String,
    @SerializedName("limit") val limitAmount: Double,
    val currency: String,
    val period: String,
    val category: String? = null,
    val active: Boolean = true,
    val startDate: String? = null
)

data class CategoryRequest(
    val name: String,
    val type: String,
    val icon: String? = null,
    val color: String? = null
)

data class CurrencyRequest(
    val code: String,
    val name: String,
    val symbol: String,
    val rate: Double
)

data class RecurringRequest(
    val name: String,
    val type: String,
    val accountId: Int,
    val amount: Double,
    val currency: String,
    val frequency: String,
    val nextDate: String,
    val endDate: String? = null,
    val category: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val active: Boolean = true
)

data class SaveTagsRequest(val tags: List<String>)
data class RenameTagRequest(val oldName: String, val newName: String)
data class LockSetupRequest(
    val newPassword: String,
    val currentPassword: String? = null,
    val timeoutMinutes: Int = 10,
    val resetQuestion: String? = null,
    val resetAnswer: String? = null
)
data class LockDisableRequest(val currentPassword: String)
data class LockVerifyRequest(val password: String)
data class LockResetVerifyRequest(val answer: String)
