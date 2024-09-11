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
package com.helger.diver.api.id;

import javax.annotation.Nonnegative;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;

/**
 * This class contains global settings for every DVRID used. Modifies the
 * validity of all DVRIDs around.
 *
 * @author Philip Helger
 * @since 1.0.2
 */
@NotThreadSafe
public final class DVRIDSettings
{
  public static final int DEFAULT_MAX_GROUP_ID_LEN = 64;
  public static final int DEFAULT_MAX_ARTIFACT_ID_LEN = 64;
  public static final int DEFAULT_MAX_VERSION_LEN = 64;
  public static final int DEFAULT_MAX_CLASSIFIER_LEN = 64;

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRIDSettings.class);

  private static int s_nMaxGroupIDLen = DEFAULT_MAX_GROUP_ID_LEN;
  private static int s_nMaxArtifactIDLen = DEFAULT_MAX_ARTIFACT_ID_LEN;
  private static int s_nMaxVersionLen = DEFAULT_MAX_VERSION_LEN;
  private static int s_nMaxClassifierLen = DEFAULT_MAX_CLASSIFIER_LEN;

  private DVRIDSettings ()
  {}

  @Nonnegative
  public static int getMaxGroupIDLen ()
  {
    return s_nMaxGroupIDLen;
  }

  public static void setMaxGroupIDLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nMaxGroupIDLen)
    {
      LOGGER.warn ("Changed the maximum group ID length of DVRID from " + s_nMaxGroupIDLen + " to " + nMaxLen);
      s_nMaxGroupIDLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getMaxArtifactIDLen ()
  {
    return s_nMaxArtifactIDLen;
  }

  public static void setMaxArtifactIDLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nMaxArtifactIDLen)
    {
      LOGGER.warn ("Changed the maximum artifact ID length of DVRID from " + s_nMaxArtifactIDLen + " to " + nMaxLen);
      s_nMaxArtifactIDLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getMaxVersionLen ()
  {
    return s_nMaxVersionLen;
  }

  public static void setMaxVersionLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nMaxVersionLen)
    {
      LOGGER.warn ("Changed the maximum version length of DVRID from " + s_nMaxVersionLen + " to " + nMaxLen);
      s_nMaxVersionLen = nMaxLen;
    }
  }

  @Nonnegative
  public static int getMaxClassifierLen ()
  {
    return s_nMaxClassifierLen;
  }

  public static void setMaxClassifierLen (@Nonnegative final int nMaxLen)
  {
    ValueEnforcer.isGT0 (nMaxLen, "MaxLen");
    if (nMaxLen != s_nMaxClassifierLen)
    {
      LOGGER.warn ("Changed the maximum classifier length of DVRID from " + s_nMaxClassifierLen + " to " + nMaxLen);
      s_nMaxClassifierLen = nMaxLen;
    }
  }
}
