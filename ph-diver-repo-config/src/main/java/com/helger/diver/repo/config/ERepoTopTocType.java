/*
 * Copyright (C) 2023-2026 Philip Helger
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
package com.helger.diver.repo.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;

/**
 * The kind of top-level ToC a configured repository has.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
public enum ERepoTopTocType implements IHasID <String>
{
  /** The repository has no top-level ToC */
  NONE ("none"),
  /**
   * The repository has a top-level ToC in the XML file
   * {@link com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML#FILENAME_TOP_TOC_DIVER_XML}
   */
  DIVER_XML ("diver-xml");

  private final String m_sID;

  ERepoTopTocType (@NonNull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  /**
   * @return The ID as used in the XML configuration. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ERepoTopTocType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ERepoTopTocType.class, sID);
  }
}
