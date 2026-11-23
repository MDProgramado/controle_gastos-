package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class WhatsappAsyncConfigTest {

  @Test
  void executorTemLimitesDefinidosERecebimentoPorChamador() {
    ThreadPoolTaskExecutor executor = new WhatsappAsyncConfig().whatsappAsyncExecutor();

    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaxPoolSize()).isEqualTo(8);
    assertThat(executor.getQueueCapacity()).isEqualTo(64);
    assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
        .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    assertThat(ReflectionTestUtils.getField(executor, "threadNamePrefix"))
        .isEqualTo("whatsapp-async-");

    executor.shutdown();
  }
}
