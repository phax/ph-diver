/*
 * Copyright (C) 2023-2024 Philip Helger (www.helger.com)
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

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.diver.api.version.IVESPseudoVersionRegistry;
import com.helger.diver.api.version.VESPseudoVersionRegistry;

/**
 * Default pseudo version registrar
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@IsSPIImplementation
public final class DefaultPseudoVersionRegistrarSPIImpl implements IVESPseudoVersionRegistrarSPI
{
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public DefaultPseudoVersionRegistrarSPIImpl ()
  {}

  public void registerPseudoVersions (@Nonnull final IVESPseudoVersionRegistry aRegistry)
  {
    aRegistry.registerPseudoVersion (VESPseudoVersionRegistry.OLDEST);
    aRegistry.registerPseudoVersion (VESPseudoVersionRegistry.LATEST_RELEASE);
    aRegistry.registerPseudoVersion (VESPseudoVersionRegistry.LATEST);
  }
}
