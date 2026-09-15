package br.com.itau.challenge.balance.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
class BalanceMessage(
    val currency: String? = null,
    val amount: BigDecimal? = null,
)