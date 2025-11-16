/*
 * Copyright (C) 2023-2025 Philip Helger & ecosio
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.toc.jaxb.RepoToc1Marshaller;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;

/**
 * Extended {@link IRepoStorage} with support for table of contents.
 *
 * @author Philip Helger
 * @since 1.0.1
 */
public interface IRepoStorageWithToc extends IRepoStorage
{
  // Top-level Table of Contents:

  /**
   * Read the top-level ToC and return the parsed data.
   *
   * @return <code>null</code> if the top-toc does not exist,
   *         non-<code>null</code> otherwise.
   */
  @Nullable
  IRepoTopTocService getTopTocService ();

  // Table of Contents per Group ID and Artifact ID:

  /**
   * Test if the table of contents for the provided Group ID and Artefact ID is
   * present or not.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @return <code>true</code> if it exists, <code>false</code> if not.
   */
  default boolean existsToc (@NonNull @Nonempty final String sGroupID, @NonNull @Nonempty final String sArtifactID)
  {
    return exists (RepoStorageKeyOfArtefact.ofToc (sGroupID, sArtifactID));
  }

  /**
   * Read the ToC bytes for the provided Group ID and Artefact ID
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @return <code>null</code> if either group or artifact do not exist, or if
   *         no ToC is present.
   */
  @Nullable
  default IRepoStorageReadItem readToc (@NonNull @Nonempty final String sGroupID,
                                        @NonNull @Nonempty final String sArtifactID)
  {
    return read (RepoStorageKeyOfArtefact.ofToc (sGroupID, sArtifactID));
  }

  /**
   * Read the ToC for the provided Group ID and Artefact ID and return the
   * parsed data.
   *
   * @param aCoord
   *        DVR Coordinate to take Group ID and Artifact ID from. May not be
   *        <code>null</code>.
   * @return <code>null</code> if either group or artifact do not exist, or if
   *         no ToC is present.
   */
  @Nullable
  default RepoToc readTocModel (@NonNull final DVRCoordinate aCoord)
  {
    return readTocModel (aCoord.getGroupID (), aCoord.getArtifactID ());
  }

  /**
   * Read the ToC for the provided Group ID and Artefact ID and return the
   * parsed data.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @return <code>null</code> if either group or artifact do not exist, or if
   *         no ToC is present.
   */
  @Nullable
  default RepoToc readTocModel (@NonNull @Nonempty final String sGroupID, @NonNull @Nonempty final String sArtifactID)
  {
    // Read bytes
    final IRepoStorageReadItem aItem = readToc (sGroupID, sArtifactID);
    if (aItem != null)
    {
      // Parse to XML
      final RepoTocType aJaxbObject = new RepoToc1Marshaller ().read (aItem.getContent ().getBufferedInputStream ());
      if (aJaxbObject != null)
      {
        // Convert to domain model
        return RepoToc.createFromJaxbObject (aJaxbObject);
      }
    }
    return null;
  }

  /**
   * Find the latest release version of the provided group ID and artifact ID.
   * This excludes snapshot versions.
   *
   * @param sGroupID
   *        Group ID to resolve. May be <code>null</code>.
   * @param sArtifactID
   *        Artifact ID to resolve. May be <code>null</code>.
   * @return <code>null</code> if either group ID does not exist or artifact ID
   *         does not exist or the combination of group ID and artifact ID only
   *         has snapshot builds.
   * @see #readTocModel(String, String)
   * @since 1.1.2
   */
  @Nullable
  default DVRCoordinate getLatestReleaseVersion (@Nullable final String sGroupID, @Nullable final String sArtifactID)
  {
    final RepoToc aToc = readTocModel (sGroupID, sArtifactID);
    if (aToc != null)
    {
      final DVRVersion aLatestVersion = aToc.getLatestReleaseVersion ();
      if (aLatestVersion != null)
        return new DVRCoordinate (sGroupID, sArtifactID, aLatestVersion);
    }
    return null;
  }

  /**
   * Find the latest version (release and snapshot) of the provided group ID and
   * artifact ID.
   *
   * @param sGroupID
   *        Group ID to resolve. May be <code>null</code>.
   * @param sArtifactID
   *        Artifact ID to resolve. May be <code>null</code>.
   * @return <code>null</code> if either group ID does not exist or artifact ID
   *         does not exist.
   * @see #readTocModel(String, String)
   * @since 1.1.2
   */
  @Nullable
  default DVRCoordinate getLatestVersion (@Nullable final String sGroupID, @Nullable final String sArtifactID)
  {
    final RepoToc aToc = readTocModel (sGroupID, sArtifactID);
    if (aToc != null)
    {
      final DVRVersion aLatestVersion = aToc.getLatestVersion ();
      if (aLatestVersion != null)
        return new DVRCoordinate (sGroupID, sArtifactID, aLatestVersion);
    }
    return null;
  }
}
