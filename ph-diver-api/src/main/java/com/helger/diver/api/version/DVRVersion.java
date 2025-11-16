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

import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementComparable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.version.Version;
import com.helger.collection.CollectionHelper;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diver.api.settings.DVRValidityHelper;

/**
 * This class contains the version of a DVR Coordinate. This can either be a static version or a
 * pseudo version. This version type has a specific kind of ordering, so that versions using the
 * classifier "SNAPSHOT" are ordered BEFORE respective release versions. Example order:
 * <ol>
 * <li>1.0</li>
 * <li>1.1-SNAPSHOT</li>
 * <li>1.1</li>
 * <li>1.2</li>
 * <li>1.3-SNAPSHOT</li>
 * <li>1.3</li>
 * </ol>
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementComparable
@MustImplementEqualsAndHashcode
public final class DVRVersion implements Comparable <DVRVersion>
{
  /** Specific qualifier for "SNAPSHOT" versions" */
  public static final String QUALIFIER_SNAPSHOT = "SNAPSHOT";
  /** Separator between major and minor and between minor and micro version */
  public static final char NUMERIC_VERSION_PART_SEPARATOR = '.';
  /** Separate between classifier and the rest (if available) */
  public static final char DEFAULT_CLASSIFIER_SEPARATOR = '-';

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRVersion.class);

  private final Version m_aStaticVersion;
  private final IDVRPseudoVersion m_aPseudoVersion;

  /**
   * Constructor - only invoked by the static factory methods below
   *
   * @param aStaticVersion
   *        Static version. May be <code>null</code>.
   * @param aPseudoVersion
   *        Pseudo version. May be <code>null</code>.
   */
  private DVRVersion (@Nullable final Version aStaticVersion, @Nullable final IDVRPseudoVersion aPseudoVersion)
  {
    ValueEnforcer.isTrue (aStaticVersion != null || aPseudoVersion != null,
                          "Either Static Version or Pseudo Version must be provided");
    ValueEnforcer.isFalse (aStaticVersion != null && aPseudoVersion != null,
                           "Only one of Static Version or Pseudo Version must be provided");
    m_aStaticVersion = aStaticVersion;
    m_aPseudoVersion = aPseudoVersion;
  }

  /**
   * @return <code>true</code> if it is a static version, <code>false</code> if it is a pseudo
   *         version
   * @see #isPseudoVersion()
   * @see #getStaticVersion()
   */
  public boolean isStaticVersion ()
  {
    return m_aStaticVersion != null;
  }

  /**
   * @param sQualifier
   *        The qualifier to check. May be <code>null</code>.,
   * @return <code>true</code> if the qualifier is "SNAPSHOT".
   * @since 3.0.0
   */
  public static boolean isStaticSnapshotVersion (@Nullable final String sQualifier)
  {
    return QUALIFIER_SNAPSHOT.equals (sQualifier);
  }

  /**
   * @param aVer
   *        The version to check. May be <code>null</code>.,
   * @return <code>true</code> if the passed version has the the qualifier "SNAPSHOT".
   * @since 1.0.1
   */
  public static boolean isStaticSnapshotVersion (@Nullable final Version aVer)
  {
    return aVer != null && isStaticSnapshotVersion (aVer.getQualifier ());
  }

  /**
   * @return <code>true</code> if this is a static version, and if the qualifier is "SNAPSHOT".
   * @see #isStaticVersion()
   */
  public boolean isStaticSnapshotVersion ()
  {
    return isStaticSnapshotVersion (m_aStaticVersion);
  }

  /**
   * @return The static version of this VER version. Guaranteed to be non-<code>null</code> if
   *         {@link #isStaticVersion()} returns true.
   * @see #isStaticVersion()
   */
  @Nullable
  public Version getStaticVersion ()
  {
    return m_aStaticVersion;
  }

  /**
   * @return <code>true</code> if it is a pseudo version, <code>false</code> if it is a static
   *         version
   * @see #isStaticVersion()
   * @see #getPseudoVersion()
   */
  public boolean isPseudoVersion ()
  {
    return m_aPseudoVersion != null;
  }

  /**
   * @return The pseudo version of this VER version. Guaranteed to be non-<code>null</code> if
   *         {@link #isPseudoVersion()} returns true.
   * @see #isPseudoVersion()
   */
  @Nullable
  public IDVRPseudoVersion getPseudoVersion ()
  {
    return m_aPseudoVersion;
  }

  @NonNull
  private static String _getAsString (@NonNull final Version aVersion,
                                      final char cClassifierSep,
                                      final boolean bEnforceAllNumbers,
                                      final boolean bEnforceAtLeastMinor)
  {
    // Different implementation then Version.getAsString (...)
    String ret = "";
    char cSep = cClassifierSep;
    boolean bMust = bEnforceAllNumbers;

    // Start from the back: classifier
    if (aVersion.hasQualifier ())
      ret = aVersion.getQualifier ();

    // Add micro version
    if (aVersion.getMicro () > 0 || bMust)
    {
      if (!ret.isEmpty ())
        ret = cSep + ret;
      ret = aVersion.getMicro () + ret;
      // Change separator to number version separator
      cSep = NUMERIC_VERSION_PART_SEPARATOR;
      bMust = true;
    }

    if (bEnforceAtLeastMinor)
      bMust = true;

    // Add minor version
    if (aVersion.getMinor () > 0 || bMust)
    {
      if (!ret.isEmpty ())
        ret = cSep + ret;
      ret = aVersion.getMinor () + ret;
      // Change separator to number version separator
      cSep = '.';
      bMust = true;
    }

    // Add major version
    // Avoid empty string
    if (aVersion.getMajor () > 0 || bMust || ret.isEmpty ())
    {
      if (!ret.isEmpty ())
        ret = cSep + ret;
      ret = aVersion.getMajor () + ret;
    }

    return ret;
  }

  @NonNull
  public static String getAsString (@NonNull final Version aVersion)
  {
    return _getAsString (aVersion, DEFAULT_CLASSIFIER_SEPARATOR, false, false);
  }

  @NonNull
  @Nonempty
  public static String getAsString (@NonNull final IDVRPseudoVersion aPseudoVersion)
  {
    return aPseudoVersion.getID ();
  }

  /**
   * @return The unified string representation of the Version.
   */
  @NonNull
  public String getAsString ()
  {
    if (m_aStaticVersion != null)
      return getAsString (m_aStaticVersion);

    return getAsString (m_aPseudoVersion);
  }

  @NonNull
  private static Version _getWithoutQualifier (@NonNull final Version aSrc)
  {
    return new Version (aSrc.getMajor (), aSrc.getMinor (), aSrc.getMicro (), null);
  }

  private static int _compareSemantically (@NonNull final Version aLhs, @NonNull final Version aRhs)
  {
    if (QUALIFIER_SNAPSHOT.equals (aLhs.getQualifier ()))
    {
      if (QUALIFIER_SNAPSHOT.equals (aRhs.getQualifier ()))
      {
        // Lhs & Rhs are Snapshots
        return aLhs.compareTo (aRhs);
      }
      // Lhs is Snapshot
      final Version aLhsClean = _getWithoutQualifier (aLhs);
      final int nCmp = aLhsClean.compareTo (aRhs);
      if (nCmp == 0)
      {
        // Snapshots comes before release
        return -1;
      }
      return nCmp;
    }

    if (QUALIFIER_SNAPSHOT.equals (aRhs.getQualifier ()))
    {
      // Rhs is Snapshot
      final Version aRhsClean = _getWithoutQualifier (aRhs);
      final int nCmp = aLhs.compareTo (aRhsClean);
      if (nCmp == 0)
      {
        // Snapshots comes before release
        return +1;
      }
      return nCmp;
    }

    // No snapshot version contained
    return aLhs.compareTo (aRhs);
  }

  /**
   * Compare a static version with a pseudo version
   *
   * @param aStaticVersion
   *        Static version. May not be <code>null</code>.
   * @param aPseudoVersion
   *        Pseudo version. May not be <code>null</code>.
   * @return -1, 0 or +1
   */
  private static int _compareWithPseudoVersion (@NonNull final Version aStaticVersion,
                                                @NonNull final IDVRPseudoVersion aPseudoVersion)
  {
    // Change sign, due to calling order
    return -aPseudoVersion.compareToVersion (aStaticVersion);
  }

  public int compareTo (@NonNull final DVRVersion rhs)
  {
    if (isStaticVersion ())
    {
      if (rhs.isStaticVersion ())
        return _compareSemantically (m_aStaticVersion, rhs.m_aStaticVersion);
      return _compareWithPseudoVersion (m_aStaticVersion, rhs.m_aPseudoVersion);
    }

    // this is a pseudo version
    if (rhs.isStaticVersion ())
    {
      // Invert result
      return -_compareWithPseudoVersion (rhs.m_aStaticVersion, m_aPseudoVersion);
    }

    // Both are psudo versions
    return m_aPseudoVersion.compareToPseudoVersion (rhs.m_aPseudoVersion);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRVersion rhs = (DVRVersion) o;
    return EqualsHelper.equals (m_aStaticVersion, rhs.m_aStaticVersion) &&
           EqualsHelper.equals (m_aPseudoVersion, rhs.m_aPseudoVersion);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_aStaticVersion).append (m_aPseudoVersion).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).appendIfNotNull ("StaticVersion", m_aStaticVersion)
                                       .appendIfNotNull ("PseudoVersion", m_aPseudoVersion)
                                       .getToString ();
  }

  @NonNull
  public static DVRVersion of (@NonNull final Version aVersion)
  {
    ValueEnforcer.notNull (aVersion, "Version");
    return new DVRVersion (aVersion, null);
  }

  @NonNull
  public static DVRVersion of (@NonNull final IDVRPseudoVersion aPseudoVersion)
  {
    ValueEnforcer.notNull (aPseudoVersion, "PseudoVersion");
    return new DVRVersion (null, aPseudoVersion);
  }

  /**
   * @return A new {@link DVRVersion} using the pseudo version "latest".
   */
  @NonNull
  public static DVRVersion latest ()
  {
    return of (DVRPseudoVersionRegistry.LATEST);
  }

  /**
   * @return A new {@link DVRVersion} using the pseudo version "latest-release".
   */
  @NonNull
  public static DVRVersion latestRelease ()
  {
    return of (DVRPseudoVersionRegistry.LATEST_RELEASE);
  }

  /**
   * Checks if the provided version is a valid static version.
   * <ul>
   * <li>1.0.0</li>
   * <li>1.0</li>
   * <li>1</li>
   * <li>1.0.0-SNAPSHOT</li>
   * <li>1.0-SNAPSHOT</li>
   * <li>1-SNAPSHOT</li>
   * <li>1.0.0.SNAPSHOT</li>
   * <li>1.0.SNAPSHOT</li>
   * <li>1.SNAPSHOT</li>
   * </ul>
   *
   * @param sVersion
   *        The version to check
   * @return <code>true</code> if the version is a valid static version, <code>false</code> if not.
   */
  public static boolean isValidStaticVersion (@Nullable final String sVersion)
  {
    // Must not be empty
    if (StringHelper.isEmpty (sVersion))
      return false;

    // Must follow the DVR Coordinate constraints
    if (!DVRValidityHelper.isValidCoordinateVersion (sVersion))
      return false;

    // Parse to Version object
    final Version aParsedVersion = Version.parse (sVersion);
    if (aParsedVersion == null)
      return false;

    // Check if the parsing result equals the original in a way
    // This section clearly would win the price for ugly coding - but the
    // positive effect on consistency is even more valuable :)
    final ICommonsOrderedSet <String> aPossibleVersions = new CommonsLinkedHashSet <> ();
    // Check different separators
    for (final char cClassifierSep : "-.".toCharArray ())
      for (final boolean bEnforceAllNumbers : new boolean [] { true, false })
        for (final boolean bEnforceMinor : new boolean [] { true, false })
        {
          final String sText = _getAsString (aParsedVersion, cClassifierSep, bEnforceAllNumbers, bEnforceMinor);
          if (sVersion.equals (sText))
          {
            // We found a match
            return true;
          }
          aPossibleVersions.add (sText);
        }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("'" +
                    sVersion +
                    "' is none of " +
                    StringImplode.imploder ()
                                 .source (aPossibleVersions, x -> "'" + x + "'")
                                 .separator (" or ")
                                 .build ());

    // Nope, invalid version
    return false;
  }

  @NonNull
  public static DVRVersion parseOrThrow (@Nullable final String sVersion) throws DVRVersionException
  {
    if (StringHelper.isEmpty (sVersion))
      throw new DVRVersionException ("DVR Version string must not be empty");

    // Check pseudo version first
    final IDVRPseudoVersion ePseudoVersion = DVRPseudoVersionRegistry.getInstance ().getFromIDOrNull (sVersion);
    if (ePseudoVersion != null)
      return of (ePseudoVersion);

    if (isValidStaticVersion (sVersion))
    {
      // Try to convert into a Version object instead
      return of (Version.parse (sVersion));
    }

    throw new DVRVersionException ("Failed to parse '" + sVersion + "' to a DVR Version");
  }

  @Nullable
  public static DVRVersion parseOrNull (@Nullable final String sVersion)
  {
    try
    {
      return parseOrThrow (sVersion);
    }
    catch (final DVRVersionException | RuntimeException ex)
    {
      LOGGER.warn (ex.getMessage ());
      return null;
    }
  }

  /**
   * Create a {@link Predicate} that can be used to filter static DVR versions. The returned
   * predicate may be used as a filter when iterating over entries. This method is only meant to
   * work with static versions and does not consider pseudo versions.
   *
   * @param aVersionsToIgnore
   *        Optional set of specific versions to ignore. This may be handy to explicitly rule out
   *        illegal versions. May be <code>null</code> or empty to indicate that no version should
   *        be ignored.
   * @param bIncludeSnapshots
   *        <code>true</code> if SNAPSHOT versions should be allowed by the resulting predicate.
   * @return Never <code>null</code>.
   */
  @NonNull
  public static Predicate <DVRVersion> getStaticVersionAcceptor (@Nullable final Set <String> aVersionsToIgnore,
                                                                 final boolean bIncludeSnapshots)
  {
    if (CollectionHelper.isEmpty (aVersionsToIgnore))
    {
      if (bIncludeSnapshots)
      {
        // We take all
        return x -> true;
      }

      // We take everything except static snapshot versions
      return x -> !x.isStaticSnapshotVersion ();
    }

    // We have something to ignore
    if (bIncludeSnapshots)
    {
      // We take all, except for the ignored versions
      return x -> !aVersionsToIgnore.contains (x.getAsString ());
    }

    // We take all except static snapshot versions and except for the ignored
    // versions
    return x -> !x.isStaticSnapshotVersion () && !aVersionsToIgnore.contains (x.getAsString ());
  }
}
