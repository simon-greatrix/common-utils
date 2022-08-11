package com.pippsford.encoding;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 22/08/2017.
 */
public class Base58Test extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[0];
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[0];
  }


  @Before
  public void setUp() {
    converter = new Base58();
  }


  @Test
  public void testZeros() {

  }

}
