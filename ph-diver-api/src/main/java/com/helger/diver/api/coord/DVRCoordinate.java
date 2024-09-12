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
package com.helger.diver.api.coord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementComparable;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.CompareHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.hashcode.IHashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.DVRException;
import com.helger.diver.api.settings.DVRValidityHelper;
import com.helger.diver.api.version.DVRPseudoVersionRegistry;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.api.version.DVRVersionException;
import com.helger.diver.api.version.IDVRPseudoVersion;

/**
 * The DVR Coordinate represents the coordinate of a single technical artefact
 * in a specific version.<br />
 * It was originally called VESID for "Validation Executor Set ID" but is now
 * used in a wider range of use cases. The name was changed for release v2 to
 * DVRID. In v3 the name was changed again to DVR Coordinate.
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
   * Constructor without classifier. All parameters must match the constraints
   * from {@link DVRValidityHelper#isValidCoordinateGroupID(String)},
   * {@link DVRValidityHelper#isValidCoordinateArtifactID(String)} and
   * {@link DVRValidityHelper#isValidCoordinateVersion(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param sVersion
   *        Version string. May neither be <code>null</code> nor empty.
   * @throws DVRVersionException
   *         if the provided version is invalid
   */
  public DVRCoordinate (@Nonnull @Nonempty final String sGroupID,
                        @Nonnull @Nonempty final String sArtifactID,
                        @Nonnull @Nonempty final String sVersion) throws DVRVersionException
  {
    this (sGroupID, sArtifactID, sVersion, (String) null);
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
   * @param sVersion
   *        Version string. May neither be <code>null</code> nor empty.
   * @param sClassifier
   *        Classifier. May be <code>null</code>.
   * @throws DVRVersionException
   *         if the provided version is invalid
   */
  public DVRCoordinate (@Nonnull @Nonempty final String sGroupID,
                        @Nonnull @Nonempty final String sArtifactID,
                        @Nonnull @Nonempty final String sVersion,
                        @Nullable final String sClassifier) throws DVRVersionException
  {
    this (sGroupID, sArtifactID, DVRVersion.parseOrThrow (sVersion), sClassifier);
  }

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
  public DVRCoordinate (@Nonnull @Nonempty final String sGroupID,
                        @Nonnull @Nonempty final String sArtifactID,
                        @Nonnull final DVRVersion aVersion)
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
  public DVRCoordinate (@Nonnull @Nonempty final String sGroupID,
                        @Nonnull @Nonempty final String sArtifactID,
                        @Nonnull final DVRVersion aVersion,
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
    m_sClassifier = StringHelper.hasText (sClassifier) ? sClassifier : null;
  }

  @Nonnull
  @Nonempty
  public String getGroupID ()
  {
    return m_sGroupID;
  }

  @Nonnull
  @Nonempty
  public String getArtifactID ()
  {
    return m_sArtifactID;
  }

  @Nonnull
  public DVRVersion getVersionObj ()
  {
    return m_aVersion;
  }

  @Nullable
  public String getClassifier ()
  {
    return m_sClassifier;
  }

  @Nonnull
  public DVRCoordinate getWithGroupID (@Nullable final String sNewGroupID)
  {
    if (EqualsHelper.equals (m_sGroupID, sNewGroupID))
      return this;
    return new DVRCoordinate (sNewGroupID, m_sArtifactID, m_aVersion, m_sClassifier);
  }

  @Nonnull
  public DVRCoordinate getWithArtifactID (@Nullable final String sNewArtifactID)
  {
    if (EqualsHelper.equals (m_sArtifactID, sNewArtifactID))
      return this;
    return new DVRCoordinate (m_sGroupID, sNewArtifactID, m_aVersion, m_sClassifier);
  }

  @Nonnull
  public DVRCoordinate getWithVersion (@Nonnull final DVRVersion aNewVersion)
  {
    if (EqualsHelper.equals (m_aVersion, aNewVersion))
      return this;
    return new DVRCoordinate (m_sGroupID, m_sArtifactID, aNewVersion, m_sClassifier);
  }

  @Nonnull
  public DVRCoordinate getWithVersion (@Nonnull final IDVRPseudoVersion aPseudoVersion)
  {
    return getWithVersion (DVRVersion.of (aPseudoVersion));
  }

  @Nonnull
  public DVRCoordinate getWithVersionLatest ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST);
  }

  @Nonnull
  public DVRCoordinate getWithVersionLatestRelease ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST_RELEASE);
  }

  @Nonnull
  public DVRCoordinate getWithClassifier (@Nullable final String sNewClassifier)
  {
    if (EqualsHelper.equals (m_sClassifier, sNewClassifier))
      return this;
    return new DVRCoordinate (m_sGroupID, m_sArtifactID, m_aVersion, sNewClassifier);
  }

  @Nonnull
  @Nonempty
  public static String getAsSingleID (@Nonnull @Nonempty final String sGroupID,
                                      @Nonnull @Nonempty final String sArtifactID,
                                      @Nonnull @Nonempty final String sVersion,
                                      @Nullable final String sClassifier)
  {
    String ret = sGroupID + PART_SEPARATOR + sArtifactID + PART_SEPARATOR + sVersion;
    if (StringHelper.hasText (sClassifier))
      ret += PART_SEPARATOR + sClassifier;
    return ret;
  }

  @Nonnull
  @Nonempty
  public String getAsSingleID ()
  {
    return getAsSingleID (m_sGroupID, m_sArtifactID, getVersionString (), m_sClassifier);
  }

  public static int compare (@Nonnull final DVRCoordinate aLeft, @Nonnull final DVRCoordinate aRight)
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

  public int compareTo (@Nonnull final DVRCoordinate aOther)
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
                                       .appendIf ("Classifier", m_sClassifier, StringHelper::hasText)
                                       .getToString ();
  }

  /**
   * Try to parse the provided coordinates String. This is the reverse operation
   * to {@link #getAsSingleID()}.
   *
   * @param sCoords
   *        The coordinate string to parse. May be <code>null</code>.
   * @return Never <code>null</code>
   * @throws DVRCoordinateException
   *         In case the layout is incorrect
   * @throws DVRVersionException
   *         In case the version is incorrect
   */
  @Nonnull
  public static DVRCoordinate parseOrThrow (@Nullable final String sCoords) throws DVRCoordinateException,
                                                                            DVRVersionException
  {
    final ICommonsList <String> aParts = StringHelper.getExploded (PART_SEPARATOR, sCoords);
    final int nSize = aParts.size ();
    if (nSize >= 3 && nSize <= 4)
      return new DVRCoordinate (aParts.get (0), aParts.get (1), aParts.get (2), nSize >= 4 ? aParts.get (3) : null);

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
