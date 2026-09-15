package br.com.itau.challenge.hello.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountTransactionMessage(
    val transaction: TransactionMessage? = null,
    val account: AccountMessage? = null,
)