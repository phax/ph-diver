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

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.collection.commons.ICommonsSortedSet;

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
  void initForRepo (@NonNull IRepoStorageWithToc aRepo);

  /**
   * Check if the provided group ID and artifact ID are contained or not.
   *
   * @param sGroupID
   *        Group ID to check. May be <code>null</code>.
   * @param sArtifactID
   *        Artifact ID to check. May be <code>null</code>.
   * @return <code>true</code> if it is contained, <code>false</code> if not
   */
  boolean containsGroupAndArtifact (@Nullable String sGroupID, @Nullable String sArtifactID);

  /**
   * Iterate all top-level group names.
   *
   * @param aGroupNameConsumer
   *        The consumer to be invoked for all top-level group names. Must not
   *        be <code>null</code>.
   * @see #getAllTopLevelGroupNames()
   */
  void iterateAllTopLevelGroupNames (@NonNull Consumer <String> aGroupNameConsumer);

  /**
   * @return A set of all contained top-level group names. Never
   *         <code>null</code> but maybe empty.
   * @see #iterateAllTopLevelGroupNames(Consumer)
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsSortedSet <String> getAllTopLevelGroupNames ()
  {
    final ICommonsSortedSet <String> ret = new CommonsTreeSet <> ();
    iterateAllTopLevelGroupNames (ret::add);
    return ret;
  }

  /**
   * Iterate all sub groups of the provided group ID.
   *
   * @param sGroupID
   *        The top-level or absolute group ID to start at. May neither be
   *        <code>null</code> nor empty.
   * @param aGroupNameConsumer
   *        The consumer to be invoked for each match. May not be
   *        <code>null</code>.
   * @param bRecursive
   *        <code>true</code> to iterate recursively, <code>false</code> to
   *        iterate just one level.
   * @see #getAllAbsoluteSubGroupNames(String)
   * @see #getAllAbsoluteSubGroupNamesRecursive(String)
   */
  void iterateAllSubGroups (@NonNull @Nonempty String sGroupID,
                            @NonNull IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                            boolean bRecursive);

  @NonNull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNamesRecursive (@NonNull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), true);
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNames (@NonNull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), false);
    return ret;
  }

  /**
   * Iterate all artifacts in the provided group ID.
   *
   * @param sGroupID
   *        The top-level or absolute group ID to iterate. May neither be
   *        <code>null</code> nor empty.
   * @param aArtifactNameConsumer
   *        The consumer to be invoked for each artifact. May not be
   *        <code>null</code>.
   * @see #getAllArtefacts(String)
   */
  void iterateAllArtifacts (@NonNull @Nonempty String sGroupID, @NonNull Consumer <String> aArtifactNameConsumer);

  @NonNull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllArtefacts (@NonNull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllArtifacts (sGroupID, ret::add);
    return ret;
  }

  /**
   * Register a new combination of group ID and artifact ID into the top-level
   * ToC. If the provided combination is already present, nothing happens and
   * success is to be returned.
   *
   * @param sGroupID
   *        Absolute Group ID to register. May neither be <code>null</code> nor
   *        empty.
   * @param sArtifactID
   *        The artifact ID to register. May neither be <code>null</code> nor
   *        empty.
   * @return {@link ESuccess} and never <code>null</code>.
   */
  @NonNull
  ESuccess registerGroupAndArtifact (@NonNull @Nonempty String sGroupID, @NonNull @Nonempty String sArtifactID);
}
