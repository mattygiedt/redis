# redis
A simple exploration into a few of the APIs supported by Redis. Redis is an open source (BSD licensed), in-memory data structure store used as a database, cache, message broker, and streaming engine.
* [Strings](https://redis.io/docs/data-types/strings/) (ie: caching)
* [PubSub](https://redis.io/docs/interact/pubsub/)
* [Streams](https://redis.io/docs/data-types/streams/)

## Dev Environment
I use VSCode + Docker and these instructions basically how I setup my development environment.

### Build the `redis-env:local` container image
Start in the project root and type:
```
mgiedt@DESKTOP:~/redis$ docker build --no-cache -f docker/Dockerfile -t redis-env:local .
```

Once built, re-open VSCode Project in Container.
I like to use two terminals inside my editor for managing the redis server and the java applications.

### Start the redis server
This is simply the default configuration from the `redis-7.2.1` release, no changes.
```
root@132a95213ae8:/workspaces/redis# redis-server config/redis.conf
```

### Build the java source and run
```
root@132a95213ae8:/workspaces/redis# mvn clean package
root@132a95213ae8:/workspaces/redis# java -jar src/java/target/redis-java-0.0.1-SNAPSHOT-shaded.jar
14:55:40.354 [main] INFO com.mattygiedt.redis.ServiceEntry -- redis-server: localhost:6379
14:55:40.564 [Thread-0] WARN com.mattygiedt.redis.ServiceEntry -- Shutdown ...
root@132a95213ae8:/workspaces/redis#
```
Your `java` terminal should look like the above, and validates that you can connect to the redis-server. You'll get a `java.net.ConnectException: Connection refused` exception if you can't.

#### Implementation Notes
The class used to run our demo is called [ServiceEntry](https://github.com/mattygiedt/redis/blob/main/src/java/src/main/java/com/mattygiedt/redis/ServiceEntry.java) and has three boolean variables you can use to isolate one of the APIs.
```
    boolean doMarketData = false;
    boolean doPubSub = false;
    boolean doStreams = false;
```
As you can see, these are all defaulted to `false` so you need to update one of them to `true`, recompile, and run.

Also, every time the ServiceEntry is executed, we execute `FLUSHALL` which deletes all the keys of all the existing databases. Simply comment out that line and your data should persist between test runs and start / stop of the `redis-server`.
```
    //
    //  Clear out any previous data
    //

    redisClient.flushAll();
```

Here's what you can expect if, for example, you set the tuple to:
```
    boolean doMarketData = false;
    boolean doPubSub = false;
    boolean doStreams = true;
```
(don't forget to recomplile!)
```
root@132a95213ae8:/workspaces/redis# java -jar src/java/target/redis-java-0.0.1-SNAPSHOT-shaded.jar
15:04:54.166 [main] INFO com.mattygiedt.redis.ServiceEntry -- redis-server: localhost:6379
15:04:54.360 [main] INFO com.mattygiedt.redis.ServiceEntry -- generating trade events ...
15:04:54.362 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999894362-0
15:04:54.437 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999894436-0
15:04:54.512 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999894512-0

...

15:04:58.947 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999898947-0
15:04:59.022 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999899022-0
15:04:59.097 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999899097-0
15:04:59.172 [main] INFO com.mattygiedt.redis.ServiceEntry -- generating trade events ... done
15:04:59.172 [main] INFO com.mattygiedt.redis.ServiceEntry -- TLT entry count: 29
15:04:59.174 [main] INFO com.mattygiedt.redis.ServiceEntry -- TLT entries size: 29
15:04:59.174 [main] INFO com.mattygiedt.redis.ServiceEntry --   first entry: 1695999894362-0 {SYMBOL=TLT, PRICE=-1.735127215E9, QUANTITY=1917366487}
15:04:59.174 [main] INFO com.mattygiedt.redis.ServiceEntry --    last entry: 1695999898721-0 {SYMBOL=TLT, PRICE=1.862200955E9, QUANTITY=-2086363333}
15:04:59.175 [main] INFO com.mattygiedt.redis.ServiceEntry -- LQD entry count: 35
15:04:59.176 [main] INFO com.mattygiedt.redis.ServiceEntry -- LQD entries size: 35
15:04:59.176 [main] INFO com.mattygiedt.redis.ServiceEntry --   first entry: 1695999894587-0 {SYMBOL=LQD, PRICE=1.946556984E9, QUANTITY=791191807}
15:04:59.176 [main] INFO com.mattygiedt.redis.ServiceEntry --    last entry: 1695999899097-0 {SYMBOL=LQD, PRICE=1.189365459E9, QUANTITY=-1412308146}
15:04:59.178 [main] INFO com.mattygiedt.redis.ServiceEntry -- tail streams: {trades:TLT=1695999898721-0, trades:LQD=1695999899097-0}
15:04:59.179 [pool-1-thread-1] INFO com.mattygiedt.redis.ServiceEntry -- generated trade event: 1695999899179-0
15:04:59.182 [main] INFO com.mattygiedt.redis.ServiceEntry -- tail returned: channel trades:LQD, entry: [1695999899179-0 {SYMBOL=LQD, PRICE=2.046712154E9, QUANTITY=-51782725}]
^C
15:05:05.221 [Thread-0] WARN com.mattygiedt.redis.ServiceEntry -- Shutdown ...
```
