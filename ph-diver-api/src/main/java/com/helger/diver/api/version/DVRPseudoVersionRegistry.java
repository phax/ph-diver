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
package com.helger.diver.api.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.version.Version;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.diver.api.version.spi.IDVRPseudoVersionRegistrarSPI;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    @Override
    public String toString ()
    {
      return "OLDEST";
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

    @Override
    public String toString ()
    {
      return "LATEST";
    }
  });

  /**
   * Latest indicates the very latest version (excluding snapshot).
   */
  public static final IDVRPseudoVersion LATEST_RELEASE;

  // Inside the static block for best formatting :)
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

      @Override
      public String toString ()
      {
        return "LATEST_RELEASE";
      }
    });
  }

  private static final class SingletonHolder
  {
    static final DVRPseudoVersionRegistry INSTANCE = new DVRPseudoVersionRegistry ();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRPseudoVersionRegistry.class);

  private final ICommonsMap <String, IDVRPseudoVersion> m_aMap = new CommonsHashMap <> ();

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
    m_aMap.clear ();

    // Register all again
    for (final IDVRPseudoVersionRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IDVRPseudoVersionRegistrarSPI.class))
      aSPI.registerPseudoVersions (this);

    if (bLog)
      LOGGER.info ("Finished reinitializing the DVRPseudoVersionRegistry with " + m_aMap.size () + " entries");
  }

  /**
   * Remove all existing registrations and re-run the SPI search
   */
  public final void reinitialize ()
  {
    _reinitialize (true);
  }

  @Nonnull
  public EChange registerPseudoVersion (@Nonnull final IDVRPseudoVersion aPseudoVersion)
  {
    ValueEnforcer.notNull (aPseudoVersion, "PseudoVersion");

    final String sKey = aPseudoVersion.getID ();
    if (m_aMap.containsKey (sKey))
    {
      LOGGER.error ("Another pseudoversion with ID '" + sKey + "' is already registered");
      return EChange.UNCHANGED;
    }
    m_aMap.put (sKey, aPseudoVersion);
    return EChange.CHANGED;
  }

  @Nullable
  public IDVRPseudoVersion getFromIDOrNull (@Nullable final String sID)
  {
    return m_aMap.get (sID);
  }

  @Nonnegative
  @VisibleForTesting
  final int size ()
  {
    return m_aMap.size ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRPseudoVersionRegistry rhs = (DVRPseudoVersionRegistry) o;
    return m_aMap.equals (rhs.m_aMap);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aMap).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Map", m_aMap).getToString ();
  }
}
