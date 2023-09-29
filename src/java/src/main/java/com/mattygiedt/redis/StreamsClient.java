package com.mattygiedt.redis;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamInfo;

//
//  https://redis.io/docs/data-types/streams/
//

@SuppressWarnings("unused")
public class StreamsClient {
  private static final Logger logger = LoggerFactory.getLogger(StreamsClient.class);

  private final RedisClient redisClient;
  public StreamsClient(final RedisClient redisClient) {
    this.redisClient = redisClient;
  }

  public StreamEntryID append(final String key, final Map<String, String> entry) {
    return redisClient.xadd(key, entry);
  }

  public long count(final String key) {
    return redisClient.xlen(key);
  }

  public List<StreamEntry> entryList(
      final String key, final StreamEntryID start, final StreamEntryID end) {
    return entryList(key, start, end, RedisClient.XRANGE_COUNT);
  }

  public List<StreamEntry> entryList(
      final String key, final StreamEntryID start, final StreamEntryID end, final int maxCount) {
    return redisClient.xrange(key, start, end, maxCount);
  }

  public List<Map.Entry<String, List<StreamEntry>>> tail(final Map<String, StreamEntryID> streams) {
    return redisClient.xread(XReadParams.xReadParams().block(0), streams);
  }

  public StreamInfo info(final String key) {
    return redisClient.xinfoStream(key);
  }
}
