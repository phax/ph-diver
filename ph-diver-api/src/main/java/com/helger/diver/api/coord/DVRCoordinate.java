/*
 * Copyright (C) 2023-2026 Philip Helger (www.helger.com)
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
package com.helger.diver.api.coord;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementComparable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.compare.CompareHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.hashcode.IHashCodeGenerator;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.diver.api.DVRException;
import com.helger.diver.api.settings.DVRValidityHelper;
import com.helger.diver.api.version.DVRPseudoVersionRegistry;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.api.version.DVRVersionException;
import com.helger.diver.api.version.IDVRPseudoVersion;

/**
 * The DVR Coordinate represents the coordinate of a single technical artefact in a specific
 * version.<br />
 * It was originally called VESID for "Validation Executor Set ID" but is now used in a wider range
 * of use cases. The name was changed for release v2 to DVRID. In v3 the name was changed again to
 * DVR Coordinate.
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementComparable
@MustImplementEqualsAndHashcode
public final class DVRCoordinate implements IDVRCoordinate, Comparable <DVRCoordinate>
{
  /** The separator char between ID elements */
  public static final char PART_SEPARATOR = ':';

  private static final Logger LOGGER = LoggerFactory.getLogger (DVRCoordinate.class);

  private final String m_sGroupID;
  private final String m_sArtifactID;
  private final DVRVersion m_aVersion;
  private final String m_sClassifier;
  // status vars
  private transient int m_nHashCode = IHashCodeGenerator.ILLEGAL_HASHCODE;

  /**
   * Constructor. All parameters must match the constraints from
   * {@link DVRValidityHelper#isValidCoordinateGroupID(String)},
   * {@link DVRValidityHelper#isValidCoordinateArtifactID(String)} and
   * {@link DVRValidityHelper#isValidCoordinateVersion(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param aVersion
   *        Version object. May not be <code>null</code>.
   * @since 1.1.2
   */
  public DVRCoordinate (@NonNull @Nonempty final String sGroupID,
                        @NonNull @Nonempty final String sArtifactID,
                        @NonNull final DVRVersion aVersion)
  {
    this (sGroupID, sArtifactID, aVersion, (String) null);
  }

  /**
   * Constructor. All parameters must match the constraints from
   * {@link DVRValidityHelper#isValidCoordinateGroupID(String)},
   * {@link DVRValidityHelper#isValidCoordinateArtifactID(String)},
   * {@link DVRValidityHelper#isValidCoordinateVersion(String)} and
   * {@link DVRValidityHelper#isValidCoordinateClassifier(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param aVersion
   *        Version object. May not be <code>null</code>.
   * @param sClassifier
   *        Classifier. May be <code>null</code>.
   */
  public DVRCoordinate (@NonNull @Nonempty final String sGroupID,
                        @NonNull @Nonempty final String sArtifactID,
                        @NonNull final DVRVersion aVersion,
                        @Nullable final String sClassifier)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.isTrue ( () -> DVRValidityHelper.isValidCoordinateGroupID (sGroupID),
                           () -> "GroupID '" + sGroupID + "' is invalid");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");
    ValueEnforcer.isTrue ( () -> DVRValidityHelper.isValidCoordinateArtifactID (sArtifactID),
                           () -> "ArtifactID '" + sArtifactID + "' is invalid");
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.isTrue ( () -> DVRValidityHelper.isValidCoordinateVersion (aVersion.getAsString ()),
                           () -> "Version '" + aVersion + "' is invalid");
    ValueEnforcer.isTrue ( () -> DVRValidityHelper.isValidCoordinateClassifier (sClassifier),
                           () -> "Classifier '" + sClassifier + "' is invalid");
    m_sGroupID = sGroupID;
    m_sArtifactID = sArtifactID;
    m_aVersion = aVersion;
    // Unify "" and null
    m_sClassifier = StringHelper.isNotEmpty (sClassifier) ? sClassifier : null;
  }

  @NonNull
  @Nonempty
  public String getGroupID ()
  {
    return m_sGroupID;
  }

  @NonNull
  @Nonempty
  public String getArtifactID ()
  {
    return m_sArtifactID;
  }

  @NonNull
  public DVRVersion getVersionObj ()
  {
    return m_aVersion;
  }

  @Nullable
  public String getClassifier ()
  {
    return m_sClassifier;
  }

  @NonNull
  public DVRCoordinate getWithGroupID (@Nullable final String sNewGroupID)
  {
    if (EqualsHelper.equals (m_sGroupID, sNewGroupID))
      return this;
    return new DVRCoordinate (sNewGroupID, m_sArtifactID, m_aVersion, m_sClassifier);
  }

  @NonNull
  public DVRCoordinate getWithArtifactID (@Nullable final String sNewArtifactID)
  {
    if (EqualsHelper.equals (m_sArtifactID, sNewArtifactID))
      return this;
    return new DVRCoordinate (m_sGroupID, sNewArtifactID, m_aVersion, m_sClassifier);
  }

  @NonNull
  public DVRCoordinate getWithVersion (@NonNull final DVRVersion aNewVersion)
  {
    if (EqualsHelper.equals (m_aVersion, aNewVersion))
      return this;
    return new DVRCoordinate (m_sGroupID, m_sArtifactID, aNewVersion, m_sClassifier);
  }

  @NonNull
  public DVRCoordinate getWithVersion (@NonNull final IDVRPseudoVersion aPseudoVersion)
  {
    return getWithVersion (DVRVersion.of (aPseudoVersion));
  }

  @NonNull
  public DVRCoordinate getWithVersionLatest ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST);
  }

  @NonNull
  public DVRCoordinate getWithVersionLatestRelease ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST_RELEASE);
  }

  @NonNull
  public DVRCoordinate getWithClassifier (@Nullable final String sNewClassifier)
  {
    if (EqualsHelper.equals (m_sClassifier, sNewClassifier))
      return this;
    return new DVRCoordinate (m_sGroupID, m_sArtifactID, m_aVersion, sNewClassifier);
  }

  @NonNull
  @Nonempty
  public static String getAsSingleID (@NonNull @Nonempty final String sGroupID,
                                      @NonNull @Nonempty final String sArtifactID,
                                      @NonNull @Nonempty final String sVersion,
                                      @Nullable final String sClassifier)
  {
    final StringBuilder ret = new StringBuilder ().append (sGroupID)
                                                  .append (PART_SEPARATOR)
                                                  .append (sArtifactID)
                                                  .append (PART_SEPARATOR)
                                                  .append (sVersion);
    if (StringHelper.isNotEmpty (sClassifier))
      ret.append (PART_SEPARATOR).append (sClassifier);
    return ret.toString ();
  }

  @NonNull
  @Nonempty
  public String getAsSingleID ()
  {
    return getAsSingleID (m_sGroupID, m_sArtifactID, getVersionString (), m_sClassifier);
  }

  public static int compare (@NonNull final DVRCoordinate aLeft, @NonNull final DVRCoordinate aRight)
  {
    int ret = aLeft.m_sGroupID.compareTo (aRight.m_sGroupID);
    if (ret == 0)
    {
      ret = aLeft.m_sArtifactID.compareTo (aRight.m_sArtifactID);
      if (ret == 0)
      {
        ret = aLeft.m_aVersion.compareTo (aRight.m_aVersion);
        if (ret == 0)
        {
          // Null-safe compare
          ret = CompareHelper.compare (aLeft.m_sClassifier, aRight.m_sClassifier);
        }
      }
    }
    return ret;
  }

  public int compareTo (@NonNull final DVRCoordinate aOther)
  {
    return compare (this, aOther);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRCoordinate rhs = (DVRCoordinate) o;
    return m_sGroupID.equals (rhs.m_sGroupID) &&
           m_sArtifactID.equals (rhs.m_sArtifactID) &&
           m_aVersion.equals (rhs.m_aVersion) &&
           EqualsHelper.equals (m_sClassifier, rhs.m_sClassifier);
  }

  @Override
  public int hashCode ()
  {
    int ret = m_nHashCode;
    if (ret == IHashCodeGenerator.ILLEGAL_HASHCODE)
    {
      // Cache for improved performance
      ret = m_nHashCode = new HashCodeGenerator (this).append (m_sGroupID)
                                                      .append (m_sArtifactID)
                                                      .append (m_aVersion)
                                                      .append (m_sClassifier)
                                                      .getHashCode ();
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("GroupID", m_sGroupID)
                                       .append ("ArtifactID", m_sArtifactID)
                                       .append ("Version", m_aVersion)
                                       .appendIf ("Classifier", m_sClassifier, StringHelper::isNotEmpty)
                                       .getToString ();
  }

  /**
   * Factory method without classifier. All parameters must match the constraints from
   * {@link DVRValidityHelper#isValidCoordinateGroupID(String)},
   * {@link DVRValidityHelper#isValidCoordinateArtifactID(String)} and
   * {@link DVRValidityHelper#isValidCoordinateVersion(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param sVersion
   *        Version string. May neither be <code>null</code> nor empty.
   * @return The created {@link DVRCoordinate} and never <code>null</code>.
   * @throws DVRVersionException
   *         if the provided version is invalid
   */
  @NonNull
  public static DVRCoordinate create (@NonNull @Nonempty final String sGroupID,
                                      @NonNull @Nonempty final String sArtifactID,
                                      @NonNull @Nonempty final String sVersion) throws DVRVersionException
  {
    return create (sGroupID, sArtifactID, sVersion, (String) null);
  }

  /**
   * Factory method for DVR coordinates. All parameters must match the constraints from
   * {@link DVRValidityHelper#isValidCoordinateGroupID(String)},
   * {@link DVRValidityHelper#isValidCoordinateArtifactID(String)},
   * {@link DVRValidityHelper#isValidCoordinateVersion(String)} and
   * {@link DVRValidityHelper#isValidCoordinateClassifier(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param sVersion
   *        Version string. May neither be <code>null</code> nor empty.
   * @param sClassifier
   *        Classifier. May be <code>null</code>.
   * @return The created {@link DVRCoordinate} and never <code>null</code>.
   * @throws DVRVersionException
   *         if the provided version is invalid
   */
  @NonNull
  public static DVRCoordinate create (@NonNull @Nonempty final String sGroupID,
                                      @NonNull @Nonempty final String sArtifactID,
                                      @NonNull @Nonempty final String sVersion,
                                      @Nullable final String sClassifier) throws DVRVersionException
  {
    return new DVRCoordinate (sGroupID, sArtifactID, DVRVersion.parseOrThrow (sVersion), sClassifier);
  }

  /**
   * Try to parse the provided coordinates String. This is the reverse operation to
   * {@link #getAsSingleID()}.
   *
   * @param sCoords
   *        The coordinate string to parse. May be <code>null</code>.
   * @return Never <code>null</code>
   * @throws DVRCoordinateException
   *         In case the layout is incorrect
   * @throws DVRVersionException
   *         In case the version is incorrect
   */
  @NonNull
  public static DVRCoordinate parseOrThrow (@Nullable final String sCoords) throws DVRCoordinateException,
                                                                            DVRVersionException
  {
    final List <String> aParts = StringHelper.getExploded (PART_SEPARATOR, sCoords);
    final int nSize = aParts.size ();
    if (nSize >= 3 && nSize <= 4)
      return create (aParts.get (0), aParts.get (1), aParts.get (2), nSize >= 4 ? aParts.get (3) : null);

    throw new DVRCoordinateException ("Invalid DVR Coordinates '" + sCoords + "' provided!");
  }

  @Nullable
  public static DVRCoordinate parseOrNull (@Nullable final String sCoords)
  {
    try
    {
      return parseOrThrow (sCoords);
    }
    catch (final DVRException | RuntimeException ex)
    {
      LOGGER.warn (ex.getMessage ());
      return null;
    }
  }
}
