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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.diver.api.DVRException;
import com.helger.diver.api.id.DVRID;

/**
 * Test class for class {@link RepoStorageKeyOfArtefact}.
 *
 * @author Philip Helger
 */
public final class RepoStorageKeyOfArtefactTest
{
  @Test
  public void testBasic () throws DVRException
  {
    RepoStorageKey aKey = RepoStorageKeyOfArtefact.of (DVRID.parseID ("com.ecosio:test-artefact:1.2.0"), ".xml");
    // Breaking change "1.2.0" -> "1.2" no trailing spaces
    assertEquals ("com/ecosio/test-artefact/1.2/test-artefact-1.2.xml", aKey.getPath ());

    aKey = RepoStorageKeyOfArtefact.of (DVRID.parseID ("com.ecosio:test-artefact:1.2.0"), ".xyz");
    assertEquals ("com/ecosio/test-artefact/1.2/test-artefact-1.2.xyz", aKey.getPath ());

    aKey = RepoStorageKeyOfArtefact.of (DVRID.parseID ("com.ecosio:test-artefact:1.2.1"), ".xyz");
    assertEquals ("com/ecosio/test-artefact/1.2.1/test-artefact-1.2.1.xyz", aKey.getPath ());

    aKey = RepoStorageKeyOfArtefact.of (DVRID.parseID ("com.ecosio:test-artefact:1.0.0"), ".xyz");
    assertEquals ("com/ecosio/test-artefact/1/test-artefact-1.xyz", aKey.getPath ());

    aKey = RepoStorageKeyOfArtefact.of (DVRID.parseID ("a:b:4"), ".xyz");
    assertEquals ("a/b/4/b-4.xyz", aKey.getPath ());
  }
}
