/*
 * Copyright (C) 2023 Philip Helger & ecosio
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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSortedMap;
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
 * Local representation of a Repository Table of Contents for a single artifact
 *
 * @author Philip Helger
 */
public class RepoToc
{
  private final String m_sGroupID;
  private final String m_sArtifactID;

  // Latest version last
  // VESVersion uses a different "compare" then Version!
  private final ICommonsSortedMap <VESVersion, OffsetDateTime> m_aVersions = new CommonsTreeMap <> ();

  // Status var
  private VESVersion m_aLatestReleaseVersion;
  private OffsetDateTime m_aLatestReleaseVersionPubDT;

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

  @Nonnull
  @Nonempty
  public final String getGroupID ()
  {
    return m_sGroupID;
  }

  @Nonnull
  @Nonempty
  public final String getArtifactID ()
  {
    return m_sArtifactID;
  }

  @Nonnegative
  public int getVersionCount ()
  {
    return m_aVersions.size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public final ICommonsSortedMap <VESVersion, OffsetDateTime> getAllVersions ()
  {
    return m_aVersions.getClone ();
  }

  @Nullable
  public VESVersion getLatestVersion ()
  {
    return m_aVersions.getLastKey ();
  }

  @Nullable
  public String getLatestVersionAsString ()
  {
    final VESVersion aVer = getLatestVersion ();
    return aVer == null ? null : aVer.getAsString ();
  }

  @Nullable
  public OffsetDateTime getLatestVersionPublicationDateTime ()
  {
    return m_aVersions.getLastValue ();
  }

  @Nullable
  public VESVersion getLatestReleaseVersion ()
  {
    return m_aLatestReleaseVersion;
  }

  @Nullable
  public String getLatestReleaseVersionAsString ()
  {
    final VESVersion aVer = getLatestReleaseVersion ();
    return aVer == null ? null : aVer.getAsString ();
  }

  @Nullable
  public OffsetDateTime getLatestReleaseVersionPublicationDateTime ()
  {
    return m_aLatestReleaseVersionPubDT;
  }

  @Nonnull
  public EChange addVersion (@Nonnull final VESVersion aVersion, @Nonnull final OffsetDateTime aPublishDT)
  {
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.isTrue (aVersion.isStaticVersion (), "Version must be static and not pseudo");
    ValueEnforcer.notNull (aPublishDT, "PublishDT");

    if (m_aVersions.containsKey (aVersion))
      return EChange.UNCHANGED;

    // Use UTC only; truncate to millisecond precision
    final OffsetDateTime aRealPublishDT = PDTFactory.getWithMillisOnly (aPublishDT.withOffsetSameInstant (ZoneOffset.UTC));
    m_aVersions.put (aVersion, aRealPublishDT);

    // Remember last non-snapshot version
    if (!aVersion.isStaticSnapshotVersion ())
      if (m_aLatestReleaseVersion == null || aVersion.compareTo (m_aLatestReleaseVersion) > 0)
      {
        m_aLatestReleaseVersion = aVersion;
        m_aLatestReleaseVersionPubDT = aRealPublishDT;
      }

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange removeVersion (@Nonnull final VESVersion aVersion)
  {
    ValueEnforcer.notNull (aVersion, "Version");
    ValueEnforcer.isTrue (aVersion.isStaticVersion (), "Version must be static and not pseudo");

    if (m_aVersions.removeObject (aVersion).isUnchanged ())
      return EChange.UNCHANGED;

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
    aRepoToc.getVersioning ()
            .getVersions ()
            .getVersion ()
            .forEach (x -> ret.addVersion (VESVersion.parseOrThrow (x.getValue ()), _toODT (x.getPublished ())));
    return ret;
  }
}
