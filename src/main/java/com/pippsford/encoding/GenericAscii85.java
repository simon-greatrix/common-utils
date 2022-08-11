package com.pippsford.encoding;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Convert data into a Ascii85 encoded form where every four bytes of data is encoded as five characters.
 *
 * <p>See <a href="http://en.wikipedia.org/wiki/Ascii85">Wikipedia description of Ascii85 encoding</a>
 *
 * <p>Note Ascii85 uses many characters which are considered special in some systems. For maximum portability, use
 * Base64 instead.
 *
 * @author Simon Greatrix
 */
// Suppressing "cognitive complexity" warnings as data validation requires a lot of if-then constructs
// which leads to a high cognitive complexity. Sometimes there are just a lot of rules to check.
@SuppressWarnings("squid:S3776")
@ThreadSafe
public class GenericAscii85 implements Converter {

  /**
   * The encoding can be quoted between "&lt;~" and "~&gt;".
   */
  final boolean useQuotes;

  /**
   * A 'y' can represent 4 consecutive spaces (0x20).
   */
  final boolean useY;

  /**
   * A 'z' can represent 4 consecutive zero bytes.
   */
  final boolean useZ;


  /**
   * Create an Ascii 85 converter.
   *
   * @param useZ      does 'z' represent four consecutive zeros?
   * @param useY      does 'y' represent four consecutive spaces (0x20)?
   * @param useQuotes is the canonical form quoted between "&lt;~" and "~&gt;"?
   */
  public GenericAscii85(boolean useZ, boolean useY, boolean useQuotes) {
    this.useZ = useZ;
    this.useY = useY;
    this.useQuotes = useQuotes;
  }


  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("squid:S135")
  @Nullable
  @Override
  public char[] clean(char[] text) {
    if (text == null) {
      return null;
    }
    int start = 0;
    int end = text.length;
    int p = indexOf(text, 0, '<', '~');
    if (p != -1) {
      start = p + 2;
    }
    p = indexOf(text, start, '~', '>');
    if (p != -1) {
      end = p;
    }

    char[] buf = new char[end - start];
    int pos = 0;
    if (useQuotes) {
      buf = TextToByte.append(buf, pos++, '<');
      buf = TextToByte.append(buf, pos++, '~');
    }

    int j = 0;
    long v = 0;
    for (int i = start; i < end; i++) {
      char ch = text[i];
      if (j == 0) {
        if (ch == 'z' && useZ) {
          buf = TextToByte.append(buf, pos++, 'z');
          continue;
        }
        if (ch == 'y' && useY) {
          buf = TextToByte.append(buf, pos++, 'y');
          continue;
        }
      }

      if (ch < '!' || ch > 'u') {
        // bad character, skip
        continue;
      }

      buf = TextToByte.append(buf, pos++, ch);
      j++;
      v = v * 85 + (ch - '!');
      if (j == 5) {
        // remove invalid patterns
        if (v >= 0x100000000L) {
          pos -= 5;
        }
        if (v == 0 && useZ) {
          pos -= 5;
          buf = TextToByte.append(buf, pos++, 'z');
        }
        if (v == 0x20202020 && useY) {
          pos -= 5;
          buf = TextToByte.append(buf, pos++, 'y');
        }
        j = 0;
        v = 0;
      }
    }

    if (j == 1) {
      // trailing character, remove it
      pos--;
    } else if (j > 1) {
      // Re-encode final bytes to ensure correct format
      for (int i = j; i <= 4; i++) {
        v *= 85;
      }
      for (int i = j; i <= 4; i++) {
        v |= 0xff << (8 * (i - j));
      }

      if (v >= 0x100000000L) {
        // final bytes would have been invalid, so remove
        pos -= j;
      } else {
        // re-encode final bytes
        for (int i = j; i <= 4; i++) {
          v /= 85;
        }
        for (int i = 1; i <= j; i++) {
          buf[pos - i] = (char) ('!' + v % 85);
          v /= 85;
        }
      }
    }

    if (useQuotes) {
      buf = TextToByte.append(buf, pos++, '~');
      buf = TextToByte.append(buf, pos++, '>');
    }

    return TextToByte.trim(buf, pos);
  }


  /**
   * Decode the provided textual representation back into binary data.
   *
   * <p>The footer of ~&gt; may be included in the input. Any characters after the first footer will be ignored.
   *
   * <p>The header of &lt;~ may be included in the input. Any characters before the header will be ignored.
   *
   * @param text textual representation
   *
   * @return binary data
   */
  @Override
  @Nullable
  public byte[] decode(char[] text) {
    if (text == null) {
      return null;
    }

    // strip out all whitespace
    int start = 0;
    int end = TextToByte.removeWhitespaceInPlace(text);

    int p = indexOf(text, 0, '<', '~');
    if (p != -1) {
      start = p + 2;
    }
    p = indexOf(text, start, '~', '>');
    if (p != -1) {
      end = p;
    }

    // get data length
    long block = 0;
    int len = 0;
    int j = 0;
    for (int i = start; i < end; i++) {
      char ch = text[i];
      if (j == 0) {
        if ((useZ && ch == 'z') || (useY && ch == 'y')) {
          // z is 4 zeroes
          // y is 4 spaces
          len += 4;
        } else {
          if ((ch < '!' || ch > 'u')) {
            throw new IllegalArgumentException(
                "Invalid ASCII85. Bad character 0x"
                    + Integer.toHexString(ch)
                    + " in input");
          }
          j++;
          block = (long) ch - '!';
        }
      } else {
        if ((ch < '!' || ch > 'u')) {
          throw new IllegalArgumentException(
              "Invalid ASCII85. Bad character 0x"
                  + Integer.toHexString(ch) + " in input");
        }
        len++;
        j++;
        block = (block * 85) + (ch - '!');
        if (block >= 0x100000000L) {
          throw new IllegalArgumentException(
              "Invalid ASCII85. Bad 5 character sequence \"" + new String(text, i - 4, 5) + "\" in input");
        }
        if (j == 5) {
          j = 0;
        }
      }
    }

    if (j == 1) {
      throw new IllegalArgumentException(
          "Invalid ASCII85. Only one character in final 5-character block of: "
              + new String(text));
    }

    byte[] data = new byte[len];
    j = 0;
    len = 0;
    block = 0;
    for (int i = start; i < end; i++) {
      char ch = text[i];
      if (j == 0) {
        if (ch == 'z') {
          // z is 4 zeroes
          data[len] = 0;
          data[len + 1] = 0;
          data[len + 2] = 0;
          data[len + 3] = 0;
          len += 4;
        } else if (ch == 'y') {
          // y is 4 spaces
          data[len] = 0x20;
          data[len + 1] = 0x20;
          data[len + 2] = 0x20;
          data[len + 3] = 0x20;
          len += 4;
        } else {
          block = (long) ch - '!';
          j++;
        }
      } else {
        block = (block * 85) + (ch - '!');
        j++;
        if (j == 5) {
          j = 0;
          data[len] = (byte) ((block & 0xff000000) >> 24);
          len++;
          data[len] = (byte) ((block & 0x00ff0000) >> 16);
          len++;
          data[len] = (byte) ((block & 0x0000ff00) >> 8);
          len++;
          data[len] = (byte) (block & 0x000000ff);
          len++;
          block = 0;
        }
      }
    }

    switch (j) {
      case 2:
        block = block * 85 * 85 * 85;
        data[len] = (byte) ((block & 0xff000000) >> 24);
        break;
      case 3:
        block = block * 85 * 85;
        data[len] = (byte) ((block & 0xff000000) >> 24);
        len++;
        data[len] = (byte) ((block & 0x00ff0000) >> 16);
        break;
      case 4:
        block = block * 85;
        data[len] = (byte) ((block & 0xff000000) >> 24);
        len++;
        data[len] = (byte) ((block & 0x00ff0000) >> 16);
        len++;
        data[len] = (byte) ((block & 0x0000ff00) >> 8);
        break;
      default:
        break;
    }
    return data;
  }


  /**
   * Encode the provided binary data in a textual form.
   *
   * @param bytes binary data
   *
   * @return textual representation
   */
  @Nullable
  @Override
  public char[] encodeChars(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // every 4 bytes requires 5 characters of output
    int fullBlocks = bytes.length / 4;
    int extraBytes = bytes.length - 4 * fullBlocks;
    int extraChars;
    switch (extraBytes) {
      case 0:
        extraChars = 0;
        break;
      case 1:
        extraChars = 3;
        break; // need one spare
      default:
        extraChars = extraBytes + 1;
        break;
    }

    // output size is 5 chars per full block, some extra chars for the
    // last block, and 4 chars for header and footer
    char[] output = new char[5 * fullBlocks + extraChars + (useQuotes ? 4 : 0)];

    // set header
    int j = 0;
    if (useQuotes) {
      output[0] = '<';
      output[1] = '~';
      j = 2;
    }
    int k = 0;
    for (int i = 0; i < fullBlocks; i++) {
      j += getBlock(output, j, bytes[k], bytes[k + 1], bytes[k + 2],
          bytes[k + 3]
      );
      k += 4;
    }

    // the final block consists of one more character than there are
    // bytes, without the special 'z' for all zero.
    switch (extraBytes) {
      case 1:
        getFinalBlock(output, j, 1, bytes[k], (byte) 0xff, (byte) 0xff);
        j += 2;
        break;
      case 2:
        getFinalBlock(output, j, 2, bytes[k], bytes[k + 1], (byte) 0xff);
        j += 3;
        break;
      case 3:
        getFinalBlock(output, j, 3, bytes[k], bytes[k + 1], bytes[k + 2]);
        j += 4;
        break;
      default:
        break;
    }

    // output footer
    if (useQuotes) {
      output[j] = '~';
      output[j + 1] = '>';
      j += 2;
    }

    return TextToByte.trim(output, j);
  }


  private int getBlock(
      char[] output, int offset, byte b0, byte b1,
      byte b2, byte b3
  ) {
    long v = (((long) (0xff & b0)) << 24) | ((0xff & b1) << 16)
        | ((0xff & b2) << 8) | (0xff & b3);

    if (v == 0 && useZ) {
      output[offset] = 'z';
      return 1;
    }

    if (v == 0x20202020 && useY) {
      output[offset] = 'y';
      return 1;
    }

    for (int i = 4; i >= 0; i--) {
      output[offset + i] = (char) ('!' + v % 85);
      v /= 85;
    }
    return 5;
  }


  private void getFinalBlock(
      char[] output, int offset, int count, byte b0, byte b1,
      byte b2
  ) {
    long v = (((long) (0xff & b0)) << 24) | ((0xff & b1) << 16)
        | ((0xff & b2) << 8) | 0xff;

    for (int i = 4; i > count; i--) {
      v /= 85;
    }
    for (int i = count; i >= 0; i--) {
      output[offset + i] = (char) ('!' + v % 85);
      v /= 85;
    }
  }


  /**
   * Find two adjacent characters in the text.
   *
   * @param text  the text
   * @param start where to start
   * @param c1    first character
   * @param c2    second character
   *
   * @return location, or -1
   */
  private int indexOf(char[] text, int start, char c1, char c2) {
    int len = text.length - 1;
    for (int i = start; i < len; i++) {
      if (text[i] == c1 && text[i + 1] == c2) {
        return i;
      }
    }
    return -1;
  }

}
