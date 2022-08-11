package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into a Base64 encoded form where every three bytes of data is encoded as four characters. This scheme uses '+' for 62, '/' for 63, and '=' for
 * the required padding.
 *
 * <p>See <a href="http://en.wikipedia.org/wiki/Base64">Wikipedia description of Base64 encoding</a>
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base64 extends GenericBase64 {

  public Base64() {
    super('+', '/', '=', true);
  }

}
