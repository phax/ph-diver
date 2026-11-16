/*
 * Copyright (C) 2023-2026 Philip Helger
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
package com.helger.diver.repo.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;

/**
 * Factory for creating a repository storage of a specific type from a
 * {@link RepoStorageDefinition}.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
public interface IRepoStorageFactory
{
  /**
   * @return The repository type ID this factory handles, as used in the <code>type</code>
   *         attribute of the configuration. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getType ();

  /**
   * Create a new repository storage from the provided definition. The repository type specific
   * properties must be validated in here. The generic settings (hash verification, top-level ToC)
   * are applied by the caller.
   *
   * @param aDefinition
   *        The definition to use. Its type matches {@link #getType()}. Never <code>null</code>.
   * @param aErrorList
   *        The error list to be filled with errors and warnings. Never <code>null</code>.
   * @return <code>null</code> if the storage could not be created. In that case at least one error
   *         was added to the error list.
   */
  @Nullable
  AbstractRepoStorageWithToc <?> createRepoStorage (@NonNull RepoStorageDefinition aDefinition,
                                                    @NonNull ErrorList aErrorList);
}
