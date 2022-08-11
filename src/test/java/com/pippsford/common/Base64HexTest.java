package com.pippsford.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import com.pippsford.encoding.Base64Hex;
import org.junit.Test;

/**
 * @author Simon Greatrix on 02/01/2020.
 */
public class Base64HexTest {

  @Test
  public void testEncoding() {
    Random random = new Random(0x7e57ab1e);
    for (int i = 0; i < 256; i++) {
      int length = 1 + random.nextInt(20);
      byte[] input = new byte[length];
      random.nextBytes(input);
      input[0] = (byte) i;

      char[] encoded = Base64Hex.encode(input);
      byte[] output = Base64Hex.decode(encoded);

      assertArrayEquals(input, output);
    }
  }


  @Test
  public void testOrdering() {
    Random random = new Random(0x7e57ab1e);
    for (int i = 0; i < 256; i++) {
      int length = 1 + random.nextInt(20);
      byte[] byte1 = new byte[length];
      byte[] byte2 = new byte[length];
      random.nextBytes(byte1);
      byte1[0] = (byte) i;
      random.nextBytes(byte2);

      char[] char1 = Base64Hex.encode(byte1);
      char[] char2 = Base64Hex.encode(byte2);

      assertEquals(Integer.signum(Arrays.compareUnsigned(byte1,byte2)), Integer.signum(Arrays.compare(char1, char2)));
    }
  }



  @Test
  public void testOrdering2() {
    Random random = new Random(0x7e57ab1e);
    for (int i = 0; i < 256; i++) {
      int length = 1 + random.nextInt(20);
      byte[] byte1 = new byte[length];
      byte[] byte2 = new byte[length];

      String iso1 = new String(byte1, StandardCharsets.ISO_8859_1);
      String iso2 = new String(byte2, StandardCharsets.ISO_8859_1);

      String enc1 = Base64Hex.encodeToString(byte1);
      String enc2 = Base64Hex.encodeToString(byte2);

      assertEquals(Integer.signum(iso1.compareTo(iso2)), Integer.signum(enc1.compareTo(enc2)));
    }
  }
}