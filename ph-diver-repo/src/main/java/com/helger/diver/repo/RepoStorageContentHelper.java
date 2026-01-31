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

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.io.stream.StreamHelper;

/**
 * Utility class
 *
 * @author Philip Helger
 */
@Immutable
public final class RepoStorageContentHelper
{
  private RepoStorageContentHelper ()
  {}

  @Nullable
  public static String getAsUtf8String (@Nullable final IRepoStorageContent aContent)
  {
    if (aContent == null)
      return null;

    // Shortcut?
    if (aContent instanceof RepoStorageContentByteArray)
      return ((RepoStorageContentByteArray) aContent).getAsUtf8String ();

    // Generic way
    final byte [] aBytes = StreamHelper.getAllBytes (aContent);
    return new String (aBytes, StandardCharsets.UTF_8);
  }
}
