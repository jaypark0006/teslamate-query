package com.teslamate.query.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
        // Controllers still use blocking JDBI; offload them off the Netty event loop.
        configurer.setExecutor(new VirtualThreadTaskExecutor("http-block-"));
    }
}
