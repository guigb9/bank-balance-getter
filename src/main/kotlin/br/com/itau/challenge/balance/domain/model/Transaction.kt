package br.com.itau.challenge.balance.domain.model

import java.math.BigDecimal
import java.util.Currency
import java.util.UUID

data class Transaction(
    val id: UUID,
    val type: TransactionTypeEnum,
    val amount: BigDecimal,
    val currency: Currency,
    val status: TransactionStatusEnum,
    val timestamp: Long,
    val accountId: UUID,
)