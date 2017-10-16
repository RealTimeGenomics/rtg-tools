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
package com.rtg.util.cli;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.IntegerOrPercentage;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.visualization.DisplayHelper;


/**
 * Encapsulates a single flag.
 * @param <T> flag value type
 */
@TestClass(value = {"com.rtg.util.cli.CFlagsTest"})
public class Flag<T> implements Comparable<Flag<T>> {

  enum Level { DEFAULT, EXTENDED, EXPERIMENTAL }

  static final String EXTENDED_FLAG_PREFIX = "X";

  static final String EXPERIMENTAL_FLAG_PREFIX = "XX";

  private final Character mFlagChar;

  private final String mFlagName;

  private final String mFlagDescription;

  private final Level mLevel;

  /** The maximum number of times the flag can occur. */
  private int mMaxCount;

  /** The minimum number of times the flag can occur. */
  private int mMinCount;

  private final Class<T> mParameterType;

  private String mParameterDescription;

  private T mParameterDefault = null;

  private String mCategory = null;

  private String mPsuedoMinMaxString = null;

  /** Optional list of valid values for the parameter. */
  private List<String> mParameterRange;

  private boolean mRangeList = false;

  /** Values supplied by the user */
  private List<T> mParameter = new ArrayList<>();

  /**
   * Creates a new <code>Flag</code> for which the name must be supplied on
   * the command line.
   *
   * @param flagChar a <code>Character</code> which can be supplied by the
   * user as an abbreviation for flagName. May be null.
   * @param flagName a <code>String</code> which is the name that the user
   * specifies on the command line to denote the flag.
   * @param flagDescription a name used when printing help messages.
   * @param minCount the minimum number of times the flag must be specified.
   * @param maxCount the maximum number of times the flag can be specified.
   * @param paramType a <code>Class</code> denoting the type of values to be
   * accepted. Maybe null for "switch" type flags.
   * @param paramDescription a description of the meaning of the flag.
   * @param paramDefault a default value that can be used for optional flags.
   * @param category The flag category
   */
  public Flag(final Character flagChar, final String flagName, final String flagDescription,
      final int minCount, final int maxCount, final Class<T> paramType, final String paramDescription,
      final T paramDefault, final String category) {
    if (flagDescription == null) {
      throw new NullPointerException();
    }
    if (flagName == null) {
      if (paramType == null) {
        throw new IllegalArgumentException();
      }
    } else {
      if (flagName.startsWith("-")) {
        throw new IllegalArgumentException("Long flag names cannot start with '-'");
      }
    }
    setCategory(category);
    mFlagName = flagName;
    mFlagChar = flagChar;
    mFlagDescription = flagDescription;

    mParameterType = paramType;
    mParameterDescription = (mParameterType == null) ? null
        : ((paramDescription == null) || (paramDescription.length() == 0)) ? autoDescription(mParameterType)
            : paramDescription.toUpperCase(Locale.getDefault());
    if (mParameterType != null) {
      setParameterDefault(paramDefault);
    }

    mMinCount = minCount;
    mMaxCount = maxCount;

    // For enums set up the limited set of values message
    final String[] range = values(mParameterType);
    if (range != null) {
      setParameterRange(range);
    }

    if (mFlagName != null) {
      if (mFlagName.startsWith(EXPERIMENTAL_FLAG_PREFIX)) {
        mLevel = Level.EXPERIMENTAL;
      } else if (mFlagName.startsWith(EXTENDED_FLAG_PREFIX)) {
        mLevel = Level.EXTENDED;
      } else {
        mLevel = Level.DEFAULT;
      }
    } else {
      mLevel = Level.DEFAULT;
    }
  }

  /**
   * @return the flag level
   */
  Level level() {
    return mLevel;
  }

  /**
   * Gets the enum object specified by str.
   * @param type of object.
   * @param str string to specify object.
   * @return object of class type (null if the type does not look sufficiently like an Enum).
   */
  static Object valueOf(final Class<?> type, final String str) {
    if (!isValidEnum(type)) {
      return null;
    }
    try {
      final String valueOfMethod = "valueOf";
      final Method m = type.getMethod(valueOfMethod, String.class);
      if (!Modifier.isStatic(m.getModifiers())) {
        return null;
      }
      final Class<?> returnType = m.getReturnType();
      if (!type.isAssignableFrom(returnType)) {
        return null;
      }
      return m.invoke(null, str.toUpperCase(Locale.getDefault()).replace('-', '_'));
    } catch (final NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      //Should never happen
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the range of values for an Enum (or at least something that looks like an Enum).
   * @param type from which values to be extracted.
   * @return the allowed values for the specified type (null if does not look sufficiently like an Enum).
   */
  static String[] values(final Class<?> type) {
    if (type == null) {
      return null;
    }
    if (!isValidEnum(type)) {
      return null;
    }
    try {
      final String valuesMethod = "values";
      final Method m = type.getMethod(valuesMethod);
      final Class<?> returnType = m.getReturnType();
      if (returnType.isArray()) {
        final Object[] ret = (Object[]) m.invoke(null);
        final String[] res = new String[ret.length];

        for (int i = 0; i < ret.length; ++i) {
          res[i] = ret[i].toString().toLowerCase(Locale.getDefault()).replace('_', '-'); // List enums as lowercase by default
        }
        return res;
      }
      return null;
    } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // Should never happen
      throw new RuntimeException(e);
    }

  }

  /**
   * Check if looks sufficiently like an Enum to be treated as one.
   * Must implement
   *    static T[] values()
   *    static T value(String)
   * @param type class type
   * @return true iff is an Enum or looks sufficiently like one.
   */
  static boolean isValidEnum(final Class<?> type) {
    if (type == null) {
      return false;
    }
    if (type.isEnum()) {
      return true;
    }

    final Method m;
    try {
      final String valuesMethod = "values";
      m = type.getDeclaredMethod(valuesMethod);
      if (m == null) {
        return false;
      }
    } catch (final SecurityException | NoSuchMethodException e) {
      return false;
    }
    if (!Modifier.isStatic(m.getModifiers())) {
      return false;
    }
    final Class<?> returnType = m.getReturnType();
    if (!returnType.isArray()) {
      return false;
    }

    final Method v;
    try {
      final String valueOfMethod = "valueOf";
      v = type.getMethod(valueOfMethod, String.class);
      if (v == null) {
        return false;
      }

    } catch (final SecurityException | NoSuchMethodException e) {
      return false;
    }
    if (!Modifier.isStatic(v.getModifiers())) {
      return false;
    }
    final Class<?> returnTypev = v.getReturnType();
    if (!type.isAssignableFrom(returnTypev)) {
      return false;
    }
    return true;
  }

  /**
   * Sets the maximum number of times the flag can be specified.
   *
   * @param count the maximum number of times the flag can be specified.
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setMaxCount(final int count) {
    if ((count < 1) || (count < mMinCount)) {
      throw new IllegalArgumentException("MaxCount (" + count
          + ") must not be 0 or less than MinCount (" + mMinCount + ")");
    }
    mMaxCount = count;
    return this;
  }

  /**
   * Gets the maximum number of times the flag can be specified.
   *
   * @return the maximum number of times the flag can be specified.
   */
  public int getMaxCount() {
    return mMaxCount;
  }

  /**
   * Sets the minimum number of times the flag can be specified.
   *
   * @param count the minimum number of times the flag can be specified.
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setMinCount(final int count) {
    if (count > mMaxCount) {
      throw new IllegalArgumentException("MinCount (" + count
          + ") must not be greater than MaxCount (" + mMaxCount + ")");
    }
    if (count == Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
      "You're crazy man -- MinCount cannot be Integer.MAX_VALUE");
    }
    mMinCount = count;
    return this;
  }

  /**
   * Gets the minimum number of times the flag can be specified.
   *
   * @return the minimum number of times the flag can be specified.
   */
  public int getMinCount() {
    return mMinCount;
  }

  /**
   * Return the number of times the flag has been set.
   *
   * @return the number of times the flag has been set.
   */
  public int getCount() {
    return mParameter.size();
  }

  /**
   * Return true if the flag has been set.
   *
   * @return true if the flag has been set.
   */
  public boolean isSet() {
    return mParameter.size() > 0;
  }

  /**
   * Gets the character name of this flag, if set.
   *
   * @return the character name of this flag, or null if no character name has
   * been set.
   */
  public Character getChar() {
    return mFlagChar;
  }

  /**
   * Gets the name of the flag.
   *
   * @return the name of the flag.
   */
  public String getName() {
    return mFlagName;
  }

  /**
   * Gets the description of the flag's purpose.
   *
   * @return the description.
   */
  public String getDescription() {
    return mFlagDescription;
  }

  /**
   * Gets the description of the flag parameter. This is usually a single word
   * that indicates a little more than the parameter type.
   *
   * @return the parameter description, or null for untyped flags.
   */
  public String getParameterDescription() {
    return mParameterDescription;
  }

  protected void setParameterDescription(String desc) {
    mParameterDescription = desc;
  }

  /**
   * Gets the type of the parameter. This will return null for untyped
   * (switch) flags. Parameters will be checked that they are of the specified
   * type.
   *
   * @return the parameter type, or null if the flag is untyped.
   */
  public Class<?> getParameterType() {
    return mParameterType;
  }

  /**
   * Gets the default value of the parameter.
   *
   * @return the default value, or null if no default has been specified.
   */
  public Object getParameterDefault() {
    return mParameterDefault;
  }

  /**
   * Sets the default value of the parameter.
   *
   * @param paramDefault a default value that can be used for optional flags.
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setParameterDefault(final T paramDefault) {
    if (mParameterType == null) {
      throw new IllegalArgumentException("Cannot set default parameter for untyped flags");
    }
    mParameterDefault = paramDefault;
    return this;
  }

  /**
   * Defines the set of strings that are valid for this flag.
   *
   * @param range a collection of Strings.
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setParameterRange(final Collection<String> range) {
    //System.err.println("setParameterRange range=" + range.toString());
    final String[] rarray = range.toArray(new String[range.size()]);
    return setParameterRange(rarray);
  }

  /**
   * Defines the set of strings that are valid for this flag.
   *
   * @param range an array of Strings.
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setParameterRange(final String[] range) {
    if (mParameterType == null) {
      throw new IllegalArgumentException("Cannot set parameter range for no-arg flags.");
    }
    if (range == null) {
      mParameterRange = null;
    } else {
      if (range.length < 1) {
        throw new IllegalArgumentException("Must specify at least one value in parameter range.");
      }
      final List<String> l = new ArrayList<>(range.length);
      for (final String s : range) {
        try {
          parseValue(s);
        } catch (final Exception e) {
          throw new IllegalArgumentException("Range value " + s + " could not be parsed.", e);
        }
        l.add(s);
      }
      mParameterRange = Collections.unmodifiableList(l);
    }
    return this;
  }

  /**
   * Override the default minimum - maximum string with one representing the given range.
   * @param min the minimum
   * @param max the maximum
   */
  public void setPsuedoMinMaxRangeString(final int min, final int max) {
    final String str = minMaxUsage(min, max, mRangeList);
    if (str.length() == 0) {
      mPsuedoMinMaxString = null;
    } else {
      mPsuedoMinMaxString = str;
    }
  }

  /**
   * Gets the list of valid parameter values, if these have been specified.
   *
   * @return a <code>List</code> containing the permitted values, or null if
   * this has not been set.
   */
  public List<String> getParameterRange() {
    return mParameterRange;
  }

  /**
   * Get the value for this flag. If the flag was not user-set, then the
   * default value is returned (if defined). The value will have been checked
   * to comply with any parameter typing. If called on an untyped flag, this
   * will return Boolean.TRUE or Boolean.FALSE appropriately.
   *
   * @return a value for this flag.
   */
  public T getValue() {
    return (isSet()) ? mParameter.get(0) : mParameterDefault;
  }

  /**
   * Get a collection of all values set for this flag. This is for flags that
   * can be set multiple times. If the flag was not user-set, then the
   * collection contains only the default value (if defined).
   *
   * @return a <code>Collection</code> of the supplied values.
   */
  public List<T> getValues() {
    final List<T> result;
    if (isSet()) {
      result = mParameter;
    } else {
      result = new ArrayList<>();
      if (mParameterDefault != null) {
        result.add(mParameterDefault);
      }
    }
    return Collections.unmodifiableList(result);
  }

  void reset() {
    mParameter = new ArrayList<>();
  }

  FlagValue<T> setValue(final String valueStr) {
    if (mParameter.size() >= mMaxCount) {
      throw new FlagCountException("Value cannot be set more than " + mMaxCount + "times for flag: " + mFlagName);
    }
    if (mParameterRange != null) {
      if (mRangeList) {
        final String[] vs = StringUtils.split(valueStr, ',');
        for (String vi : vs) {
          if (!mParameterRange.contains(vi)) {
            throw new IllegalArgumentException("A value supplied is not in the set of allowed values.");
          }
        }
      } else {
        if (!mParameterRange.contains(valueStr)) {
          throw new IllegalArgumentException("Value supplied is not in the set of allowed values.");
        }
      }
    }
    if (mRangeList) {
      final String[] valueStrs = StringUtils.split(valueStr, ',');
      final List<T> values = new ArrayList<>(valueStrs.length);
      for (final String valueStr2 : valueStrs) {
        final T value = parseValue(valueStr2);
        mParameter.add(value);
        values.add(value);
      }
      return new FlagValue<>(this, values);
    } else {
      final T value = parseValue(valueStr);
      mParameter.add(value);
      return new FlagValue<>(this, value);
    }
  }

  /**
   * Converts the string representation of a parameter value into the
   * appropriate Object. This default implementation knows how to convert
   * based on the parameter type for several common types. Override for custom
   * parsing.
   * @param valueStr the value to parse
   * @return the parsed value
   */
  T parseValue(final String valueStr) {
    return Flag.instanceHelper(mParameterType, valueStr);
  }

  @Override
  public boolean equals(final Object other) {
    return other instanceof Flag && getName().equals(((Flag<?>) other).getName());
  }

  @Override
  public int hashCode() {
    return getName() == null ? 0 : getName().hashCode();
  }

  @Override
  public int compareTo(final Flag<T> other) {
    if (other == null) {
      return -1;
    }
    if (other.getName() != null) {
      return getName().compareTo(other.getName());
    }
    return -1;
  }

  /** @return a compact usage string (prefers char name if present). */
  final String getCompactFlagUsage() {
    return getCompactFlagUsage(new DisplayHelper());
  }
  String getCompactFlagUsage(DisplayHelper dh) {
    final StringBuilder sb = new StringBuilder();
    final String flagName = (getChar() != null) ? CFlags.SHORT_FLAG_PREFIX + getChar() : CFlags.LONG_FLAG_PREFIX + getName();
    sb.append(dh.decorateForeground(flagName, DisplayHelper.THEME_LITERAL_COLOR));
    final String usage = getParameterDescription();
    if (usage.length() > 0) {
      sb.append(' ').append(dh.decorateForeground(usage, DisplayHelper.THEME_TYPE_COLOR));
    }
    return sb.toString();
  }

  /** @return a usage string. */
  final String getFlagUsage() {
    return getFlagUsage(new DisplayHelper());
  }
  String getFlagUsage(DisplayHelper dh) {
    final StringBuilder sb = new StringBuilder();
    final String flagName = CFlags.LONG_FLAG_PREFIX + getName();
    sb.append(dh.decorateForeground(flagName, DisplayHelper.THEME_LITERAL_COLOR));
    if (getParameterType() != null) {
      sb.append('=').append(dh.decorateForeground(getParameterDescription(), DisplayHelper.THEME_TYPE_COLOR));
    }
    return sb.toString();
  }

  static String minMaxUsage(int min, int max, boolean allowCsv) {
    final StringBuilder ret = new StringBuilder();
    if (min >= 1 && max > 1) {
      if (max == Integer.MAX_VALUE) {
        ret.append("Must be specified ").append(min).append(" or more times");
      } else if (max - min == 0) {
        ret.append("Must be specified ").append(min).append(" times");
      } else if (max - min == 1) {
        ret.append("Must be specified ").append(min).append(" or ").append(max).append(" times");
      } else {
        ret.append("Must be specified ").append(min).append(" to ").append(max).append(" times");
      }
    } else {
      if (min == 0) {
        if (max > 1) {
          if (max == Integer.MAX_VALUE) {
            ret.append("May be specified 0 or more times");
          } else {
            ret.append("May be specified up to ").append(max).append(" times");
          }
        }
      }
    }
    if (ret.length() > 0 && allowCsv) {
      ret.append(", or as a comma separated list");
    }
    return ret.toString();
  }

  void appendLongFlagUsage(final WrappingStringBuilder wb, final int longestUsageLength) {
    wb.append("  ");
    if (getChar() == null) {
      wb.append("    ");
    } else {
      final String flagName = CFlags.SHORT_FLAG_PREFIX + getChar();
      wb.append(wb.displayHelper().decorateForeground(flagName, DisplayHelper.THEME_LITERAL_COLOR)).append(", ");
    }

    final int len = getFlagUsage().length();
    //final String disp = getFlagUsage();
    final String disp = getFlagUsage(wb.displayHelper());
    wb.append(disp);
    for (int i = 0; i < longestUsageLength - len; ++i) {
      wb.append(" ");
    }
    wb.append(" ");

    final String description = getUsageDescription();

    wb.wrapText(description);
    wb.append(CFlags.LS);
  }

  /**
   * The description including default values and bounds/limits
   * @return description string
   */
  public String getUsageDescription() {
    final StringBuilder description = new StringBuilder(getDescription());

    final List<String> range = getParameterRange();
    if (range != null) {
      if (description.length() > 0) {
        description.append(". ");
      }
      description.append("Allowed values are ").append(Arrays.toString(range.toArray()));
    }
    final String minMaxUsage;
    if (mPsuedoMinMaxString != null) {
      minMaxUsage = mPsuedoMinMaxString;
    } else {
      minMaxUsage = minMaxUsage(getMinCount(), getMaxCount(), mRangeList);
    }
    if (minMaxUsage.length() > 0) {
      description.append(". ").append(minMaxUsage);
    }

    final Object def = getParameterDefault();
    if (def != null) {
      final String defs;
      if (def instanceof Double) {
        defs = Utils.realFormat((Double) def);
      } else if (isValidEnum(mParameterType)) {
        defs = def.toString().toLowerCase(Locale.getDefault());
      } else {
        defs = def.toString();
      }
      if (description.length() > 0) {
        description.append(" ");
      }
      description.append("(Default is ").append(defs).append(")");
    }
    return description.toString();
  }

  private static String autoDescription(final Class<?> type) {
    final String result = type.getName();
    return result.substring(result.lastIndexOf('.') + 1).toUpperCase(Locale.getDefault());
  }

  private static final Set<String> BOOLEAN_AFFIRMATIVE = new HashSet<>();
  private static final Set<String> BOOLEAN_NEGATIVE = new HashSet<>();
  static {
    BOOLEAN_AFFIRMATIVE.addAll(Arrays.asList("true", "yes", "y", "t", "1", "on", "aye", "hai", "ja", "da", "ya", "positive", "fer-shure", "totally", "affirmative", "+5v"));
    BOOLEAN_NEGATIVE.addAll(Arrays.asList("false", "no", "n", "f", "0", "off"));
  }

  @SuppressWarnings("unchecked")
  static <T> T instanceHelper(final Class<T> type, final String stringRep) {
    try {
      if (type == Boolean.class) {
        final String lStr = stringRep.toLowerCase(Locale.getDefault());
        if (BOOLEAN_AFFIRMATIVE.contains(lStr)) {
          return (T) Boolean.TRUE;
        } else if (BOOLEAN_NEGATIVE.contains(lStr)) {
          return (T) Boolean.FALSE;
        } else {
          throw new IllegalArgumentException("Invalid boolean value " + stringRep);
        }
      } else if (type == Byte.class) {
        return (T) Byte.valueOf(stringRep);
      } else if (type == Character.class) {
        return (T) Character.valueOf(stringRep.charAt(0));
      } else if (type == Float.class) {
        return (T) Float.valueOf(stringRep);
      } else if (type == Double.class) {
        return (T) Double.valueOf(stringRep);
      } else if (type == Integer.class) {
        return (T) Integer.valueOf(stringRep);
      } else if (type == Long.class) {
        return (T) Long.valueOf(stringRep);
      } else if (type == Short.class) {
        return (T) Short.valueOf(stringRep);
      } else if (type == File.class) {
        return (T) new File(stringRep);
      } else if (type == URL.class) {
        return (T) new URL(stringRep);
      } else if (type == String.class) {
        return (T) stringRep;
      } else if (isValidEnum(type)) {
        return (T) valueOf(type, stringRep);
      } else if (type == Class.class) {
        return (T) Class.forName(stringRep);
      } else if (type == IntegerOrPercentage.class) {
        return (T) IntegerOrPercentage.valueOf(stringRep);
      }
    } catch (final MalformedURLException | ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(""); // We rely on this message being empty
    }
    throw new IllegalArgumentException("Unknown parameter type: " + type);
  }

  /**
   * When enabled, this flag can take a comma-separated list of range values
   * and produce an list of those values
   * @return this flag, so calls can be chained.
   */
  public Flag<T> enableCsv() {
    mRangeList = true;
    return this;
  }

  /**
   * @param category the category to set
   * @return this flag, so calls can be chained.
   */
  public Flag<T> setCategory(final String category) {
    mCategory = category;
    return this;
  }

  /**
   * @return the category
   */
  public String getCategory() {
    return mCategory;
  }
}
