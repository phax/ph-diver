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
package com.helger.diver.api.version.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.diver.api.version.IDVRPseudoVersionRegistry;

/**
 * SPI pseudo version registration interface
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@IsSPIInterface
public interface IDVRPseudoVersionRegistrarSPI
{
  /**
   * Register all pseudo versions of this library to the provided registry.
   *
   * @param aRegistry
   *        The registry to register to. Never <code>null</code>.
   */
  void registerPseudoVersions (@Nonnull IDVRPseudoVersionRegistry aRegistry);
}
