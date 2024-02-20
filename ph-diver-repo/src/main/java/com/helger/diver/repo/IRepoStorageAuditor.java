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
