package com.mattygiedt.redis;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mattygiedt.flatbuffer.marketdata.EpochNanos;
import com.mattygiedt.flatbuffer.marketdata.MarketDataSnapshot;
import com.mattygiedt.flatbuffer.marketdata.Price;
import com.mattygiedt.flatbuffer.marketdata.Quantity;
import com.mattygiedt.flatbuffer.marketdata.Source;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class MarketDataSnapshotFactory {
  public static long getEpochNanos(final Instant instant) {
    return TimeUnit.SECONDS.toNanos(instant.getEpochSecond()) + instant.getNano();
  }

  public static MarketDataSnapshot generateRandomMarketDataSnapshot(final int instrumentId) {
    final FlatBufferBuilder builder = new FlatBufferBuilder(256);
    final ThreadLocalRandom random = ThreadLocalRandom.current();

    MarketDataSnapshot.startMarketDataSnapshot(builder);
    MarketDataSnapshot.addInstrumentId(builder, instrumentId);
    MarketDataSnapshot.addSource(builder, Source.BPIPE);
    MarketDataSnapshot.addCreateTm(builder,
        EpochNanos.createEpochNanos(
            builder, MarketDataSnapshotFactory.getEpochNanos(Instant.now())));

    MarketDataSnapshot.addBidPrice(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addBidYield(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addBidSpread(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addBidGspread(builder, Price.createPrice(builder, random.nextInt()));

    MarketDataSnapshot.addAskPrice(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addAskYield(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addAskSpread(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addAskGspread(builder, Price.createPrice(builder, random.nextInt()));

    MarketDataSnapshot.addBidSize(builder, Quantity.createQuantity(builder, random.nextInt()));
    MarketDataSnapshot.addAskSize(builder, Quantity.createQuantity(builder, random.nextInt()));

    MarketDataSnapshot.addLastTradeSize(
        builder, Quantity.createQuantity(builder, random.nextInt()));
    MarketDataSnapshot.addLastTradePrice(builder, Price.createPrice(builder, random.nextInt()));
    MarketDataSnapshot.addLastTradeTm(
        builder, EpochNanos.createEpochNanos(builder, getEpochNanos(Instant.now())));

    MarketDataSnapshot.finishMarketDataSnapshotBuffer(
        builder, MarketDataSnapshot.endMarketDataSnapshot(builder));

    return MarketDataSnapshotFactory.of(builder.sizedByteArray());
  }

  public static MarketDataSnapshot of(final byte[] bytes) {
    return MarketDataSnapshot.getRootAsMarketDataSnapshot(ByteBuffer.wrap(bytes));
  }

  public static void assign(final byte[] bytes, final MarketDataSnapshot snapshot) {
    MarketDataSnapshot.getRootAsMarketDataSnapshot(ByteBuffer.wrap(bytes), snapshot);
  }
}
