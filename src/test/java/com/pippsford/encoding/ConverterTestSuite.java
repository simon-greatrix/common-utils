package com.pippsford.encoding;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Simon Greatrix on 16/08/2017.
 */
@RunWith(Suite.class)
@SuiteClasses({
    Ascii85Test.class, Ascii85BToATest.class, Base32Test.class, Base32CrockfordTest.class,
    Base32LowerHexTest.class, Base32HexTest.class, ZBase32Test.class, Base128Test.class, Base64Test.class,
    Base64URLTest.class, HexTest.class, TextToByteTest.class
})
public class ConverterTestSuite {

}
