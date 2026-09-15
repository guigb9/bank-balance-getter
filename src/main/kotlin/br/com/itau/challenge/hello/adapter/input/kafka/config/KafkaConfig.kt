package br.com.itau.challenge.hello.adapter.input.kafka.config

import br.com.itau.challenge.hello.adapter.input.kafka.exception.InvalidMessageFormatException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import software.amazon.awssdk.thirdparty.jackson.core.JsonProcessingException

@Configuration
class KafkaConfig(
    private val kafkaTemplate: KafkaTemplate<Any, Any>
) {

    @Bean
    fun deadLetterPublishingRecoverer(): DeadLetterPublishingRecoverer {
        return DeadLetterPublishingRecoverer(kafkaTemplate)
    }

    @Bean
    fun kafkaErrorHandler(): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)

        val backOff = FixedBackOff(
            500L,
            10L,
        )

        return DefaultErrorHandler(
            recoverer,
            backOff
        ).apply {
            addNotRetryableExceptions(
                IllegalArgumentException::class.java,
                JsonProcessingException::class.java,
                InvalidMessageFormatException::class.java,
            )
        }
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        kafkaErrorHandler: DefaultErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {

        return ConcurrentKafkaListenerContainerFactory<String, Any>().apply {
            setConcurrency(100)
            setConsumerFactory(consumerFactory)
            setCommonErrorHandler(kafkaErrorHandler)
        }
    }
}
