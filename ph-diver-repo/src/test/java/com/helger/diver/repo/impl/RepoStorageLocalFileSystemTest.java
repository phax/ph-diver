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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.junit.Test;

import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.state.ESuccess;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.mock.MockRepoStorageLocalFileSystem;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;

/**
 * Test class for class {@link RepoStorageLocalFileSystem}.
 *
 * @author Philip Helger
 */
public final class RepoStorageLocalFileSystemTest
{
  @Nonnull
  private static RepoStorageLocalFileSystem _createRepo ()
  {
    return new MockRepoStorageLocalFileSystem (ERepoWritable.WITH_WRITE, ERepoDeletable.WITH_DELETE);
  }

  @Test
  public void testRead ()
  {
    final RepoStorageLocalFileSystem aRepo = _createRepo ();

    RepoStorageItem aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio.test", "a", "1"), ".txt"));
    assertNotNull (aItem);
    assertEquals ("A", aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio.test", "b", "1"), ".txt"));
    assertNotNull (aItem);
    assertEquals ("B", aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio.test", "c", "1"), ".txt"));
    assertNull (aItem);
  }

  @Test
  public void testWrite ()
  {
    final RepoStorageLocalFileSystem aRepo = _createRepo ();

    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio.test", "fs-written", "1"),
                                                                       ".txt");
    // Ensure not existing
    assertNull (aRepo.read (aKey));

    final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();

    try
    {
      // Write
      final ESuccess eSuccess = aRepo.write (aKey, RepoStorageItem.ofUtf8 (sUploadedPayload));
      assertTrue (eSuccess.isSuccess ());

      // Read again
      final RepoStorageItem aItem = aRepo.read (aKey);
      assertNotNull (aItem);
      assertEquals (sUploadedPayload, aItem.getDataAsUtf8String ());
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());
    }
    finally
    {
      // Cleanup
      File f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR, "com/ecosio/test/fs-written/1/fs-written-1.txt");
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR,
                    "com/ecosio/test/fs-written/1/fs-written-1.txt" + RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete ToC as well
      f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR,
                    "com/ecosio/test/fs-written/" + RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR,
                    "com/ecosio/test/fs-written/" +
                                                                  RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML +
                                                                  RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete Top-ToC as well
      f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR,
                    RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (MockRepoStorageLocalFileSystem.TEST_REPO_DIR,
                    RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML + RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);
    }
  }
}
