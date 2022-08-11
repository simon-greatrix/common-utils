package com.pippsford.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Creates Version 7 UUIDs as defined in the Peabody proposal.
 *
 * <p>See: https://web.archive.org/web/20220620082741/https://datatracker.ietf.org/doc/html/draft-peabody-dispatch-new-uuid-format
 *
 * @author Simon Greatrix on 27/06/2022.
 */
public class Uuid7 {

  private static final Object LOCK = new Object();

  /** Random number generator. */
  private static final SecureRandom RANDOM = new SecureRandom();

  private static int lastRand = 0;

  private static long lastTime = Long.MIN_VALUE;


  /**
   * Create a type 7 UUID. These UUIDs can be ordered by their natural bit ordering into time order, include a millisecond precision timestamp, and at least
   * 30 random bits. At most 4096 UUIDs can be generated in any given millisecond. If more than that are generated then the quota available to future
   * milliseconds will be used.
   *
   * @return A new type 7 UUID
   */
  public static UUID create() {
    // 48-bit timestamp
    long timeStamp;
    int rand1;

    synchronized (LOCK) {
      timeStamp = System.currentTimeMillis() & 0xffff_ffff_ffffL;
      if (timeStamp > lastTime) {
        // In a new millisecond.
        lastTime = timeStamp;
        lastRand = rand1 = RANDOM.nextInt(0x1000);
      } else {
        // In the same (or older) millisecond, so just increment the count.
        lastRand = rand1 = (lastRand + 1) & 0xfff;
        if (lastRand == 0) {
          // looped over into a new millisecond
          lastTime = (lastTime + 1) & 0xffff_ffff_ffffL;
        }
        timeStamp = lastTime;
      }
    }

    long uuid1 = (timeStamp << 16) | 0x7000 | rand1;
    long uuid2 = 0x8000_0000_0000_0000L | RANDOM.nextLong() & 0x3fff_ffff_ffff_ffffL;
    return new UUID(uuid1, uuid2);
  }

}
