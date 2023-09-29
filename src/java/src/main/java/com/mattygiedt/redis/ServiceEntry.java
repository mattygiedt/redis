package com.mattygiedt.redis;

import com.mattygiedt.flatbuffer.marketdata.MarketDataSnapshot;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.resps.StreamEntry;

public class ServiceEntry {
  private static final Logger logger = LoggerFactory.getLogger(ServiceEntry.class);

  private static final AtomicBoolean running = new AtomicBoolean(true);

  public static void main(final String[] args) throws Exception {
    //
    //  I'm too lazy to add a logback.xml...
    //

    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(ch.qos.logback.classic.Level.toLevel("info"));

    //
    //  Compile time flags that should be runtime parameters...
    //

    boolean doMarketData = false;
    boolean doPubSub = false;
    boolean doStreams = true;

    //
    //  Where is our redis-server running?
    //

    final HostAndPort redisServerAddr = new HostAndPort("localhost", 6379);
    logger.info("redis-server: {}", redisServerAddr.toString());

    //
    //  Create the redis client on the redis server addr
    //

    final RedisClient redisClient = new RedisClient(redisServerAddr);

    //
    //  Clear out any previous data
    //

    redisClient.flushAll();

    //
    //  Create an executor to run all our examples
    //

    final ExecutorService executor = Executors.newCachedThreadPool();

    if (doMarketData) {
      //
      //  Generate some mock instruments
      //

      final Map<String, Integer> equityTickers =
          Map.of("AAPL", 497953706, "META", 763907149, "GOOGL", 590742684);

      final Map<String, Integer> bondEtfTickers =
          Map.of("TLT", 87623463, "LQD", 12398734, "HYG", 547834987);

      //
      //  Example market data callback(s)
      //

      final Consumer<MarketDataSnapshot> equityMarketDataCallback = (msg) -> {
        logger.info("EQTY instr_id: {}, bid_prc: {}", msg.instrumentId(), msg.bidPrice().value());
      };

      final Consumer<MarketDataSnapshot> bondEtfMarketDataCallback = (msg) -> {
        logger.info(
            "BOND_ETF instr_id: {}, bid_prc: {}", msg.instrumentId(), msg.bidPrice().value());
      };

      //
      //  Start two market data sources, one for each ticker map
      //

      executor.execute(new MarketDataSource(redisClient, equityTickers).getRunnable());
      executor.execute(new MarketDataSource(redisClient, bondEtfTickers).getRunnable());

      //
      //  Start two equity ticker market data consumers
      //

      executor.execute(
          new MarketDataClient(redisClient, equityTickers, equityMarketDataCallback).getRunnable());
      executor.execute(
          new MarketDataClient(redisClient, equityTickers, equityMarketDataCallback).getRunnable());

      //
      //  Start two bond etf market data consumers
      //

      executor.execute(new MarketDataClient(redisClient, bondEtfTickers, bondEtfMarketDataCallback)
                           .getRunnable());
      executor.execute(new MarketDataClient(redisClient, bondEtfTickers, bondEtfMarketDataCallback)
                           .getRunnable());
    }

    if (doPubSub) {
      //
      //  Create some PubSub clients
      //

      final String[] channels = new String[] {"EQTY", "ETF"};
      final PubSubClient publisher = new PubSubClient(redisClient);
      final PubSubClient receiver = new PubSubClient(redisClient);

      //
      //  After the publisher receveives it's first message, unsubscribe from that channel
      //

      publisher.subscribe((ch, data) -> {
        logger.info("PUBLISHER recv channel: {}, message: {}", ch, new String(data));
        publisher.unsubscribe(ch);
      }, channels);

      //
      //  The recveiver receives...
      //

      receiver.subscribe((ch, data) -> {
        logger.info("RECEIVER recv channel: {}, message: {}", ch, new String(data));
      }, channels);

      //
      //  Publish some messages
      //

      executor.execute(() -> {
        final StringBuilder sb = new StringBuilder();

        while (running.get()) {
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            logger.error("PubSub error", e);
          }

          sb.setLength(0);
          final String data = sb.append("UPDATE:")
                                  .append(MarketDataSnapshotFactory.getEpochNanos(Instant.now()))
                                  .toString();

          if (ThreadLocalRandom.current().nextBoolean()) {
            publisher.publish(channels[0], data.getBytes());
          } else {
            publisher.publish(channels[1], data.getBytes());
          }
        }
      });
    }

    if (doStreams) {
      final StreamsClient streamsClient = new StreamsClient(redisClient);
      final Map<String, String> eventFields = new HashMap<>();

      //
      //  Push some stream events into redis
      //

      logger.info("generating trade events ...");

      for (int i = 0; i < 64; i++) {
        eventFields.clear();
        eventFields.put("SYMBOL", "LQD");
        eventFields.put("PRICE", Double.toString(ThreadLocalRandom.current().nextInt()));
        eventFields.put("QUANTITY", Integer.toString(ThreadLocalRandom.current().nextInt()));
        streamsClient.append("trades", eventFields);

        logger.info("generated trade event: {}", i);
        Thread.sleep(100);
      }

      logger.info("generating trade events ... done");

      logger.info("entry count: {}", streamsClient.count("trades"));

      final List<StreamEntry> entries = streamsClient.entryList("trades", null, null);
      logger.info("entries size: {}", entries.size());
      logger.info(" first entry: {}", entries.get(0));
      logger.info("  last entry: {}", entries.get(entries.size() - 1));
    }

    //
    //  Add a shutdown handler...
    //

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        logger.warn("Shutdown ...");
        executor.shutdown();
      } catch (Exception ex) {
        logger.error("Shutdown error:", ex);
      }
    }));
  }
}
