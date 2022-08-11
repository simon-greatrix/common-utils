package com.pippsford.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

/**
 * A utility class which allows a symmetric key to be references as a key pair.
 *
 * @author Simon Greatrix on 02/07/2018.
 */
@SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder") // Static and instance methods do not overload each other.
public class SymmetricKey implements SecretKey, PrivateKey, PublicKey {

  public static KeyPair toKeyPair(SecretKey secretKey) {
    return new SymmetricKey(secretKey).toKeyPair();
  }


  public static PrivateKey toPrivateKey(SecretKey secretKey) {
    return new SymmetricKey(secretKey).toKeyPair().getPrivate();
  }


  public static PublicKey toPublicKey(SecretKey secretKey) {
    return new SymmetricKey(secretKey).toKeyPair().getPublic();
  }


  private final SecretKey secretKey;


  public SymmetricKey(byte[] key, String algorithm) {
    secretKey = new SecretKeySpec(key, algorithm);
  }


  public SymmetricKey(SecretKey secretKey) {
    this.secretKey = secretKey;
  }


  @Override
  public void destroy() throws DestroyFailedException {
    secretKey.destroy();
  }


  @Override
  public String getAlgorithm() {
    return secretKey.getAlgorithm();
  }


  @Override
  public byte[] getEncoded() {
    return secretKey.getEncoded();
  }


  @Override
  public String getFormat() {
    return secretKey.getFormat();
  }


  @Override
  public boolean isDestroyed() {
    return secretKey.isDestroyed();
  }


  public KeyPair toKeyPair() {
    return new KeyPair(this, this);
  }

}
