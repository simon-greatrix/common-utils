package com.pippsford.encoding;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into a Ascii85 encoded form where every four bytes of data is encoded as five characters.
 *
 * <p>See <a href="http://en.wikipedia.org/wiki/Ascii85">Wikipedia description of Ascii85 encoding</a>
 *
 * <p>Note Ascii85 uses many characters which are considered special in some systems. For maximum portability, use Base64 instead.
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Ascii85 extends GenericAscii85 {

  public Ascii85() {
    super(true, false, true);
  }

}
