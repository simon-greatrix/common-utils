package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 18/08/2017.
 */
public class Base64Test extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[]{"@", "333~", "33~3", "3~33", "~333"};
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]{"3"};
  }


  @Before
  public void setUp() {
    converter = new Base64();
  }

}
