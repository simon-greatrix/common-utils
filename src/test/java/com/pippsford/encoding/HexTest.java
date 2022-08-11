package com.pippsford.encoding;

import org.junit.Before;

/**
 * @author Simon Greatrix on 18/08/2017.
 */
public class HexTest extends ConverterTest {

  @Override
  protected String[] getBadDecodeValues() {
    return new String[]{
        "ab~d",
        "abc~",
        "abc"
    };
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]{"123", "1234abCD", "123zyx  xyz321", "０１２８９ＡＤＦａｄｆ"};
  }


  @Before
  public void setUp() {
    converter = new Hex();
  }

}
