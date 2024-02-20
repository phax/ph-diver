package com.helger.diver.repo;

import javax.annotation.Nonnull;

/**
 * Base interface for a {@link RepoStorageItem}.
 *
 * @author Philip Helger
 */
public interface IRepoStorageItem
{
  /**
   * @return The main content of the item. Never <code>null</code>.
   */
  @Nonnull
  IRepoStorageContent getContent ();

  /**
   * @return The verification state of this item. Never <code>null</code>.
   */
  @Nonnull
  ERepoHashState getHashState ();
}
