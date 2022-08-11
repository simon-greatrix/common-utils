package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 18/08/2017.
 */
public class Base64URLTest extends ConverterTest {

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
    converter = new Base64URL();
  }

}
