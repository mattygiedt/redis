package com.mattygiedt.redis;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamFullInfo;
import redis.clients.jedis.resps.StreamInfo;

public final class RedisClient {
  private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);
  public static final XAddParams DEFAULT_XADD_PARAMS = XAddParams.xAddParams();
  public static final int XRANGE_COUNT = 1024 * 8;

  private static class JedisPoolProvider {
    private final ConcurrentHashMap<HostAndPort, JedisPool> poolMap = new ConcurrentHashMap<>();
    private final JedisPoolConfig poolConfig = getJedisPoolConfig();

    public JedisPool getOrCreatePool(final HostAndPort hostAndPort) {
      return poolMap.computeIfAbsent(hostAndPort,
          k -> new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort()));
    }

    private JedisPoolConfig getJedisPoolConfig() {
      final JedisPoolConfig poolConfig = new JedisPoolConfig();
      poolConfig.setMaxTotal(128);
      poolConfig.setMaxIdle(128);
      poolConfig.setMinIdle(16);
      poolConfig.setTestOnBorrow(true);
      poolConfig.setTestOnReturn(true);
      poolConfig.setTestWhileIdle(true);
      poolConfig.setNumTestsPerEvictionRun(3);
      poolConfig.setBlockWhenExhausted(true);
      return poolConfig;
    }
  }

  private static class SingletonHolder {
    public static final JedisPoolProvider PROVIDER = new JedisPoolProvider();
  }

  public static JedisPool getJedisPool(final HostAndPort hostAndPort) {
    return SingletonHolder.PROVIDER.getOrCreatePool(hostAndPort);
  }

  private final JedisPool jedisPool;

  public RedisClient(final String host, final int port) {
    this(new HostAndPort(host, port));
  }

  public RedisClient(final HostAndPort hostAndPort) {
    this.jedisPool = RedisClient.getJedisPool(hostAndPort);
  }

  ////////////////////////////////////////////////////////////////////
  //  set
  //

  public String set(final String key, final byte[] value) {
    return set(key.getBytes(), value);
  }

  public String set(final byte[] key, final byte[] value) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.set(key, value);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::set", ex);
    }

    return null;
  }

  ////////////////////////////////////////////////////////////////////
  //  get
  //

  public byte[] get(final String key) {
    return get(key.getBytes());
  }

  public byte[] get(final byte[] key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.get(key);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::get", ex);
    }

    return null;
  }

  ////////////////////////////////////////////////////////////////////
  //  hset
  //

  public long hset(final String key, final String field, final byte[] value) {
    return hset(key.getBytes(), field.getBytes(), value);
  }

  public long hset(final byte[] key, final byte[] field, final byte[] value) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.hset(key, field, value);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::hset", ex);
    }

    return -1;
  }

  ////////////////////////////////////////////////////////////////////
  //  hget
  //

  public byte[] hget(final String key, final String field) {
    return hget(key.getBytes(), field.getBytes());
  }

  public byte[] hget(final byte[] key, final byte[] field) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.hget(key, field);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::hget", ex);
    }

    return null;
  }

  ////////////////////////////////////////////////////////////////////
  //  exists
  //

  public boolean exists(final String key) {
    return exists(key.getBytes());
  }

  public boolean exists(final byte[] key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.exists(key);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::exists", ex);
    }

    return false;
  }

  ////////////////////////////////////////////////////////////////////
  //  flushAll
  //

  public void flushAll() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.flushAll();
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::flushAll", ex);
    }
  }

  ////////////////////////////////////////////////////////////////////
  //  pub_sub
  //
  //    subscribe
  //    publish
  //

  public void subscribe(final BinaryJedisPubSub pubSub, final String... channels) {
    final byte[][] arr = new byte[channels.length][];

    for (int i = 0; i < channels.length; i++) {
      arr[i] = channels[i].getBytes();
    }

    subscribe(pubSub, arr);
  }

  public void subscribe(final BinaryJedisPubSub pubSub, final byte[]... channels) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.subscribe(pubSub, channels);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::flushAll", ex);
    }
  }

  public void publish(final String channel, final byte[] data) {
    publish(channel.getBytes(), data);
  }

  public void publish(final byte[] channel, final byte[] data) {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.publish(channel, data);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::publish", ex);
    }
  }

  ////////////////////////////////////////////////////////////////////
  //  streams
  //
  //    XADD adds a new entry to a stream.
  //    XREAD reads one or more entries, starting at a given position and moving forward in time.
  //    XRANGE returns a range of entries between two supplied entry IDs.
  //    XLEN returns the length of a stream.
  //    XTRIM truncates stream to maxsize
  //

  public StreamEntryID xadd(final String key, final Map<String, String> hash) {
    return xadd(key, hash, DEFAULT_XADD_PARAMS);
  }

  public StreamEntryID xadd(
      final String key, final Map<String, String> hash, final XAddParams params) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xadd(key, hash, params);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xadd", ex);
    }

    return null;
  }

  public List<Map.Entry<String, List<StreamEntry>>> xread(
      final XReadParams params, final Map<String, StreamEntryID> streams) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xread(params, streams);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xread", ex);
    }

    return null;
  }

  public List<StreamEntry> xrange(
      final String key, final StreamEntryID start, final StreamEntryID end) {
    return xrange(key, start, end, XRANGE_COUNT);
  }

  public List<StreamEntry> xrange(
      final String key, final StreamEntryID start, final StreamEntryID end, final int count) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xrange(key, start, end, count);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xrange", ex);
    }

    return null;
  }

  public long xlen(final String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xlen(key);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xlen", ex);
    }

    return -1;
  }

  public long xtrim(final String key, final long maxLength) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xtrim(key, maxLength, true);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xtrim", ex);
    }

    return -1;
  }

  public StreamInfo xinfoStream(final String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xinfoStream(key);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xrange", ex);
    }

    return null;
  }

  public StreamFullInfo xinfoStreamFull(final String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.xinfoStreamFull(key);
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::xrange", ex);
    }

    return null;
  }
}
