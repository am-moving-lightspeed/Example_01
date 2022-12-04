package org.example.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@Data
public class ProcessingJournalEntry<T> {

  private LocalDateTime startTime;

  private LocalDateTime endTime;

  private T result;

  private Consumer<T> processorCallback;

  // Filter out entries from current result (type T) that already exist (processed)
  // in previous results (type List<T>) and return updated current result (type T).
  private BiFunction<T, List<T>, T> filterCallback;

  public ProcessingJournalEntry(LocalDateTime processingStartTime) {
    this.startTime = processingStartTime;
  }
}
