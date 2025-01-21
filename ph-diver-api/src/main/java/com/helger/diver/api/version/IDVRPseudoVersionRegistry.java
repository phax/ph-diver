/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.diver.api.version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.state.EChange;

/**
 * Base interface for a Pseudo version registry.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@NotThreadSafe
public interface IDVRPseudoVersionRegistry
{
  /**
   * Register the provided pseudo version.
   *
   * @param aPseudoVersion
   *        The pseudo version to register. Must not be <code>null</code>.
   * @return {@link EChange#CHANGED} if it was added, {@link EChange#UNCHANGED}
   *         if it was already present. Never <code>null</code>.
   */
  @Nonnull
  EChange registerPseudoVersion (@Nonnull IDVRPseudoVersion aPseudoVersion);

  /**
   * Try to resolve the pseudo version with the provided ID.
   *
   * @param sID
   *        The pseudo version ID to look up.
   * @return <code>null</code> if no such pseudo version is present.
   */
  @Nullable
  IDVRPseudoVersion getFromIDOrNull (@Nullable String sID);
}
