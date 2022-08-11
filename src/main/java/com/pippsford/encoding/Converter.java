package com.pippsford.encoding;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface for a class which provides a way of transforming binary data into a safe textual form such as hexadecimal,
 * Base64, or quoted-printable.
 *
 * <p>Implementations should be thread-safe.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public interface Converter {

  /**
   * Remove invalid characters from an encoded form. May also canonicalize the representation if such a concept has
   * meaning.
   *
   * @param text the encoded form
   *
   * @return cleaned up encoding
   */
  @Nullable
  char[] clean(char[] text);


  /**
   * Remove invalid characters from an encoded form. May also canonicalize the representation if such a concept has
   * meaning.
   *
   * @param text the encoded form
   *
   * @return cleaned up encoding
   */
  @Nullable
  default String clean(String text) {
    return text != null ? new String(clean(text.toCharArray())) : null;
  }


  /**
   * Decode the provided textual representation back into binary data.
   *
   * @param text textual representation
   *
   * @return binary data
   */
  @Nullable
  byte[] decode(char[] text);


  /**
   * Decode the provided textual representation back into binary data.
   *
   * @param text textual representation
   *
   * @return binary data
   */
  @Nullable
  default byte[] decode(String text) {
    return (text != null) ? decode(text.toCharArray()) : null;
  }


  /**
   * Encode the provided binary data in a textual form.
   *
   * @param bytes binary data
   *
   * @return textual representation
   */
  @Nullable
  default String encode(byte[] bytes) {
    return (bytes != null) ? new String(encodeChars(bytes)) : null;
  }


  /**
   * Encode the provided binary data in a textual form.
   *
   * @param bytes binary data
   *
   * @return textual representation
   */
  @Nullable
  char[] encodeChars(byte[] bytes);

}
