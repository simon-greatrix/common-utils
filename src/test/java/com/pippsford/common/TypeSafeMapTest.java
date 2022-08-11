package com.pippsford.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 16/03/2018.
 */
public class TypeSafeMapTest {

  HashMap<String, Object> hash;

  TypeSafeMap instance;

  Map<String, Object> map;


  @Test
  public void asBoolean() {
    assertFalse(TypeSafeMap.asBoolean(null));
    assertFalse(TypeSafeMap.asBoolean(Boolean.FALSE));
    assertTrue(TypeSafeMap.asBoolean(Boolean.TRUE));
    assertFalse(TypeSafeMap.asBoolean(""));
    assertFalse(TypeSafeMap.asBoolean("FaLsE"));
    assertTrue(TypeSafeMap.asBoolean("NiChOlAs"));
    assertTrue(TypeSafeMap.asBoolean(new Object()));
    assertTrue(TypeSafeMap.asBoolean(Double.MIN_VALUE));
    assertFalse(TypeSafeMap.asBoolean(Double.valueOf(0d)));
    assertFalse(TypeSafeMap.asBoolean(Double.NaN));
    assertTrue(TypeSafeMap.asBoolean(Float.MIN_VALUE));
    assertTrue(TypeSafeMap.asBoolean(Float.MAX_VALUE));
    assertFalse(TypeSafeMap.asBoolean(Float.valueOf(0f)));
    assertFalse(TypeSafeMap.asBoolean(Float.NaN));

    assertFalse(TypeSafeMap.asBoolean(Byte.valueOf((byte) 0)));
    assertFalse(TypeSafeMap.asBoolean(Short.valueOf((short) 0)));
    assertFalse(TypeSafeMap.asBoolean(Integer.valueOf(0)));
    assertFalse(TypeSafeMap.asBoolean(Long.valueOf(0)));

    assertTrue(TypeSafeMap.asBoolean(Byte.valueOf((byte) 10)));
    assertTrue(TypeSafeMap.asBoolean(Short.valueOf((short) 10)));
    assertTrue(TypeSafeMap.asBoolean(Integer.valueOf(10)));
    assertTrue(TypeSafeMap.asBoolean(Long.valueOf(10)));

    BigInteger bi = BigInteger.ONE.shiftLeft(64);
    assertEquals(0L, bi.longValue());
    assertTrue(TypeSafeMap.asBoolean(bi));

    assertFalse(TypeSafeMap.asBoolean(BigInteger.ZERO));

    BigDecimal bd = new BigDecimal(Double.MIN_VALUE);
    bd = bd.divide(new BigDecimal(2));
    assertFalse(bd.doubleValue() < 0);
    assertFalse(bd.doubleValue() > 0);
    assertTrue(TypeSafeMap.asBoolean(bd));
    assertFalse(TypeSafeMap.asBoolean(BigDecimal.ZERO));
  }


  @Test
  public void asDouble() {
    Double d = Double.valueOf("123.789");
    assertEquals(d.toString(), TypeSafeMap.asDouble(d).toString());
    assertEquals(d.toString(), TypeSafeMap.asDouble("123.789").toString());
    d = Double.valueOf("456");
    assertEquals(d.toString(), TypeSafeMap.asDouble(Long.valueOf(456)).toString());
    assertNull(TypeSafeMap.asDouble(null));
    assertNull(TypeSafeMap.asDouble("zero"));
    assertNull(TypeSafeMap.asDouble(new Object()));
  }


  @Test
  public void asInt() {
    assertEquals(Integer.valueOf(12), TypeSafeMap.asInt(Integer.valueOf(12)));
    assertEquals(Integer.valueOf(12), TypeSafeMap.asInt(Long.valueOf(12)));
    assertEquals(Integer.valueOf(56), TypeSafeMap.asInt("56"));
    assertNull(TypeSafeMap.asInt(null));
    assertNull(TypeSafeMap.asInt("zero"));
    assertNull(TypeSafeMap.asInt(new Object()));
  }


  @Test
  public void asList() {
    assertNull(TypeSafeMap.asList(TypeSafeList.F_INT, null));

    TypeSafeList<Integer> tsli = new TypeSafeList<>(TypeSafeList.F_INT);
    assertSame(tsli, TypeSafeMap.asList(TypeSafeList.F_INT, tsli));

    assertNull(TypeSafeMap.asList(TypeSafeList.F_INT, Arrays.asList("zero")));

    List<Integer> ints = Arrays.asList(5, 4, 3, 2, 1);
    List<Object> vals = Arrays.asList(5, "4", 3L, 2, "1");
    assertEquals(ints, TypeSafeMap.asList(TypeSafeList.F_INT, vals));

    assertEquals(ints, TypeSafeMap.asList(TypeSafeList.F_INT, new Object[]{5, "4", 3L, 2L, "1"}));
    assertNull(TypeSafeMap.asList(TypeSafeList.F_INT, new Object[]{"zero"}));

    assertEquals(Collections.singletonList(1), TypeSafeMap.asList(TypeSafeList.F_INT, "1"));
    assertNull(TypeSafeMap.asList(TypeSafeList.F_INT, "zero"));
  }


  @Test
  public void asLong() {
    assertEquals(Long.valueOf(12), TypeSafeMap.asLong(Integer.valueOf(12)));
    assertEquals(Long.valueOf(12), TypeSafeMap.asLong(Long.valueOf(12)));
    assertEquals(Long.valueOf(56), TypeSafeMap.asLong("56"));
    assertNull(TypeSafeMap.asLong(null));
    assertNull(TypeSafeMap.asLong("zero"));
    assertNull(TypeSafeMap.asLong(new Object()));
  }


  @Test
  public void asMap() {
    assertNull(TypeSafeMap.asMap(null));

    TypeSafeMap tsm = new TypeSafeMap(new HashMap<>());
    assertSame(tsm, TypeSafeMap.asMap(tsm));

    HashMap<String, Object> hash2 = new HashMap<>();
    hash2.putAll(hash);
    assertEquals(hash2, TypeSafeMap.asMap(hash2));
  }


  @Test
  public void asString() {
    assertNull(TypeSafeMap.asString(null));
    assertNull(TypeSafeMap.asString(new HashMap<>()));
    assertNull(TypeSafeMap.asString(new ArrayList<>()));
    assertNull(TypeSafeMap.asString(new int[5]));

    assertEquals("Fred", TypeSafeMap.asString("Fred"));
    assertEquals("123", TypeSafeMap.asString(123));
  }


  @Test
  public void clear() {
    instance.clear();
    verify(map).clear();
  }


  @Test
  public void containsKey() {
    instance.containsKey("foo");
    verify(map).containsKey(eq("foo"));
  }


  @Test
  public void containsValue() {
    instance.containsValue("foo");
    verify(map).containsValue(eq("foo"));
  }


  @Test
  public void entrySet() {
    instance.entrySet();
    verify(map).entrySet();
  }


  @Test
  public void get() {
    instance.get("foo");
    verify(map).get(eq("foo"));
  }


  @Test
  public void get1() {
    map.put("foo", "bar");
    assertEquals("bar", instance.get(String.class, "foo", "fubar"));
    assertEquals(Integer.valueOf(123), instance.get(Integer.class, "foo", 123));
    assertEquals("null", instance.get(String.class, "n/a", "null"));
  }


  @Test
  public void getBoolean() {
    map.put("key", "true");
    assertTrue(instance.getBoolean("key"));
    assertFalse(instance.getBoolean("lock"));
  }


  @Test
  public void getBoolean1() {
    map.put("key", "true");
    assertTrue(instance.getBoolean("key", true));
    assertTrue(instance.getBoolean("key", false));
    assertTrue(instance.getBoolean("lock", true));
    assertFalse(instance.getBoolean("lock", false));
  }


  @Test
  public void getDouble() {
    map.put("key", 123.456d);
    assertEquals(Double.valueOf(123.456d), instance.getDouble("key"));
    assertNull(instance.getDouble("lock"));
  }


  @Test
  public void getDouble1() {
    Double d1 = Double.valueOf(123.456d);
    Double d2 = Double.valueOf(456.789d);
    map.put("key", d1);
    assertEquals(d1, Double.valueOf(instance.getDouble("key", 456.123d)));
    assertEquals(d2, Double.valueOf(instance.getDouble("lock", 456.789d)));
  }


  @Test
  public void getInt() {
    Integer i1 = Integer.valueOf(123);
    map.put("key", i1);
    assertEquals(i1, instance.getInt("key"));
    assertNull(instance.getInt("lock"));
  }


  @Test
  public void getInt1() {
    map.put("key", 123);
    assertEquals(123, instance.getInt("key", 456));
    assertEquals(456, instance.getInt("lock", 456));
  }


  @Test
  public void getList() {
    map.put("key", new Object[]{1, "2", "3", 4});
    List<Integer> ints = Arrays.asList(1, 2, 3, 4);
    assertEquals(ints, instance.getList(TypeSafeList.F_INT, "key"));
    assertNull(instance.getList(TypeSafeList.F_INT, "lock"));
  }


  @Test
  public void getLong() {
    Long l1 = Long.valueOf(123L);
    map.put("key", l1);
    assertEquals(l1, instance.getLong("key"));
    assertNull(instance.getLong("lock"));
  }


  @Test
  public void getLong1() {
    map.put("key", 123L);
    assertEquals(123L, instance.getLong("key", 456L));
    assertEquals(456L, instance.getLong("lock", 456L));
  }


  @Test
  public void getMap() {
    map.put("key", new HashMap<>());
    assertNotNull(instance.getMap("key"));
    assertNull(instance.getMap("lock"));
  }


  @Test
  public void getString() {
    map.put("key", 123);
    assertEquals("123", instance.getString("key"));
    assertNull(instance.getString("lock"));
  }


  @Test
  public void getString1() {
    map.put("key", 123);
    assertEquals("123", instance.getString("key", "word"));
    assertEquals("word", instance.getString("lock", "word"));
  }


  @Test
  public void isEmpty() {
    assertFalse(instance.isEmpty());
    verify(map).isEmpty();
  }


  @Test
  public void keySet() {
    assertEquals(hash.keySet(), instance.keySet());
    verify(map).keySet();
  }


  @Test
  public void put() {
    instance.put("foo", "bar");
    verify(map).put(eq("foo"), eq("bar"));
  }


  @Test
  public void putAll() {
    HashMap<String, String> h = new HashMap<>();
    h.put("foo", "bar");
    instance.putAll(h);
    verify(map).putAll(eq(h));
  }


  @Test
  public void remove() {
    instance.remove("foo");
    verify(map).remove(eq("foo"));
  }


  @Test
  public void remove1() {
    Object o = new Object();
    instance.remove(o);
    verify(map).remove(eq(o));
  }


  @Before
  public void setUp() {
    hash = new HashMap<>();
    Random rand = new Random(0x7e57ab1e);
    for (int i = 0; i < 10; i++) {
      hash.put(Integer.toString(rand.nextInt(), 36), rand.nextInt());
    }
    map = spy(hash);
    instance = new TypeSafeMap(map);
  }


  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void testEquals() {
    assertFalse(instance.equals("foo"));
    assertTrue(instance.equals(map));
  }


  @Test
  public void testHashCode() {
    assertEquals(map.hashCode(), instance.hashCode());
  }


  @Test
  public void values() {
    HashSet<Object> hs1 = new HashSet<>();
    hs1.addAll(hash.values());
    HashSet<Object> hs2 = new HashSet<>();
    hs2.addAll(instance.values());
    assertEquals(hs1, hs2);
    verify(map).values();
  }

}