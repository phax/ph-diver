/*
 * Copyright (C) 2023-2024 Philip Helger & ecosio
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
package com.helger.diver.repo.toc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.version.VESVersion;
import com.helger.diver.repo.toc.jaxb.v10.RTVersionListType;
import com.helger.diver.repo.toc.jaxb.v10.RTVersionType;
import com.helger.diver.repo.toc.jaxb.v10.RTVersioningType;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;

/**
 * Local representation of a Repository Table of Contents (ToC) for a single
 * artifact. The key is the combination of Group ID and Artefact ID. A table of
 * contents can only contain static versions. Pseudo versions can never be in a
 * ToC.
 *
 * @author Philip Helger
 * @since 1.0.1
 */
@NotThreadSafe
public class RepoToc
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoToc.class);

  private final String m_sGroupID;
  private final String m_sArtifactID;

  // Oldest version first; Latest version last
  // VESVersion uses a different "compare" then Version!
  private final ICommonsSortedMap <VESVersion, OffsetDateTime> m_aVersions = new CommonsTreeMap <> ();

  // Status variables
  private VESVersion m_aLatestReleaseVersion;
  private OffsetDateTime m_aLatestReleaseVersionPubDT;

  /**
   * Constructor
   *
   * @param sGroupID
   *        VESID Group ID of the ToC. May neither be <code>null</code> nor
   *        empty.
   * @param sArtifactID
   *        VESID Artifact ID of the ToC. May neither be <code>null</code> nor
   *        empty.
   */
  public RepoToc (@Nonnull @Nonempty final String sGroupID, @Nonnull @Nonempty final String sArtifactID)
  {
    this (sGroupID, sArtifactID, null);
  }

  @VisibleForTesting
  RepoToc (@Nonnull @Nonempty final String sGroupID,
           @Nonnull @Nonempty final String sArtifactID,
           @Nullable final Map <VESVersion, OffsetDateTime> aVersions)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");
    m_sGroupID = sGroupID;
    m_sArtifactID = sArtifactID;
    if (aVersions != null)
      aVersions.forEach (this::addVersion);
  }

  /**
   * @return The VESID Group ID as provided in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getGroupID ()
  {
    return m_sGroupID;
  }

  /**
   * @return The VESID Artefact ID as provided in the constructor. Neither
   *         <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public final String getArtifactID ()
  {
    return m_sArtifactID;
  }

  /**
   * @return The total number of contained versions. Always &ge; 0.
   */
  @Nonnegative
  public final int getVersionCount ()
  {
    return m_aVersions.size ();
  }

  /**
   * @return A copy of all contained versions as a map from version to its
   *         publication date. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsSortedMap <VESVersion, OffsetDateTime> getAllVersions ()
  {
    return m_aVersions.getClone ();
  }

  /**
   * @return A copy of all contained versions as a sorted set. Never
   *         <code>null</code>.
   * @since 1.1.0
   */
  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsSortedSet <VESVersion> getAllVersionsOnly ()
  {
    return m_aVersions.copyOfKeySet ();
  }

  /**
   * Get the latest overall version, including snapshot versions.
   *
   * @return The latest overall version. May be <code>null</code> if no version
   *         is contained.
   * @see #getLatestReleaseVersion()
   */
  @Nullable
  public final VESVersion getLatestVersion ()
  {
    return m_aVersions.getLastKey ();
  }

  /**
   * Get the latest overall version number, including snapshot versions.
   *
   * @return The version number of the latest overall version. May be
   *         <code>null</code> if no version is contained.
   * @see #getLatestReleaseVersionAsString()
   */
  @Nullable
  public final String getLatestVersionAsString ()
  {
    final VESVersion aVer = getLatestVersion ();
    return aVer == null ? null : aVer.getAsString ();
  }

  /**
   * Get the latest overall publication date time, including snapshot versions.
   *
   * @return The publication date time of the latest overall version. May be
   *         <code>null</code> if no version is contained.
   * @see #getLatestReleaseVersionPublicationDateTime()
   */
  @Nullable
  public final OffsetDateTime getLatestVersionPublicationDateTime ()
  {
    return m_aVersions.getLastValue ();
  }

  /**
   * Get the latest overall version, without snapshot versions.
   *
   * @return The latest overall release version. May be <code>null</code> if no
   *         version is contained.
   * @see #getLatestVersion()
   */
  @Nullable
  public final VESVersion getLatestReleaseVersion ()
  {
    return m_aLatestReleaseVersion;
  }

  /**
   * Get the latest overall version number, without snapshot versions.
   *
   * @return The version number of the latest overall release version. May be
   *         <code>null</code> if no version is contained.
   * @see #getLatestVersionAsString()
   */
  @Nullable
  public final String getLatestReleaseVersionAsString ()
  {
    final VESVersion aVer = getLatestReleaseVersion ();
    return aVer == null ? null : aVer.getAsString ();
  }

  /**
   * Get the latest overall publication date time, without snapshot versions.
   *
   * @return The publication date time of the latest overall release version.
   *         May be <code>null</code> if no version is contained.
   * @see #getLatestVersionPublicationDateTime()
   */
  @Nullable
  public final OffsetDateTime getLatestReleaseVersionPublicationDateTime ()
  {
    return m_aLatestReleaseVersionPubDT;
  }

  /**
   * Check if the provided version is contained or not.
   *
   * @param aVersion
   *        The version to check. May be <code>null</code>.
   * @return <code>true</code> if the version is contained, <code>false</code>
   *         if not.
   * @since 1.1.0
   */
  public final boolean containsVersion (@Nullable final VESVersion aVersion)
  {
    return aVersion != null && m_aVersions.containsKey (aVersion);
  }

  /**
   * Get the publication date of the specified version.
   *
   * @param aVersion
   *        The version to query. May be <code>null</code>
   * @return <code>null</code> if the provided version is <code>null</code> or
   *         not present.
   * @since 1.1.2
   */
  @Nullable
  public final OffsetDateTime getPublicationDateTimeOfVersion (@Nullable final VESVersion aVersion)
  {
    if (aVersion == null)
      return null;
    return m_aVersions.get (aVersion);
  }

  @Nonnull
  private EChange _addVersion (@Nonnull final VESVersion aVersion,
                               @Nonnull final OffsetDateTime aPublishDT,
                               final boolean bDoLog)
  {
    if (!aVersion.isStaticVersion ())
    {
      if (bDoLog)
        LOGGER.error ("Only static versions can be added to a table of contents. The version '" +
                      aVersion.getAsString () +
                      "' is not allowed.");
      return EChange.UNCHANGED;
    }

    if (m_aVersions.containsKey (aVersion))
    {
      if (bDoLog)
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("The version '" + aVersion.getAsString () + "' is already contained in the table of contents.");
      return EChange.UNCHANGED;
    }

    // Use UTC only; truncate to millisecond precision
    final OffsetDateTime aRealPublishDT = PDTFactory.getWithMillisOnly (aPublishDT.withOffsetSameInstant (ZoneOffset.UTC));
    m_aVersions.put (aVersion, aRealPublishDT);

    // Remember last non-snapshot version
    if (!aVersion.isStaticSnapshotVersion ())
      if (m_aLatestReleaseVersion == null || aVersion.compareTo (m_aLatestReleaseVersion) > 0)
      {
        m_aLatestReleaseVersion = aVersion;
        m_aLatestReleaseVersionPubDT = aRealPublishDT;

        if (bDoLog)
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("The added version '" +
                          aVersion.getAsString () +
                          "' is now the latest release version in the table of contents");
      }

    return EChange.CHANGED;
  }

  /**
   * Add a new version to the ToC.
   *
   * @param aVersion
   *        The version to be added. May not be <code>null</code>. Must be a
   *        static version - pseudo versions are not allowed.
   * @param aPublishDT
   *        The publication date time to use. May not be <code>null</code>. It
   *        is internally converted to UTC and cut after millisecond precision.
   * @return {@link EChange#CHANGED} if the version was successfully added,
   *         {@link EChange#UNCHANGED} otherwise.
   */
  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public EChange addVersion (@Nonnull final VESVersion aVersion, @Nonnull final OffsetDateTime aPublishDT)
  {
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.notNull (aPublishDT, "PublishDT");

    return _addVersion (aVersion, aPublishDT, true);
  }

  @Nonnull
  @OverridingMethodsMustInvokeSuper
  public EChange removeVersion (@Nonnull final VESVersion aVersion)
  {
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.isTrue (aVersion.isStaticVersion (), "Version must be static and not pseudo");

    if (m_aVersions.removeObject (aVersion).isUnchanged ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The version '" + aVersion.getAsString () + "' is not contained in the table of contents.");
      return EChange.UNCHANGED;
    }

    // Remember last non-snapshot version
    m_aLatestReleaseVersion = null;
    m_aLatestReleaseVersionPubDT = null;
    if (m_aVersions.isNotEmpty ())
    {
      // Copy from Set to List
      final ICommonsList <Map.Entry <VESVersion, OffsetDateTime>> aVersionList = new CommonsArrayList <> (m_aVersions.entrySet ());
      // Iterate backwards
      final ListIterator <Map.Entry <VESVersion, OffsetDateTime>> aIter = aVersionList.listIterator (aVersionList.size ());
      while (aIter.hasPrevious ())
      {
        final Map.Entry <VESVersion, OffsetDateTime> aCur = aIter.previous ();
        if (!aCur.getKey ().isStaticSnapshotVersion ())
        {
          m_aLatestReleaseVersion = aCur.getKey ();
          m_aLatestReleaseVersionPubDT = aCur.getValue ();

          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("After deletion of '" +
                          aVersion.getAsString () +
                          "' the new latest release version '" +
                          m_aLatestReleaseVersion.getAsString () +
                          "' is used in the table of contents");

          break;
        }
      }
    }

    return EChange.CHANGED;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final RepoToc rhs = (RepoToc) o;
    return m_sGroupID.equals (rhs.m_sGroupID) &&
           m_sArtifactID.equals (rhs.m_sArtifactID) &&
           m_aVersions.equals (rhs.m_aVersions);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sGroupID).append (m_sArtifactID).append (m_aVersions).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("GroupID", m_sGroupID)
                                       .append ("ArtifactID", m_sArtifactID)
                                       .append ("Versions", m_aVersions)
                                       .getToString ();
  }

  @Nonnull
  public RepoTocType getAsJaxbObject ()
  {
    final RepoTocType ret = new RepoTocType ();
    ret.setGroupId (m_sGroupID);
    ret.setArtifactId (m_sArtifactID);
    {
      final RTVersioningType aVersioning = new RTVersioningType ();
      // Element is mandatory
      aVersioning.setLatest (StringHelper.getNotNull (getLatestVersionAsString (), ""));
      // Element is mandatory
      aVersioning.setLatestRelease (StringHelper.getNotNull (getLatestReleaseVersionAsString (), ""));
      {
        final RTVersionListType aVersions = new RTVersionListType ();
        for (final Map.Entry <VESVersion, OffsetDateTime> aEntry : m_aVersions.entrySet ())
        {
          final RTVersionType aVersion = new RTVersionType ();
          aVersion.setPublished (XMLOffsetDateTime.of (aEntry.getValue ()));
          aVersion.setValue (aEntry.getKey ().getAsString ());
          aVersions.addVersion (aVersion);
        }
        aVersioning.setVersions (aVersions);
      }
      ret.setVersioning (aVersioning);
    }
    return ret;
  }

  @Nonnull
  private static OffsetDateTime _toODT (@Nonnull final XMLOffsetDateTime aXODT)
  {
    return aXODT.hasOffset () ? aXODT.toOffsetDateTime () : aXODT.withOffsetSameInstant (ZoneOffset.UTC)
                                                                 .toOffsetDateTime ();
  }

  @Nonnull
  public static RepoToc createFromJaxbObject (@Nonnull final RepoTocType aRepoToc)
  {
    final RepoToc ret = new RepoToc (aRepoToc.getGroupId (), aRepoToc.getArtifactId ());
    // Try to read as silently as possible, without unnecessary logging
    aRepoToc.getVersioning ()
            .getVersions ()
            .getVersion ()
            .forEach (x -> ret._addVersion (VESVersion.parseOrThrow (x.getValue ()),
                                            _toODT (x.getPublished ()),
                                            false));
    return ret;
  }
}
