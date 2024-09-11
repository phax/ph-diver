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

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.version.Version;

/**
 * Default implementation of {@link IDVRPseudoVersion}
 *
 * @author Philip Helger
 * @since 1.2.0
 */
public class DVRPseudoVersion implements IDVRPseudoVersion
{
  private final String m_sID;
  private final IDVRPseudoVersionComparable m_aComparable;

  public DVRPseudoVersion (@Nonnull @Nonempty final String sID, @Nonnull final IDVRPseudoVersionComparable aComparable)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (aComparable, "Comparable");
    m_sID = sID;
    m_aComparable = aComparable;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  public int compareToPseudoVersion (@Nonnull final IDVRPseudoVersion aOtherPseudoVersion)
  {
    ValueEnforcer.notNull (aOtherPseudoVersion, "OtherPseudoVersion");

    // Same pseudo version is always identical
    if (m_sID.equals (aOtherPseudoVersion.getID ()))
      return 0;

    // Pass to handler
    return m_aComparable.compareToPseudoVersion (aOtherPseudoVersion);
  }

  public int compareToVersion (@Nonnull final Version aStaticVersion)
  {
    ValueEnforcer.notNull (aStaticVersion, "StaticVersion");

    // Pass to handler
    return m_aComparable.compareToVersion (aStaticVersion);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRPseudoVersion rhs = (DVRPseudoVersion) o;
    return m_sID.equals (rhs.m_sID);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ID", m_sID).getToString ();
  }
}
