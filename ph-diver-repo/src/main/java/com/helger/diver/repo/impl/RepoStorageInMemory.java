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
package com.helger.diver.repo.impl;

import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;

/**
 * This class implements an in-memory repository storage. That is used for extra
 * quick turn around times. To avoid memory overload, the maximum number of
 * elements in memory storage can be configured in the constructor.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class RepoStorageInMemory extends AbstractRepoStorageWithToc <RepoStorageInMemory>
{
  public static final class MaxSizeMap extends CommonsLinkedHashMap <String, byte []>
  {
    private final int m_nMaxSize;

    public MaxSizeMap (@Nonnegative final int nMaxSize)
    {
      m_nMaxSize = nMaxSize;
    }

    @Override
    protected boolean removeEldestEntry (final java.util.Map.Entry <String, byte []> eldest)
    {
      return size () > m_nMaxSize;
    }
  }

  public static final int DEFAULT_MAX_SIZE = 1_000;
  public static final ERepoWritable DEFAULT_CAN_WRITE = ERepoWritable.WITH_WRITE;
  public static final ERepoDeletable DEFAULT_CAN_DELETE = ERepoDeletable.WITH_DELETE;
  public static final boolean DEFAULT_ALLOW_OVERWRITE = true;
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageInMemory.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsOrderedMap <String, byte []> m_aCache;
  private final boolean m_bAllowOverwrite;

  public RepoStorageInMemory (@Nonnegative final int nMaxSize,
                              @NonNull @Nonempty final String sID,
                              @NonNull final ERepoWritable eWriteEnabled,
                              @NonNull final ERepoDeletable eDeleteEnabled,
                              @NonNull final IRepoTopTocService aTopTocService,
                              final boolean bAllowOverwrite)
  {
    super (RepoStorageType.IN_MEMORY, sID, eWriteEnabled, eDeleteEnabled, aTopTocService);
    ValueEnforcer.isGT0 (nMaxSize, "Max size must be > 0");
    m_aCache = new MaxSizeMap (nMaxSize);
    m_bAllowOverwrite = bAllowOverwrite;
  }

  public boolean exists (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final String sRealKey = aKey.getPath ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Checking for existance in-memory '" + sRealKey + "'");

    final boolean bExists = m_aRWLock.readLockedBoolean ( () -> m_aCache.containsKey (sRealKey));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("In-memory object '" + sRealKey + "' " + (bExists ? "exists" : "does not exist"));

    return bExists;
  }

  @Override
  @Nullable
  protected InputStream getInputStream (@NonNull final RepoStorageKey aKey)
  {
    final String sRealKey = aKey.getPath ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Reading from in-memory '" + sRealKey + "'");

    final byte [] aData = m_aRWLock.readLockedGet ( () -> m_aCache.get (sRealKey));
    if (aData == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to read from in-memory '" + sRealKey + "'");
      return null;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found in-memory '" + sRealKey + "' with " + aData.length + " bytes");
    return new NonBlockingByteArrayInputStream (aData);
  }

  public final boolean isAllowOverwrite ()
  {
    return m_bAllowOverwrite;
  }

  @NonNull
  private ESuccess _write (@NonNull final RepoStorageKey aKey,
                           @NonNull final IRepoStorageContent aContent,
                           final boolean bAllowOverwrite)
  {
    ValueEnforcer.isTrue ( () -> aContent.isLengthAnInt (),
                           () -> "Content Length " + aContent.getLength () + " is too large (>2GB) to store in Memory");

    final String sRealKey = aKey.getPath ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Writing to in-memory '" + sRealKey + "'");

    // Read into memory
    final byte [] aPayload = StreamHelper.getAllBytes (aContent);

    final ESuccess eSuccess;
    if (bAllowOverwrite)
    {
      // Just overwrite
      // Use the source payload
      m_aRWLock.writeLocked ( () -> m_aCache.put (sRealKey, aPayload));
      eSuccess = ESuccess.SUCCESS;
    }
    else
    {
      // Don't overwrite
      m_aRWLock.writeLock ().lock ();
      try
      {
        if (m_aCache.containsKey (sRealKey))
          eSuccess = ESuccess.FAILURE;
        else
        {
          // Use the source payload
          m_aCache.put (sRealKey, aPayload);
          eSuccess = ESuccess.SUCCESS;
        }
      }
      finally
      {
        m_aRWLock.writeLock ().unlock ();
      }
    }

    if (eSuccess.isSuccess ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully wrote to in-memory '" + sRealKey + "'");
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to overwritew in-memory item '" + sRealKey + "'");
    }
    return eSuccess;
  }

  @Override
  @NonNull
  protected ESuccess writeObject (@NonNull final RepoStorageKey aKey, @NonNull final IRepoStorageContent aContent)
  {
    return _write (aKey, aContent, m_bAllowOverwrite);
  }

  /**
   * This method is intended to be used to fill a {@link RepoStorageInMemory}
   * from the outside, when it is marked as read-only.
   *
   * @param aKey
   *        The key to register.
   * @param aContent
   *        The content to be accessible.
   * @return this for chaining
   */
  @NonNull
  public RepoStorageInMemory registerObject (@NonNull final RepoStorageKey aKey,
                                             @NonNull final IRepoStorageContent aContent)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aContent, "Content");

    // Never allow overwrite
    _write (aKey, aContent, false);
    return this;
  }

  @Override
  @NonNull
  protected ESuccess deleteObject (@NonNull final RepoStorageKey aKey)
  {
    final String sRealKey = aKey.getPath ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Deleting from in-memory '" + sRealKey + "'");

    final boolean bDeleted = m_aRWLock.writeLockedBoolean ( () -> m_aCache.remove (sRealKey) != null);
    if (!bDeleted)
    {
      LOGGER.warn ("Failed to delete in-memory object '" + sRealKey + "'");
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully deleted in-memory object '" + sRealKey + "'");
    return ESuccess.SUCCESS;
  }

  /**
   * @param sID
   *        The ID of repo to use. May neither be <code>null</code> nor empty.
   * @return A writable and deletable {@link RepoStorageInMemory} with a maximum
   *         of 1000 entries. Never <code>null</code>.
   * @see #DEFAULT_MAX_SIZE
   * @see #DEFAULT_CAN_WRITE
   * @see #DEFAULT_CAN_DELETE
   * @see #DEFAULT_ALLOW_OVERWRITE
   */
  @NonNull
  public static RepoStorageInMemory createDefault (@NonNull @Nonempty final String sID)
  {
    return createDefault (sID, DEFAULT_CAN_WRITE, DEFAULT_CAN_DELETE);
  }

  /**
   * @return A writable and deletable {@link RepoStorageInMemory} with a maximum
   *         of 1000 entries. Never <code>null</code>.
   * @param sID
   *        The ID of repo to use. May neither be <code>null</code> nor empty.
   * @param eWriteEnabled
   *        Write enabled?
   * @param eDeleteEnabled
   *        Delete enabled?
   * @see #DEFAULT_MAX_SIZE
   * @see #DEFAULT_ALLOW_OVERWRITE
   */
  @NonNull
  public static RepoStorageInMemory createDefault (@NonNull @Nonempty final String sID,
                                                   @NonNull final ERepoWritable eWriteEnabled,
                                                   @NonNull final ERepoDeletable eDeleteEnabled)
  {
    return new RepoStorageInMemory (DEFAULT_MAX_SIZE,
                                    sID,
                                    eWriteEnabled,
                                    eDeleteEnabled,
                                    new RepoTopTocServiceRepoBasedXML (),
                                    DEFAULT_ALLOW_OVERWRITE);
  }
}
