package io.nosqlbench.nb.ratelimiter;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JMH {
    static ThreadDrivenTokenPool tokenPool = new ThreadDrivenTokenPool(new RateSpec(1E9, 1.1),
            Map.of("alias", "tokenrl"), 1);
    static long array[] = new long[1000000];

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void only_block_and_take() {
        tokenPool.blockAndTake(0);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void only_refill() {
        tokenPool.refill(0);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public long nanotime() {
        return System.nanoTime();
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Threads(1)
    public void storetoisolatedarray() {
        array[1234] = 5678;
    }

    @Param({ "1", "4", "16", "64", "256" })
    int intseparation;

    /// hopefully we can observe some point where the loads + stores become much
    /// more efficient
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Group("sharing")
    public void storecontended1() {
        array[0] = array[0] + 1;
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Group("sharing")
    public void storecontended2() {
        array[intseparation] = array[intseparation] + 5678;
    }

    public static void main(String[] args) throws RunnerException {
        final int[] thread_counts = { 256 };// {1, 4, 16, 64, 256};
        ChainedOptionsBuilder opt = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .forks(1)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(1));
        for (int thread_count : thread_counts) {
            new Runner(opt.threads(thread_count).build()).run();
            System.out.println("^ RESULTS FOR THREAD COUNT " + thread_count + " ^");
        }
    }
}
