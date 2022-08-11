package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.UUID;

import org.junit.Test;

/**
 * @author Simon Greatrix on 27/06/2022.
 */
public class Uuid7Test {


  @Test
  public void testOrdering() {
    UUID[] uuids = new UUID[100000];
    for (int i = uuids.length - 1; i >= 0; i--) {
      uuids[i] = Uuid7.create();
    }

    // All UUIDs are unique
    HashSet<UUID> set = new HashSet<>();
    for (UUID u : uuids) {
      set.add(u);
    }
    assertEquals(uuids.length, set.size());

    // UUIDs are strictly increasing.
    for (int i = uuids.length - 1; i >= 1; i--) {
      assertTrue(uuids[i] + " to " + uuids[i-1], uuids[i - 1].toString().compareTo(uuids[i].toString()) >0);
      assertTrue(uuids[i-1].getMostSignificantBits() > uuids[i].getMostSignificantBits());
    }
  }

  @Test
  public void testVersion() {
    UUID uuid = Uuid7.create();
    assertEquals(7,uuid.version());
    assertEquals(2,uuid.variant());
  }
}