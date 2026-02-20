package com.helger.diver.repo;

import org.jspecify.annotations.NonNull;

import com.helger.base.state.ESuccess;

/**
 * Package private class
 *
 * @author Philip Helger
 */
final class InternalMultipleRepoStorageAuditor implements IRepoStorageAuditor
{
  private final IRepoStorageAuditor m_aFirst;
  private final IRepoStorageAuditor m_aSecond;

  InternalMultipleRepoStorageAuditor (@NonNull final IRepoStorageAuditor aFirst,
                                      @NonNull final IRepoStorageAuditor aSecond)
  {
    m_aFirst = aFirst;
    m_aSecond = aSecond;
  }

  public void onRead (@NonNull final IRepoStorage aRepo,
                      @NonNull final RepoStorageKey aKey,
                      @NonNull final ESuccess eSuccess)
  {
    m_aFirst.onRead (aRepo, aKey, eSuccess);
    m_aSecond.onRead (aRepo, aKey, eSuccess);
  }

  public void onWrite (@NonNull final IRepoStorage aRepo,
                       @NonNull final RepoStorageKey aKey,
                       @NonNull final IRepoStorageContent aContent,
                       @NonNull final ESuccess eSuccess)
  {
    m_aFirst.onWrite (aRepo, aKey, aContent, eSuccess);
    m_aSecond.onWrite (aRepo, aKey, aContent, eSuccess);
  }

  public void onDelete (@NonNull final IRepoStorage aRepo,
                        @NonNull final RepoStorageKey aKey,
                        @NonNull final ESuccess eSuccess)
  {
    m_aFirst.onDelete (aRepo, aKey, eSuccess);
    m_aSecond.onDelete (aRepo, aKey, eSuccess);
  }
}
