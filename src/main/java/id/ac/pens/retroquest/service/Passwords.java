package id.ac.pens.retroquest.service;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private Passwords() {}

  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "pbkdf2$210000$"
        + Base64.getEncoder().encodeToString(salt)
        + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, 210000));
  }

  public static boolean verify(String password, String encoded) {
    try {
      var p = encoded.split("\\$");
      return p.length == 4
          && p[0].equals("pbkdf2")
          && MessageDigest.isEqual(
              Base64.getDecoder().decode(p[3]),
              derive(password, Base64.getDecoder().decode(p[2]), Integer.parseInt(p[1])));
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int rounds) {
    var spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    } finally {
      spec.clearPassword();
    }
  }
}
