package com.taskscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test to verify the application context loads correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class DistributedTaskSchedulerApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with all the configured beans and dependencies
    }
}