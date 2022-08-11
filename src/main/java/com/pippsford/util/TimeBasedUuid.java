package com.pippsford.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of UUID generator that uses time/location based generation method (variant 1).
 *
 * <p>A time based UUID may be used as a nonce where 256-bit security is required. For such usage a nonce should not be
 * expected to repeat more often than a (0.5 * security-strength)-bit random number is expected to repeat. Due to the
 * birthday problem a (0.5 * 256)-bit or 128 bit random number is expected to repeat within 2^64 values.
 *
 * <p>The time-based UUID comprises a 60 bit clock time, a 16 bit sequence number and a 96 bit network ID. The
 * combination of clock time and sequence exceeds the required values before repetition on a particular network
 * address.
 *
 * <p>In order to create nonces that are unique across different processes on the same machine, it is necessary to
 * combine the type 1 UUID with a process identifier.
 */
public class TimeBasedUuid {

  /** Logger for this class. */
  private static final Logger LOG = LoggerFactory.getLogger(
      TimeBasedUuid.class);

  /** Secure random number generator. */
  private static final Random RANDOM = new SecureRandom();

  /** Default instance. */
  private static TimeBasedUuid instance = null;


  /**
   * Create a Type-1 UUID using the default MAC address.
   *
   * @return a newly generated UUID
   */
  public static UUID create() {
    if (instance == null) {
      instance = new TimeBasedUuid(null);
    }
    return instance.generate();
  }


  /**
   * Get the MAC address to use with this computer.
   *
   * @return the MAC address
   */
  private static byte[] getAddress() {
    try {
      byte[] data = AccessController.doPrivileged(
          (PrivilegedAction<byte[]>) TimeBasedUuid::getAddressWithPrivilege);
      if (data != null) {
        return data;
      }
    } catch (SecurityException e) {
      LOG.warn(
          "Cannot get MAC for local host. Require permission \"NetPermission getNetworkInformation\" and \"SocketPermission localhost, resolve\".");
    }

    // Must create random multi-cast address. We will create one from an
    // unused block in the CF range. The CF range is currently closed but
    // was intended for when there is no appropriate regular organizational
    // unit identifier (OUI) which would normally constitute the first three
    // bytes of the MAC address.
    byte[] data = new byte[6];
    RANDOM.nextBytes(data);
    data[0] = (byte) 0xcf;
    data[1] = (byte) (data[1] | 0x80);
    return data;
  }


  /**
   * Attempt to get the local MAC address after privilege has been asserted.
   *
   * @return the MAC address or null.
   */
  @SuppressWarnings("squid:S1168")
  private static byte[] getAddressWithPrivilege() {
    byte[] mac = getFromLocalHost();
    if (mac != null) {
      return mac;
    }

    // now try all interfaces
    LinkedList<NetworkInterface> list = new LinkedList<>();
    try {
      Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
      list.addAll(Collections.list(en));
    } catch (SocketException se) {
      // cannot process interfaces
      LOG.warn("Failed to get network interfaces.", se);
    }

    while (!list.isEmpty()) {
      NetworkInterface networkInterface = list.removeFirst();
      // skip nulls, if any
      if (networkInterface == null) {
        continue;
      }
      try {
        mac = getMACAddress(networkInterface);
        if (mac != null) {
          return mac;
        }

        // queue up sub-interfaces in order
        LinkedList<NetworkInterface> tmp = new LinkedList<>();
        Enumeration<NetworkInterface> en = networkInterface.getSubInterfaces();
        while (en.hasMoreElements()) {
          tmp.addLast(en.nextElement());
        }
        while (!tmp.isEmpty()) {
          list.addFirst(tmp.removeLast());
        }
      } catch (SocketException se) {
        // ignore this interface
        LOG.warn(
            "Failed to get localhost hardware address or sub-interfaces for "
                + networkInterface.getDisplayName(),
            se
        );
      }
    }

    return null;
  }


  /**
   * Attempt to get the MAC address associated with local host.
   *
   * @return the MAC address or null
   */
  @SuppressWarnings("squid:S1168")
  private static byte[] getFromLocalHost() {
    try {
      // first try local host
      InetAddress localHost = InetAddress.getLocalHost();
      if (!localHost.isLoopbackAddress()) {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
            localHost);
        return getMACAddress(networkInterface);
      }
    } catch (IOException e) {
      // possibly the look-up of local host failed
      LOG.warn("Failed to get localhost hardware address.", e);
    }

    // not found
    return null;
  }


  /**
   * Get a MAC hardware address associated with a network interface.
   *
   * @param networkInterface the interface
   *
   * @return the address, or null
   */
  @SuppressWarnings("squid:S1168")
  private static byte[] getMACAddress(NetworkInterface networkInterface) throws SocketException {
    if (networkInterface != null && !networkInterface.isLoopback()) {
      byte[] data = networkInterface.getHardwareAddress();
      if (data != null && data.length == 6) {
        return data;
      }
    }
    return null;
  }


  /** The Ethernet address this generator is associated with. */
  private final long ethernetAddress;


  /**
   * Create a Type 1 UUID generator for the specified MAC address.
   *
   * @param address the MAC address (6 bytes)
   */
  TimeBasedUuid(final byte[] address) {
    if (address != null && address.length != 6) {
      throw new IllegalArgumentException(
          "MAC Address must contain 48 bits");
    }
    UuidTime.init(RANDOM);
    byte[] addr = address;
    if (addr == null) {
      addr = getAddress();
    }

    long v = 0;
    for (int i = 0; i < 6; i++) {
      v = (v << 8) | (addr[i] & 0xff);
    }
    ethernetAddress = v;
  }


  /**
   * Generate a Type 1 UUID for this address.
   *
   * @return a new UUID
   */
  public UUID generate() {
    UuidTime time = UuidTime.newTimeStamp();
    final long rawTimestamp = time.getTime();
    final int sequence = time.getSequence();

    // first 32 bits are the lowest 32 bits of the time
    long l1 = (rawTimestamp & 0xffffffffL) << 32;

    // next 16 bits are the middle of the time
    l1 |= (rawTimestamp & 0xffff00000000L) >> 16;

    // next 4 bits are the version code
    l1 |= 0x1000;

    // last 12 bits are the next 12 bits of the time
    l1 |= (rawTimestamp & 0xfff000000000000L) >> 48;

    // the top 4 bits of the time are lost

    long l2 = ((long) sequence) << 48;
    l2 |= ethernetAddress;

    return new UUID(l1, l2);
  }

}



