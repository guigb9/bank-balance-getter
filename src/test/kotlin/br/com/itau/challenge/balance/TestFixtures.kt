package br.com.itau.challenge.balance

import br.com.itau.challenge.balance.domain.model.Account
import br.com.itau.challenge.balance.domain.model.AccountStatusEnum
import br.com.itau.challenge.balance.domain.model.Balance
import br.com.itau.challenge.balance.domain.model.Transaction
import br.com.itau.challenge.balance.domain.model.TransactionStatusEnum
import br.com.itau.challenge.balance.domain.model.TransactionTypeEnum
import java.math.BigDecimal
import java.util.Currency
import java.util.UUID

val ACCOUNT_ID: UUID = UUID.fromString("5b19c8b6-0cc4-4c72-a989-0c2ee15fa975")
val OWNER_ID: UUID = UUID.fromString("315e3cfe-f4af-4cd2-b298-a449e614349a")
val TRANSACTION_ID: UUID = UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543")

fun account(
    updatedAt: Long = 1_751_641_364_589_998,
    amount: BigDecimal = BigDecimal("183.12"),
) = Account(
    id = ACCOUNT_ID,
    ownerId = OWNER_ID,
    status = AccountStatusEnum.ENABLED,
    balance = Balance(amount = amount, currency = "BRL"),
    createdAt = 1_634_874_339_000_000,
    updatedAt = updatedAt,
)

fun transaction() = Transaction(
    id = TRANSACTION_ID,
    type = TransactionTypeEnum.CREDIT,
    amount = BigDecimal("97.07"),
    currency = Currency.getInstance("BRL"),
    status = TransactionStatusEnum.APPROVED,
    timestamp = 1_751_641_364_589_998,
    accountId = ACCOUNT_ID,
)
