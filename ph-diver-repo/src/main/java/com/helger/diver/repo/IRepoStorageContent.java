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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.io.IHasInputStreamAndReader;

/**
 * The main content of an {@link IRepoStorageItem}.
 *
 * @author Philip Helger
 */
public interface IRepoStorageContent extends IHasInputStreamAndReader
{
  /**
   * @return The number of bytes of the item. Must be &ge; 0.
   */
  @Nonnegative
  long getLength ();

  // Use stream based where possible
  @Deprecated
  @Nonnull
  byte [] getAllBytesNoCopy ();

  /**
   * @return The whole content as an UTF-8 String. This is primarily available
   *         for testing purposes. May be <code>null</code>.
   */
  @Nullable
  String getAsUtf8String ();
}
