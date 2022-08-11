package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
public class Ascii85BToATest extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[]{
        "€2345",
        "x2345",
        "12x45",
        "12€45",
        "1"
    };
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]{
        "  <~~>  ",
        "<~123 45     ",
        "12345uuuUU12345",
        "zyzy!!!!!+<VdL",
        "!!!!!+<VdLzz",
        "!!!!!!"
    };
  }


  @Before
  public void setUp() {
    converter = new Ascii85BToA();
  }

}
