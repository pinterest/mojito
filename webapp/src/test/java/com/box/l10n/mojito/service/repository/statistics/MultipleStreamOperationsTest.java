package com.box.l10n.mojito.service.repository.statistics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Test to compare performance of multiple stream operations vs single stream operations */
public class MultipleStreamOperationsTest {
  private static final int ITERATIONS = 100000;
  private List<Data> dataList = new ArrayList<>();

  public MultipleStreamOperationsTest() {
    for (int i = 0; i < 100000; i++) {
      Data data = new Data("Number: " + i);
      data.setUsed(i % 10 == 0 ? true : false);
      data.setTestString(i % 2 == 0 ? "Test String" : null);
      dataList.add(data);
    }
  }

  private class Data {
    private String data;
    private boolean isUsed;

    private String testString;

    public Data(String data) {
      this.data = data;
    }

    public String getData() {
      return data;
    }

    public boolean isUsed(boolean b) {
      return isUsed;
    }

    public void setUsed(boolean used) {
      isUsed = used;
    }

    public String getTestString() {
      return testString;
    }

    public void setTestString(String testString) {
      this.testString = testString;
    }
  }

  private class StreamOperations {
    private long count = 0L;
    private long sum = 0L;

    private long isUsedCount = 0L;

    private long isNotUsedCount = 0L;

    private long testStringUsedCount = 0L;

    private long testStringNotUsedCount = 0L;

    public long getCount() {
      return count;
    }

    public long getSum() {
      return sum;
    }

    public void setCount(long count) {
      this.count = count;
    }

    public void setSum(long sum) {
      this.sum = sum;
    }

    public long getIsUsedCount() {
      return isUsedCount;
    }

    public void setIsUsedCount(long isUsedCount) {
      this.isUsedCount = isUsedCount;
    }

    public long getTestStringUsedCount() {
      return testStringUsedCount;
    }

    public void setTestStringUsedCount(long testStringUsedCount) {
      this.testStringUsedCount = testStringUsedCount;
    }

    public long getTestStringNotUsedCount() {
      return testStringNotUsedCount;
    }

    public void setTestStringNotUsedCount(long testStringNotUsedCount) {
      this.testStringNotUsedCount = testStringNotUsedCount;
    }

    public long getIsNotUsedCount() {
      return isNotUsedCount;
    }

    public void setIsNotUsedCount(long isNotUsedCount) {
      this.isNotUsedCount = isNotUsedCount;
    }
  }

  private long measure(Function<StreamOperations, Void> testFunction) {
    StreamOperations streamOperations = new StreamOperations();
    long startTime = System.currentTimeMillis();
    testFunction.apply(streamOperations);
    long endTime = System.currentTimeMillis();
    return endTime - startTime;
  }

  @Test
  public void comparePerformanceOfStreamOperations() {
    double totalTimeMultiStream = 0;
    double totalTimeSingleStream = 0;
    double singleStreamFasterCount = 0;
    double multiStreamFasterCount = 0;

    for (int i = 0; i < ITERATIONS; i++) {
      long multiStreamOpTime = measure(this::multiStreamOp);
      long singleStreamOpTime = measure(this::singleStreamOp);

      if (singleStreamOpTime < multiStreamOpTime) {
        singleStreamFasterCount++;
      } else if (multiStreamOpTime < singleStreamOpTime) {
        multiStreamFasterCount++;
      }
      totalTimeMultiStream += multiStreamOpTime;
      totalTimeSingleStream += singleStreamOpTime;
    }

    double averageTimeMultiStream = totalTimeMultiStream / ITERATIONS;
    double averageTimeSingleStream = totalTimeSingleStream / ITERATIONS;

    double equalPerfPercentage =
        ((ITERATIONS - singleStreamFasterCount - multiStreamFasterCount) / ITERATIONS) * 100;
    double singleStreamFasterPercentage = (singleStreamFasterCount / ITERATIONS) * 100;
    double multiStreamFasterPercentage = (multiStreamFasterCount / ITERATIONS) * 100;

    String fasterApproach =
        averageTimeMultiStream < averageTimeSingleStream ? "Multi Stream" : "Single Stream";
    double timeDifference = Math.abs(averageTimeMultiStream - averageTimeSingleStream);
    double percentageDifference =
        (timeDifference / Math.max(averageTimeMultiStream, averageTimeSingleStream)) * 100;

    DecimalFormat df = new DecimalFormat("#.##");
    System.out.println("After running " + ITERATIONS + " times:");
    System.out.println("Average Multi Stream Operations Time: " + averageTimeMultiStream + "ms");
    System.out.println("Average Single Stream Operations Time: " + averageTimeSingleStream + "ms");
    System.out.println(
        "Single Stream was faster in "
            + df.format(singleStreamFasterPercentage)
            + " % of executions");
    System.out.println(
        "Multi Stream was faster in "
            + df.format(multiStreamFasterPercentage)
            + " % of executions");
    System.out.println(
        "Both approaches had equal performance in "
            + df.format(equalPerfPercentage)
            + " % of executions");
    System.out.println(
        fasterApproach
            + " operations is faster by approximately "
            + df.format(percentageDifference)
            + " %");
  }

  private Void multiStreamOp(StreamOperations streamOperations) {
    streamOperations.setCount(
        dataList.stream().filter(d -> d.getData().startsWith("Number: 1")).count());

    streamOperations.setSum(
        dataList.stream()
            .mapToLong(d -> Long.parseLong(d.getData().replace("Number: ", "")))
            .sum());

    streamOperations.setIsUsedCount(dataList.stream().filter(d -> d.isUsed(true)).count());

    streamOperations.setIsNotUsedCount(dataList.stream().filter(d -> !d.isUsed(true)).count());

    streamOperations.setTestStringUsedCount(
        dataList.stream().filter(d -> d.getTestString() != null).count());

    streamOperations.setTestStringNotUsedCount(
        dataList.stream().filter(d -> d.getTestString() == null).count());

    return null;
  }

  private Void singleStreamOp(StreamOperations streamOperations) {
    dataList.stream()
        .forEach(
            d -> {
              if (d.getData().startsWith("Number: 1")) {
                streamOperations.setCount(streamOperations.getCount() + 1);
              }

              streamOperations.setSum(
                  streamOperations.getSum() + Long.parseLong(d.getData().replace("Number: ", "")));

              streamOperations.setIsUsedCount(
                  streamOperations.getIsUsedCount() + (d.isUsed(true) ? 1 : 0));

              streamOperations.setIsNotUsedCount(
                  streamOperations.getIsNotUsedCount() + (!d.isUsed(true) ? 1 : 0));

              streamOperations.setTestStringUsedCount(
                  streamOperations.getTestStringUsedCount() + (d.getTestString() != null ? 1 : 0));

              streamOperations.setTestStringNotUsedCount(
                  streamOperations.getTestStringNotUsedCount()
                      + (d.getTestString() == null ? 1 : 0));
            });

    return null;
  }

  @Test
  public void testEquality() {
    StreamOperations multiStreamOperations = new StreamOperations();
    StreamOperations singleStreamOperations = new StreamOperations();

    multiStreamOp(multiStreamOperations);
    singleStreamOp(singleStreamOperations);

    assertTrue(multiStreamOperations.getCount() == singleStreamOperations.getCount());
    assertTrue(multiStreamOperations.getSum() == singleStreamOperations.getSum());
    assertTrue(multiStreamOperations.getIsUsedCount() == singleStreamOperations.getIsUsedCount());
    assertTrue(
        multiStreamOperations.getIsNotUsedCount() == singleStreamOperations.getIsNotUsedCount());
    assertTrue(
        multiStreamOperations.getTestStringUsedCount()
            == singleStreamOperations.getTestStringUsedCount());
    assertTrue(
        multiStreamOperations.getTestStringNotUsedCount()
            == singleStreamOperations.getTestStringNotUsedCount());
  }
}
