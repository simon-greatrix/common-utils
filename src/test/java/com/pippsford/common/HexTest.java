package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;
import org.junit.Test;

/**
 * @author Simon Greatrix on 13/08/2018.
 */
public class HexTest {


  private String bytesToHex(byte[] array) {
    StringBuilder buf = new StringBuilder(array.length * 2);
    for (byte b : array) {
      buf.append(String.format("%02x", (b & 0xff)));
    }
    return buf.toString();
  }


  @Test
  public void decode() {
    assertNull(Hex.decode(null));

    // Odd number of characters
    testDecodeFail("123", "Input data contains an odd number of characters \"123\"");

    testDecodeFail(":a", "Invalid character ':' at position 0 in input.");
    testDecodeFail("b 12", "Invalid character ' ' at position 1 in input.");
    testDecodeFail("B|", "Invalid character '|' at position 1 in input.");
    testDecodeFail("b€12", "Invalid character '€' at position 1 in input.");
    testDecodeFail("12€c", "Invalid character '€' at position 2 in input.");

    Random random = new Random(0x7e57ab1e);
    for (int i = 0; i < 100; i++) {
      int l = random.nextInt(32);
      byte[] b = new byte[l];
      random.nextBytes(b);

      String encoded = bytesToHex(b).toLowerCase();
      if (random.nextBoolean()) {
        encoded = encoded.toUpperCase();
      }
      assertTrue(Arrays.equals(b, Hex.decode(encoded)));
    }
  }


  @Test
  public void encode() {
    assertNull(Hex.encode(null));

    Random random = new Random(0x7e57ab1e);
    for (int i = 0; i < 100; i++) {
      int l = random.nextInt(32);
      byte[] b = new byte[l];
      random.nextBytes(b);

      String encoded = Hex.encode(b);
      assertEquals(bytesToHex(b).toLowerCase(), encoded);
    }
  }


  private void testDecodeFail(String s, String msg) {
    try {
      Hex.decode(s);
      fail("Decoding should have failed for \"" + s + "\"");
    } catch (IllegalArgumentException e) {
      assertEquals(msg, e.getMessage());
      // correct
    }
  }
}
