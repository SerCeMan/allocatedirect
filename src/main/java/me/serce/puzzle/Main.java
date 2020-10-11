package me.serce.puzzle;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.ByteBufPayload;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    logger.info("starting backend");

    var acceptor = SocketAcceptor.with(new RSocket() {
      @Override
      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.from(payloads)
            .map(p -> ByteBufPayload.create("ack: " + p.getDataUtf8()));
      }
    });
    int port = 13131;
    var server = RSocketServer //
        .create(acceptor)//
        .bind(TcpServerTransport.create(port))
        .block();
    var client = RSocketConnector.create() //
        .connect(TcpClientTransport.create(port))
        .block();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("shutting down");
      client.dispose();
      server.dispose();
    }));

    for (int i = 0; i < 2; i++) {
      launchEchoChannel(client);
    }
    Thread.currentThread().join();
  }

  private static void launchEchoChannel(RSocket client) {
    var out = DirectProcessor.<String>create();
    var prev = new AtomicReference<>("init");
    var counter = new AtomicInteger(0);
    client.requestChannel(out.map(ByteBufPayload::create))
        .subscribe( //
            p -> {
              var expected = "ack: " + prev.get();
              if (!Objects.equals(expected, p.getDataUtf8())) {
                throw new RuntimeException("failed, expected: " + expected + ", got " + p.getDataUtf8());
              }
              var next = new byte[ThreadLocalRandom.current().nextInt(256, 1024)];
              ThreadLocalRandom.current().nextBytes(next);
              prev.set(new String(next));
              out.onNext(prev.get());
              if (counter.getAndIncrement() % 50000 == 0) {
                logger.info("processed {} messages", counter.get());
              }
            }, //
            e -> logger.error("error arrived", e), //
            () -> logger.info("completed"));
    out.onNext(prev.get());
  }
}
