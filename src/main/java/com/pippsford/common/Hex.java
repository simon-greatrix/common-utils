package com.pippsford.common;

import java.util.Arrays;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility class for bytes to hex conversions.
 *
 * @author Simon Greatrix on 13/08/2018.
 */
public class Hex {


  private static final String BAD_CHAR = "Invalid character '%s' at position %d in input.";

  private static final char[] chars = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };

  private static final char[] highChars = new char[256];

  private static final short[] highValues = new short[128];

  private static final char[] lowChars = new char[256];

  private static final short[] lowValues = new short[128];


  /**
   * Convert hexadecimal to bytes. The input must contain only valid hexadecimal digits, with no whitespace nor other characters.
   *
   * @param hex the hexadecimal data
   *
   * @return the bytes
   */
  @Nullable
  public static byte[] decode(CharSequence hex) {
    if (hex == null) {
      return null;
    }
    int len = hex.length();
    if ((len & 1) == 1) {
      throw new IllegalArgumentException("Input data contains an odd number of characters \"" + hex + "\"");
    }
    len >>= 1;
    byte[] output = new byte[len];
    int j = 0;
    for (int i = 0; i < len; i++) {
      char high = hex.charAt(j++);
      char low = hex.charAt(j++);
      int v;
      try {
        v = highValues[high] | lowValues[low];
      } catch (ArrayIndexOutOfBoundsException e) {
        if (high >= 128) {
          throw new IllegalArgumentException(String.format(BAD_CHAR, high, j - 2));
        }
        throw new IllegalArgumentException(String.format(BAD_CHAR, low, j - 1));
      }
      if ((v & 0x300) != 0) {
        if ((v & 0x200) != 0) {
          throw new IllegalArgumentException(String.format(BAD_CHAR, high, j - 2));
        }
        throw new IllegalArgumentException(String.format(BAD_CHAR, low, j - 1));
      }
      output[i] = (byte) v;
    }
    return output;
  }


  /**
   * Encode the bytes as hexadecimal.
   *
   * @param bytes the bytes
   *
   * @return the hexadecimal
   */
  @SuppressFBWarnings("PL_PARALLEL_LISTS")
  @Nullable
  public static String encode(byte[] bytes) {
    char[] output = encodeChars(bytes);
    return output != null ? new String(output) : null;
  }


  /**
   * Encode the provided binary data in a textual form.
   *
   * @param bytes binary data
   *
   * @return textual representation
   */
  @Nullable
  public static char[] encodeChars(byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    int len = bytes.length;
    char[] output = new char[len * 2];
    int j = 0;
    for (byte aByte : bytes) {
      int b = aByte & 0xff;
      output[j++] = highChars[b];
      output[j++] = lowChars[b];
    }

    return output;
  }


  /**
   * Test if a hexadecimal representation matches a byte array.
   *
   * @param hex   the hexadecimal
   * @param bytes the bytes
   *
   * @return true if the representation matches the data
   */
  public static boolean equals(String hex, byte[] bytes) {
    if (hex == null) {
      return bytes == null;
    }
    if (bytes == null) {
      return false;
    }
    int len = bytes.length;
    if (hex.length() != len * 2) {
      return false;
    }

    int j = 0;
    for (int i = 0; i < len; i++) {
      char high = hex.charAt(j++);
      char low = hex.charAt(j++);
      int v;
      try {
        v = highValues[high] | lowValues[low];
      } catch (ArrayIndexOutOfBoundsException e) {
        return false;
      }
      if ((bytes[i] & 0xff) != v) {
        return false;
      }
    }

    return true;
  }


  static {
    Arrays.fill(highValues, (short) 0x200);
    Arrays.fill(lowValues, (short) 0x100);

    for (int i = 0; i < 16; i++) {
      for (int j = 0; j < 16; j++) {
        highChars[i * 16 + j] = chars[i];
        lowChars[j * 16 + i] = chars[i];
      }
      highValues[chars[i]] = (short) (i << 4);
      lowValues[chars[i]] = (short) i;
    }
    for (int i = 'A'; i <= 'F'; i++) {
      int v = i - 'A' + 10;
      highValues[i] = (short) (v << 4);
      lowValues[i] = (short) v;
    }
  }
}
