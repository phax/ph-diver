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

import com.helger.commons.version.Version;

/**
 * Helper interface to ensure that versions and pseudo version can be kept in
 * strict order.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
public interface IDVRPseudoVersionComparable
{
  /**
   * Compare this object to the provided pseudo version.
   *
   * @param aOtherPseudoVersion
   *        The pseudo version to compare to. Never <code>null</code>.
   * @return a value &lt; 0 if this is &lt; other version; value 0 if this =
   *         other version; value &gt; 0 if this is &gt; other version
   */
  int compareToPseudoVersion (@Nonnull IDVRPseudoVersion aOtherPseudoVersion);

  /**
   * Compare this object to the provided static version.
   *
   * @param aOtherStaticVersion
   *        The static version to compare to. Never <code>null</code>.
   * @return a value &lt; 0 if this is &lt; other version; value 0 if this =
   *         other version; value &gt; 0 if this is &gt; other version
   */
  int compareToVersion (@Nonnull Version aOtherStaticVersion);
}
