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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.toc.IRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;

/**
 * A repository storage that was created from a {@link RepoStorageDefinition}, together with its
 * top-level ToC service. Since v4.4.0 it is {@link AutoCloseable}: if the repository storage holds
 * resources (e.g. an HTTP or S3 client created by its factory), it implements
 * {@link AutoCloseable} itself and is closed by {@link #close()}.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class ConfiguredRepoStorage implements AutoCloseable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ConfiguredRepoStorage.class);

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

  /**
   * Close the provided repository storage, if it implements {@link AutoCloseable}. Exceptions are
   * logged and not propagated.
   *
   * @param aRepoStorage
   *        The repository storage to close. May not be <code>null</code>.
   * @since 4.4.0
   */
  static void closeRepoStorage (@NonNull final IRepoStorage aRepoStorage)
  {
    if (aRepoStorage instanceof final AutoCloseable aCloseable)
      try
      {
        aCloseable.close ();
      }
      catch (final Exception ex)
      {
        LOGGER.warn ("Failed to close repository storage '" + aRepoStorage.getID () + "'", ex);
      }
  }

  /**
   * Close the repository storage, if it holds resources. The object must not be used afterwards.
   *
   * @since 4.4.0
   */
  public void close ()
  {
    closeRepoStorage (m_aRepoStorage);
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
