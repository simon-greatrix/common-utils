package com.pippsford.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Permission;
import java.util.HashSet;
import java.util.UUID;
import org.junit.Test;

/**
 * @author Simon Greatrix on 05/10/2017.
 */
public class TimeBasedUuidTest {

  @Test
  public void create() throws Exception {

    HashSet<UUID> uuids = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      assertTrue(uuids.add(TimeBasedUuid.create()));
    }
  }


  @Test
  public void security1() throws Exception {
    SecurityManager sm = System.getSecurityManager();
    try {
      Thread myThread = Thread.currentThread();
      SecurityManager tmp = new SecurityManager() {
        @Override
        public void checkConnect(String name, int port) {
          if (Thread.currentThread().equals(myThread) && port == -1) {
            throw new SecurityException("TESTING");
          }
        }


        @Override
        public void checkPermission(Permission perm) {
          // Do nothing
        }


        @Override
        public void checkPermission(Permission perm, Object contect) {
          // Do nothing
        }

      };
      System.setSecurityManager(tmp);

      TimeBasedUuid uuid = new TimeBasedUuid(null);
      UUID actual = uuid.generate();
      assertEquals(1, actual.version());

    } finally {
      try {
        System.setSecurityManager(sm);
      } catch (SecurityException e) {
        e.printStackTrace();
      }
    }
  }


  @Test
  public void security2() throws Exception {
    SecurityManager sm = System.getSecurityManager();
    try {
      Thread myThread = Thread.currentThread();
      SecurityManager tmp = new SecurityManager() {
        @Override
        public void checkConnect(String name, int port) {
          if (Thread.currentThread().equals(myThread) && port == -1) {
            throw new SecurityException("TESTING");
          }
        }


        @Override
        public void checkPermission(Permission perm) {
          if (perm.getName().equals("getNetworkInformation")) {
            throw new SecurityException("TESTING");
          }
        }


        @Override
        public void checkPermission(Permission perm, Object contect) {
          // Do nothing
        }

      };
      System.setSecurityManager(tmp);

      TimeBasedUuid uuid = new TimeBasedUuid(null);
      UUID actual = uuid.generate();
      assertEquals(1, actual.version());

    } finally {
      try {
        System.setSecurityManager(sm);
      } catch (SecurityException e) {
        e.printStackTrace();
      }
    }
  }

}