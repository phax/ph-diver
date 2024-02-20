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
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.type.ObjectType;

/**
 * This is an abstract representation of a file retrieved from a repository. It
 * contains the data and a hashing status. It does not contain information of a
 * folder or filename - that may be taken from {@link RepoStorageKey}.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class RepoStorageItem implements IRepoStorageItem
{
  public static final ObjectType OT_REPO_STORAGE_ITEM = new ObjectType ("repo.storage.item");

  private final IRepoStorageContent m_aContent;
  private final ERepoHashState m_eHashState;

  public RepoStorageItem (@Nonnull final IRepoStorageContent aContent, @Nonnull final ERepoHashState eHashState)
  {
    ValueEnforcer.notNull (aContent, "Content");
    ValueEnforcer.notNull (eHashState, "HashState");
    m_aContent = aContent;
    m_eHashState = eHashState;
  }

  @Nonnull
  public final IRepoStorageContent getContent ()
  {
    return m_aContent;
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
                                       .append ("HashState", m_eHashState)
                                       .getToString ();
  }
}
