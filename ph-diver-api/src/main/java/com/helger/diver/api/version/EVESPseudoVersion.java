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
package com.helger.diver.api.version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * VES supported pseudo versions.
 *
 * @author Philip Helger
 */
public enum EVESPseudoVersion implements IHasID <String>
{
  /**
   * Indicates the latest version.
   */
  LATEST ("latest");

  private final String m_sID;

  EVESPseudoVersion (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public int compareToSemantically (@Nonnull final EVESPseudoVersion eOtherPseudoVersion)
  {
    if (this == eOtherPseudoVersion)
      return 0;

    // Add implementation if other pseudo versions are contained in the future
    throw new UnsupportedOperationException ();
  }

  @Nullable
  public static EVESPseudoVersion getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EVESPseudoVersion.class, sID);
  }
}
