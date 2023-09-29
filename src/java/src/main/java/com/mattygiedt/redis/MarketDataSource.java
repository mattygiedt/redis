package com.mattygiedt.redis;

import com.mattygiedt.flatbuffer.marketdata.MarketDataSnapshot;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketDataSource {
  private static final Logger logger = LoggerFactory.getLogger(MarketDataSource.class);

  private static final AtomicBoolean running = new AtomicBoolean(true);

  private final RedisClient redisClient;
  private final Runnable producer;

  public MarketDataSource(final RedisClient redisClient, final Map<String, Integer> tickers) {
    this.redisClient = redisClient;

    this.producer = () -> {
      while (running.get()) {
        tickers.forEach((key, val) -> {
          final MarketDataSnapshot snapshot =
              MarketDataSnapshotFactory.generateRandomMarketDataSnapshot(val);
          final byte[] bytes = snapshot.getByteBuffer().array();

          this.redisClient.set(key, bytes);
        });

        try {
          Thread.sleep(1500);
        } catch (InterruptedException e) {
          logger.error("InteruptEx", e);
        }
      }
    };
  }

  public Runnable getRunnable() {
    return producer;
  }

  public void shutdown() {
    running.set(false);
  }
}
