package com.mattygiedt.redis;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mattygiedt.flatbuffer.marketdata.EpochNanos;
import com.mattygiedt.flatbuffer.marketdata.MarketDataSnapshot;
import com.mattygiedt.flatbuffer.marketdata.Price;
import com.mattygiedt.flatbuffer.marketdata.Quantity;
import com.mattygiedt.flatbuffer.marketdata.Source;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;

public class ServiceEntry {
  private static final Logger logger = LoggerFactory.getLogger(ServiceEntry.class);

  private static final AtomicBoolean running = new AtomicBoolean(true);
  private static final AtomicInteger cacheReads = new AtomicInteger(0);
  private static final AtomicInteger cacheWrites = new AtomicInteger(0);

  private static final Map<String, Integer> tickers =
      Map.of("AAPL", 497953706, "META", 763907149, "GOOGL", 590742684);

  public static class MarketDataSource implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataSource.class);
    private final RedisClient redisClient;

    public MarketDataSource(final HostAndPort redisServer) {
      this.redisClient = new RedisClient(redisServer);
    }

    @Override
    public void run() {
      while (running.get()) {
        tickers.forEach((key, val) -> {
          redisClient.set(key, makeMarketDataSnapshot(val));
          cacheWrites.incrementAndGet();
        });

        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.error("InteruptEx", e);
        }
      }
    }

    private long getEpochNanos(final Instant instant) {
      return TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
    }

    private byte[] makeMarketDataSnapshot(final int instrumentId) {
      final FlatBufferBuilder builder = new FlatBufferBuilder(256);
      final ThreadLocalRandom random = ThreadLocalRandom.current();

      MarketDataSnapshot.startMarketDataSnapshot(builder);
      MarketDataSnapshot.addInstrumentId(builder, instrumentId);
      MarketDataSnapshot.addSource(builder, Source.BPIPE);
      MarketDataSnapshot.addCreateTm(
          builder, EpochNanos.createEpochNanos(builder, getEpochNanos(Instant.now())));

      MarketDataSnapshot.addBidPrice(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addBidYield(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addBidSpread(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addBidGspread(builder, Price.createPrice(builder, random.nextInt()));

      MarketDataSnapshot.addAskPrice(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addAskYield(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addAskSpread(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addAskGspread(builder, Price.createPrice(builder, random.nextInt()));

      MarketDataSnapshot.addBidSize(builder, Quantity.createQuantity(builder, random.nextInt()));
      MarketDataSnapshot.addAskSize(builder, Quantity.createQuantity(builder, random.nextInt()));

      MarketDataSnapshot.addLastTradeSize(
          builder, Quantity.createQuantity(builder, random.nextInt()));
      MarketDataSnapshot.addLastTradePrice(builder, Price.createPrice(builder, random.nextInt()));
      MarketDataSnapshot.addLastTradeTm(
          builder, EpochNanos.createEpochNanos(builder, getEpochNanos(Instant.now())));

      MarketDataSnapshot.finishMarketDataSnapshotBuffer(
          builder, MarketDataSnapshot.endMarketDataSnapshot(builder));

      return builder.dataBuffer().array();
    }
  }

  public static class MarketDataClient implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MarketDataClient.class);
    private final RedisClient redisClient;

    public MarketDataClient(final HostAndPort redisServer) {
      this.redisClient = new RedisClient(redisServer);
    }

    @Override
    public void run() {
      final MarketDataSnapshot snapshot = new MarketDataSnapshot();

      while (running.get()) {
        for (final String key : tickers.keySet()) {
          MarketDataSnapshot.getRootAsMarketDataSnapshot(
              ByteBuffer.wrap(redisClient.get(key)), snapshot);
          cacheReads.incrementAndGet();
        }

        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.error("InteruptEx", e);
        }
      }
    }
  }

  public static void main(final String[] args) throws Exception {
    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(ch.qos.logback.classic.Level.toLevel("info"));

    final HostAndPort redisServer = new HostAndPort("localhost", 6379);

    logger.info("redis-server: {}", redisServer.toString());

    final Thread sourceThread = new Thread(new MarketDataSource(redisServer));
    final Thread clientThread_1 = new Thread(new MarketDataClient(redisServer));
    final Thread clientThread_2 = new Thread(new MarketDataClient(redisServer));
    final Thread clientThread_3 = new Thread(new MarketDataClient(redisServer));

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        running.set(false);
        logger.warn("Shutdown ...");
        logger.info("Cache reads: {}", cacheReads.get());
        logger.info("Cache writes: {}", cacheWrites.get());
      } catch (Exception ex) {
        logger.error("Shutdown error:", ex);
      }
    }));

    sourceThread.start();
    clientThread_1.start();
    clientThread_2.start();
    clientThread_3.start();

    sourceThread.join();
    clientThread_1.join();
    clientThread_2.join();
    clientThread_3.join();
  }
}
