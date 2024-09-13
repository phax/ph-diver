/*
 * Copyright (C) 2023-2024 Philip Helger & ecosio
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
package com.helger.diver.repo;

import com.helger.commons.id.IHasID;

/**
 * Read-only interface to classify a repository storage type
 *
 * @author Philip Helger
 */
public interface IRepoStorageType extends IHasID <String>
{
  /**
   * @return <code>true</code> if the data is persisted on a disk, database or
   *         so, <code>false</code> if it is only kept in memory.
   */
  boolean isPersistent ();

  /**
   * @return <code>true</code> if the repository is accessed using network
   *         technology, <code>false</code> if it is a local source. In offline
   *         mode, remote repositories cannot be accessed.
   */
  boolean isRemote ();
}
