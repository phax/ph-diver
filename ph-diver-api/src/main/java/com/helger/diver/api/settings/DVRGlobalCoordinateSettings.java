/*
 * Copyright (C) 2023-2024 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnegative;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;

/**
 * This class contains global settings for every DVR coordinate used. Modifies
 * the validity of all DVR coordinates around, so handle with care.
 *
 * @author Philip Helger
 * @since 1.0.2
 */
@NotThreadSafe
public final class DVRGlobalCoordinateSettings
{
  public static final int DEFAULT_MIN_LEN = 1;
  public static final int DEFAULT_GROUP_ID_MAX_LEN = 64;
  public static final int DEFAULT_ARTIFACT_ID_MAX_LEN = 64;
  public static final int DEFAULT_VERSION_MAX_LEN = 64;
  public static final int DEFAULT_CLASSIFIER_MAX_LEN = 64;

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRGlobalCoordinateSettings.class);

  private static int s_nGroupIDMinLen = DEFAULT_MIN_LEN;
  private static int s_nGroupIDMaxLen = DEFAULT_GROUP_ID_MAX_LEN;

  private static int s_nArtifactIDMinLen = DEFAULT_MIN_LEN;
  private static int s_nArtifactIDMaxLen = DEFAULT_ARTIFACT_ID_MAX_LEN;

  private static int s_nVersionMinLen = DEFAULT_MIN_LEN;
  private static int s_nVersionMaxLen = DEFAULT_VERSION_MAX_LEN;

  private static int s_nClassifierMinLen = DEFAULT_MIN_LEN;
  private static int s_nClassifierMaxLen = DEFAULT_CLASSIFIER_MAX_LEN;

  private DVRGlobalCoordinateSettings ()
  {}

  @Nonnegative
  public static int getGroupIDMinLen ()
  {
    return s_nGroupIDMinLen;
  }

  @Nonnegative
  public static int getGroupIDMaxLen ()
  {
    return s_nGroupIDMaxLen;
  }

  public static void setGroupIDMaxLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nGroupIDMaxLen)
    {
      LOGGER.warn ("Changed the maximum group ID length of DVR Coordinate from " + s_nGroupIDMaxLen + " to " + nMaxLen);
      s_nGroupIDMaxLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getArtifactIDMinLen ()
  {
    return s_nArtifactIDMinLen;
  }

  @Nonnegative
  public static int getArtifactIDMaxLen ()
  {
    return s_nArtifactIDMaxLen;
  }

  public static void setArtifactIDMaxLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nArtifactIDMaxLen)
    {
      LOGGER.warn ("Changed the maximum artifact ID length of DVR Coordinate from " +
                   s_nArtifactIDMaxLen +
                   " to " +
                   nMaxLen);
      s_nArtifactIDMaxLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getVersionMinLen ()
  {
    return s_nVersionMinLen;
  }

  @Nonnegative
  public static int getVersionMaxLen ()
  {
    return s_nVersionMaxLen;
  }

  public static void setVersionMaxLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nVersionMaxLen)
    {
      LOGGER.warn ("Changed the maximum version length of DVR Coordinate from " + s_nVersionMaxLen + " to " + nMaxLen);
      s_nVersionMaxLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getClassifierMinLen ()
  {
    return s_nClassifierMinLen;
  }

  @Nonnegative
  public static int getClassifierMaxLen ()
  {
    return s_nClassifierMaxLen;
  }

  public static void setClassifierMaxLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nClassifierMaxLen)
    {
      LOGGER.warn ("Changed the maximum classifier length of DVR Coordinate from " +
                   s_nClassifierMaxLen +
                   " to " +
                   nMaxLen);
      s_nClassifierMaxLen = nMaxLen;
    }
  }
}
