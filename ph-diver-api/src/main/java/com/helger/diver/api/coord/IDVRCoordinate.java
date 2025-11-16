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
package com.helger.diver.api.coord;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.diver.api.version.DVRVersion;

/**
 * The DVR Coordinate represents the coordinate of a single technical artefact in a specific
 * version.<br />
 * It was originally called VESID for "Validation Executor Set ID" but is now used in a wider range
 * of use cases. The name was changed for release v2 to DVRID. In v3 the name was changed again to
 * DVR Coordinate.<br/>
 * This is the read-only interface for a single DVR Coordinate.
 *
 * @author Philip Helger
 * @since 3.0.0
 */
public interface IDVRCoordinate
{
  /**
   * @return The coordinate's group ID. May never be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getGroupID ();

  /**
   * @return The coordinate's artifact ID. May never be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getArtifactID ();

  /**
   * @return The coordinate's version as a single string. May never be <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  default String getVersionString ()
  {
    return getVersionObj ().getAsString ();
  }

  /**
   * @return The coordinates version object. Never <code>null</code>.
   */
  @NonNull
  DVRVersion getVersionObj ();

  /**
   * @return <code>true</code> if a classifier is present, <code>false</code> if not.
   */
  default boolean hasClassifier ()
  {
    return StringHelper.isNotEmpty (getClassifier ());
  }

  /**
   * @return The coordinate's optional classifier ID. May be <code>null</code> or empty.
   */
  @Nullable
  String getClassifier ();

  /**
   * @return A joint String representation of the coordinates. The different parts are separated by
   *         a colon (:) character. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getAsSingleID ();
}
