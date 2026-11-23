package com.controlefinanceiro.whatsapp;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Executor limitado para o trabalho assíncrono do WhatsApp (webhook + envios). */
@Configuration
public class WhatsappAsyncConfig {

  public static final String EXECUTOR_NAME = "whatsappAsyncExecutor";

  @Bean(EXECUTOR_NAME)
  public ThreadPoolTaskExecutor whatsappAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("whatsapp-async-");
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(64);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(15);
    executor.initialize();
    return executor;
  }
}
