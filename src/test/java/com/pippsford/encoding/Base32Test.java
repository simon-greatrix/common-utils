package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
public class Base32Test extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[]{
        "1",
        "333",
        "666666",
        "€€€€€€€€",
        "]]]]]]]]"

    };
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]{
        "aaaa",
        "AAaa",
        "aaaab"
    };
  }


  @Before
  public void setUp() {
    converter = new Base32();
  }

}
