package br.com.itau.challenge

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@SpringBootTest
class ApplicationTests {

	@MockitoBean
	lateinit var dynamoDbClient: DynamoDbClient

	@Test
	fun contextLoads() {
	}

}
