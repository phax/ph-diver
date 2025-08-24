/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.diver.api.settings;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.cache.regex.RegExHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Helper class to check DVR Coordinate consistency. It is provided in its own package, to avoid
 * cyclic package dependencies between "coord" and "version".
 *
 * @author Philip Helger
 */
@Immutable
public final class DVRValidityHelper
{
  private DVRValidityHelper ()
  {}

  private static boolean _isValidPart (@Nonnull final String sPart,
                                       @Nonnegative final int nMinLen,
                                       @Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isTrue ( () -> nMinLen <= nMaxLen,
                           () -> "Min length (" + nMinLen + ") must be <= Max length (" + nMaxLen + ")");
    return RegExHelper.stringMatchesPattern ("[a-zA-Z0-9_\\-\\.]{" + nMinLen + "," + nMaxLen + "}", sPart);
  }

  /**
   * Check if the provided part is a syntactically valid coordinate Group ID.
   *
   * @param sPart
   *        The part to be checked. May be <code>null</code>.
   * @return <code>true</code> if it is valid, <code>false</code> if not.
   */
  public static boolean isValidCoordinateGroupID (@Nullable final String sPart)
  {
    if (StringHelper.isEmpty (sPart))
      return false;
    return _isValidPart (sPart,
                         DVRGlobalCoordinateSettings.getGroupIDMinLen (),
                         DVRGlobalCoordinateSettings.getGroupIDMaxLen ());
  }

  /**
   * Check if the provided part is a syntactically valid coordinate Artifact ID.
   *
   * @param sPart
   *        The part to be checked. May be <code>null</code>.
   * @return <code>true</code> if it is valid, <code>false</code> if not.
   */
  public static boolean isValidCoordinateArtifactID (@Nullable final String sPart)
  {
    if (StringHelper.isEmpty (sPart))
      return false;
    return _isValidPart (sPart,
                         DVRGlobalCoordinateSettings.getArtifactIDMinLen (),
                         DVRGlobalCoordinateSettings.getArtifactIDMaxLen ());
  }

  /**
   * Check if the provided part is a syntactically valid coordinate Version.
   *
   * @param sPart
   *        The part to be checked. May be <code>null</code>.
   * @return <code>true</code> if it is valid, <code>false</code> if not.
   */
  public static boolean isValidCoordinateVersion (@Nullable final String sPart)
  {
    if (StringHelper.isEmpty (sPart))
      return false;
    return _isValidPart (sPart,
                         DVRGlobalCoordinateSettings.getVersionMinLen (),
                         DVRGlobalCoordinateSettings.getVersionMaxLen ());
  }

  /**
   * Check if the provided part is a syntactically valid coordinate Classifier.
   *
   * @param sPart
   *        The part to be checked. May be <code>null</code>.
   * @return <code>true</code> if it is valid, <code>false</code> if not.
   */
  public static boolean isValidCoordinateClassifier (@Nullable final String sPart)
  {
    // Classifier is optional
    if (StringHelper.isEmpty (sPart))
      return true;
    return _isValidPart (sPart,
                         DVRGlobalCoordinateSettings.getClassifierMinLen (),
                         DVRGlobalCoordinateSettings.getClassifierMaxLen ());
  }

}
