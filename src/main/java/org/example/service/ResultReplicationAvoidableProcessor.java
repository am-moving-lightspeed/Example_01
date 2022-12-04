package org.example.service;

import org.example.model.ProcessingJournalEntry;
import org.springframework.core.task.TaskExecutor;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResultReplicationAvoidableProcessor<T> {

  public static final String INIT_METHOD_NAME = "initialize";

  private final TaskExecutor executor;

  private final List<ProcessingJournalEntry<T>> processedEntitiesJournal
      = new LinkedList<>();
  private final BlockingQueue<ProcessingJournalEntry<T>> preprocessedEntities
      = new LinkedBlockingQueue<>();

  public ResultReplicationAvoidableProcessor(TaskExecutor executor) {
    this.executor = executor;
  }

  public void initialize() {
    executor.execute(this::processNextInQueue);
  }

  public void process(Supplier<T> preprocessor, Consumer<T> processor,
      BiFunction<T, List<T>, T> resultsFilter) {
    var preprocessingResult = preprocessAndPopulateResult(preprocessor);
    preprocessingResult.setProcessorCallback(processor);
    preprocessingResult.setFilterCallback(resultsFilter);
    preprocessedEntities.add(preprocessingResult);
  }

  private ProcessingJournalEntry<T> preprocessAndPopulateResult(Supplier<T> preprocessor) {
    var journalEntry = new ProcessingJournalEntry<T>(LocalDateTime.now());
    journalEntry.setResult(preprocessor.get());
    return journalEntry;
  }

  private void processNextInQueue() {
    // Daemon thread infinitely takes elements from queue and processes them.
    // If the queue is empty, it waits until the new element appears.
    // No need to interrupt the cycle. The thread will be killed on JVM shutdown.
    // (Хз, как красиво убрать warning.)
    while (true) {
      var journalEntry = getNextEntry();
      if (journalEntry == null) {
        continue;
      }
      filterResultAndProcess(journalEntry);
      processedEntitiesJournal.add(journalEntry);
      cleanUpIrrelevantProcessedEntities();
    }
  }

  private ProcessingJournalEntry<T> getNextEntry() {
    try {
      return preprocessedEntities.take();
    } catch (InterruptedException e) {
      return null;
    }
  }

  private void filterResultAndProcess(ProcessingJournalEntry<T> journalEntry) {
    var processedEntities = processedEntitiesJournal.stream()
        // Выбрать те, которые завершились уже после начала данного задания.
        // Результаты тех, которые завершились раньше начала данного задания,
        // никак не пересекутся с результатами данного задания, поэтому не рассм. их.
        .filter(processedEntity ->
            isTaskStartedBeforeComparable(journalEntry, processedEntity))
        .map(ProcessingJournalEntry::getResult)
        .toList();
    // Удалить уже обработанные результаты, чтобы исключить дублирование.
    var filteredResult = journalEntry.getFilterCallback().apply(
        journalEntry.getResult(), processedEntities);

    // Обработать то, что осталось (например, сохранить в БД).
    journalEntry.getProcessorCallback().accept(filteredResult);
    journalEntry.setEndTime(LocalDateTime.now());
    journalEntry.setResult(filteredResult);
    cleanUpIrrelevantProcessedEntities();
  }

  private void cleanUpIrrelevantProcessedEntities() {
    var unusedEntities = processedEntitiesJournal.stream()
        .filter(processedEntity -> !hasDependantsInQueue(processedEntity)).toList();
    processedEntitiesJournal.removeAll(unusedEntities);
  }

  private boolean hasDependantsInQueue(ProcessingJournalEntry<T> processedEntity) {
    return preprocessedEntities.stream()
        .anyMatch(preprocessedEntity ->
            isTaskStartedBeforeComparable(preprocessedEntity, processedEntity));
  }

  private boolean isTaskStartedBeforeComparable(ProcessingJournalEntry<T> task,
      ProcessingJournalEntry<T> comparableTask) {
    return task.getStartTime().isBefore(comparableTask.getEndTime());
  }
}
