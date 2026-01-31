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
 * Callback interface for a repository storage auditor. Implementations of this
 * interface may e.g. log all actions in a DB or so.
 *
 * @author Philip Helger
 */
public interface IRepoStorageAuditor
{
  void onRead (@NonNull IRepoStorage aRepo, @NonNull RepoStorageKey aKey, @NonNull ESuccess eSuccess);

  void onWrite (@NonNull IRepoStorage aRepo, @NonNull RepoStorageKey aKey, @NonNull ESuccess eSuccess);

  void onDelete (@NonNull IRepoStorage aRepo, @NonNull RepoStorageKey aKey, @NonNull ESuccess eSuccess);

  /**
   * The "nil" object
   */
  IRepoStorageAuditor DO_NOTHING_AUDITOR = new IRepoStorageAuditor ()
  {
    public void onRead (@NonNull final IRepoStorage aRepo,
                        @NonNull final RepoStorageKey aKey,
                        @NonNull final ESuccess eSuccess)
    {
      // empty
    }

    public void onWrite (@NonNull final IRepoStorage aRepo,
                         @NonNull final RepoStorageKey aKey,
                         @NonNull final ESuccess eSuccess)
    {
      // empty
    }

    public void onDelete (@NonNull final IRepoStorage aRepo,
                          @NonNull final RepoStorageKey aKey,
                          @NonNull final ESuccess eSuccess)
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
