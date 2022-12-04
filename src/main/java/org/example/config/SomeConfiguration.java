package org.example.config;

import org.example.model.Order;
import org.example.service.ResultReplicationAvoidableProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
public class SomeConfiguration {

  @Bean
  public TaskExecutor singleThreadTaskExecutor() {
    var executor = Executors.newSingleThreadExecutor(daemonThreadFactory());
    return new ConcurrentTaskExecutor(executor);
  }

  // Singleton by default
  @Bean(initMethod = ResultReplicationAvoidableProcessor.INIT_METHOD_NAME)
  public ResultReplicationAvoidableProcessor<List<Order>>
  resultReplicationAvoidableRegistry(TaskExecutor taskExecutor) {
    return new ResultReplicationAvoidableProcessor<>(taskExecutor);
  }

  private ThreadFactory daemonThreadFactory() {
    return runnable -> {
      var thread = new Thread(runnable);
      thread.setDaemon(true);
      return thread;
    };
  }
}
