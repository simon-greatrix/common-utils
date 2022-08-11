package com.pippsford.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Handle the invocation of methods on an object. The object must contain a
 * method of the same name and same parameter types.
 *
 * @author Simon Greatrix
 */
public class SimpleInvocationHandler implements InvocationHandler {

  /**
   * Get a new proxy instance for the object.
   *
   * @param cls    the interface the object is supposed to implement
   * @param object the object
   * @param <T>    the desired interface type
   *
   * @return the proxy which provides the interface onto the object
   */
  public static <T> T newProxy(Class<T> cls, Object object) {
    // if proxy not needed, don't create one
    if (cls.isInstance(object)) {
      return cls.cast(object);
    }

    // create a proxy
    Object o = Proxy.newProxyInstance(cls.getClassLoader(),
        new Class<?>[]{cls}, new SimpleInvocationHandler(object)
    );
    return cls.cast(o);
  }


  /** The object to invoke the method on. */
  private final Object instance;

  /** The method to actually invoke when an invocation is made. */
  private final CopyOnWriteMap<Method, Method> methods = new CopyOnWriteMap<>();


  /**
   * Create new invocation handler.
   *
   * @param object the object upon which the methods will be invoked.
   */
  public SimpleInvocationHandler(Object object) {
    instance = object;
  }


  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // get the method to actually invoke
    Method actual = methods.get(method);
    if (actual == null) {
      // is there such a method?
      if (methods.containsKey(method)) {
        throw new NoSuchMethodError("Class "
            + instance.getClass().getName()
            + " does not implement \"" + method + "\"");
      }

      // get the method with the same name and parameters
      try {
        actual = instance.getClass().getMethod(
            method.getName(),
            method.getParameterTypes()
        );
      } catch (NoSuchMethodException e) {
        methods.put(method, null);
        Throwable thrown = new NoSuchMethodError("Class "
            + instance.getClass().getName()
            + " does not implement \"" + method + "\"");
        thrown.initCause(e);
        throw thrown;
      }

      // verify the method return type
      if (!actual.getReturnType().equals(method.getReturnType())) {
        methods.put(method, null);
        throw new NoSuchMethodError("Return types do not match for \""
            + method + "\". Expected "
            + method.getReturnType() + " but got "
            + actual.getReturnType());
      }

      // Although the method is public, the class may not be.
      actual.setAccessible(true);

      // save for later
      methods.put(method, actual);
    }

    // invoke the method
    try {
      return actual.invoke(instance, args);
    } catch (InvocationTargetException e) {
      // restore correct exceptions
      throw e.getCause();
    }
  }

}
