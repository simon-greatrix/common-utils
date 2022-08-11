package com.pippsford.encoding;

import static org.junit.Assert.assertNull;

import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 31/07/2017.
 */
public class Base128Test extends ConverterTest {

  @Test
  public void basic() {
    Assert.assertEquals("", TextToByte.BASE128.encode(new byte[0]));
    assertNull(TextToByte.BASE128.encode(null));
    assertNull(TextToByte.BASE128.decode((String) null));

    // 7 bytes goes into 8 characters. 32=4*7+4, so becomes 4*8+4+1.
    Assert.assertEquals(4 * 8 + 5, TextToByte.BASE128.encode(new byte[32]).length());
  }


  @Override
  protected String[] getBadDecodeValues() {
    return new String[0];
  }


  @Override
  protected String[] getCleanTestValues() {
    return new String[]{
        "11€€11",
        "11  !!"
    };
  }


  @Before
  public void setUp() {
    converter = new Base128();
  }


  @Test
  public void test() {
    Random rand = new Random(0x7e57ab1e);
    for (int i = 0; i < 1000; i++) {
      int j = rand.nextInt(256);
      byte[] test = new byte[j];
      rand.nextBytes(test);

      StringBuilder enc = new StringBuilder().append(TextToByte.BASE128.encode(test));
      if (enc.length() == 0) {
        enc.append(' ');
      }
      if (rand.nextBoolean()) {
        int p = rand.nextInt(enc.length());
        enc.insert(p, ' ');
      }
      if (rand.nextBoolean()) {
        int p = rand.nextInt(enc.length());
        enc.insert(p, (char) 0x300);
      }
      if (rand.nextBoolean()) {
        int p = rand.nextInt(enc.length());
        enc.insert(p, '\n');
      }
      byte[] out = TextToByte.BASE128.decode(enc.toString());
      Assert.assertEquals(HexDump.dump(test), HexDump.dump(out));
    }
  }

}
