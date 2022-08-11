package com.pippsford.encoding;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A generic Base 64 converter that can handle the normal variations between different Base 64 schemes.
 *
 * @author Simon Greatrix
 */
// Suppressing "cognitive complexity" warnings as data validation requires a lot of if-then constructs
// which leads to a high cognitive complexity. Sometimes there are just a lot of rules to check.
@SuppressWarnings("squid:S3776")
@ThreadSafe
public class GenericBase64 implements Converter {

  private static final String ERROR_INVALID_CHAR = "Input text has invalid character 0x%x at position %d : %s";

  /**
   * Characters for each of the values 0 to 63.
   */
  final char[] chars = new char[64];

  /**
   * Must padding be applied on encoding?.
   */
  final boolean mustPad;

  /**
   * The padding character.
   */
  final char pad;

  /**
   * Contributions of each character to each of the three bytes in a block. Each byte has two contributing characters.
   */
  final byte[][] values = new byte[6][128];


  /**
   * Create a new generic Base 64 converter.
   *
   * @param c62     the character to use for 62
   * @param c63     the character to use for 63
   * @param pad     the padding character, if any
   * @param mustPad whether encoding must be padded
   */
  public GenericBase64(char c62, char c63, char pad, boolean mustPad) {
    this.pad = pad;
    this.mustPad = mustPad;
    int j = 0;
    for (char i = 'A'; i <= 'Z'; i++) {
      chars[j] = i;
      j++;
    }
    for (char i = 'a'; i <= 'z'; i++) {
      chars[j] = i;
      j++;
    }
    for (char i = '0'; i <= '9'; i++) {
      chars[j] = i;
      j++;
    }
    chars[62] = c62;
    chars[63] = c63;

    for (int i = 0; i < 6; i++) {
      for (j = 0; j < 128; j++) {
        values[i][j] = -1;
      }
    }

    for (int i = 0; i < 64; i++) {
      j = chars[i];
      values[5][j] = (byte) i;
      values[4][j] = (byte) ((i & 0x03) << 6);
      values[3][j] = (byte) ((i & 0x3c) >> 2);
      values[2][j] = (byte) ((i & 0x0f) << 4);
      values[1][j] = (byte) ((i & 0x30) >> 4);
      values[0][j] = (byte) ((i & 0x3f) << 2);
    }
  }


  /**
   * {@inheritDoc}
   */
  @Nullable
  @Override
  public char[] clean(char[] text) {
    if (text == null) {
      return null;
    }
    char[] buf = new char[text.length];
    int pos = 0;
    for (char ch : text) {
      if ((ch < 128) && values[5][ch] != -1) {
        buf = TextToByte.append(buf, pos++, ch);
      }
    }

    int r = pos % 4;
    // Try to repair trailing bits by adding a zero.
    char lastChar;
    switch (r) {
      case 1:
        // Incomplete first byte, always append zero
        buf = TextToByte.append(buf, pos++, chars[0]);
        r = 2;
        break;
      case 2:
        // Incomplete second byte if bits not zero
        lastChar = lastChar(text);
        if (values[2][lastChar] != 0) {
          buf = TextToByte.append(buf, pos++, chars[0]);
          r = 3;
        }
        break;
      case 3:
        // Incomplete third byte if bits not zero
        lastChar = lastChar(text);
        if (values[4][lastChar] != 0) {
          buf = TextToByte.append(buf, pos++, chars[0]);
          r = 0;
        }
        break;
      default:
        break;
    }

    if (mustPad) {
      if (r >= 2) {
        buf = TextToByte.append(buf, pos++, pad);
      }
      if (r == 2) {
        buf = TextToByte.append(buf, pos++, pad);
      }
    }

    return TextToByte.trim(buf, pos);
  }


  /**
   * Decode the provided textual representation back into binary data.
   *
   * @param text textual representation
   *
   * @return binary data
   */
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

    // validate length after padding removed
    int rem = end & 0x3;
    if (rem == 1) {
      throw new IllegalArgumentException(
          "Input text has invalid length of " + end + ": " + new String(text));
    }

    // every 4 characters produces 3 bytes
    int byteLen = 3 * ((end - rem) / 4);
    if (rem == 2) {
      byteLen += 1;
    } else if (rem == 3) {
      byteLen += 2;
    }

    byte[] data = new byte[byteLen];

    // extract data from text
    int ti = 0;
    int r = 0;
    char c0;
    char c1 = '\0';
    for (int di = 0; di < byteLen; di++) {
      if (r == 0) {
        c0 = text[ti];
        if ((c0 > 127) || values[5][c0] == -1) {
          throw new IllegalArgumentException(String.format(ERROR_INVALID_CHAR, (int) c0, ti, new String(text)));
        }
        ti++;
        c1 = text[ti];
        if ((c1 > 127) || values[5][c1] == -1) {
          throw new IllegalArgumentException(String.format(ERROR_INVALID_CHAR, (int) c1, ti, new String(text)));
        }
        ti++;
      } else {
        c0 = c1;
        c1 = text[ti];
        if ((c1 > 127) || values[5][c1] == -1) {
          throw new IllegalArgumentException(String.format(ERROR_INVALID_CHAR, (int) c1, ti, new String(text)));
        }
        ti++;
      }

      data[di] = (byte) (values[r * 2][c0] + values[r * 2 + 1][c1]);
      r = (r + 1) % 3;
    }

    return data;
  }


  /**
   * Encode the provided binary data in a textual form.
   *
   * @param bytes binary data
   *
   * @return textual representation
   */
  @Nullable
  @Override
  public char[] encodeChars(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // every three bytes requires 4 characters of output
    int fullBlocks = bytes.length / 3;
    int extraBytes = bytes.length - 3 * fullBlocks;

    int textLen = 4 * fullBlocks;
    if (extraBytes != 0) {
      if (mustPad) {
        textLen += 4;
      } else {
        textLen += 1 + extraBytes;
      }
    }
    char[] output = new char[textLen];

    int b;
    for (int i = 0; i < fullBlocks; i++) {
      int j = i * 3;
      int k = i * 4;
      b = ((0xff & bytes[j]) << 16) | ((0xff & bytes[j + 1]) << 8) | (0xff & bytes[j + 2]);
      output[k] = chars[(b >> 18) & 0x3f];
      output[k + 1] = chars[(b >> 12) & 0x3f];
      output[k + 2] = chars[(b >> 6) & 0x3f];
      output[k + 3] = chars[b & 0x3f];
    }

    int k = fullBlocks * 4;
    int j = fullBlocks * 3;

    switch (extraBytes) {
      case 1:
        b = 0xff & bytes[j];
        output[k] = chars[(b >> 2) & 0x3f];
        output[k + 1] = chars[(b & 0x3) << 4];
        if (mustPad) {
          output[k + 2] = pad;
          output[k + 3] = pad;
        }
        break;
      case 2:
        b = ((0xff & bytes[j]) << 8) | (0xff & bytes[j + 1]);
        output[k] = chars[(b >> 10) & 0x3f];
        output[k + 1] = chars[(b >> 4) & 0x3f];
        output[k + 2] = chars[(b << 2) & 0x3f];
        if (mustPad) {
          output[k + 3] = pad;
        }
        break;
      default:
        break;
    }

    return output;
  }


  private char lastChar(char[] text) {
    for (int i = text.length - 1; i >= 0; i--) {
      char ch = text[i];
      if ((ch < 128) && values[5][ch] != -1) {
        return ch;
      }
    }

    // never happens
    return (char) 0;
  }

}
