package com.mattygiedt.redis;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisClient {
  private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

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
  //  flushAll
  //

  public void flushAll() {
    try (Jedis jedis = jedisPool.getResource()) {
      jedis.flushAll();
    } catch (Exception ex) {
      logger.error("Exception caught in jedis::flushAll", ex);
    }
  }
}
