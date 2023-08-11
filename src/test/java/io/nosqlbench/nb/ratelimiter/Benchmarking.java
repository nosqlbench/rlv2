/*
 * Copyright (c) 2022-2023 nosqlbench
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.nosqlbench.nb.ratelimiter;

import io.nosqlbench.nb.ratelimiter.RateSpec.Verb;
import io.nosqlbench.nb.testutils.Perf;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

public class Benchmarking {
    private static final Function<RateSpec, RateLimiter> rlFunction = rs -> new HybridRateLimiter(
            Map.of("alias", "tokenrl"),
            "hybrid",
            rs.withVerb(Verb.configure));
    private static final Function<RateSpec, RateLimiter> rlNoopFunction = rs -> new NoopRateLimiter(
            Map.of("alias", "tokenrl"),
            "hybrid",
            rs.withVerb(Verb.configure));
    private final RateLimiterPerfTestMethods methods = new RateLimiterPerfTestMethods();

    void benchmark(Function<RateSpec, RateLimiter> rl, String outfile, int param) {
        final int iterations = 5;
        final int warmups = 1;
        try (FileWriter out = new FileWriter(outfile, true)) {
            for (int thread_count = 1; thread_count < 30000; thread_count *= 2) {
                for (int i = 0; i < warmups + iterations; ++i) {
                    final Perf perf = this.methods.testRateLimiterMultiThreadedContention(rl,
                            new RateSpec(1.0E12, 1.1),
                            100_000_000, thread_count);
                    if (i >= warmups) {
                        System.out.println(perf.getLastResult());
                        out.write(param + "," + thread_count + "," + perf.getLastResult().getOpsPerSec() + "\n");
                        out.flush();
                    }
                    System.gc();

                }
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Benchmarking b = new Benchmarking();
        b.benchmark(rlNoopFunction, "counts_nop", 0);
        b.benchmark(rlFunction, "counts_default", 0);
        for (int step_ = 1; true; step_ *= 2) {
            final int step = step_;
            b.benchmark(rs -> new HybridAdjustedRateLimiter(
                    Map.of("alias", "tokenrl"),
                    "hybrid",
                    rs.withVerb(Verb.configure),
                    step), "counts_amoritzed", step);
        }
    }
}
