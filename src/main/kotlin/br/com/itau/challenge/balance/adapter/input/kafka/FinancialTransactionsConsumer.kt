package br.com.itau.challenge.balance.adapter.input.kafka

import br.com.itau.challenge.balance.adapter.input.kafka.dto.AccountTransactionMessage
import br.com.itau.challenge.balance.adapter.input.kafka.exception.InvalidMessageFormatException
import br.com.itau.challenge.balance.adapter.input.kafka.mapper.toEntity
import br.com.itau.challenge.balance.port.input.FinancialTransactionProcessorUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class FinancialTransactionsConsumer(
    val objectMapper: ObjectMapper,
    val financialTransactionProcessorUseCase: FinancialTransactionProcessorUseCase,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @KafkaListener(topics = ["\${financial-processed-transactions.topic-name}"])
    fun consume(payload: String) {
        try {
            val message = objectMapper.readValue(payload, AccountTransactionMessage::class.java)

            logger.info("Received message ${objectMapper.writeValueAsString(message)}")

            financialTransactionProcessorUseCase.processTransaction(
                account = message.account?.toEntity(updatedAt = message.transaction?.timestamp) ?: throw InvalidMessageFormatException("account"),
                transaction = message.transaction?.toEntity(accountId = message.account.id) ?: throw InvalidMessageFormatException("transaction"),
            )

            logger.debug("Transaction ${message.transaction.id} of account ${message.account.id} proccessed")
        } catch (e: Exception) {
            logger.error("Error while processing transaction ${objectMapper.writeValueAsBytes(payload)}", e)
            throw e
        }

    }
}