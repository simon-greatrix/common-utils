package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into a Base64 encoded form where every three bytes of data is encoded as four characters. This scheme is URL and filename safe and uses '-'
 * for 62, '_' for 63, and '=' for the optional padding.
 *
 * <p>See <a href="http://en.wikipedia.org/wiki/Base64">Wikipedia description of Base64 encoding</a>
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base64URL extends GenericBase64 {

  public Base64URL() {
    super('-', '_', '=', false);
  }

}
