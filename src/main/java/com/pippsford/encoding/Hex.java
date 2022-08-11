package com.pippsford.encoding;

import java.nio.CharBuffer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into a hexadecimal format where every byte is represented by two hexadecimal digits.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Hex implements Converter {

  /**
   * Remove invalid characters from an encoded form. May also canonicalize the representation if such a concept has
   * meaning.
   *
   * @param text the encoded form
   *
   * @return cleaned up encoding
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
      char good;
      if ('0' <= ch && ch <= '9') {
        good = ch;
      } else if ('a' <= ch && ch <= 'f') {
        good = ch;
      } else if ('A' <= ch && ch <= 'F') {
        good = (char) (ch + 32);
      } else if ('０' <= ch && ch <= '９') {
        good = (char) (ch - 0xfee0);
      } else if ('ａ' <= ch && ch <= 'ｆ') {
        good = (char) (ch - 0xfee0);
      } else if ('Ａ' <= ch && ch <= 'Ｆ') {
        good = (char) (ch - 0xff00);
      } else {
        good = '!';
      }
      if (good != '!') {
        buf[pos++] = good;
      }
    }
    if (pos % 2 != 0) {
      buf = TextToByte.append(buf, pos++, '0');
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
  public byte[] decode(char[] text) {
    if (text == null) {
      return null;
    }

    int end = TextToByte.removeWhitespaceInPlace(text);
    if (end % 2 != 0) {
      throw new IllegalArgumentException("Input has an odd number of hex digits: " + new String(text));
    }

    return com.pippsford.common.Hex.decode(CharBuffer.wrap(text));
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
    return com.pippsford.common.Hex.encodeChars(bytes);
  }

}
