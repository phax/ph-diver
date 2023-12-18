/*
 * Copyright (C) 2023 Philip Helger & ecosio
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

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.state.ESuccess;

/**
 * Base interface for a top-level ToC
 *
 * @author Philip Helger
 * @since 1.1.0
 */
public interface IRepoTopTocService
{
  /**
   * Only invoked by the repository that uses this Top ToC service. This method
   * must be called before any other method is called.
   *
   * @param aRepo
   *        The repository that uses this service.
   */
  void initForRepo (@Nonnull IRepoStorageWithToc aRepo);

  void iterateAllTopLevelGroupNames (@Nonnull Consumer <String> aGroupNameConsumer);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsSortedSet <String> getAllTopLevelGroupNames ()
  {
    final ICommonsSortedSet <String> ret = new CommonsTreeSet <> ();
    iterateAllTopLevelGroupNames (ret::add);
    return ret;
  }

  void iterateAllSubGroups (@Nonnull @Nonempty String sGroupID,
                            @Nonnull IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                            boolean bRecursive);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNamesRecursive (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), true);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNames (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), false);
    return ret;
  }

  void iterateAllArtifacts (@Nonnull @Nonempty String sGroupID, @Nonnull Consumer <String> aArtifactNameConsumer);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllArtefacts (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllArtifacts (sGroupID, ret::add);
    return ret;
  }

  @Nonnull
  ESuccess registerGroupAndArtifact (@Nonnull @Nonempty String sGroupID, @Nonnull @Nonempty String sArtifactID);
}
