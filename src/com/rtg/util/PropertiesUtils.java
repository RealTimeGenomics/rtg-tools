/*
 * Copyright (c) 2016. Real Time Genomics Limited.
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

package com.rtg.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.rtg.util.diagnostic.ErrorType;


/**
 * Utility class for property file loading and parsing.
 */
public final class PropertiesUtils {
  private PropertiesUtils() { }

  static final String VARIANT_PRIORS = "com/rtg/variant/priors/";
  static final String ALIGNMENT_PENALTIES = "com/rtg/alignment/penalties/";

  /**
   * Types of properties files, and where they live
   */
  public enum PropertyType {
    /** Error property sub-directory **/
   ERROR_PROPERTY(VARIANT_PRIORS + "error/"),
    /** Prior property sub-directory **/
    PRIOR_PROPERTY(VARIANT_PRIORS + "prior/"),
    /** CNV property sub-directory **/
    CNV_PROPERTY(VARIANT_PRIORS + "cnv/"),
    /** Alignment error penalties */
    ALIGNMENT_PROPERTY_TYPE(ALIGNMENT_PENALTIES);


    final String mPath;
    PropertyType(String path) {
      mPath = path;
    }

    /**
     * @return the path to the properties
     */
    public String path() {
      return mPath;
    }
  }

  /**
   * Method for loading a priors properties resource file or a properties file.
   * @param propsFile the priors property file or resource to load.
   * @param propertyType the type of property file being loaded (sub-directory).
   * @return Loaded properties object.
   * @throws InvalidParamsException when the property file does not exist.
   * @throws IOException when there is an error reading the property files.
   */
  public static Properties getPriorsResource(final String propsFile, final PropertyType propertyType) throws InvalidParamsException, IOException {
    final Properties pr = new Properties();
    InputStream props = null;
    try {
      final InputStream def = Resources.getResourceAsStream(propertyType.path() + propsFile + ".properties");
      if (def != null) {
        props = def;
      } else {
        try {
          props = new FileInputStream(propsFile);
        } catch (final FileNotFoundException e) {
          throw new InvalidParamsException(ErrorType.INFO_ERROR, "Invalid prior option \"" + propsFile + "\"");
        }
      }
      try {
        pr.load(props);
      } catch (final IOException e) {
        throw new InvalidParamsException(ErrorType.PROPS_LOAD_FAILED, "Priors", propsFile, e.getMessage());
      } catch (final IllegalArgumentException e) {
        throw new InvalidParamsException(ErrorType.PROPS_INVALID, "Priors", propsFile);
      }
    } finally {
      if (props != null) {
        props.close();
      }
    }
    return pr;
  }
}
