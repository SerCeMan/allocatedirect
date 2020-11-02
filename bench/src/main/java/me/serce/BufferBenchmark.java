package me.serce;

import org.openjdk.jmh.annotations.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

@Fork(1)
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 2500, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 2, time = 5000, timeUnit = TimeUnit.MILLISECONDS)
public class BufferBenchmark {

  public static final int SIZE = 8 * 1024 * 1024;

  @Param({"direct", "heap"})
  String bufferType;
  ByteBuffer buffer;

  @Setup
  public void setUp() {
    switch (bufferType) {
      case "direct":
        buffer = ByteBuffer.allocateDirect(SIZE);
        break;
      case "heap":
        buffer = ByteBuffer.allocate(SIZE);
        break;
      default:
        throw new AssertionError();
    }
  }

  static {
    try (FileOutputStream is = new FileOutputStream("/tmp/file1")) {
      byte[] bytes = new byte[SIZE];
      ThreadLocalRandom.current().nextBytes(bytes);
      is.write(bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }


//  @Benchmark
//  public ByteBuffer heap() {
//    return ByteBuffer.allocate(1024);
//  }
//
//  @Benchmark
//  public ByteBuffer direct() {
//    return ByteBuffer.allocateDirect(1024);
//  }

//  @Benchmark
//  public void combined(Blackhole bh) {
//    bh.consume(ByteBuffer.allocate(1024));
//    bh.consume(ByteBuffer.allocateDirect(1024));
//  }

  @Benchmark
  public void copyFiles() throws Exception {
    buffer.rewind();
    try (FileChannel channel1 = FileChannel.open(Paths.get("/tmp/file1"), READ);
         FileChannel channel2 = FileChannel.open(Paths.get("/tmp/file2"), WRITE)) {
      while (buffer.hasRemaining()) {
        channel1.read(buffer);
      }
      for (int i = 0; i < SIZE; i += 8) {
        long variable = buffer.getLong(i);
        buffer.putLong(i, variable & ThreadLocalRandom.current().nextLong());
      }
      buffer.flip();
      while (buffer.hasRemaining()) {
        channel2.write(buffer);
      }
    }
  }
//
//  @Benchmark
//  public ByteBuffer directNio() {
//    return ByteBuffer.allocateDirect(1024);
//  }


//
//  @Benchmark
//  @Threads(8)
//  @Fork(jvmArgsAppend = {
//      "-XX:+UnlockExperimentalVMOptions",
//      "-XX:+DisableExplicitGC",
//      "-XX:+UseG1GC",
//      "-Xms128M",
//      "-Xmx128M",
//      "-Xlog:safepoint,gc*:file=/tmp/gc_g1/gc.log:time,level,tags:filecount=10,filesize=4000k",
//  })
//  public void g1(Blackhole blackhole) {
//    doWork(blackhole);
//  }
//
//  @Benchmark
//  @Threads(8)
//  @Fork(jvmArgsAppend = {
//      "-XX:+UnlockExperimentalVMOptions",
//      "-XX:+DisableExplicitGC",
//      "-XX:+UseZGC",
//      "-Xms128M",
//      "-Xmx128M",
//      "-Xlog:safepoint,gc*:file=/tmp/gc_zgc/gc.log:time,level,tags:filecount=10,filesize=4000k",
//  })
//  public void zgc(Blackhole blackhole) {
//    doWork(blackhole);
//  }
//
//  @Benchmark
//  @Threads(8)
//  @Fork(jvmArgsAppend = {
//      "-XX:+UnlockExperimentalVMOptions",
//      "-XX:+DisableExplicitGC",
//      "-XX:+UseShenandoahGC",
//      "-Xms128M",
//      "-Xmx128M",
//      "-Xlog:safepoint,gc*:file=/tmp/gc_shen/gc.log:time,level,tags:filecount=10,filesize=4000k",
//  })
//  public void shenandoah(Blackhole blackhole) {
//    doWork(blackhole);
//  }
//
//  private static void doWork(Blackhole blackhole) {
//    var len = 256;
//    var bf = ByteBuffer.allocateDirect(len);
//    var coef = 5;
//    var bf2 = ByteBuffer.allocate(len / coef);
//    for (int j = 0; j < len / 4; j++) {
//      bf.putInt(ThreadLocalRandom.current().nextInt());
//    }
//    for (int j = 0; j < len / (4 * coef); j++) {
//      bf2.putInt(ThreadLocalRandom.current().nextInt());
//    }
//    blackhole.consume(bf);
//    blackhole.consume(bf2);
//    Blackhole.consumeCPU(1_000_00);
//  }
}
