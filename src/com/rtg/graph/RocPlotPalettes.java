/*
 * Copyright (c) 2018. Real Time Genomics Limited.
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
package com.rtg.graph;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.rtg.util.Resources;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Provide a built-in set of named palettes
 */
public final class RocPlotPalettes {

  private static final String DEFAULT_CONFIG = "com/rtg/graph/palettes.properties";

  // Property key used to indicate the name of the default palette
  private static final String DEFAULT = "default_palette";

  /** The public palette instance */
  public static final RocPlotPalettes SINGLETON = new RocPlotPalettes();


  private final Map<String, Color[]> mPalettes = new TreeMap<>();
  private final String mDefault;

  private RocPlotPalettes() {
    try (InputStream is = Resources.getResourceAsStream(DEFAULT_CONFIG)) {
      String defaultPalette = null;
      final Properties props = new Properties();
      props.load(is);
      for (String pname : props.stringPropertyNames()) {
        if (DEFAULT.equals(pname)) {
          defaultPalette = props.getProperty(pname);
        } else {
          final String palette = props.getProperty(pname);
          mPalettes.put(pname, Arrays.stream(palette.split(","))
            .map(String::trim).map(Color::decode)
            .toArray(Color[]::new));
        }
      }
      if (mPalettes.size() == 0) {
        throw new NoTalkbackSlimException("No named palettes found in palette config!");
      } else if (defaultPalette == null) {
        throw new NoTalkbackSlimException("No default palette specified via " + DEFAULT + " property!");
      } else if (!mPalettes.containsKey(defaultPalette)) {
        throw new NoTalkbackSlimException("Specified default palette " + defaultPalette + " was not provided!");
      }
      mDefault = defaultPalette;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the name of the default palette
   */
  public String defaultName() {
    return mDefault;
  }

  /**
   * @return the set of known palettes
   */
  public Set<String> names() {
    return mPalettes.keySet();
  }

  /**
   * @param name the palette name
   * @return the named palette
   */
  public Color[] getPalette(String name) {
    return mPalettes.get(name);
  }
}
