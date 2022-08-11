package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
public class Base32LowerHexTest extends Base32Test {

  @Before
  public void setUp() {
    converter = new Base32LowerHex();
  }

}
