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
package com.helger.diver.repo.toc;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;

/**
 * Callback interface for iterating group names in a top-level ToC.
 *
 * @author Philip Helger
 * @since 1.1.0
 */
@FunctionalInterface
public interface IRepoTopTocGroupNameConsumer
{
  /**
   * Consumer callback
   *
   * @param sRelativeGroupName
   *        Relative group name. Neither <code>null</code> nor empty.
   * @param aAbsoluteGroupName
   *        Absolute group name. Neither <code>null</code> nor empty.
   */
  void accept (@NonNull @Nonempty String sRelativeGroupName, @NonNull @Nonempty String aAbsoluteGroupName);
}
