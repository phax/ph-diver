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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementComparable;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.CompareHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.hashcode.IHashCodeGenerator;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.DVRException;
import com.helger.diver.api.version.DVRPseudoVersionRegistry;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.api.version.DVRVersionException;
import com.helger.diver.api.version.IDVRPseudoVersion;

/**
 * The DVRID represents the coordinates of a single technical artefact in a
 * specific version. It was originally called VESID for "Validation Executor Set
 * ID" but is now used in a wider range of use cases. The name was changed for
 * release v2.0.0
 *
 * @author Philip Helger
 */
@Immutable
@MustImplementComparable
@MustImplementEqualsAndHashcode
public final class DVRID implements Comparable <DVRID>
{
  /** The separator char between ID elements */
  public static final char ID_SEPARATOR = ':';

  private final String m_sGroupID;
  private final String m_sArtifactID;
  private final DVRVersion m_aVersion;
  private final String m_sClassifier;
  // status vars
  private int m_nHashCode = IHashCodeGenerator.ILLEGAL_HASHCODE;

  private static boolean _isValidPart (@Nonnull final String sPart, @Nonnegative final int nMaxLen)
  {
    return RegExHelper.stringMatchesPattern ("[a-zA-Z0-9_\\-\\.]{1," + nMaxLen + "}", sPart);
  }

  public static boolean isValidGroupID (@Nullable final String sPart)
  {
    if (StringHelper.hasNoText (sPart))
      return false;
    return _isValidPart (sPart, DVRIDSettings.getMaxGroupIDLen ());
  }

  public static boolean isValidArtifactID (@Nullable final String sPart)
  {
    if (StringHelper.hasNoText (sPart))
      return false;
    return _isValidPart (sPart, DVRIDSettings.getMaxArtifactIDLen ());
  }

  public static boolean isValidVersion (@Nullable final String sPart)
  {
    if (StringHelper.hasNoText (sPart))
      return false;
    return _isValidPart (sPart, DVRIDSettings.getMaxVersionLen ());
  }

  public static boolean isValidClassifier (@Nullable final String sPart)
  {
    // Classifier is optional
    if (StringHelper.hasNoText (sPart))
      return true;
    return _isValidPart (sPart, DVRIDSettings.getMaxClassifierLen ());
  }

  /**
   * Constructor without classifier. All parameters must match the constraints
   * from {@link #isValidGroupID(String)}, {@link #isValidArtifactID(String)}
   * and {@link #isValidVersion(String)}.
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
  public DVRID (@Nonnull @Nonempty final String sGroupID,
                @Nonnull @Nonempty final String sArtifactID,
                @Nonnull @Nonempty final String sVersion) throws DVRVersionException
  {
    this (sGroupID, sArtifactID, sVersion, (String) null);
  }

  /**
   * Constructor. All parameters must match the constraints from
   * {@link #isValidGroupID(String)}, {@link #isValidArtifactID(String)},
   * {@link #isValidVersion(String)} and {@link #isValidClassifier(String)}.
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
  public DVRID (@Nonnull @Nonempty final String sGroupID,
                @Nonnull @Nonempty final String sArtifactID,
                @Nonnull @Nonempty final String sVersion,
                @Nullable final String sClassifier) throws DVRVersionException
  {
    this (sGroupID, sArtifactID, DVRVersion.parseOrThrow (sVersion), sClassifier);
  }

  /**
   * Constructor. All parameters must match the constraints from
   * {@link #isValidGroupID(String)}, {@link #isValidArtifactID(String)} and
   * {@link #isValidVersion(String)}.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @param aVersion
   *        Version object. May not be <code>null</code>.
   * @since 1.1.2
   */
  public DVRID (@Nonnull @Nonempty final String sGroupID,
                @Nonnull @Nonempty final String sArtifactID,
                @Nonnull final DVRVersion aVersion)
  {
    this (sGroupID, sArtifactID, aVersion, (String) null);
  }

  /**
   * Constructor. All parameters must match the constraints from
   * {@link #isValidGroupID(String)}, {@link #isValidArtifactID(String)},
   * {@link #isValidVersion(String)} and {@link #isValidClassifier(String)}.
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
  public DVRID (@Nonnull @Nonempty final String sGroupID,
                @Nonnull @Nonempty final String sArtifactID,
                @Nonnull final DVRVersion aVersion,
                @Nullable final String sClassifier)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.isTrue (isValidGroupID (sGroupID), () -> "GroupID '" + sGroupID + "' is invalid");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");
    ValueEnforcer.isTrue (isValidArtifactID (sArtifactID), () -> "ArtifactID '" + sArtifactID + "' is invalid");
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.isTrue (isValidVersion (aVersion.getAsString ()), () -> "Version '" + aVersion + "' is invalid");
    ValueEnforcer.isTrue (isValidClassifier (sClassifier), () -> "Classifier '" + sClassifier + "' is invalid");
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
  @Nonempty
  public String getVersionString ()
  {
    return m_aVersion.getAsString ();
  }

  @Nonnull
  public DVRVersion getVersionObj ()
  {
    return m_aVersion;
  }

  public boolean hasClassifier ()
  {
    return StringHelper.hasText (m_sClassifier);
  }

  @Nullable
  public String getClassifier ()
  {
    return m_sClassifier;
  }

  @Nonnull
  public DVRID getWithArtifactID (@Nullable final String sNewArtifactID)
  {
    if (EqualsHelper.equals (m_sArtifactID, sNewArtifactID))
      return this;
    return new DVRID (m_sGroupID, sNewArtifactID, m_aVersion, m_sClassifier);
  }

  @Nonnull
  public DVRID getWithVersion (@Nonnull final DVRVersion aNewVersion)
  {
    if (EqualsHelper.equals (m_aVersion, aNewVersion))
      return this;
    return new DVRID (m_sGroupID, m_sArtifactID, aNewVersion, m_sClassifier);
  }

  @Nonnull
  public DVRID getWithVersion (@Nonnull final IDVRPseudoVersion aPseudoVersion)
  {
    return getWithVersion (DVRVersion.of (aPseudoVersion));
  }

  @Nonnull
  public DVRID getWithVersionLatest ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST);
  }

  @Nonnull
  public DVRID getWithVersionLatestRelease ()
  {
    return getWithVersion (DVRPseudoVersionRegistry.LATEST_RELEASE);
  }

  @Nonnull
  public DVRID getWithClassifier (@Nullable final String sNewClassifier)
  {
    if (EqualsHelper.equals (m_sClassifier, sNewClassifier))
      return this;
    return new DVRID (m_sGroupID, m_sArtifactID, m_aVersion, sNewClassifier);
  }

  @Nonnull
  @Nonempty
  public String getAsSingleID ()
  {
    String ret = m_sGroupID + ID_SEPARATOR + m_sArtifactID + ID_SEPARATOR + getVersionString ();
    if (hasClassifier ())
      ret += ID_SEPARATOR + m_sClassifier;
    return ret;
  }

  public int compareTo (final DVRID aOther)
  {
    int ret = m_sGroupID.compareTo (aOther.m_sGroupID);
    if (ret == 0)
    {
      ret = m_sArtifactID.compareTo (aOther.m_sArtifactID);
      if (ret == 0)
      {
        ret = m_aVersion.compareTo (aOther.m_aVersion);
        if (ret == 0)
          ret = CompareHelper.compare (m_sClassifier, aOther.m_sClassifier);
      }
    }
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final DVRID rhs = (DVRID) o;
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
      ret = m_nHashCode = new HashCodeGenerator (this).append (m_sGroupID)
                                                      .append (m_sArtifactID)
                                                      .append (m_aVersion)
                                                      .append (m_sClassifier)
                                                      .getHashCode ();
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

  @Nonnull
  public static DVRID parseID (@Nullable final String sDVRID) throws DVRIDException, DVRVersionException
  {
    final ICommonsList <String> aParts = StringHelper.getExploded (ID_SEPARATOR, sDVRID);
    final int nSize = aParts.size ();
    if (nSize >= 3 && nSize <= 4)
      return new DVRID (aParts.get (0), aParts.get (1), aParts.get (2), nSize >= 4 ? aParts.get (3) : null);

    throw new DVRIDException ("Invalid DVRID '" + sDVRID + "' provided!");
  }

  @Nullable
  public static DVRID parseIDOrNull (@Nullable final String sCVRID)
  {
    try
    {
      return parseID (sCVRID);
    }
    catch (final DVRException | RuntimeException ex)
    {
      return null;
    }
  }
}
