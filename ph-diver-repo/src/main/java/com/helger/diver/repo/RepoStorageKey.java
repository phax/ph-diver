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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A key that identifies a single item to be exchanged. It is an abstract
 * interpretation of a combination of folder and filename.
 *
 * @author Philip Helger
 */
@Immutable
public class RepoStorageKey
{
  /**
   * The filename suffix for the hash values of uploaded content.
   */
  public static final String FILE_EXT_SHA256 = ".sha256";

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageKey.class);

  private final String m_sPath;

  public RepoStorageKey (@NonNull @Nonempty final String sPath)
  {
    ValueEnforcer.notEmpty (sPath, "Path");
    ValueEnforcer.isFalse ( () -> sPath.startsWith ("/"), "Path should not start with a Slash");
    ValueEnforcer.isFalse ( () -> sPath.endsWith ("/"), "Path should not end with a Slash");

    m_sPath = sPath;
  }

  @NonNull
  @Nonempty
  public final String getPath ()
  {
    return m_sPath;
  }

  @NonNull
  @ReturnsMutableCopy
  public RepoStorageKey getKeyHashSha256 ()
  {
    final String sPath = getPath ();
    if (sPath.endsWith (FILE_EXT_SHA256))
    {
      // Seems like a doubled hash key
      LOGGER.warn ("You are trying to create a RepoStorageKey SHA-256 of something that already seems to be a SHA-256 key: '" +
                   sPath +
                   "'");
    }
    return new RepoStorageKey (sPath + FILE_EXT_SHA256);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final RepoStorageKey rhs = (RepoStorageKey) o;
    return m_sPath.equals (rhs.m_sPath);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sPath).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Path", m_sPath).getToString ();
  }
}
