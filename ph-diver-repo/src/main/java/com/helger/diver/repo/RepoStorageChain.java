/*
 * Copyright (C) 2023-2025 Philip Helger & ecosio
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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * A chain of {@link IRepoStorage} objects for reading from multiple sources,
 * including local storage.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class RepoStorageChain implements IRepoStorageBase
{
  public static final boolean DEFAULT_CACHE_REMOTE_CONTENT = true;
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageChain.class);

  private final ICommonsList <IRepoStorage> m_aReadingStorages;
  private final ICommonsList <IRepoStorage> m_aWritableStorages;
  private boolean m_bCacheRemoteContent = DEFAULT_CACHE_REMOTE_CONTENT;

  /**
   * Constructor. The order of storages is maintained.
   *
   * @param aReadingStorages
   *        The storages to be considered. May neither be <code>null</code> nor
   *        empty.
   * @param aWritableStorages
   *        The list of writable storages to save remote read artifacts to. May
   *        be <code>null</code>. If this list is <code>null</code> or empty, an
   *        eventually received artefact from a remote storage is not saved
   *        locally.
   */
  public RepoStorageChain (@Nonnull final List <? extends IRepoStorage> aReadingStorages,
                           @Nullable final List <? extends IRepoStorage> aWritableStorages)
  {
    ValueEnforcer.notEmptyNoNullValue (aReadingStorages, "ReadingStorages");
    if (aWritableStorages != null)
    {
      ValueEnforcer.noNullValue (aWritableStorages, "WritablePersistentStorages");
      for (final IRepoStorage aStorage : aWritableStorages)
        ValueEnforcer.isTrue (aStorage.canWrite (), "Writable storage must be writable");
    }
    m_aReadingStorages = new CommonsArrayList <> (aReadingStorages);
    m_aWritableStorages = new CommonsArrayList <> (aWritableStorages);
  }

  @Nonnull
  @Nonempty
  public final ICommonsList <IRepoStorage> internalGetAllStorages ()
  {
    return m_aReadingStorages.getClone ();
  }

  @Nonnull
  public final ICommonsList <IRepoStorage> internalGetAllWritableStorages ()
  {
    return m_aWritableStorages.getClone ();
  }

  /**
   * @return <code>true</code> of remote content that was read, should be
   *         written to a persistent local repository for faster access next
   *         time.
   */
  public final boolean isCacheRemoteContent ()
  {
    return m_bCacheRemoteContent;
  }

  @Nonnull
  public final RepoStorageChain setCacheRemoteContent (final boolean bCacheRemoteContent)
  {
    m_bCacheRemoteContent = bCacheRemoteContent;
    return this;
  }

  public boolean exists (@Nonnull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Checking for existence of '" +
                    aKey.getPath () +
                    "' in " +
                    m_aReadingStorages.getAllMapped (IRepoStorage::getRepoTypeID));

    for (final IRepoStorage aStorage : m_aReadingStorages)
    {
      // Try to read item
      if (aStorage.exists (aKey))
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Found '" +
                        aKey.getPath () +
                        "' in storage " +
                        aStorage.getID () +
                        " of type " +
                        aStorage.getRepoTypeID ());
        return true;
      }
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Failed to find '" + aKey.getPath () + "' in any of the contained storages");
    return false;
  }

  @Nullable
  public IRepoStorageReadItem read (@Nonnull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to read '" +
                    aKey.getPath () +
                    "' from " +
                    m_aReadingStorages.getAllMapped (IRepoStorage::getRepoTypeID));

    for (final IRepoStorage aReadStorage : m_aReadingStorages)
    {
      // Try to read item
      final IRepoStorageReadItem aReadItem = aReadStorage.read (aKey);
      if (aReadItem != null)
      {
        final String sMsg = "Successfully read '" +
                            aKey.getPath () +
                            "' from " +
                            aReadStorage.getRepoTypeID () +
                            " with hash state '" +
                            aReadItem.getHashState ().getDisplayName () +
                            "'";
        if (aReadItem.getHashState () != ERepoHashState.VERIFIED_MATCHING)
          LOGGER.warn (sMsg);
        else
        {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug (sMsg);
        }

        // Only copy the artefact if:
        // 1. The source repository is remote
        // 2. Caching of content is enabled
        // 3. It is a real artifact and not just "any" data (like TopToC)
        // 4. The data can be read multiple times
        if (aReadStorage.getRepoType ().isRemote () &&
            m_bCacheRemoteContent &&
            aKey instanceof RepoStorageKeyOfArtefact &&
            aReadItem.getContent ().isReadMultiple ())
        {
          // Item was read from remote
          if (m_aWritableStorages.isNotEmpty ())
          {
            // Store locally
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Storing '" +
                            aKey.getPath () +
                            "' to " +
                            m_aWritableStorages.getAllMapped (IRepoStorage::getRepoTypeID));
            for (final IRepoStorage aWritableStorage : m_aWritableStorages)
              aWritableStorage.write (aKey, aReadItem.getContent ());
          }
        }
        return aReadItem;
      }

      // else try reading from next repo
    }

    // Not found in any storage
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Failed to read '" + aKey.getPath () + "' from any of the contained storages");

    return null;
  }

  @Nonnull
  @ReturnsMutableObject
  public static RepoStorageChain of (@Nonnull @Nonempty final IRepoStorage... aStorages)
  {
    ValueEnforcer.notNullNoNullValue (aStorages, "Storages");

    final ICommonsList <IRepoStorage> aAll = new CommonsArrayList <> (aStorages);

    // Determine all writable ones
    final ICommonsList <IRepoStorage> aWritableOnes = aAll.getAll (IRepoStorage::canWrite);

    return of (aAll, aWritableOnes);
  }

  @Nonnull
  @ReturnsMutableObject
  public static RepoStorageChain of (@Nonnull @Nonempty final ICommonsList <? extends IRepoStorage> aStorages,
                                     @Nullable final ICommonsList <? extends IRepoStorage> aWritableStorages)
  {
    ValueEnforcer.notNullNoNullValue (aStorages, "Storages");

    return new RepoStorageChain (aStorages, aWritableStorages);
  }
}
