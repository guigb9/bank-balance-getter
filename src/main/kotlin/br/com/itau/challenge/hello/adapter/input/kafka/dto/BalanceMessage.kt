package br.com.itau.challenge.hello.adapter.input.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
class BalanceMessage(
    val currency: String? = null,
    val amount: BigDecimal? = null,
)