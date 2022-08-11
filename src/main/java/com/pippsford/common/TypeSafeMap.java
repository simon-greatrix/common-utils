package com.pippsford.common;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A map with type specific getters.
 *
 * @author Simon Greatrix on 30/08/2017.
 */
public class TypeSafeMap extends AbstractMap<String, Object> {

  /**
   * Convert an object to a {@link BigDecimal}.
   *
   * @param o the object
   *
   * @return the number as a BigDecimal
   */
  public static BigDecimal asBigDecimal(Object o) {
    if (o instanceof Number) {
      Number n = (Number) o;
      if (n instanceof BigDecimal) {
        return (BigDecimal) n;
      }
      if (n instanceof BigInteger) {
        return new BigDecimal((BigInteger) n);
      }
      if (n instanceof Float || n instanceof Double) {
        return new BigDecimal(n.toString());
      }

      // Assume it is some kind of integer
      return BigDecimal.valueOf(n.longValue());
    }

    if (o instanceof String) {
      try {
        return new BigDecimal((String) o);
      } catch (NumberFormatException e) {
        // not a number
        return null;
      }
    }

    if (o instanceof byte[]) {
      return new BigDecimal(new BigInteger((byte[]) o));
    }

    // No conversion possible
    return null;
  }


  /**
   * Convert an object to a {@link BigInteger}.
   *
   * @param o the object
   *
   * @return the object as a BigInteger
   */
  public static BigInteger asBigInteger(Object o) {
    if (o instanceof Number) {
      Number n = (Number) o;
      if (n instanceof BigInteger) {
        return (BigInteger) n;
      }
      if (n instanceof BigDecimal) {
        return ((BigDecimal) n).toBigInteger();
      }
      if (n instanceof Float || n instanceof Double) {
        return new BigDecimal(n.toString()).toBigInteger();
      }

      // Assume it is some kind of integer
      return BigInteger.valueOf(n.longValue());
    }

    if (o instanceof String) {
      try {
        return new BigInteger((String) o);
      } catch (NumberFormatException e) {
        // not a number
        return null;
      }
    }

    if (o instanceof byte[]) {
      return new BigInteger((byte[]) o);
    }

    // No conversion possible
    return null;
  }


  /**
   * Convert any object to a boolean, using standard rules for what is "truth-y" and "false-y".
   *
   * @param o the object
   *
   * @return a boolean
   */
  public static boolean asBoolean(Object o) {
    // null is false-y
    if (o == null) {
      return false;
    }

    // Boolean is obvious
    if (o instanceof Boolean) {
      return ((Boolean) o);
    }
    if (o instanceof AtomicBoolean) {
      return ((AtomicBoolean) o).get();
    }

    // Empty string and "false" are false-y
    if (o instanceof String) {
      String s = (String) o;
      return !s.isEmpty() && !s.equalsIgnoreCase("false");
    }

    // Zero and NaN are false-y, and everything else is true-y
    if (!(o instanceof Number)) {
      // Not some kind of number, so must be true-y
      return true;
    }

    // Java has 8 standard number types. 4 of them are simple integers, but the others require special handling.
    Number n = (Number) o;

    // A Float and Double may be NaN
    if (n instanceof Double || n instanceof Float) {
      // Thanks to IEEE-754, NaN is neither less than nor greater than any other number, so this matches both 0 and NaN:
      double d = n.doubleValue();
      return ((d < 0) || (d > 0));
    }

    // Big types have to be compared to zero
    if (n instanceof BigInteger) {
      return ((BigInteger) n).signum() != 0;
    }
    if (n instanceof BigDecimal) {
      // NB: BigDecimal does not support NaN, so only need to compare to zero.
      return ((BigDecimal) n).signum() != 0;
    }

    // It's a simple integer
    return n.longValue() != 0;
  }


  /**
   * Convert any object to a double.
   *
   * @param n the object
   *
   * @return the Double, or null if no conversion possible.
   */
  public static Double asDouble(Object n) {
    if (n instanceof Double) {
      return (Double) n;
    }
    if (n instanceof Number) {
      return ((Number) n).doubleValue();
    }
    if (n instanceof String) {
      try {
        return Double.valueOf((String) n);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }


  /**
   * Convert any object to an integer.
   *
   * @param n the object
   *
   * @return the Integer, or null if no conversion possible
   */
  public static Integer asInt(Object n) {
    if (n instanceof Integer) {
      return (Integer) n;
    }
    if (n instanceof Number) {
      return ((Number) n).intValue();
    }
    if (n instanceof String) {
      try {
        return Integer.decode((String) n);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }


  /**
   * Convert any object to a type safe list. If the input is a collection, list, array, or singleton, it is converted
   * into a List. If necessary, all values in
   * the list are type checked.
   *
   * @param function a function that can be used to convert values (may be null)
   * @param o        the object to convert
   * @param <T>      the required member class
   *
   * @return the list, or null if missing
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> TypeSafeList<T> asList(Function<Object, T> function, Object o) {
    if (o == null) {
      return null;
    }

    if (o instanceof TypeSafeList && ((TypeSafeList<?>) o).getConvertor().equals(function)) {
      return (TypeSafeList<T>) o;
    }

    if (o instanceof Collection<?>) {
      Collection<?> c = (Collection<?>) o;
      TypeSafeList<T> tsl = new TypeSafeList<>(new ArrayList<T>(c.size()), function);
      try {
        for (Object v : c) {
          tsl.add(function.apply(v));
        }
      } catch (ClassCastException e) {
        return null;
      }
      return tsl;
    }

    if (o.getClass().isArray()) {
      // an array
      int s = Array.getLength(o);
      TypeSafeList<T> tsl = new TypeSafeList<>(new ArrayList<>(s), function);
      for (int i = 0; i < s; i++) {
        try {
          tsl.addObject(i, Array.get(o, i));
        } catch (ClassCastException e) {
          return null;
        }
      }
      return tsl;
    }

    try {
      T t = function.apply(o);
      return new TypeSafeList<>(Collections.singletonList(t), function);
    } catch (ClassCastException e) {
      // No conversion possible
      return null;
    }
  }


  /**
   * Convert any object to a Long.
   *
   * @param n the object
   *
   * @return the Long, or null if no conversion possible
   */
  public static Long asLong(Object n) {
    if (n instanceof Long) {
      return (Long) n;
    }
    if (n instanceof Number) {
      return ((Number) n).longValue();
    }
    if (n instanceof String) {
      try {
        return Long.decode((String) n);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }


  /**
   * Convert an object to a TypeSafeMap.
   *
   * @param m the object
   *
   * @return the TypeSafeMap or null if no translation was possible
   */
  public static TypeSafeMap asMap(Object m) {
    if (m instanceof TypeSafeMap) {
      return (TypeSafeMap) m;
    }
    if (m instanceof Map<?, ?>) {
      // We are just going to assume the keys are strings
      @SuppressWarnings("unchecked")
      Map<String, Object> m2 = (Map<String, Object>) m;
      return new TypeSafeMap(m2);
    }

    // No translation
    return null;
  }


  /**
   * Convert any object to a Number.
   *
   * @param n the object
   *
   * @return the Number, or null if no conversion possible
   */
  public static Number asNumber(Object n) {

    if (n instanceof Number) {
      return ((Number) n);
    }
    if (n instanceof String) {
      String txt = (String) n;
      // if it contains a '.', or something that could be an 'E+x' or an 'E-x' use BigDecimal.
      if (txt.indexOf('.') != -1 || txt.indexOf('E') != -1 || txt.indexOf('e') != -1) {
        return new BigDecimal(txt);
      }

      // Must be an integer.
      try {
        int length = txt.length();
        // Integer.MAX_VALUE takes 10 characters
        if (length < 10) {
          return Integer.valueOf(txt);
        }
        // Integer.MIN_VALUE takes 11 characters
        if (length < 12) {
          // 10 or 11 characters could be a long or an int
          long l = Long.parseLong(txt);
          if (Integer.MIN_VALUE <= l && l <= Integer.MAX_VALUE) {
            return (int) l;
          }
          return l;
        }
        // Long.MAX_VALUE takes 19 characters
        if (length < 19) {
          return Long.valueOf(txt);
        }
        // Long.MIN_VALUE takes 20 characters
        if (length < 21) {
          BigInteger bi = new BigInteger(txt);
          if (bi.bitLength() <= 63) {
            return bi.longValueExact();
          }
          return bi;
        }

        // it's a BigInteger
        return new BigInteger(txt);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }


  /**
   * Convert any single value to a String. Maps, collections and arrays are not converted.
   *
   * @param o the value
   *
   * @return the String, or null if no conversion is possible
   */
  public static String asString(Object o) {
    if ((o == null) || (o instanceof Map<?, ?>) || (o instanceof Collection<?>) || o.getClass().isArray()) {
      return null;
    }
    return String.valueOf(o);
  }


  /**
   * The actual map.
   */
  protected final Map<String, Object> map;


  public TypeSafeMap() {
    this(new HashMap<>());
  }


  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public TypeSafeMap(Map<String, Object> map) {
    this.map = map;
  }


  @Override
  public void clear() {
    map.clear();
  }


  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }


  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }


  @Override
  public Set<Entry<String, Object>> entrySet() {
    return map.entrySet();
  }


  @Override
  public boolean equals(Object o) {
    return map.equals(o);
  }


  @Override
  public Object get(Object key) {
    return map.get(key);
  }


  /**
   * Get a value from this map in a type-safe way. If the value in the map is missing or of the wrong type, the default
   * is returned instead.
   *
   * @param cls          the required class
   * @param name         the property name
   * @param defaultValue the default value
   * @param <T>          the required class
   *
   * @return the value, if it is present and of the correct class, or the default
   */
  public <T> T get(Class<T> cls, String name, T defaultValue) {
    Object o = map.get(name);
    if (o == null) {
      return defaultValue;
    }
    if (cls.isInstance(o)) {
      return cls.cast(o);
    }
    return defaultValue;
  }


  /**
   * Get a boolean from this map. Not present leads to false.
   *
   * @param name the parameter to get
   *
   * @return a boolean
   */
  public boolean getBoolean(String name) {
    return asBoolean(map.get(name));
  }


  /**
   * Get a boolean from this map.
   *
   * @param name         the parameter to get
   * @param defaultValue the default is no parameter is present.
   *
   * @return a boolean
   */
  public boolean getBoolean(String name, boolean defaultValue) {
    if (map.containsKey(name)) {
      return getBoolean(name);
    }
    return defaultValue;
  }


  public Double getDouble(String name) {
    return asDouble(map.get(name));
  }


  public double getDouble(String name, double defaultValue) {
    Double d = getDouble(name);
    return (d != null) ? d : defaultValue;
  }


  /**
   * Get an integer from the map. The value may be stored as an Integer, Long, BigInteger, or String.
   *
   * @param name the property to retrieve
   *
   * @return the value, or null
   */
  public Integer getInt(String name) {
    return asInt(map.get(name));
  }


  public int getInt(String name, int defaultValue) {
    Integer v = getInt(name);
    return (v != null) ? v : defaultValue;
  }


  /**
   * Get a type-safe list from the map.
   *
   * @param function the conversion function
   * @param name     the name by which the list is stored
   * @param <T>      the required member class
   *
   * @return the list, or null if missing
   */
  @Nullable
  public <T> TypeSafeList<T> getList(Function<Object, T> function, String name) {
    return asList(function, map.get(name));
  }


  /**
   * Get a 64-bit long from the map. The value may be stored as an Integer, Long, BigInteger, or String
   *
   * @param name the property to retrieve
   *
   * @return the value, or null
   */
  public Long getLong(String name) {
    return asLong(map.get(name));
  }


  /**
   * Get a 64-bit long from the map. The value may be stored as an Integer, Long, BigInteger, or String
   *
   * @param name         the property to retrieve
   * @param defaultValue value to return if the map does not contain a value
   *
   * @return the value, or the default
   */
  public long getLong(String name, long defaultValue) {
    Long l = getLong(name);
    return (l != null) ? l : defaultValue;
  }


  /**
   * Get a sub-map from this map.
   *
   * @param name the map's name
   *
   * @return the map, or null if missing
   */
  @Nullable
  public TypeSafeMap getMap(String name) {
    @SuppressWarnings("unchecked")
    Map<String, Object> m = (Map<String, Object>) get(Map.class, name, null);
    return (m != null) ? new TypeSafeMap(m) : null;
  }


  /**
   * Get a Number from the map. The value may be stored as an Integer, Long, BigInteger, or String
   *
   * @param name the property to retrieve
   *
   * @return the value, or null
   */
  public Number getNumber(String name) {
    return asNumber(map.get(name));
  }


  /**
   * Get a Number from the map. The value may be stored as an Integer, Long, BigInteger, or String
   *
   * @param name         the property to retrieve
   * @param defaultValue value to return if the map does not contain a value
   *
   * @return the value, or the default
   */
  public Number getNumber(String name, Number defaultValue) {
    Number l = getNumber(name);
    return (l != null) ? l : defaultValue;
  }


  public String getString(String name) {
    return asString(map.get(name));
  }


  public String getString(String name, String defaultValue) {
    String s = getString(name);
    return (s != null) ? s : defaultValue;
  }


  @Override
  public int hashCode() {
    return map.hashCode();
  }


  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }


  @Override
  public Set<String> keySet() {
    return map.keySet();
  }


  @Override
  public Object put(String name, Object value) {
    return map.put(name, value);
  }


  @Override
  public void putAll(Map<? extends String, ?> m) {
    map.putAll(m);
  }


  @Override
  public Object remove(Object key) {
    return map.remove(key);
  }


  /**
   * Removes the mapping for the specified key from this map if present.
   *
   * @param key key whose mapping is to be removed from the map
   *
   * @return the previous value associated with <code>key</code>, or <code>null</code> if there was no mapping for <code>key</code>.
   *     (A <code>null</code> return can also
   *     indicate that the map previously associated <code>null</code> with <code>key</code>.)
   */
  public Object remove(String key) {
    return map.remove(key);
  }


  @Override
  public Collection<Object> values() {
    return map.values();
  }

}
