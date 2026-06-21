package com.tracker.MoneyTracker.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Dedicated thread pool for AI service async operations.
 * <p>
 * Isolates AI context-building (parallel DB calls) from the common
 * ForkJoinPool to avoid starving other request-processing threads.
 */
@Configuration
public class AsyncConfig {

    /**
     * Fixed thread pool for AI-related async work.
     * 4 threads is sufficient since we run at most 3 parallel DB calls per request.
     */
    @Bean(name = "aiExecutor")
    public Executor aiExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ai-async");
            t.setDaemon(true);
            return t;
        });
    }
}
