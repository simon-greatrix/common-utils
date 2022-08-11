package com.pippsford.encoding;


import java.nio.ByteBuffer;

/**
 * Stringify a binary data block.
 *
 * @author Simon Greatrix
 */
public class HexDump {

  private static final String ALL_ZERO = String
      .format("%%06x  %-50s  %-17s", " 00 00 00 00 00 00 00 00   00 00 00 00 00 00 00 00",
          "........ ........"
      );

  private static final String NL = "\n";


  private static void doZeros(StringBuilder buf, int allZeroCount, int end) {
    if (allZeroCount == 0) {
      return;
    }

    // log first line of all zeros normally
    buf.append(String.format(ALL_ZERO, end - 16 * allZeroCount)).append(NL);

    // if 4 or more lines, we skip some
    if (allZeroCount >= 4) {
      buf.append(String
          .format("%6s  %-50s  %-17s", "******", " (Skipping " + (allZeroCount - 2) + " lines)",
              "*"
          )).append(NL);
    }

    if (allZeroCount == 3) {
      // if exactly 3 lines, not worth skipping
      buf.append(String.format(ALL_ZERO, end - 32)).append(NL);
    }

    if (allZeroCount > 2) {
      // include last line
      buf.append(String.format(ALL_ZERO, end - 16)).append(NL);
    }
  }


  /**
   * Dump a binary buffer to a nice text format.
   *
   * @param buf the buffer
   *
   * @return nicely formatted text
   */
  public static String dump(ByteBuffer buf) {
    if (buf == null) {
      return "null";
    }
    byte[] arr = new byte[buf.remaining()];
    int pos = buf.position();
    buf.get(arr);
    buf.position(pos);
    return dump(arr, 0, arr.length);
  }


  /**
   * Dump a binary buffer to a nice text format.
   *
   * @param buf the buffer
   * @param off where to start printing. If out of range, starts at beginning of array.
   * @param len number of bytes to print. If negative or too large, runs to end of array.
   *
   * @return nicely formatted text
   */
  // Sonarqube complains about the complexity of this method.
  @SuppressWarnings("squid:S3776")
  public static String dump(byte[] buf, int off, int len) {
    if (buf == null) {
      return "null";
    }
    int o = Math.max(0, off);
    int l = Math.max(0, Math.min(len, buf.length - o));

    StringBuilder buffer = new StringBuilder();
    buffer.append(NL);
    StringBuilder bufBytes = new StringBuilder();
    StringBuilder bufChars = new StringBuilder();

    int allZeroCount = 0;
    boolean seenNotZero = true;
    for (int i = 0; i < l; i++) {
      int v = 0xff & buf[i + o];
      if (v != 0) {
        seenNotZero = true;
      }
      bufBytes.append(String.format(" %02x", v));
      bufChars.append(String.format(
          "%c",
          ((32 <= v) && (v <= 126)) ? Character.valueOf((char) v)
              : Character.valueOf('.')
      ));
      if ((i % 16) == 7) {
        // half way break
        bufBytes.append("  ");
        bufChars.append(" ");
      } else if ((i % 16) == 15) {
        // end of line
        if (seenNotZero) {
          doZeros(buffer, allZeroCount, i - 15);
          buffer.append(String.format("%06x  %-50s  %-17s", i - 15,
              bufBytes, bufChars
          )).append(NL);
          allZeroCount = 0;
          seenNotZero = false;
        } else {
          allZeroCount++;
        }
        bufBytes.setLength(0);
        bufChars.setLength(0);
      }
    }

    // add last line
    doZeros(buffer, allZeroCount, 16 * (l / 16));
    if (l % 16 != 0) {
      buffer.append(String.format("%06x  %-50s  %-17s",
          16 * (l / 16), bufBytes,
          bufChars
      )).append(NL);
    }

    // ensure output ready and return
    return buffer.toString();
  }


  /**
   * Dump a binary buffer to a nice text format.
   *
   * @param buf the buffer
   *
   * @return nicely formatted text
   */
  public static String dump(byte[] buf) {
    if (buf == null) {
      return "null";
    }
    return dump(buf, 0, buf.length);
  }


  private HexDump() {
    // unused
  }

}
