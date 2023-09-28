# redis

### Build the `redis-env:local` container image
```
~/redis$ docker build --no-cache -f docker/Dockerfile -t redis-env:local .
```
Once built, re-open VSCode Project in Container.

### Start the redis server
```
root@132a95213ae8:/workspaces/redis# redis-server config/redis.conf
```

### Build the java source and run
```
root@132a95213ae8:/workspaces/redis# mvn clean package
root@132a95213ae8:/workspaces/redis# java -jar src/java/target/redis-java-0.0.1-SNAPSHOT-shaded.jar
01:11:29.314 [main] INFO com.mattygiedt.redis.ServiceEntry -- redis-server: localhost:6379
^C01:11:35.418 [Thread-4] WARN com.mattygiedt.redis.ServiceEntry -- Shutdown ...
01:11:35.418 [Thread-4] INFO com.mattygiedt.redis.ServiceEntry -- Cache reads: 1044
01:11:35.419 [Thread-4] INFO com.mattygiedt.redis.ServiceEntry -- Cache writes: 348
root@132a95213ae8:/workspaces/redis#
```
