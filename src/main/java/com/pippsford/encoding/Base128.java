package com.pippsford.encoding;

import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Represent binary in Base-128 using ISO-8859-1 digits and letters. Base-128 encodes every bit of the encoded message
 * is 0.875 bits of the input, whereas Base-64 is 0.75 only bits.
 *
 * <p>Base-128 is a bad idea if using UTF-8 encoding. As UTF-8 requires 16 bits to encode many of the characters used,
 * the encoding efficiency falls to 0.58 bits of input per output bit.
 *
 * @author Simon Greatrix on 31/07/2017.
 */
@ThreadSafe
public class Base128 implements Converter {

  /**
   * Digits for each 128 values. The digits are chosen to be decimal numbers, lowercase letters, or uppercase letters.
   */
  private static final char[] DIGITS = new char[128];

  /**
   * Reverse look-up for digits to the value they represent.
   */
  private static final int[] REVERSE = new int[256];

  static {
    Arrays.fill(REVERSE, -1);
    char i = 32;
    int j = 0;
    while (j < 128) {
      i++;
      int t = Character.getType(i);
      if (t == Character.DECIMAL_DIGIT_NUMBER || t == Character.LOWERCASE_LETTER || t == Character.UPPERCASE_LETTER
          || t == Character.CURRENCY_SYMBOL) {
        DIGITS[j] = i;
        REVERSE[i] = j;
        j++;
      }
    }
  }


  @Nullable
  @Override
  public char[] clean(char[] text) {
    if (text == null) {
      return null;
    }
    char[] buf = new char[text.length];
    int pos = 0;
    for (char ch : text) {
      if (ch < 256 && REVERSE[ch] != -1) {
        TextToByte.append(buf, pos++, ch);
      }
    }

    int byteLength = 7 * (pos / 8);
    int extra = pos % 8;
    switch (extra) {
      case 0:
        break;
      case 1:
        byteLength++;
        break;
      default:
        byteLength += extra - 1;
        break;
    }
    if (isInvalidLast(text, byteLength)) {
      buf = TextToByte.append(buf, pos++, DIGITS[0]);
    }

    return TextToByte.trim(buf, pos);
  }


  /**
   * Decode base-128 represented binary data back to the original data.
   *
   * @param str the base-128 representation
   *
   * @return the binary data
   */
  @Nullable
  @Override
  public byte[] decode(char[] str) {
    // if null input, return null
    if (str == null) {
      return null;
    }

    int byteLength = getByteLength(str);

    // parse the digits
    int c1 = -1;
    int c2;
    int p = 0;
    byte[] output = new byte[byteLength];
    for (char ch : str) {
      // ignore non-digits
      if (ch > 255 || REVERSE[ch] == -1) {
        continue;
      }

      if (c1 == -1) {
        c1 = REVERSE[ch];
      } else {
        c2 = REVERSE[ch];

        output[p] = (byte) (0xff & ((c1 << (1 + p % 7)) | (c2 >> (6 - p % 7))));
        if (p % 7 < 6) {
          c1 = c2;
        } else {
          c1 = -1;
        }
        p++;
      }
    }

    if (p != byteLength) {
      output[p] = (byte) (0xff & (c1 << (1 + p % 7)));
    }

    return output;
  }


  /**
   * Encode binary data in base-128 format.
   *
   * @param data the binary data
   *
   * @return the base 128 format
   */
  @Override
  @Nullable
  public char[] encodeChars(byte[] data) {
    if (data == null) {
      return null;
    }

    // Convert to longs
    long[] values = new long[(data.length + 6) / 7];
    int p = 48;
    long v = 0;
    for (int i = 0; i < data.length; i++) {
      v |= (0xffL & data[i]) << p;
      p -= 8;
      if (p < 0) {
        values[i / 7] = v;
        p = 48;
        v = 0;
      }
    }
    if (p != 48) {
      values[values.length - 1] = v;
    }

    int charLength = 8 * (data.length / 7);
    if (data.length % 7 != 0) {
      charLength += 1 + (data.length % 7);
    }
    char[] buf = new char[charLength];
    int pos = 0;

    // Process blocks of 7 bytes into 8 characters
    int z = data.length;
    for (int i = 0; z > 0; i++) {
      p = 49;
      int y = Math.min(z, 7);
      for (int j = 0; j <= y; j++) {
        v = (values[i] >>> p) & 0x7f;
        p -= 7;
        buf[pos++] = DIGITS[(int) v];
      }
      z -= 7;
    }

    return buf;
  }


  private int getByteLength(char[] str) {
    // find out how many real characters
    int charCount = 0;
    for (char ch : str) {
      if (ch < 256 && REVERSE[ch] != -1) {
        charCount++;
      }
    }

    // how many bytes for this many digits?
    int byteLength = 7 * (charCount / 8);
    if (charCount % 8 != 0) {
      byteLength += (charCount % 8) - 1;

      // check last character for trailing bits
      if (isInvalidLast(str, byteLength)) {
        byteLength++;
      }
    }

    return byteLength;
  }


  private boolean isInvalidLast(char[] str, int byteCount) {
    int lastChar = -1;
    for (int i = str.length - 1; i >= 0; i--) {
      lastChar = str[i];
      if (lastChar < 256 && REVERSE[lastChar] != -1) {
        lastChar = REVERSE[lastChar];
        break;
      }
    }

    // can't be invalid if no characters
    if (lastChar == -1) {
      return false;
    }

    int p = byteCount % 7;

    // can't be invalid if no odd characters
    if (p == 0) {
      return false;
    }
    return (0xff & (lastChar << (1 + p))) != 0;
  }

}
