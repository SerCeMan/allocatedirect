package me.serce;

import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

@Fork(1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 3, timeUnit = TimeUnit.MILLISECONDS)
public class AllocateBuffer2 {
  @Param({"128"}) int size;

  @Fork(jvmArgsAppend = {
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:PrintAssemblyOptions=intel",
    "-XX:CompileCommand=print,*AllocateBuffer2.heap*",
  })
  @Benchmark
  public ByteBuffer heap() {
    return ByteBuffer.allocate(size);
  }

  @Fork(jvmArgsAppend = {
      "-XX:+UnlockDiagnosticVMOptions",
      "-XX:PrintAssemblyOptions=intel",
      "-XX:CompileCommand=print,*AllocateBuffer2.direct*",
  })
  @Benchmark
  public ByteBuffer direct() {
    return ByteBuffer.allocateDirect(size);
  }
}
