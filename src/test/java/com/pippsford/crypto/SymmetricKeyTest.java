package com.pippsford.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/02/2020.
 */
public class SymmetricKeyTest {

  SecretKey key = new SecretKeySpec("A secret key".getBytes(StandardCharsets.US_ASCII), "TEST");


  @Test
  public void destroy() throws DestroyFailedException {
    SymmetricKey symmetricKey = new SymmetricKey(key);
    try {
      symmetricKey.destroy();
      assertTrue(symmetricKey.isDestroyed());
    } catch (DestroyFailedException e) {
      // ah well, we tried
    }
  }


  @Test
  public void getAlgorithm() {
    assertEquals("TEST", new SymmetricKey(key).getAlgorithm());
  }


  @Test
  public void getEncoded() {
    byte[] array = key.getEncoded();
    SymmetricKey symmetricKey = new SymmetricKey(key);
    assertArrayEquals(array, symmetricKey.getEncoded());
  }


  @Test
  public void getFormat() {
    assertEquals("RAW", new SymmetricKey(key).getFormat());
  }


  @Test
  public void isDestroyed() {
    assertFalse(new SymmetricKey(key).isDestroyed());
  }


  @Test
  public void toKeyPair() {
    KeyPair keyPair = SymmetricKey.toKeyPair(key);
    assertNotNull(keyPair.getPrivate());
    assertNotNull(keyPair.getPublic());
  }


  @Test
  public void toPrivateKey() {
    assertNotNull(SymmetricKey.toPrivateKey(key));
  }


  @Test
  public void toPublicKey() {
    assertNotNull(SymmetricKey.toPublicKey(key));
  }
}