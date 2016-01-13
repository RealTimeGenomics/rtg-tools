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

package com.rtg.util.test.params;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.rtg.util.StringUtils;

import junit.framework.Assert;

/**
 */
public final class TestParams {

  private static final boolean OBFUSCATED;
  static {
    final Class<?> clazz = TestParams.class;
    boolean obf;
    try {
      clazz.getDeclaredField("OBFUSCATED");
      obf = false;
    } catch (final NoSuchFieldException e) {
      obf = true;
    }
    OBFUSCATED = obf;
  }

  private static final Set<String> EXCLUDE_METHODS_PARAMS = new HashSet<>();
  static {
    EXCLUDE_METHODS_PARAMS.add("cloneBuilder");
    EXCLUDE_METHODS_PARAMS.add("cloneSet");
    EXCLUDE_METHODS_PARAMS.add("globalIntegrity");
    EXCLUDE_METHODS_PARAMS.add("integrity");
    EXCLUDE_METHODS_PARAMS.add("toString");
  }

  private static final Set<String> EXCLUDE_METHODS_BUILDER = new HashSet<>();
  static {
    EXCLUDE_METHODS_BUILDER.add("set");
  }

  private final Class<?> mParams;
  private final Class<?> mBuilder;
  private final Set<String> mExcludeParams;
  private final Set<String> mExcludeBuilder;

  private final StringBuilder mErr = new StringBuilder();

  private boolean mError = false;

  /**
   * Tester for parameter classes.
   * @param params the class of the parameters object under test.
   * @param builder the class of the corresponding builder object.
   */
  public TestParams(final Class<?> params, final Class<?> builder) {
    mParams = params;
    mBuilder = builder;
    mErr.append(StringUtils.LS);
    mErr.append("Params  class ").append(params.getCanonicalName()).append(StringUtils.LS);
    mErr.append("Builder class ").append(builder.getCanonicalName()).append(StringUtils.LS);
    mExcludeBuilder = new HashSet<>(EXCLUDE_METHODS_BUILDER);
    mExcludeParams = new HashSet<>(EXCLUDE_METHODS_PARAMS);
  }

  /**
   * Add a params method exclusion
   * @param method the method name
   * @return this object, for convenience
   */
  public TestParams excludeParams(String method) {
    mExcludeParams.add(method);
    return this;
  }

  private void error(final boolean ok, final String msg) {
    if (ok) {
      return;
    }
    error(msg);
  }

  private void error(final String msg) {
    mError = true;
    mErr.append(msg).append(StringUtils.LS);
  }

  /**
   * Check if there have been any errors in the analysis.
   */
  public void check() {
    if (!OBFUSCATED) {
      paramsTest();
    }
    if (mError) {
      Assert.fail(toString());
    }
  }

  /**
   * Check that the params class and its associated builder conform to the expected patterns.
   */
  private void paramsTest() {
    checkParams();
    checkBuilder();
    checkClasses();
    final Map<String, Class<?>> fieldsParams = fieldsParams(mParams);
    final Set<String> fieldParamsSet = fieldsParams.keySet();
    final Map<String, Class<?>> fieldsBuilder = fieldsBuilder(mBuilder);
    final Set<String> fieldBuilderSet = fieldsBuilder.keySet();
    final Map<String, Class<?>> methodsParams = methodsParams();
    final Set<String> methodSet = methodsParams.keySet();
    methodsBuilder();

    for (final String fieldName : fieldParamsSet) {
      final boolean containsMethod = methodSet.contains(fieldName);
      error(containsMethod, "Params Field:" + fieldName + " does not have an associated method");
      final boolean containsBuilderField = fieldBuilderSet.contains(fieldName);
      error(containsBuilderField, "Params Field:" + fieldName + " does not have a corresponding field in Builder");
      final Class<?> fieldClass = fieldsParams.get(fieldName);
      if (containsMethod) {
        final Class<?> methodReturnClass = methodsParams.get(fieldName);
        error(fieldClass.equals(methodReturnClass), "Params: " + fieldName + " return type of method:" + methodReturnClass.getCanonicalName() + " does not equal type of field:" + fieldClass.getCanonicalName());
      }
      if (containsBuilderField) {
        final Class<?> fieldBuilderClass = fieldsBuilder.get(fieldName);
        error(fieldClass.equals(fieldBuilderClass), "Both: " + fieldName + " type of params field:" + fieldClass.getCanonicalName() + " does not equal type of builder field:" + fieldBuilderClass.getCanonicalName());
      }
    }
    for (final String fieldName : fieldBuilderSet) {
      error(fieldParamsSet.contains(fieldName), "Builder Field:" + fieldName + " does not have a corresponding field in Params");
    }
    for (final String methodName : methodSet) {
      error(fieldParamsSet.contains(methodName), "Params Method:" + methodName + " does not have an associated variable");
    }

    //    if (cloneBuilder) {
    //      //TODO test cloning
    //    }
  }

  /**
   * Check information just from parameters class, constructors and modifiers.
   */
  private void checkParams() {
    final int paramsModifiers = mParams.getModifiers();
    final boolean paramsPublic = Modifier.isPublic(paramsModifiers);
    error(paramsPublic, "Params: not public");

    final Constructor<?>[] constructors = mParams.getConstructors();
    for (final Constructor<?> cons : constructors) {
      final int modifiers = cons.getModifiers();
      error(!Modifier.isPublic(modifiers), "Params: constructor is public");
      final Class<?>[] parameterTypes = cons.getParameterTypes();
      final boolean ok = parameterTypes.length == 1;
      error(ok, "Params: constructor does not have exactly one parameter");
      if (ok) {
        error(parameterTypes[0] == mBuilder, "Params: constructor does not have builder as argument");
      }
    }

  }

  /**
   * Check information just from builder class, constructors and modifiers.
   */
  private void checkBuilder() {
    final int builderModifiers = mBuilder.getModifiers();
    final boolean builderPublic = Modifier.isPublic(builderModifiers);
    error(builderPublic, "Builder: not public");

    final Constructor<?>[] constructors = mBuilder.getConstructors();
    error(constructors.length == 1, "Builder: has defined constructors");
    final Constructor<?> constructor = constructors[0];
    final int modifiers = constructor.getModifiers();
    error(modifiers == Modifier.PUBLIC, "Builder: constructor has modifiers other than public set: " + Modifier.toString(modifiers));
    error(constructor.getParameterTypes().length == 0, "Builder: constructor has parameters");
  }

  /**
   * Checks that class level info is compatible betwen classes.
   */
  private void checkClasses() {
    final int paramsModifiers = mParams.getModifiers();
    final boolean paramsFinal = Modifier.isFinal(paramsModifiers);
    final int builderModifiers = mBuilder.getModifiers();
    final boolean builderFinal = Modifier.isFinal(builderModifiers);
    error(paramsFinal == builderFinal, "Params,Builder: final modifier does not agree.");
  }

  private Map<String, Class<?>> methodsParams() {
    final Map<String, Class<?>> methodMap = new HashMap<>();
    boolean toString = false;
    final Method[] otherMethods = mParams.getMethods();
    for (final Method method : otherMethods) {
      //System.err.println(method);
      final String name = method.getName();
      if (name.equals("toString")) {
        toString = true;
        continue;
      }
    }
    final Method[] methods = mParams.getDeclaredMethods();
    methods: for (final Method method : methods) {
      //System.err.println(method);
      final int modifiers = method.getModifiers();
      if (Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers)) {
        continue;
      }
      final String name = method.getName();
      if (name.equals("toString")) {
        toString = true;
        continue;
      }
      if (mExcludeParams.contains(name)) {
        continue;
      }
      final boolean ok = Modifier.isPublic(modifiers);
      error(ok, "Method:" + name + " not public");
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length > 0) {
        continue;
      }
      final Annotation[] annotations = method.getAnnotations();
      for (final Annotation annotation : annotations) {
        if (annotation.annotationType().equals(ParamsNoField.class)) {
          continue methods;
        }
      }
      final Class<?> returnType = method.getReturnType();
      methodMap.put(name, returnType);
      //System.err.println(name);
    }
    error(toString, "toString() not defined");
    return methodMap;
  }

  String convertName(final String name) {
    if (name.length() < 2 || !name.substring(0, 1).equals("m")) {
      error(true, "Invalid name for field: " + name);
      return null;
    }
    return name.substring(1, 2).toLowerCase(Locale.getDefault()) + name.substring(2);
  }

  private Map<String, Class<?>> fieldsParams(final Class<?> params) {
    final Map<String, Class<?>> fieldMap = new HashMap<>();
    final Field[] fields = params.getDeclaredFields();
    for (final Field field : fields) {
      //System.err.println(field);
      final int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers)) {
        continue;
      }
      final String name = field.getName();
      final String cname = convertName(name);
      if (cname != null) {
        final Class<?> type = field.getType();
        fieldMap.put(cname, type);
        //System.err.println(name + " " + cname);
        final boolean ok = Modifier.isFinal(modifiers) && Modifier.isPrivate(modifiers);
        error(ok, "Params Field:" + name + " not private and final");
      }
    }
    return fieldMap;
  }

  private Map<String, Class<?>> fieldsBuilder(final Class<?> builder) {
    final Map<String, Class<?>> fieldMap = new HashMap<>();
    final Field[] fields = builder.getDeclaredFields();
    for (final Field field : fields) {
      //System.err.println(field);
      final int modifiers = field.getModifiers();
      if (Modifier.isStatic(modifiers)) {
        continue;
      }
      final String name = field.getName();
      final String cname = convertName(name);
      if (cname != null) {
        //System.err.println(name + " " + cname);
        final Class<?> type = field.getType();
        fieldMap.put(cname, type);
        final boolean ok = !Modifier.isPublic(modifiers);
        error(ok, "Builder Field:" + name + " not private or protected or package local");
      }
    }
    return fieldMap;
  }

  private Map<String, Class<?>> methodsBuilder() {
    final Map<String, Class<?>> methodMap = new HashMap<>();
    final Method[] methods = mBuilder.getDeclaredMethods();
    //    boolean cloneBuilder = false;
    boolean self = false;
    boolean create = false;
    methods: for (final Method method : methods) {
      //System.err.println(method);
      final int modifiers = method.getModifiers();
      if (Modifier.isStatic(modifiers)) {
        continue;
      }
      final String name = method.getName();
      final Class<?>[] parameterTypes = method.getParameterTypes();

      if (name.equals("self")) {
        self = true;
        final boolean ok = parameterTypes.length == 0 && (Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers));
        error(ok, "Builder: self should have 0 parameters and be protected or private");
        continue;
      }
      if (name.equals("create")) {
        create = true;
        final boolean ok = parameterTypes.length == 0 && Modifier.isPublic(modifiers);
        error(ok, "Builder: create should have 0 parameters and be public");
        continue;
      }
      if (mExcludeBuilder.contains(name)) {
        continue;
      }
      final boolean ok = Modifier.isPublic(modifiers);
      error(ok, "Builder: method:" + name + " not public");
      final Annotation[] annotations = method.getAnnotations();
      for (final Annotation annotation : annotations) {
        if (annotation.annotationType().equals(BuilderNotSet.class)) {
          continue methods;
        }
      }
      final boolean argsOk = parameterTypes.length == 1;
      error(argsOk, "Builder: method " + name + " does not have exactly one argument");
      if (!argsOk) {
        continue;
      }
      final Class<?> arg = parameterTypes[0];
      final Class<?> returnType = method.getReturnType();
      if (returnType != mBuilder) {
        error("Builder: method " + name + " does not return builder " + returnType.getCanonicalName());
        continue;
      }
      methodMap.put(name, arg);
      //System.err.println(returnType + " " + name + "(" + arg.getCanonicalName() + ")");
    }
    final boolean abstractBuilder = Modifier.isAbstract(mBuilder.getModifiers());
    error(self || abstractBuilder, "Builder: is not abstract and method self not defined.");
    error(create || abstractBuilder, "Builder: is not abstract and method create not defined.");
    return methodMap;
  }


  @Override
  public String toString() {
    return (mError ? "" : "ok" + StringUtils.LS) + mErr.toString();
  }

}
