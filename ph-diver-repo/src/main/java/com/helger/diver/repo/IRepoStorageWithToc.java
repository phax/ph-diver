package com.helger.diver.repo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;

/**
 * Extended {@link IRepoStorage} with support for table of contents.
 *
 * @author Philip Helger
 * @since 1.0.1
 */
public interface IRepoStorageWithToc extends IRepoStorage
{
  /**
   * Read the ToC for a single artifact
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @return <code>null</code> if either group or artifact do not exist, or if
   *         no ToC is present.
   */
  @Nullable
  RepoStorageItem readToc (@Nonnull @Nonempty String sGroupID, @Nonnull @Nonempty String sArtifactID);
}
