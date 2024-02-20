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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;

/**
 * This is an abstract representation of a file retrieved from a repository. It
 * contains the content and a hashing status. It does not contain information of
 * a folder or filename - that may be taken from {@link RepoStorageKey}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class RepoStorageReadItem implements IRepoStorageReadItem
{
  public static final ObjectType OT_REPO_STORAGE_READ_ITEM = new ObjectType ("repo.storage.read.item");

  private final IRepoStorageContent m_aContent;
  private final byte [] m_aExpectedDigest;
  private final byte [] m_aCalculatedDigest;
  private final ERepoHashState m_eHashState;

  public RepoStorageReadItem (@Nonnull final IRepoStorageContent aContent,
                              @Nullable final byte [] aExpectedDigest,
                              @Nullable final byte [] aCalculatedDigest,
                              @Nonnull final ERepoHashState eHashState)
  {
    ValueEnforcer.notNull (aContent, "Content");
    ValueEnforcer.notNull (eHashState, "HashState");
    m_aContent = aContent;
    m_aExpectedDigest = ArrayHelper.getCopy (aExpectedDigest);
    m_aCalculatedDigest = ArrayHelper.getCopy (aCalculatedDigest);
    m_eHashState = eHashState;
  }

  @Nonnull
  public final IRepoStorageContent getContent ()
  {
    return m_aContent;
  }

  public final boolean hasExpectedDigest ()
  {
    return m_aExpectedDigest != null && m_aExpectedDigest.length > 0;
  }

  @Nullable
  @ReturnsMutableCopy
  public final byte [] getExpectedDigest ()
  {
    return ArrayHelper.getCopy (m_aExpectedDigest);
  }

  public final boolean hasCalculatedDigest ()
  {
    return m_aCalculatedDigest != null && m_aCalculatedDigest.length > 0;
  }

  @Nullable
  @ReturnsMutableCopy
  public final byte [] getCalculatedDigest ()
  {
    return ArrayHelper.getCopy (m_aCalculatedDigest);
  }

  @Nonnull
  public final ERepoHashState getHashState ()
  {
    return m_eHashState;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Content", m_aContent)
                                       .append ("ExpectedDigest", m_aExpectedDigest)
                                       .append ("CalculatedDigest", m_aCalculatedDigest)
                                       .append ("HashState", m_eHashState)
                                       .getToString ();
  }
}
