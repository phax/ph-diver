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
import org.jspecify.annotations.Nullable;

import com.helger.base.state.ESuccess;

/**
 * Callback interface for a repository storage auditor. Implementations of this interface may e.g.
 * log all actions in a DB or so.
 *
 * @author Philip Helger
 */
public interface IRepoStorageAuditor
{
  /**
   * Called after a read on a repository
   *
   * @param aRepo
   *        The repository on which the action took place. Never <code>null</code>.
   * @param aKey
   *        They repository key that was read. Never <code>null</code>.
   * @param eSuccess
   *        Whether the read action was successful or not. Never <code>null</code>.
   */
  default void onRead (@NonNull final IRepoStorage aRepo,
                       @NonNull final RepoStorageKey aKey,
                       @NonNull final ESuccess eSuccess)
  {}

  /**
   * Called after a write on a repository
   *
   * @param aRepo
   *        The repository on which the action took place. Never <code>null</code>.
   * @param aKey
   *        They repository key that was written. Never <code>null</code>.
   * @param aContent
   *        The content that was supposed to be written. Never <code>null</code>. Since v4.2.0.
   * @param eSuccess
   *        Whether the write action was successful or not. Never <code>null</code>.
   */
  default void onWrite (@NonNull final IRepoStorage aRepo,
                        @NonNull final RepoStorageKey aKey,
                        @NonNull final IRepoStorageContent aContent,
                        @NonNull final ESuccess eSuccess)
  {}

  /**
   * Called after a delete on a repository
   *
   * @param aRepo
   *        The repository on which the action took place. Never <code>null</code>.
   * @param aKey
   *        They repository key that was deleted. Never <code>null</code>.
   * @param eSuccess
   *        Whether the delete action was successful or not. Never <code>null</code>.
   */
  default void onDelete (@NonNull final IRepoStorage aRepo,
                         @NonNull final RepoStorageKey aKey,
                         @NonNull final ESuccess eSuccess)
  {}

  @NonNull
  default IRepoStorageAuditor and (@Nullable final IRepoStorageAuditor aSecond)
  {
    return aSecond == null ? this : and (this, aSecond);
  }

  @Nullable
  static IRepoStorageAuditor and (@Nullable final IRepoStorageAuditor aFirst,
                                  @Nullable final IRepoStorageAuditor aSecond)
  {
    if (aFirst == null)
      return aSecond;

    if (aSecond == null)
      return aFirst;

    // Both are present
    return new InternalMultipleRepoStorageAuditor (aFirst, aSecond);
  }
}
