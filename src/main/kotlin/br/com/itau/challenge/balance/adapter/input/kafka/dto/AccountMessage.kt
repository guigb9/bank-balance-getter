package br.com.itau.challenge.balance.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountMessage(
    val id: String? = null,
    val owner: String? = null,
    @JsonProperty(value = "created_at")
    val createdAt: Long? = null,
    val status: String? = null,
    val balance: BalanceMessage? = null,
)