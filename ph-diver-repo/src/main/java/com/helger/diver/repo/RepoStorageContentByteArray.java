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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.base.array.bytes.ByteArrayWrapper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.io.resource.IReadableResource;

/**
 * This is the default implementation of {@link IRepoStorageContent} based on a
 * byte array.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class RepoStorageContentByteArray implements IRepoStorageContent
{
  private final ByteArrayWrapper m_aBytes;

  public RepoStorageContentByteArray (@NonNull final ByteArrayWrapper aBytes)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    m_aBytes = aBytes;
  }

  @NonNull
  public InputStream getInputStream ()
  {
    return m_aBytes.getInputStream ();
  }

  @NonNull
  @Override
  public InputStream getBufferedInputStream ()
  {
    // No need to buffer further
    return getInputStream ();
  }

  public boolean isReadMultiple ()
  {
    return true;
  }

  @Nonnegative
  public long getLength ()
  {
    return m_aBytes.size ();
  }

  @NonNull
  public String getAsUtf8String ()
  {
    return m_aBytes.getBytesAsString (StandardCharsets.UTF_8);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Bytes", m_aBytes).getToString ();
  }

  /**
   * Create a new item, that does not copy the byte array for performance
   * reasons.
   *
   * @param aContent
   *        The data to be wrapped.
   * @return A new item and never <code>null</code>.
   */
  @NonNull
  public static RepoStorageContentByteArray of (@NonNull final byte [] aContent)
  {
    ValueEnforcer.notNull (aContent, "Content");

    return new RepoStorageContentByteArray (new ByteArrayWrapper (aContent, false));
  }

  /**
   * Create a new item, based on the UTF-8 bytes of the provided string
   *
   * @param sContent
   *        The UTF-8 bytes to store.
   * @return A new item and never <code>null</code>.
   */
  @NonNull
  public static RepoStorageContentByteArray ofUtf8 (@NonNull final String sContent)
  {
    ValueEnforcer.notNull (sContent, "String Content");

    return of (sContent.getBytes (StandardCharsets.UTF_8));
  }

  @NonNull
  public static RepoStorageContentByteArray of (@NonNull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Resource");

    return of (StreamHelper.getAllBytes (aRes));
  }
}
