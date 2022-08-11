package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Z-Base32 encoding. This does not support partial octets.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class ZBase32 extends GenericBase32 {

  /**
   * New converter.
   */
  public ZBase32() {
    super(new char[]{
        'y', 'b', 'n', 'd', 'r', 'f', 'g', '8',
        'e', 'j', 'k', 'm', 'c', 'p', 'q', 'x',
        'o', 't', '1', 'u', 'w', 'i', 's', 'z',
        'a', '3', '4', '5', 'h', '7', '6', '9'
    }, ' ', false, false);
  }

}
