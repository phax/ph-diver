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
package com.helger.diver.repo.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import com.helger.commons.state.ESuccess;
import com.helger.diver.api.DVRException;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageContentHelper;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.toc.RepoToc;

/**
 * Test class for class {@link RepoStorageInMemory}.
 *
 * @author Philip Helger
 */
public final class RepoStorageInMemoryTest
{
  @Test
  public void testReadWriteReadDelete () throws DVRException
  {
    final RepoStorageInMemory aRepo = RepoStorageInMemory.createDefault ("unittest",
                                                                         ERepoWritable.WITH_WRITE,
                                                                         ERepoDeletable.WITH_DELETE);
    assertTrue (aRepo.canWrite ());
    assertTrue (aRepo.canDelete ());
    assertTrue (aRepo.isEnableTocUpdates ());

    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new DVRCoordinate ("com.ecosio", "local", "1"), ".txt");
    // Ensure not existing
    assertNull (aRepo.read (aKey));

    final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();

    try
    {
      // Write
      ESuccess eSuccess = aRepo.write (aKey, RepoStorageContentByteArray.ofUtf8 (sUploadedPayload));
      assertTrue (eSuccess.isSuccess ());

      {
        final RepoToc aToc = aRepo.readTocModel (aKey.getCoordinate ());
        assertNotNull (aToc);
        assertEquals (1, aToc.getVersionCount ());
      }

      // Read again
      IRepoStorageReadItem aItem = aRepo.read (aKey);
      assertNotNull (aItem);
      assertEquals (sUploadedPayload, RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());

      // Delete
      eSuccess = aRepo.delete (aKey);
      assertTrue (eSuccess.isSuccess ());

      {
        final RepoToc aToc = aRepo.readTocModel (aKey.getCoordinate ());
        assertNotNull (aToc);
        assertEquals (0, aToc.getVersionCount ());
      }

      // Read again
      aItem = aRepo.read (aKey);
      assertNull (aItem);
    }
    finally
    {
      // Cleanup
      aRepo.delete (aKey);
    }
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testReadOnly () throws DVRException
  {
    final RepoStorageInMemory aRepo = RepoStorageInMemory.createDefault ("unittest",
                                                                         ERepoWritable.WITHOUT_WRITE,
                                                                         ERepoDeletable.WITHOUT_DELETE);
    assertFalse (aRepo.canWrite ());
    assertFalse (aRepo.canDelete ());
    assertTrue (aRepo.isAllowOverwrite ());

    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new DVRCoordinate ("com.ecosio", "local", "1"), ".txt");
    final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();

    // Register only payload, but no hash
    aRepo.registerObject (aKey, RepoStorageContentByteArray.ofUtf8 (sUploadedPayload));

    // Read again
    final IRepoStorageReadItem aItem = aRepo.read (aKey);
    assertNotNull (aItem);
    assertEquals (sUploadedPayload, RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    // Throws exception
    aRepo.write (aKey, RepoStorageContentByteArray.ofUtf8 ("something else"));
  }
}
