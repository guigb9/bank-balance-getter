package br.com.itau.challenge.hello.domain.model

import java.math.BigDecimal
import java.util.Currency
import java.util.UUID
import kotlin.time.Instant

data class Transaction(
    val id: UUID,
    val type: TransactionTypeEnum,
    val amount: BigDecimal,
    val currency: Currency,
    val status: TransactionStatusEnum,
    val timestamp: Instant,
    val accountId: UUID,
)