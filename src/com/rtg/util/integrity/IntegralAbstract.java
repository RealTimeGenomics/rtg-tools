/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rtg.util.integrity;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.rtg.util.Utils;

/**
 * Used as base implementation for classes that have integrity
 * constraints and string buffer based <code>toString</code>.
 *
 */
public abstract class IntegralAbstract implements Integrity {

  /** System dependent line separator. */
  public static final String LS = System.lineSeparator();

  @Override
  public boolean globalIntegrity() {
    integrity();
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }

  /**
   * Return a default human readable version of obj.
   * @param sb string builder to append to
   */
  public void toString(final StringBuilder sb) {
    toString(this, sb);
    sb.append(LS);
  }

  /**
   * Return a default human readable version of obj.
   * @param obj the object to be output.
   * @return the string
   */
  public static String toString(Object obj) {
    final StringBuilder sb = new StringBuilder();
    toString(obj, sb);
    return sb.toString();
  }

  /**
   * Get all the fields in this class and its super-classes.
   * @param clazz class to start from.
   * @return all the fields.
   */
  static ArrayList<Field> allFields(final Class<?> clazz) {
    final ArrayList<Field> res = new ArrayList<>();
    Class<?> cl = clazz;
    while (cl != null) {
      //System.err.println(cl.getCanonicalName());
      final Field[] fields = cl.getDeclaredFields();
      Collections.addAll(res, fields);
      cl = cl.getSuperclass();
    }
    return res;
  }

  /**
   * Put a default human readable version of obj into sb.
   * @param obj the object to be output.
   * @param sb where the result is placed.
   */
  public static void toString(final Object obj, final StringBuilder sb) {
    toString(obj, sb, "");
  }

  /**
   * Put a default human readable version of obj into sb.
   * @param obj the object to be output.
   * @param sb where the result is placed.
   */
  private static void toString(final Object obj, final StringBuilder sb, final String prefix) {
    if (obj == null) {
      sb.append("null");
      return;
    }
    if (obj instanceof Iterable) {
      iterableToString(sb, (Iterable<?>) obj, prefix);
      return;
    }
    final Class<?> clazz = obj.getClass();
    if (toStringDeclared(clazz)) {
      sb.append(prefix);
      sb.append(obj.toString());
      return;
    }

    final String name = clazz.getSimpleName();
    sb.append(name);
    if (clazz.isArray()) {
      sb.append(LS);
      arrayToString(sb, obj, clazz, prefix);
      return;
    }
    final ArrayList<Field> fields = allFields(clazz);
    //primitives first - on one line
    for (final Field field : fields) {
      setAccessible(field);
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      final Class<?> fieldClass = field.getType();
      if (fieldClass.isPrimitive()) {
        final String fieldName = field.getName();
        sb.append(prefix);
        sb.append(" ").append(fieldName).append("=");
        primitiveToString(obj, sb, field, fieldClass);
      }
    }
    for (final Field field : fields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      final Class<?> fieldClass = field.getType();
      if (fieldClass.isPrimitive()) {
        continue;
      }
      final String fieldName = field.getName();
      sb.append(LS).append(prefix).append(fieldName).append(":");
      final Object fieldObject = fieldObject(obj, field);
      if (fieldObject == null) {
        continue;
      }
      if (fieldClass.isArray()) {
        arrayToString(sb, fieldObject, fieldClass, prefix);
      } else {
        objectToString(sb, fieldObject, prefix);
      }
    }
  }

  private static void setAccessible(final Field field) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        field.setAccessible(true);
        return null;
      }
    });
  }

  /**
   * Get the object corresponding to a field.
   * Return a null and otherwise ignore checked exceptions.
   * @param obj object containing the field.
   * @param field to be extracted.
   * @return the object corresponding to the field (or null if cannot be extracted).
   */
  private static Object fieldObject(final Object obj, final Field field) {
    try {
      return field.get(obj);
    } catch (final IllegalArgumentException e) {
      return null;
    } catch (final IllegalAccessException e) {
      return null;
    }
  }

  /**
   * Find out if the class (or a superclass) has declared the <code>toString()</code> method.
   * Excludes Object itself and
   * @param clazz the class to find if there is a declared <code>toString()</code> method.
   * @return true if there is a <code>toString()</code> method declared, false otherwise.
   */
  static boolean toStringDeclared(final Class<?> clazz) {
    if (clazz == null) {
      return false;
    }
    if (clazz == Object.class || clazz == IntegralAbstract.class) {
      return false;
    }
    try {
      final String methodName = "toString";
      final Method m = clazz.getDeclaredMethod(methodName);
      if (m == null) {
        return toStringDeclared(clazz.getSuperclass());
      }
    } catch (final SecurityException e) {
      return false;
    } catch (final NoSuchMethodException e) {
      return toStringDeclared(clazz.getSuperclass());
    }
    return true;
  }

  /**
   * Display a field that is an array.
   * @param sb where the resulting output is placed.
   * @param fieldObj the object corresponding to the field.
   * @param fieldClass the class of the object in the field.
   * @param prefix on each line.
   */
  private static void arrayToString(final StringBuilder sb, final Object fieldObj, final Class<?> fieldClass, final String prefix) {
    final Class<?> componentClass = fieldClass.getComponentType();
    final int length = Array.getLength(fieldObj);
    sb.append(prefix).append("[").append(length).append("]");
    if (componentClass.isPrimitive()) {
      for (int i = 0; i < length; i++) {
        if (i % 10 == 0) {
          sb.append(LS).append(prefix).append("[").append(i).append("] ");
        } else {
          sb.append(", ");
        }
        final Object objC = Array.get(fieldObj, i);
        if (componentClass == Double.TYPE) {
          final Double objD = (Double) objC;
          sb.append(Utils.realFormat(objD, 4));
        } else if (componentClass == Float.TYPE) {
          final Float objF = (Float) objC;
          sb.append(Utils.realFormat(objF, 4));
        } else {
          sb.append(objC);
        }
      }
    } else {
      for (int i = 0; i < length; i++) {
        sb.append(LS).append(prefix).append("[").append(i).append("] ");
        final Object obj = Array.get(fieldObj, i);
        if (obj == null) {
          sb.append("null");
          continue;
        }
        final Class<?> clazz = obj.getClass();
        if (toStringDeclared(clazz)) {
          sb.append(prefix);
          sb.append(obj.toString());
          continue;
        }
        toString(obj, sb, prefix + "  ");
      }
    }
  }

  /**
   * Display a field that is an <code>Iterable</code>.
   * @param sb where the resulting output is placed.
   * @param iterable the object that can be iterated over.
   * @param prefix on each line.
   */
  private static void iterableToString(final StringBuilder sb, final Iterable<?> iterable, final String prefix) {
    final Iterator<?> it = iterable.iterator();
    sb.append(LS).append(prefix).append("{");
    while (it.hasNext()) {
      sb.append(LS).append(prefix).append(" ");
      final Object obj = it.next();
      if (obj == null) {
        sb.append("null");
        continue;
      }
      final Class<?> clazz = obj.getClass();
      if (toStringDeclared(clazz)) {
        sb.append(prefix);
        sb.append(obj.toString());
        continue;
      }
      toString(obj, sb, prefix + "  ");
    }
    sb.append(LS).append(prefix).append("}");
  }

  /**
   * Output an object that is not an array. If <code>toString()</code> (other than the base definition in Object) is declared then
   * use that otherwise make a recursive call.
   * @param sb where the output is to be placed.
   * @param obj to be output.
   * @param prefix to be prepended to each line
   */
  private static void objectToString(final StringBuilder sb, final Object obj, final String prefix) {
    if (obj instanceof Iterable) {
      iterableToString(sb, (Iterable<?>) obj, prefix);
      return;
    }
    final Class<?> clazz = obj.getClass();
    if (toStringDeclared(clazz)) {
      sb.append(prefix);
      sb.append(obj.toString());
    } else {
      toString(obj, sb, prefix + "  ");
    }
  }

  /**
   * Output a field containing a primitive.
   * @param obj object containing the field.
   * @param sb where the output is to be placed.
   * @param field to be output.
   * @param fieldClass class of the field.
   */
  private static void primitiveToString(final Object obj, final StringBuilder sb, final Field field, final Class<?> fieldClass) {
    try {
      if (fieldClass == Double.TYPE || fieldClass == Float.TYPE) {

        final double dbl = field.getDouble(obj);
        sb.append(Utils.realFormat(dbl, 4));

      } else if (fieldClass == Long.TYPE || fieldClass == Integer.TYPE || fieldClass == Short.TYPE || fieldClass == Byte.TYPE) {

        final long fInt = field.getLong(obj);
        sb.append(fInt);

      } else if (fieldClass == Boolean.TYPE) {

        final boolean bool = field.getBoolean(obj);
        sb.append(bool);

      } else if (fieldClass == Character.TYPE) {

        final char ch = field.getChar(obj);
        sb.append("'").append(ch).append("'");

      }
    } catch (final IllegalArgumentException | IllegalAccessException e) {
      // keep going
    }
  }

}
