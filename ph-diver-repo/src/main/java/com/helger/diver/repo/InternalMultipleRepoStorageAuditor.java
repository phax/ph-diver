/*
 * Copyright (C) 2023-2026 Philip Helger & ecosio
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
