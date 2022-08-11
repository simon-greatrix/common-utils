package com.pippsford.encoding;


import java.util.Arrays;

/**
 * Text to Byte conversion for text encoded data.
 *
 * @author Simon Greatrix
 */
public class TextToByte {

  /**
   * Common ASCII85 Converter.
   */
  public static final Ascii85 ASCII85 = new Ascii85();

  /**
   * Common ASCII85 B-to-A Converter.
   */
  public static final Ascii85BToA ASCII85_B_TO_A = new Ascii85BToA();

  /**
   * Common Base128 Converter.
   */
  public static final Base128 BASE128 = new Base128();

  /**
   * Common Base32 Converter.
   */
  public static final Base32 BASE32 = new Base32();

  /**
   * Common Crockford's Base32 Converter.
   */
  public static final Base32Crockford BASE32_CROCKFORD = new Base32Crockford();

  /**
   * Common Base32 Hex Converter.
   */
  public static final Base32Hex BASE32_HEX = new Base32Hex();

  /**
   * Common Base64 Converter.
   */
  public static final Base64 BASE64 = new Base64();

  /**
   * Common Base64URL Converter.
   */
  public static final Base64URL BASE64URL = new Base64URL();

  /**
   * Common Hex Converter.
   */
  public static final Hex HEX = new Hex();

  /**
   * Common Z-Base32 Hex Converter.
   */
  public static final ZBase32 ZBASE32 = new ZBase32();


  static char[] append(char[] text, int pos, char ch) {
    if (pos < text.length) {
      text[pos] = ch;
      return text;
    }
    char[] newText = new char[pos + 8];
    System.arraycopy(text, 0, newText, 0, pos);
    Arrays.fill(text, (char) 0);
    newText[pos] = ch;
    return newText;
  }


  /**
   * Remove all whitespace from an encoded form.
   *
   * @param text encoded form
   *
   * @return a new array containing the encoded form without whitespace
   */
  public static char[] removeWhitespace(char[] text) {
    char[] buf = text.clone();
    int newLength = removeWhitespaceInPlace(buf);
    if (newLength == text.length) {
      return buf;
    }

    char[] ret = new char[newLength];
    System.arraycopy(buf, 0, ret, 0, newLength);
    Arrays.fill(buf, (char) 0);
    return ret;
  }


  /**
   * Remove all whitespace from an encoded form.
   *
   * @param text encoded form
   *
   * @return encoded form without whitespace
   */
  public static String removeWhitespace(String text) {
    char[] chars = text.toCharArray();
    int newLength = removeWhitespaceInPlace(chars);

    String out = text;
    if (text.length() != newLength) {
      out = new String(chars, 0, newLength);
    }
    Arrays.fill(chars, (char) 0);
    return out;
  }


  /**
   * Compact text in place by removing whitespace.
   *
   * @param text the text to compact
   *
   * @return the number of characters remaining after compaction.
   */
  public static int removeWhitespaceInPlace(char[] text) {
    int j = 0;
    for (int i = 0; i < text.length; i++) {
      char ch = text[i];
      if (Character.isWhitespace(ch)) {
        continue;
      }
      text[j] = ch;
      j++;
    }
    int newLength = j;
    while (j < text.length) {
      text[j] = ' ';
      j++;
    }
    return newLength;
  }


  static char[] trim(char[] buf, int pos) {
    if (pos == buf.length) {
      return buf;
    }

    char[] newBuf = new char[pos];
    System.arraycopy(buf, 0, newBuf, 0, pos);
    Arrays.fill(buf, (char) 0);
    return newBuf;
  }


  /**
   * Unnecessary constructor as required by style guidelines.
   */
  TextToByte() {
    // required by style guidelines.
  }

}
