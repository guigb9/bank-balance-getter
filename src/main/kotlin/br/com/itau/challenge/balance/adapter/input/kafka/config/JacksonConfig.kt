package br.com.itau.challenge.balance.adapter.input.kafka.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper() : ObjectMapper {
        return jacksonMapperBuilder().findAndAddModules().build()
    }
}