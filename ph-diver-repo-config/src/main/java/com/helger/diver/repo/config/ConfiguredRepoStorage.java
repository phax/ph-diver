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

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.diver.repo.toc.IRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;

/**
 * A repository storage that was created from a {@link RepoStorageDefinition}, together with its
 * top-level ToC service.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class ConfiguredRepoStorage
{
  private final RepoStorageDefinition m_aDefinition;
  private final IRepoStorageWithToc m_aRepoStorage;
  private final IRepoTopTocService m_aTopTocService;

  public ConfiguredRepoStorage (@NonNull final RepoStorageDefinition aDefinition,
                                @NonNull final IRepoStorageWithToc aRepoStorage,
                                @Nullable final IRepoTopTocService aTopTocService)
  {
    ValueEnforcer.notNull (aDefinition, "Definition");
    ValueEnforcer.notNull (aRepoStorage, "RepoStorage");
    m_aDefinition = aDefinition;
    m_aRepoStorage = aRepoStorage;
    m_aTopTocService = aTopTocService;
  }

  /**
   * @return The definition the storage was created from. Never <code>null</code>.
   */
  @NonNull
  public RepoStorageDefinition getDefinition ()
  {
    return m_aDefinition;
  }

  /**
   * @return The repository ID. Shortcut for <code>getDefinition ().getID ()</code>.
   */
  @NonNull
  public String getID ()
  {
    return m_aDefinition.getID ();
  }

  /**
   * @return The created repository storage. Never <code>null</code>.
   */
  @NonNull
  public IRepoStorageWithToc getRepoStorage ()
  {
    return m_aRepoStorage;
  }

  /**
   * @return The initialised top-level ToC service of the repository. Only present if the top-level
   *         ToC type is not {@link ERepoTopTocType#NONE}.
   */
  @Nullable
  public IRepoTopTocService getTopTocService ()
  {
    return m_aTopTocService;
  }

  public boolean hasTopTocService ()
  {
    return m_aTopTocService != null;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Definition", m_aDefinition)
                                       .append ("RepoStorage", m_aRepoStorage)
                                       .appendIfNotNull ("TopTocService", m_aTopTocService)
                                       .getToString ();
  }
}
