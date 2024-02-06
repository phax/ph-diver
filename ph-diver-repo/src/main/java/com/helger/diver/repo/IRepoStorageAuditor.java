package com.helger.diver.repo;

import javax.annotation.Nonnull;

import com.helger.commons.state.ESuccess;

/**
 * Callback interface for a repository storage auditor. Implementations of this
 * interface may e.g. log all actions in a DB or so.
 *
 * @author Philip Helger
 */
public interface IRepoStorageAuditor
{
  void onRead (@Nonnull IRepoStorage aRepo, @Nonnull RepoStorageKey aKey, @Nonnull ESuccess eSuccess);

  void onWrite (@Nonnull IRepoStorage aRepo, @Nonnull RepoStorageKey aKey, @Nonnull ESuccess eSuccess);

  void onDelete (@Nonnull IRepoStorage aRepo, @Nonnull RepoStorageKey aKey, @Nonnull ESuccess eSuccess);

  /**
   * The "nil" object
   */
  IRepoStorageAuditor DO_NOTHING_AUDITOR = new IRepoStorageAuditor ()
  {
    public void onRead (@Nonnull final IRepoStorage aRepo,
                        @Nonnull final RepoStorageKey aKey,
                        @Nonnull final ESuccess eSuccess)
    {
      // empty
    }

    public void onWrite (@Nonnull final IRepoStorage aRepo,
                         @Nonnull final RepoStorageKey aKey,
                         @Nonnull final ESuccess eSuccess)
    {
      // empty
    }

    public void onDelete (@Nonnull final IRepoStorage aRepo,
                          @Nonnull final RepoStorageKey aKey,
                          @Nonnull final ESuccess eSuccess)
    {
      // empty
    }

    @Override
    public String toString ()
    {
      return "DO_NOTHING_AUDITOR";
    }
  };
}
