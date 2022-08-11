package com.pippsford.encoding;

import java.util.Arrays;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Base 32 encoding as defined in RFC4648. Five bytes of binary data become eight characters.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
// Suppressing "cognitive complexity" warnings as data validation requires a lot of if-then constructs
// which leads to a high cognitive complexity. Sometimes there are just a lot of rules to check.
@SuppressWarnings("squid:S3776")
public class GenericBase32 implements Converter {

  /**
   * Value matchers for overflow detection.
   */
  static final int[] OVERFLOWS = {12, -1, 2, -1, 5, 7, -1, 10};

  /**
   * Characters for each of the values 0 to 31.
   */
  final char[] chars;

  /**
   * Must padding be applied on encoding?.
   */
  final boolean mustPad;

  /**
   * The padding character.
   */
  final char pad;

  /**
   * Is upper case preferred?.
   */
  final boolean preferUpper;

  /**
   * Contributions of each character to each of the three bytes in a block. Each byte has two or three contributing
   * characters. The final column is all zero as it makes some code easier.
   */
  final byte[][] values = new byte[13][128];


  /**
   * Create a new generic Base 32 converter.
   *
   * @param chars       the character to use. Must be all of one case.
   * @param pad         the padding character, if any
   * @param preferUpper if true, prefer upper case characters
   * @param mustPad     whether encoding must be padded
   */
  public GenericBase32(char[] chars, char pad, boolean preferUpper, boolean mustPad) {
    this.chars = chars.clone();
    this.pad = pad;
    this.preferUpper = preferUpper;
    this.mustPad = mustPad;

    for (int i = 0; i < 12; i++) {
      Arrays.fill(values[i], (byte) -1);
    }
    // Not strictly necessary as Java initialises to zero.
    Arrays.fill(values[12], (byte) 0);

    for (int i = 0; i < 32; i++) {
      int j = this.chars[i];
      // 11111222 22333334 44445555 56666677 77788888
      // 0    1   2 3    4 5   6    78    9  0  1
      values[0][j] = (byte) (i << 3);
      values[1][j] = (byte) ((i & 0b11100) >> 2);
      values[2][j] = (byte) ((i & 0b00011) << 6);
      values[3][j] = (byte) (i << 1);
      values[4][j] = (byte) ((i & 0b10000) >> 4);
      values[5][j] = (byte) ((i & 0b01111) << 4);
      values[6][j] = (byte) ((i & 0b11110) >> 1);
      values[7][j] = (byte) ((i & 0b00001) << 7);
      values[8][j] = (byte) (i << 2);
      values[9][j] = (byte) ((i & 0b11000) >> 3);
      values[10][j] = (byte) ((i & 0b00111) << 5);
      values[11][j] = (byte) i;
    }

    // encode other case
    for (int i = 0; i < 12; i++) {
      for (int j = 'a'; j <= 'z'; j++) {
        byte v = values[i][j];
        if (v != -1) {
          values[i][j - 32] = v;
        }
      }
      for (int j = 'A'; j <= 'Z'; j++) {
        byte v = values[i][j];
        if (v != -1) {
          values[i][j + 32] = v;
        }
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
    for (char aText : text) {
      char ch = aText;
      if ((ch < 128) && values[11][ch] != -1) {
        if (preferUpper) {
          // convert to upper case
          if ('a' <= ch && ch <= 'z') {
            ch -= 32;
          }
        } else {
          // convert to lower case
          if ('A' <= ch && ch <= 'Z') {
            ch += 32;
          }
        }
        buf = TextToByte.append(buf, pos++, ch);
      }
    }

    int r = pos % 8;
    char ch = (pos > 0) ? buf[pos - 1] : chars[0];

    // Try to repair any trailing bits by adding a zero.
    while (OVERFLOWS[r] == -1 || values[OVERFLOWS[r]][ch] != 0) {
      buf = TextToByte.append(buf, pos++, chars[0]);
      r = (r + 1) % 8;
    }

    if (mustPad && r != 0) {
      while (r < 8) {
        r++;
        buf = TextToByte.append(buf, pos++, pad);
      }
    }

    return TextToByte.trim(buf, pos);
  }


  @Nullable
  @Override
  public byte[] decode(char[] text) {
    if (text == null) {
      return null;
    }

    int end = TextToByte.removeWhitespaceInPlace(text);

    while ((end > 0) && (text[end - 1] == pad)) {
      end--;
    }
    if (end == 0) {
      return new byte[0];
    }

    // Validate length after padding removed.
    final int rem = end & 0x7;
    if (OVERFLOWS[rem] == -1) {
      throw new IllegalArgumentException(
          "Input text has invalid length of " + end + ": " + new String(text));
    }

    // every 8 characters produces 5 bytes, so multiply by 5/8 = 0.625
    int byteLen = (int) (end * 0.625);
    byte[] data = new byte[byteLen];

    // extract data from text
    int di = 0;
    int r = 0;
    char c0 = '\0';
    char c1 = '\0';
    for (int ti = 0; ti < end; ti++) {
      char cn = text[ti];
      if ((cn > 127) || values[0][cn] == -1) {
        throw new IllegalArgumentException(
            "Input text has invalid character 0x"
                + Integer.toHexString(cn)
                + " at position " + ti + ":" + new String(text));
      }

      // 00000111 11222223 33334444 45555566 66677777
      switch (r) {
        case 0:
          c0 = cn;
          break;
        case 1:
          data[di] = (byte) (values[0][c0] | values[1][cn]);
          di++;
          c0 = cn;
          break;
        case 2:
        case 5:
          c1 = cn;
          break;
        case 3:
          data[di] = (byte) (values[2][c0] | values[3][c1] | values[4][cn]);
          c0 = cn;
          di++;
          break;
        case 4:
          data[di] = (byte) (values[5][c0] | values[6][cn]);
          c0 = cn;
          di++;
          break;
        case 6:
          data[di] = (byte) (values[7][c0] | values[8][c1] | values[9][cn]);
          c0 = cn;
          di++;
          break;
        case 7:
          data[di] = (byte) (values[10][c0] | values[11][cn]);
          di++;
          break;
        default:
          // Required by coding rules. There is literally no way to reach this line but a 'default' case was
          // required.
          throw new AssertionError("Remainder modulo-8 was not between 0 and 7 inclusive.");
      }
      r = (r + 1) & 0x7;
    }

    // Verify final bits are zero
    if (values[OVERFLOWS[rem]][text[end - 1]] != 0) {
      throw new IllegalArgumentException(
          "Trailing bits detected in encoding \"..." + new String(text, end - rem, rem) + "\".");
    }

    return data;
  }


  @Nullable
  @Override
  public char[] encodeChars(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // every five bytes requires 8 characters of output
    int fullBlocks = bytes.length / 5;
    int extraBytes = bytes.length - 5 * fullBlocks;

    int textLen = 8 * fullBlocks;
    if (extraBytes != 0) {
      if (mustPad) {
        textLen += 8;
      } else {
        switch (extraBytes) {
          case 1:
            textLen += 2;
            break;
          case 2:
            textLen += 4;
            break;
          case 3:
            textLen += 5;
            break;
          case 4:
            textLen += 7;
            break;
          default:
            break;
        }
      }
    }
    char[] output = new char[textLen];

    byte b0;
    byte b1;
    byte b2;
    byte b3;
    byte b4;
    // 00000111 11222223 33334444 45555566 66677777
    for (int i = 0; i < fullBlocks; i++) {
      int j = i * 5;
      b0 = bytes[j];
      b1 = bytes[j + 1];
      b2 = bytes[j + 2];
      b3 = bytes[j + 3];
      b4 = bytes[j + 4];

      int k = i * 8;
      output[k] = chars[(b0 & 0b11111000) >> 3];
      output[k + 1] = chars[((b0 & 0b00000111) << 2) + ((b1 & 0b11000000) >> 6)];
      output[k + 2] = chars[(b1 & 0b00111110) >> 1];
      output[k + 3] = chars[((b1 & 0b00000001) << 4) + ((b2 & 0b11110000) >> 4)];
      output[k + 4] = chars[((b2 & 0b00001111) << 1) + ((b3 & 0b10000000) >> 7)];
      output[k + 5] = chars[(b3 & 0b01111100) >> 2];
      output[k + 6] = chars[((b3 & 0b00000011) << 3) + ((b4 & 0b11100000) >> 5)];
      output[k + 7] = chars[(b4 & 0b00011111)];
    }

    int k = fullBlocks * 8;
    int j = fullBlocks * 5;
    switch (extraBytes) {
      case 1:
        b0 = bytes[j];
        output[k] = chars[(b0 & 0b11111000) >> 3];
        output[k + 1] = chars[((b0 & 0b00000111) << 2)];
        if (mustPad) {
          output[k + 2] = pad;
          output[k + 3] = pad;
          output[k + 4] = pad;
          output[k + 5] = pad;
          output[k + 6] = pad;
          output[k + 7] = pad;
        }
        break;
      case 2:
        b0 = bytes[j];
        b1 = bytes[j + 1];
        output[k] = chars[(b0 & 0b11111000) >> 3];
        output[k + 1] = chars[((b0 & 0b00000111) << 2) + ((b1 & 0b11000000) >> 6)];
        output[k + 2] = chars[(b1 & 0b00111110) >> 1];
        output[k + 3] = chars[((b1 & 0b00000001) << 4)];
        if (mustPad) {
          output[k + 4] = pad;
          output[k + 5] = pad;
          output[k + 6] = pad;
          output[k + 7] = pad;
        }
        break;
      case 3:
        b0 = bytes[j];
        b1 = bytes[j + 1];
        b2 = bytes[j + 2];
        output[k] = chars[(b0 & 0b11111000) >> 3];
        output[k + 1] = chars[((b0 & 0b00000111) << 2) + ((b1 & 0b11000000) >> 6)];
        output[k + 2] = chars[(b1 & 0b00111110) >> 1];
        output[k + 3] = chars[((b1 & 0b00000001) << 4) + ((b2 & 0b11110000) >> 4)];
        output[k + 4] = chars[(b2 & 0b00001111) << 1];
        if (mustPad) {
          output[k + 5] = pad;
          output[k + 6] = pad;
          output[k + 7] = pad;
        }
        break;
      case 4:
        b0 = bytes[j];
        b1 = bytes[j + 1];
        b2 = bytes[j + 2];
        b3 = bytes[j + 3];
        output[k] = chars[(b0 & 0b11111000) >> 3];
        output[k + 1] = chars[((b0 & 0b00000111) << 2) + ((b1 & 0b11000000) >> 6)];
        output[k + 2] = chars[(b1 & 0b00111110) >> 1];
        output[k + 3] = chars[((b1 & 0b00000001) << 4) + ((b2 & 0b11110000) >> 4)];
        output[k + 4] = chars[((b2 & 0b00001111) << 1) + ((b3 & 0b10000000) >> 7)];
        output[k + 5] = chars[(b3 & 0b01111100) >> 2];
        output[k + 6] = chars[(b3 & 0b00000011) << 3];
        if (mustPad) {
          output[k + 7] = pad;
        }
        break;
      default:
        break;
    }

    return output;
  }

}
