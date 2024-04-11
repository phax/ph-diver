package com.helger.diver.api.version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.version.Version;

/**
 * Registry for all known {@link IVESPseudoVersion} instances.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class VESPseudoVersionRegistry
{
  private static final VESPseudoVersionRegistry INSTANCE = new VESPseudoVersionRegistry ();

  /**
   * Latest indicates the very latest version.
   */
  public static final IVESPseudoVersion LATEST;
  static
  {
    LATEST = getInstance ().registerPseudoVersion (new VESPseudoVersion ("latest", new IPseudoVersionComparable ()
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
    }));
  }

  /**
   * Oldest indicates the very first (oldest) version.
   */
  public static final IVESPseudoVersion OLDEST;
  static
  {
    OLDEST = getInstance ().registerPseudoVersion (new VESPseudoVersion ("oldest", new IPseudoVersionComparable ()
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
    }));
  }

  private final ICommonsMap <String, IVESPseudoVersion> m_aPVs = new CommonsHashMap <> ();

  private VESPseudoVersionRegistry ()
  {}

  @Nonnull
  public static VESPseudoVersionRegistry getInstance ()
  {
    return INSTANCE;
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
