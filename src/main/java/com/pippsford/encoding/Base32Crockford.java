package com.pippsford.encoding;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Crockford's Base32 encoding. The characters 'o', and 'O' (as in Oscar) are decoded as '0' (zero). The characters 'i',
 * and 'I' (as in India), 'l' and 'L' (as
 * in Lima) are decoded as '1' (one).
 *
 * @author Simon Greatrix
 */
@ThreadSafe
public class Base32Crockford extends GenericBase32 {

  /**
   * Create new encoder.
   */
  public Base32Crockford() {
    super(new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q',
        'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    }, '=', true, false);

    char[] matches = {'o', 'O'};
    for (char ch : matches) {
      for (int i = 0; i < 12; i++) {
        values[i][ch] = values[i]['0'];
      }
    }
    matches = new char[]{'i', 'I', 'L', 'l'};
    for (char ch : matches) {
      for (int i = 0; i < 12; i++) {
        values[i][ch] = values[i]['1'];
      }
    }
  }


  @Nullable
  @Override
  public char[] clean(char[] text) {
    if (text == null) {
      return null;
    }
    char[] cleaned = super.clean(text);
    for (int i = 0; i < cleaned.length; i++) {
      char ch = cleaned[i];
      switch (ch) {
        case 'O':
          cleaned[i] = '0';
          break;
        case 'I': // falls through
        case 'i': // falls through
        case 'L': // falls through
        case 'l': // falls through
          cleaned[i] = '1';
          break;
        default:
          break;
      }
    }

    return cleaned;
  }

}
