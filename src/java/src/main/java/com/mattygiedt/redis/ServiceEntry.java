package com.mattygiedt.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceEntry {
  private static final Logger logger = LoggerFactory.getLogger(ServiceEntry.class);

  public static void main(final String[] args) throws Exception {
    //
    //  By default, logging is set at debug, which gRPC uses heavily.
    //  Here's a hacktastic way to configure log_level -> info at runtime.
    //

    final ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(ch.qos.logback.classic.Level.toLevel("info"));

    logger.info("Hello, World!!!");
  }
}
