package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 18/08/2017.
 */
public class Base32CrockfordTest extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[0];
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]
        {"oO00Oo00", "1LlI1LI1", "\"€2345\"1234"};
  }


  @Before
  public void setUp() {
    converter = new Base32Crockford();
  }

}
