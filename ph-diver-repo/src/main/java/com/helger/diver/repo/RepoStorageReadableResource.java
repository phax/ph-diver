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

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.UnsupportedOperation;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.io.resource.IReadableResource;

/**
 * Implementation of {@link IReadableResource} for {@link RepoStorageKey} and
 * {@link IRepoStorageContent}.
 *
 * @author Philip Helger
 */
public class RepoStorageReadableResource implements IReadableResource
{
  private final RepoStorageKey m_aKey;
  private final IRepoStorageContent m_aContent;

  /**
   * Constructor.
   *
   * @param aKey
   *        The repository key that was read. May not be <code>null</code>.
   * @param aContent
   *        The repository content that was read. May not be <code>null</code>.
   */
  public RepoStorageReadableResource (@NonNull final RepoStorageKey aKey, @NonNull final IRepoStorageContent aContent)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aContent, "Content");
    m_aKey = aKey;
    m_aContent = aContent;
  }

  @NonNull
  public final RepoStorageKey getRepoStorageKey ()
  {
    return m_aKey;
  }

  @NonNull
  public final IRepoStorageContent getRepoStorageContent ()
  {
    return m_aContent;
  }

  @NonNull
  public InputStream getInputStream ()
  {
    return m_aContent.getInputStream ();
  }

  @NonNull
  @Override
  public InputStream getBufferedInputStream ()
  {
    return m_aContent.getBufferedInputStream ();
  }

  public boolean isReadMultiple ()
  {
    return m_aContent.isReadMultiple ();
  }

  @NonNull
  @Nonempty
  public String getResourceID ()
  {
    return getPath ();
  }

  @NonNull
  @Nonempty
  public String getPath ()
  {
    return m_aKey.getPath ();
  }

  public boolean exists ()
  {
    return true;
  }

  @Nullable
  public URL getAsURL ()
  {
    // Not applicable atm
    return null;
  }

  @Nullable
  public File getAsFile ()
  {
    // Not applicable atm
    return null;
  }

  @UnsupportedOperation
  public IReadableResource getReadableCloneForPath (final String sPath)
  {
    throw new UnsupportedOperationException ("Clone not supported");
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Key", m_aKey).append ("Content", m_aContent).getToString ();
  }
}
