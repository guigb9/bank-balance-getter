package br.com.itau.challenge.hello.adapter.input.kafka.dto


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

//Faço assim pra evitar o erro de non default creator do jackson

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransactionMessage(
    val id: String? = null,
    val type: String? = null,
    val currency: String? = null,
    val amount: BigDecimal? = null,
    val status: String? = null,
    val timestamp: Long? = null,
)