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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
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
  // {
  // // Read bytes
  // final RepoStorageItem aItem = readTopToc ();
  // if (aItem != null)
  // {
  // // Parse to XML
  // final RepoTopTocType aJaxbObject = new RepoTopToc1Marshaller ().read
  // (aItem.data ().bytes ());
  // if (aJaxbObject != null)
  // {
  // // Convert to domain model
  // return RepoTopTocServiceFileBased.createFromJaxbObject (aJaxbObject);
  // }
  // }
  // return null;
  // }

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
  default boolean existsToc (@Nonnull @Nonempty final String sGroupID, @Nonnull @Nonempty final String sArtifactID)
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
  default RepoStorageItem readToc (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull @Nonempty final String sArtifactID)
  {
    return read (RepoStorageKeyOfArtefact.ofToc (sGroupID, sArtifactID));
  }

  /**
   * Read the ToC for the provided Group ID and Artefact ID and return the
   * parsed data.
   *
   * @param aVESID
   *        VESID to take Group ID and Artifact ID from. May not be
   *        <code>null</code>.
   * @return <code>null</code> if either group or artifact do not exist, or if
   *         no ToC is present.
   */
  @Nullable
  default RepoToc readTocModel (@Nonnull final VESID aVESID)
  {
    return readTocModel (aVESID.getGroupID (), aVESID.getArtifactID ());
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
  default RepoToc readTocModel (@Nonnull @Nonempty final String sGroupID, @Nonnull @Nonempty final String sArtifactID)
  {
    // Read bytes
    final RepoStorageItem aItem = readToc (sGroupID, sArtifactID);
    if (aItem != null)
    {
      // Parse to XML
      final RepoTocType aJaxbObject = new RepoToc1Marshaller ().read (aItem.data ().bytes ());
      if (aJaxbObject != null)
      {
        // Convert to domain model
        return RepoToc.createFromJaxbObject (aJaxbObject);
      }
    }
    return null;
  }
}
