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
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.version.Version;
import com.helger.diver.api.version.spi.IVESPseudoVersionRegistrarSPI;

/**
 * Registry for all known {@link IVESPseudoVersion} instances.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@NotThreadSafe
public class VESPseudoVersionRegistry implements IVESPseudoVersionRegistry
{
  /**
   * Oldest indicates the very first (oldest) version.
   */
  public static final IVESPseudoVersion OLDEST = new VESPseudoVersion ("oldest", new IPseudoVersionComparable ()
  {
    public int compareToPseudoVersion (@Nonnull final IVESPseudoVersion aOtherPseudoVersion)
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
   * Latest indicates the very latest version.
   */
  public static final IVESPseudoVersion LATEST = new VESPseudoVersion ("latest", new IPseudoVersionComparable ()
  {
    public int compareToPseudoVersion (@Nonnull final IVESPseudoVersion aOtherPseudoVersion)
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

  private static final class SingletonHolder
  {
    static final VESPseudoVersionRegistry INSTANCE = new VESPseudoVersionRegistry ();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (VESPseudoVersionRegistry.class);

  private final ICommonsMap <String, IVESPseudoVersion> m_aPVs = new CommonsHashMap <> ();

  private VESPseudoVersionRegistry ()
  {
    _reinitialize (false);
  }

  @Nonnull
  public static VESPseudoVersionRegistry getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }

  private void _reinitialize (final boolean bLog)
  {
    if (bLog)
      LOGGER.info ("Reinitializing the VESPseudoVersionRegistry");

    // Remove existing
    m_aPVs.clear ();

    // Register all again
    for (final IVESPseudoVersionRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (IVESPseudoVersionRegistrarSPI.class))
      aSPI.registerPseudoVersions (this);

    if (bLog)
      LOGGER.info ("Finished reinitializing the VESPseudoVersionRegistry with " + m_aPVs.size () + " entries");
  }

  public final void reinitialize ()
  {
    _reinitialize (true);
  }

  @Nonnull
  public IVESPseudoVersion registerPseudoVersion (@Nonnull final IVESPseudoVersion aPseudoVersion)
  {
    ValueEnforcer.notNull (aPseudoVersion, "PseudoVersion");

    final String sKey = aPseudoVersion.getID ();
    if (m_aPVs.containsKey (sKey))
      throw new IllegalArgumentException ("Another pseudoversion with ID '" + sKey + "' is already registered");
    m_aPVs.put (sKey, aPseudoVersion);
    return aPseudoVersion;
  }

  @Nullable
  public IVESPseudoVersion getFromIDOrNull (@Nullable final String sID)
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
    final VESPseudoVersionRegistry rhs = (VESPseudoVersionRegistry) o;
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
