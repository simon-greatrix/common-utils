package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Base32 "Hex" encoding, using lower case letters.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base32LowerHex extends GenericBase32 {

  /**
   * Create new encoder.
   */
  public Base32LowerHex() {
    super(new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
        'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v'
    }, '=', false, false);
  }

}
