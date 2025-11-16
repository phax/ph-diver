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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;

/**
 * Base implementation of {@link IRepoStorage} on a local file system.
 *
 * @author Philip Helger
 */
public class RepoStorageLocalFileSystem extends AbstractRepoStorageWithToc <RepoStorageLocalFileSystem>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageLocalFileSystem.class);

  private final File m_aBaseDir;

  public RepoStorageLocalFileSystem (@NonNull final File aBaseDir,
                                     @NonNull @Nonempty final String sID,
                                     @NonNull final ERepoWritable eWriteEnabled,
                                     @NonNull final ERepoDeletable eDeleteEnabled,
                                     @NonNull final IRepoTopTocService aTopTocService)
  {
    super (RepoStorageType.LOCAL_FILE_SYSTEM, sID, eWriteEnabled, eDeleteEnabled, aTopTocService);
    ValueEnforcer.notNull (aBaseDir, "BaseDir");
    ValueEnforcer.isFalse (aBaseDir.isFile (), "Base Directory may not be an existing file");
    FileOperationManager.INSTANCE.createDirRecursiveIfNotExisting (aBaseDir.getAbsoluteFile ());
    m_aBaseDir = aBaseDir.getAbsoluteFile ();
  }

  /**
   * @return The base directory as provided in the constructor. Never
   *         <code>null</code>.
   */
  @NonNull
  public final File getBaseDirectory ()
  {
    return m_aBaseDir;
  }

  @NonNull
  public final File getRelativeFile (@NonNull final RepoStorageKey aKey)
  {
    return new File (m_aBaseDir, aKey.getPath ());
  }

  public boolean exists (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final File fSrc = getRelativeFile (aKey);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Checking for existance in local file system '" + fSrc.getAbsolutePath () + "'");

    final boolean bExists = FileHelper.existsFile (fSrc);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Local file system object '" +
                    fSrc.getAbsolutePath () +
                    "' " +
                    (bExists ? "exists" : "does not exist"));

    return bExists;
  }

  @Override
  @Nullable
  protected InputStream getInputStream (@NonNull final RepoStorageKey aKey)
  {
    final File fSrc = getRelativeFile (aKey);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Reading from local file system '" + fSrc.getAbsolutePath () + "'");

    final InputStream ret = FileHelper.getBufferedInputStream (fSrc);
    if (ret == null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to read from local file system '" + fSrc.getAbsolutePath () + "'");
    }
    else
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Found on local file system '" + fSrc.getAbsolutePath () + "'");
    }
    return ret;
  }

  @Override
  @NonNull
  protected ESuccess writeObject (@NonNull final RepoStorageKey aKey, @NonNull final IRepoStorageContent aContent)
  {
    final File fTarget = getRelativeFile (aKey);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Writing to local file system '" + fTarget.getAbsolutePath () + "'");

    // Use the source payload
    try (final OutputStream aFOS = FileHelper.getOutputStream (fTarget))
    {
      if (StreamHelper.copyByteStream ()
                      .from (aContent.getBufferedInputStream ())
                      .closeFrom (true)
                      .to (aFOS)
                      .closeTo (true)
                      .build ()
                      .isFailure ())
      {
        LOGGER.error ("Failed to write local file system object '" + fTarget.getAbsolutePath () + "'");
        return ESuccess.FAILURE;
      }
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to write local file system object '" + fTarget.getAbsolutePath () + "'", ex);
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully wrote to local file system '" + fTarget.getAbsolutePath () + "'");
    return ESuccess.SUCCESS;
  }

  @Override
  @NonNull
  protected ESuccess deleteObject (@NonNull final RepoStorageKey aKey)
  {
    final File fTarget = getRelativeFile (aKey);
    LOGGER.info ("Deleting from local file system '" + fTarget.getAbsolutePath () + "'");

    if (FileOperationManager.INSTANCE.deleteFile (fTarget).isFailure ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to delete local file system object '" + fTarget.getAbsolutePath () + "'");
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully deleted local file system object '" + fTarget.getAbsolutePath () + "'");
    return ESuccess.SUCCESS;
  }
}
