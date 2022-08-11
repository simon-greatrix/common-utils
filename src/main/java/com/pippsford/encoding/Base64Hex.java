package com.pippsford.encoding;

import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * A base 64 encoding where the binary ordering of the encoding and the original <strong>unsigned</strong> data match. The characters used are '-' (hyphen),
 * '0' to '9', 'A' to 'Z', '_' (underscore), and 'a' to 'z'. There is no padding character. This is the same alphabet as used by URL-safe Base64 encoding, but
 * the binary order of the associated bit patterns matches the ASCII order of the characters.
 *
 * @author Simon Greatrix on 02/01/2020.
 */
public class Base64Hex {

  private static final String ERROR_INVALID_CHAR = "Input text has invalid character 0x%x at position %d : %s";

  private static final Base64Hex INSTANCE = new Base64Hex();


  /**
   * Decode some encoded data.
   *
   * @param data the data to decode
   *
   * @return the bytes
   */
  public static byte[] decode(char[] data) {
    return INSTANCE.doDecode(data);
  }


  /**
   * Decode some encoded data.
   *
   * @param data the data to decode
   *
   * @return the bytes
   */
  @Nullable
  public static byte[] decode(String data) {
    if (data == null) {
      return null;
    }
    return INSTANCE.doDecode(data.toCharArray());
  }


  /**
   * Encode some data.
   *
   * @param data the data
   *
   * @return the encoded form
   */
  public static char[] encode(byte[] data) {
    return INSTANCE.doEncode(data);
  }


  /**
   * Encode some data.
   *
   * @param data the data
   *
   * @return the encoded form
   */
  public static String encodeToString(byte[] data) {
    return new String(INSTANCE.doEncode(data));
  }


  /**
   * Characters for each of the values 0 to 63.
   */
  final char[] chars = {
      '-', '0', '1', '2', '3', '4', '5', '6',
      '7', '8', '9', 'A', 'B', 'C', 'D', 'E',
      'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
      'V', 'W', 'X', 'Y', 'Z', '_', 'a', 'b',
      'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
      'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
      's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
  };

  /**
   * Contributions of each character to each of the three bytes in a block. Each byte has two contributing characters.
   */
  final byte[][] values = new byte[6][128];


  /**
   * Create a new generic Base 64 converter.
   */
  private Base64Hex() {
    Arrays.sort(chars);

    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < 128; j++) {
        values[i][j] = -1;
      }
    }

    for (int i = 0; i < 64; i++) {
      int j = chars[i];
      values[5][j] = (byte) i;
      values[4][j] = (byte) ((i & 0x03) << 6);
      values[3][j] = (byte) ((i & 0x3c) >> 2);
      values[2][j] = (byte) ((i & 0x0f) << 4);
      values[1][j] = (byte) ((i & 0x30) >> 4);
      values[0][j] = (byte) ((i & 0x3f) << 2);
    }
  }


  /**
   * Decode the provided textual representation back into binary data.
   *
   * @param text textual representation
   *
   * @return binary data
   */
  @Nullable
  byte[] doDecode(char[] text) {
    if (text == null) {
      return null;
    }

    int end = text.length;
    if (end == 0) {
      return new byte[0];
    }

    // validate length
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
      } else {
        c0 = c1;
      }
      c1 = text[ti];
      if ((c1 > 127) || values[5][c1] == -1) {
        throw new IllegalArgumentException(String.format(ERROR_INVALID_CHAR, (int) c1, ti, new String(text)));
      }
      ti++;

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
  char[] doEncode(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // every three bytes requires 4 characters of output
    int fullBlocks = bytes.length / 3;
    int extraBytes = bytes.length - 3 * fullBlocks;

    int textLen = 4 * fullBlocks;
    if (extraBytes != 0) {
      textLen += 1 + extraBytes;
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
        break;
      case 2:
        b = ((0xff & bytes[j]) << 8) | (0xff & bytes[j + 1]);
        output[k] = chars[(b >> 10) & 0x3f];
        output[k + 1] = chars[(b >> 4) & 0x3f];
        output[k + 2] = chars[(b << 2) & 0x3f];
        break;
      default:
        break;
    }

    return output;
  }

}
