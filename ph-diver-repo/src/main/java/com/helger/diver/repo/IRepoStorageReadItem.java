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

import com.helger.annotation.style.ReturnsMutableCopy;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base interface for the data of an item in a repository as retrieved by
 * reading.
 *
 * @author Philip Helger
 */
public interface IRepoStorageReadItem
{
  /**
   * @return The main content of the item. Never <code>null</code>.
   */
  @Nonnull
  IRepoStorageContent getContent ();

  /**
   * @return <code>true</code> if this repo item has an expected hash value
   *         assigned with it.
   */
  boolean hasExpectedDigest ();

  /**
   * @return The stored hash bytes of this repo item or <code>null</code>.
   * @see #hasExpectedDigest()
   */
  @Nullable
  @ReturnsMutableCopy
  byte [] getExpectedDigest ();

  /**
   * @return <code>true</code> if a hash value was calculated during reading,
   *         <code>false</code> if not.
   */
  boolean hasCalculatedDigest ();

  /**
   * @return The calculated hash bytes of this repo item or <code>null</code>.
   * @see #hasCalculatedDigest()
   */
  @Nullable
  @ReturnsMutableCopy
  byte [] getCalculatedDigest ();

  /**
   * @return The verification state of this item. Never <code>null</code>.
   */
  @Nonnull
  ERepoHashState getHashState ();
}
