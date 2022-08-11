package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into the b-to-a Ascii85 encoded form where every four bytes of data is encoded as five characters. The btoa form uses 'z' to represent
 * 0x00000000, 'y' to represent 0x20202020, and does not quote the text.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Ascii85BToA extends GenericAscii85 {

  public Ascii85BToA() {
    super(true, true, false);
  }

}
