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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.io.ByteArrayWrapper;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.ToStringGenerator;

/**
 * This is the default implementation of {@link IRepoStorageContent} based on a
 * byte array.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class RepoStorageContent implements IRepoStorageContent
{
  private final ByteArrayWrapper m_aBytes;

  private RepoStorageContent (@Nonnull final ByteArrayWrapper aBytes)
  {
    ValueEnforcer.notNull (aBytes, "Bytes");
    m_aBytes = aBytes;
  }

  @Nonnull
  public InputStream getInputStream ()
  {
    return m_aBytes.getInputStream ();
  }

  @Nonnull
  @Override
  public InputStream getBufferedInputStream ()
  {
    // No need to buffer further
    return m_aBytes.getInputStream ();
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

  @Deprecated
  @Nonnegative
  public byte [] getAllBytesNoCopy ()
  {
    return m_aBytes.bytes ();
  }

  @Nonnull
  @ReturnsMutableObject
  public ByteArrayWrapper data ()
  {
    return m_aBytes;
  }

  @Nonnull
  public String getAsString (@Nonnull final Charset aCharset)
  {
    return m_aBytes.getBytesAsString (aCharset);
  }

  @Nonnull
  public String getAsUtf8String ()
  {
    return getAsString (StandardCharsets.UTF_8);
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
  @Nonnull
  public static RepoStorageContent of (@Nonnull final byte [] aContent)
  {
    ValueEnforcer.notNull (aContent, "Content");

    return new RepoStorageContent (new ByteArrayWrapper (aContent, false));
  }

  /**
   * Create a new item, based on the UTF-8 bytes of the provided string
   *
   * @param sContent
   *        The UTF-8 bytes to store.
   * @return A new item and never <code>null</code>.
   */
  @Nonnull
  public static RepoStorageContent ofUtf8 (@Nonnull final String sContent)
  {
    ValueEnforcer.notNull (sContent, "String Content");

    return of (sContent.getBytes (StandardCharsets.UTF_8));
  }

  @Nonnull
  public static RepoStorageContent of (@Nonnull final IReadableResource aRes)
  {
    ValueEnforcer.notNull (aRes, "Resource");

    return of (StreamHelper.getAllBytes (aRes));
  }
}
