package com.pippsford.encoding;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
public class Ascii85Test extends Ascii85BToATest {


  @Before
  public void setUp() {
    converter = new Ascii85();
  }


  @Test
  public void testEmpty() {
    assertEquals("<~~>", converter.clean(""));
    assertEquals("<~~>", converter.encode(new byte[0]));
    assertEquals(0, converter.decode("").length);
  }

}
