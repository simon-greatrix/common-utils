package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * RFC-3548 Base32 encoding. This uses the upper case letters 'A' to 'Z' and the digits 2 to 7. Padding is required.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base32 extends GenericBase32 {

  /**
   * Create new encoder.
   */
  public Base32() {
    super(new char[]{
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
        'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
        'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
        'Y', 'Z', '2', '3', '4', '5', '6', '7'
    }, '=', true, true);
  }

}
