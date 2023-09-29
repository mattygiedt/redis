package com.mattygiedt.redis;

import com.mattygiedt.flatbuffer.marketdata.MarketDataSnapshot;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarketDataClient {
  private static final Logger logger = LoggerFactory.getLogger(MarketDataClient.class);
  private static final AtomicBoolean running = new AtomicBoolean(true);

  private final RedisClient redisClient;
  private final Runnable consumer;

  public MarketDataClient(final RedisClient redisClient, final Map<String, Integer> tickers,
      final Consumer<MarketDataSnapshot> handler) {
    this.redisClient = redisClient;

    this.consumer = () -> {
      final MarketDataSnapshot snapshot = new MarketDataSnapshot();

      while (running.get()) {
        for (final String key : tickers.keySet()) {
          if (this.redisClient.exists(key)) {
            final byte[] bytes = this.redisClient.get(key);
            MarketDataSnapshotFactory.assign(bytes, snapshot);

            //
            //  Do something with the snapshot...
            //

            handler.accept(snapshot);
          } else {
            logger.warn("no mkt_data key: {}", key);
          }
        }

        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          logger.error("MarketDataClient error", e);
        }
      }
    };
  }

  public Runnable getRunnable() {
    return consumer;
  }

  public void shutdown() {
    running.set(false);
  }
}
