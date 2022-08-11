package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Base32 "Hex" encoding.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base32Hex extends GenericBase32 {

  /**
   * Create new encoder.
   */
  public Base32Hex() {
    super(new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V'
    }, '=', true, false);
  }

}
