package br.com.itau.challenge.balance.adapter.input.web.dto

import java.math.BigDecimal

data class BalanceResponse(
    val amount: BigDecimal,
    val currency: String,
)