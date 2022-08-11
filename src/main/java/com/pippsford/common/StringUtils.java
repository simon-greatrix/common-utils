package com.pippsford.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for common String functions.
 */
public class StringUtils {

  private static final String NL = System.getProperty("line.separator");

  private static final char UNKNOWN = (char) 0xfffd;


  /**
   * cleanString.
   * <p>Asset Identifiers may not contain '|' or '*' characters.
   * '|' is used as the separator between Namespace and Class IDs whilst
   * '*' interferes with wildcard matching for POAs.</p>
   *
   * @param toClean :
   *
   * @return :
   */
  @Nonnull
  public static String cleanString(String toClean) {

    String rVal = (toClean == null ? "" : toClean);

    if (rVal.contains("|")) {
      rVal = rVal.replace('|', ' ');
    }

    if (rVal.contains("*")) {
      rVal = rVal.replace('*', '+');
    }

    return rVal;
  }


  /**
   * Prevent CRLF injection. We may actually want a multi-line log message so our current implementation is to indent subsequent lines. We also replace
   * control codes and similar with the unicode replacement character.
   *
   * @param input the text to make safe for a log
   *
   * @return safe text
   */
  @SuppressWarnings("squid:S3776") // No refactoring is known to simplify this
  @Nonnull
  public static String logSafe(@Nullable String input) {
    if (input == null) {
      return "[null]";
    }

    // check for dodgy characters
    boolean isSafe = true;
    int len = input.length();
    for (int i = 0; i < len; i++) {
      char ch = input.charAt(i);
      int type = Character.getType(ch);
      if (type == Character.CONTROL || type == Character.SURROGATE || type == Character.PRIVATE_USE || type == Character.UNASSIGNED) {
        isSafe = false;
        break;
      }
    }

    // We are OK with surrogate pairs that indicate valid characters, but we want to keep the check above as simple as possible.
    if (isSafe) {
      return input;
    }

    StringBuilder buffer = new StringBuilder(len);

    int pos = 0;
    int horiz = 0;
    boolean lastWasEOL = false;

    while (pos < len) {
      if (lastWasEOL) {
        buffer.append("|       ");
        horiz = 0;
        lastWasEOL = false;
      }
      char ch = input.charAt(pos++);
      if (Character.isHighSurrogate(ch)) {
        char ch2 = (pos < len) ? input.charAt(pos) : 'x';
        if (Character.isLowSurrogate(ch2)) {
          // valid surrogate pair
          buffer.append(ch).append(ch2);
          pos++;
        } else {
          // orphaned high surrogate
          buffer.append(UNKNOWN);
        }
        horiz++;
      } else if (ch == '\n') {
        // handle LF+CR
        if (pos < len && input.charAt(pos) == '\r') {
          pos++;
        }
        // remove indenting if this is an empty line
        if (horiz == 0) {
          buffer.setLength(Math.max(7, buffer.length()) - 7);
        }
        buffer.append(NL);
        lastWasEOL = true;
      } else if (ch == '\r') {
        // handle CR+LF
        if (pos < len && input.charAt(pos) == '\n') {
          pos++;
        }
        // remove indenting if this is an empty line
        if (horiz == 0) {
          buffer.setLength(Math.max(7, buffer.length()) - 7);
        }
        buffer.append(NL);
        lastWasEOL = true;
      } else if (ch == '\t') {
        int tab = 8 - (horiz % 8);
        buffer.append(" ".repeat(Math.max(0, tab)));
        horiz += tab;
      } else {
        int type = Character.getType(ch);
        if (type == Character.CONTROL || type == Character.SURROGATE || type == Character.PRIVATE_USE || type == Character.UNASSIGNED) {
          buffer.append(UNKNOWN);
        } else {
          buffer.append(ch);
        }
        horiz++;
      }
    }

    return buffer.toString();
  }


  /**
   * matchString(). Test string vs classic Windows-style pattern match (i.e. * and ? wildcard only, NOT REGEX).
   *
   * @param item    :
   * @param pattern :
   *
   * @return : Boolean.
   */
  @SuppressWarnings("squid:S3776") // No refactoring is known to simplify this
  public static boolean matchString(String item, String pattern) {

    // Nulls return false.

    if ((item == null) || (pattern == null)) {
      return false;
    }

    // Initialise

    int itemLength = item.length();
    int patternLength = pattern.length();
    int ic = 0;
    int pc = 0;
    int backtrack = -1;
    boolean inStar = false; // Processing a '*' pattern sequence.
    int qCount = 0; // Number of outstanding '?' characters to process.

    // Zero length pattern.

    if (patternLength == 0) {
      return (itemLength == 0);
    }

    // OK...

    while (true) {

      // Pattern wildcard sequence...

      if ((pattern.charAt(pc) == '*') || (pattern.charAt(pc) == '?')) {
        // '*' indicates 'inStar' sequence, '?' increments 'qCount' a character count the must be present.

        do {
          if (pattern.charAt(pc) == '?') {
            qCount++;
          } else {
            inStar = true;
          }
          pc++;
        } while ((pc < patternLength) && ((pattern.charAt(pc) == '*') || (pattern.charAt(pc) == '?')));

        if (inStar) {
          backtrack = pc;
        }

      } else if ((ic < itemLength) && (pattern.charAt(pc) == item.charAt(ic))) {
        // Character match. All good, but ends an 'inStar' sequence.

        inStar = false;

        do {
          pc++;
          ic++;
        } while ((pc < patternLength) && (ic < itemLength) && (pattern.charAt(pc) == item.charAt(ic)));

      } else if (inStar) {
        // 'inStar', proceed until either we run out of item or get a character match.
        inStar = false;

        do {
          ic++;
        } while ((ic < itemLength) && (pattern.charAt(pc) != item.charAt(ic)));
      } else {

        if ((ic < itemLength) && (backtrack >= 0)) {
          inStar = true;
          pc = backtrack;
        } else {
          // We've run out of options, exit false.
          return false;
        }
      }

      // '?'s happened
      if (qCount > 0) {
        ic += qCount;
        qCount = 0;
        if (ic > itemLength) {
          return false;
        }
      }

      // Done ?
      if (pc == patternLength) {
        return ((inStar) || (ic == itemLength));
      }

    }

  }


  /**
   * Replace nulls with an empty string to ensure the result is non-null.
   *
   * @param s possibly null string
   *
   * @return input or empty string
   */
  @Nonnull
  public static String notNull(@Nullable String s) {
    return s != null ? s : "";
  }


  private StringUtils() {
    // do nothing
  }

}
