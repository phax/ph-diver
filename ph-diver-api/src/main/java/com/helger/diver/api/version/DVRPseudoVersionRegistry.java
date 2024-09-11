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
package com.helger.diver.api.version;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.version.Version;
import com.helger.diver.api.version.spi.IDVRPseudoVersionRegistrarSPI;

/**
 * Registry for all known {@link IDVRPseudoVersion} instances.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@NotThreadSafe
public class DVRPseudoVersionRegistry implements IDVRPseudoVersionRegistry
{
  /**
   * Oldest indicates the very first (oldest) version.
   */
  public static final IDVRPseudoVersion OLDEST = new DVRPseudoVersion ("oldest", new IDVRPseudoVersionComparable ()
  {
    public int compareToPseudoVersion (@Nonnull final IDVRPseudoVersion aOtherPseudoVersion)
    {
      // OLDEST is always smaller
      return -1;
    }

    public int compareToVersion (@Nonnull final Version aStaticVersion)
    {
      // OLDEST is always smaller
      return -1;
    }
  });

  /**
   * Latest indicates the very latest version (including snapshot).
   */
  public static final IDVRPseudoVersion LATEST = new DVRPseudoVersion ("latest", new IDVRPseudoVersionComparable ()
  {
    public int compareToPseudoVersion (@Nonnull final IDVRPseudoVersion aOtherPseudoVersion)
    {
      // LATEST is always greater
      return +1;
    }

    public int compareToVersion (@Nonnull final Version aStaticVersion)
    {
      // LATEST is always greater
      return +1;
    }
  });

  /**
   * Latest indicates the very latest version (excluding snapshot).
   */
  public static final IDVRPseudoVersion LATEST_RELEASE;
  static
  {
    LATEST_RELEASE = new DVRPseudoVersion ("latest-release", new IDVRPseudoVersionComparable ()
    {
      public int compareToPseudoVersion (@Nonnull final IDVRPseudoVersion aOtherPseudoVersion)
      {
        // We are before LATEST
        if (aOtherPseudoVersion.equals (LATEST))
          return -1;

        // LATEST_RELEASE is always greater than the rest
        return +1;
      }

      public int compareToVersion (@Nonnull final Version aStaticVersion)
      {
        // LATEST_RELEASE is always greater
        return +1;
      }
    });
  }

  private static final class SingletonHolder
  {
    static final DVRPseudoVersionRegistry INSTANCE = new DVRPseudoVersionRegistry ();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRPseudoVersionRegistry.class);

  private final ICommonsMap <String, IDVRPseudoVersion> m_aPVs = new CommonsHashMap <> ();

  private DVRPseudoVersionRegistry ()
  {
    _reinitialize (false);
  }

  @Nonnull
  public static DVRPseudoVersionRegistry getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  private void _reinitialize (final boolean bLog)
  {
    if (bLog)
      LOGGER.info ("Reinitializing the DVRPseudoVersionRegistry");

    // Remove existing
    m_aPVs.clear ();

    // Register all again
    for (final IDVRPseudoVersionRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IDVRPseudoVersionRegistrarSPI.class))
      aSPI.registerPseudoVersions (this);

    if (bLog)
      LOGGER.info ("Finished reinitializing the DVRPseudoVersionRegistry with " + m_aPVs.size () + " entries");
  }

  public final void reinitialize ()
  {
    _reinitialize (true);
  }

  @Nonnull
  public EChange registerPseudoVersion (@Nonnull final IDVRPseudoVersion aPseudoVersion)
  {
    ValueEnforcer.notNull (aPseudoVersion, "PseudoVersion");

    final String sKey = aPseudoVersion.getID ();
    if (m_aPVs.containsKey (sKey))
    {
      LOGGER.error ("Another pseudoversion with ID '" + sKey + "' is already registered");
      return EChange.UNCHANGED;
    }
    m_aPVs.put (sKey, aPseudoVersion);
    return EChange.CHANGED;
  }

  @Nullable
  public IDVRPseudoVersion getFromIDOrNull (@Nullable final String sID)
  {
    return m_aPVs.get (sID);
  }

  @Nonnegative
  @VisibleForTesting
  final int size ()
  {
    return m_aPVs.size ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRPseudoVersionRegistry rhs = (DVRPseudoVersionRegistry) o;
    return m_aPVs.equals (rhs.m_aPVs);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aPVs).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Map", m_aPVs).getToString ();
  }
}
