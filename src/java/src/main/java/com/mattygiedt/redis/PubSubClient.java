package com.mattygiedt.redis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedisPubSub;

public class PubSubClient {
  private static final Logger logger = LoggerFactory.getLogger(PubSubClient.class);
  private static final ExecutorService executor = Executors.newCachedThreadPool();

  private final RedisClient redisClient;
  private final Map<String, BinaryJedisPubSub> channelMap;

  public PubSubClient(final RedisClient redisClient) {
    this.redisClient = redisClient;
    this.channelMap = new HashMap<>();
  }

  public void publish(final String channel, final byte[] data) {
    redisClient.publish(channel, data);
  }

  public void subscribe(final BiConsumer<String, byte[]> consumer, final String[] channels) {
    final BinaryJedisPubSub jedisPubSub = new BinaryJedisPubSub() {
      @Override
      public void onMessage(final byte[] channel, final byte[] message) {
        final String channelStr = new String(channel);
        logger.info("onMessage channel: {}, msg: {}", channelStr, Arrays.toString(message));
        consumer.accept(channelStr, message);
      }

      @Override
      public void onSubscribe(final byte[] channel, int subscribedChannels) {
        logger.info(
            "onSubscribe channel {}, numSubscribed: {}", new String(channel), subscribedChannels);
      }

      @Override
      public void onUnsubscribe(final byte[] channel, int subscribedChannels) {
        logger.info(
            "onUnsubscribe channel {}, numSubscribed: {}", new String(channel), subscribedChannels);
      }
    };

    for (final String channel : channels) {
      channelMap.put(channel, jedisPubSub);
    }

    executor.execute(() -> redisClient.subscribe(jedisPubSub, channels));
  }

  public void unsubscribe(final String channel) {
    channelMap.get(channel).unsubscribe(channel.getBytes());
  }

  public void shutdown() {
    channelMap.forEach((k, v) -> v.unsubscribe(k.getBytes()));
  }
}
