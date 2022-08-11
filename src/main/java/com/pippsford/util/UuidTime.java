package com.pippsford.util;

import java.security.SecureRandom;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Representation of the time and sequence used to generate Type-1 UUIDs.
 */
final class UuidTime {

  /**
   * UUIDs need time from the beginning of Gregorian calendar (15-OCT-1582), need to apply this offset from the System
   * current time.
   */
  private static final long CLOCK_OFFSET = 0x01b21dd213814000L;

  /**
   * Initial time which may be negative. Used to ensure we get a positive time difference.
   */
  private static final long INIT_NANO = System.nanoTime();

  /**
   * Sequence number for a given clock value. The RFC requires it be initialised from a secure random source.
   */
  private static short clockSequence = 0;

  /** Has the timer been initialised?. */
  private static boolean isInitDone = false;

  /**
   * Timestamp value last used for generating a UUID (along with {@link #sequence}. Usually the same as {@link
   * #timeStamp} , but not always (system clock moved backwards). Note that this value is guaranteed to be monotonically
   * increasing; that is, at given absolute time points t1 and t2 (where t2 is after t1), t1 {@literal <=}  t2 will always hold
   * true.
   */
  private static long lastUsedTimestamp = 0L;


  /**
   * Initialize this timer. Ideally the random number generator will provide a secure random value to initialise the
   * sequence with.
   *
   * @param random RNG
   */
  @SuppressFBWarnings("DMI_RANDOM_USED_ONLY_ONCE")
  static synchronized void init(final Random random) {
    if (isInitDone) {
      return;
    }

    Random rand = random;
    if (rand == null) {
      rand = new SecureRandom();
    }
    clockSequence = (short) rand.nextInt(0x10000);
    isInitDone = true;
  }


  /**
   * Method that constructs a unique timestamp.
   *
   * @return 64-bit timestamp to use for constructing UUID
   */
  public static synchronized UuidTime newTimeStamp() {
    long sysTime = System.currentTimeMillis();
    long nanoTime = ((System.nanoTime() - INIT_NANO) / 100) % 10000;
    sysTime = sysTime * 10000 + nanoTime + CLOCK_OFFSET;

    // If time is in past, move up
    if (sysTime < lastUsedTimestamp) {
      sysTime = lastUsedTimestamp;
    }

    // Get the sequence
    short seq = clockSequence;
    if (sysTime == lastUsedTimestamp) {
      seq++;
      clockSequence = seq;
      if (seq == 0) {
        sysTime++;
      }
    }

    lastUsedTimestamp = sysTime;

    return new UuidTime(sysTime, seq);
  }


  /**
   * A 16-bit sequence number unique with the current 100 nanosecond interval.
   */
  private final int sequence;

  /**
   * The number of 100 nanosecond intervals that have passed since the start of the Gregorian calendar.
   */
  private final long timeStamp;


  /**
   * New time and sequence.
   *
   * @param timeStamp the time stamp
   * @param sequence  the sequence
   */
  private UuidTime(long timeStamp, short sequence) {
    this.timeStamp = timeStamp;
    this.sequence = sequence & 0xffff;
  }


  /**
   * Get the 16-bit sequence value.
   *
   * @return the sequence value
   */
  public int getSequence() {
    return sequence;
  }


  /**
   * Get the 64-bit 100 nanosecond time value.
   *
   * @return the time value
   */
  public long getTime() {
    return timeStamp;
  }

}
