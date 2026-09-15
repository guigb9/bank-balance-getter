package br.com.itau.challenge.balance.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountTransactionMessage(
    val transaction: TransactionMessage? = null,
    val account: AccountMessage? = null,
)