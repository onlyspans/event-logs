package com.onlyspans.eventlogs;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test - requires running PostgreSQL and Kafka")
class EventLogsApplicationTests {

	@Test
	void contextLoads() {
	}

}
