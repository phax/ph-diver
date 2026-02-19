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
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;

/**
 * Empty implementation of {@link IRepoTopTocService}.
 *
 * @author Philip Helger
 * @since 4.2.0
 */
public class DoNothingRepoTopTocService implements IRepoTopTocService
{
  // Default instance
  public static final DoNothingRepoTopTocService INSTANCE = new DoNothingRepoTopTocService ();

  public void initForRepo (@NonNull final IRepoStorageWithToc aRepo)
  {}

  public void refreshFromRepo ()
  {}

  public boolean containsGroupAndArtifact (@Nullable final String sGroupID, @Nullable final String sArtifactID)
  {
    return false;
  }

  public void iterateAllTopLevelGroupNames (@NonNull final Consumer <String> aGroupNameConsumer)
  {}

  public void iterateAllSubGroups (@NonNull @Nonempty final String sGroupID,
                                   @NonNull final IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                                   final boolean bRecursive)
  {}

  public void iterateAllArtifacts (@NonNull @Nonempty final String sGroupID,
                                   @NonNull final Consumer <String> aArtifactNameConsumer)
  {}

  @NonNull
  public ESuccess registerGroupAndArtifact (@NonNull @Nonempty final String sGroupID,
                                            @NonNull @Nonempty final String sArtifactID)
  {
    return ESuccess.SUCCESS;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
