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
package com.helger.diver.repo.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageChain;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.http.mock.LocalJettyRunner;
import com.helger.diver.repo.http.mock.MockRepoStorageLocalFileSystem;
import com.helger.diver.repo.impl.RepoStorageInMemory;
import com.helger.diver.repo.impl.RepoStorageLocalFileSystem;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;
import com.helger.httpclient.HttpClientManager;

/**
 * Test class for class {@link RepoStorageChain}.
 *
 * @author Philip Helger
 */
public final class RepoStorageChainFuncTest
{
  private static final LocalJettyRunner JETTY_HELPER = LocalJettyRunner.createDefaultTestInstance (ERepoWritable.WITH_WRITE,
                                                                                                   ERepoDeletable.WITH_DELETE);

  @BeforeClass
  public static void beforeClass () throws Exception
  {
    JETTY_HELPER.startJetty ();
  }

  @AfterClass
  public static void afterClass () throws Exception
  {
    JETTY_HELPER.stopJetty ();
  }

  @Test
  public void testReadAndCacheAndRead ()
  {
    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-only", "1"),
                                                                       ".txt");

    final RepoStorageInMemory aRepoInMemory = RepoStorageInMemory.createDefault ("unittest-local",
                                                                                 ERepoWritable.WITH_WRITE,
                                                                                 ERepoDeletable.WITHOUT_DELETE);
    final RepoStorageLocalFileSystem aRepoLocalFS = new MockRepoStorageLocalFileSystem (ERepoWritable.WITH_WRITE,
                                                                                        ERepoDeletable.WITH_DELETE);
    final RepoStorageHttp aRepoHttp = new RepoStorageHttp (new HttpClientManager (),
                                                           LocalJettyRunner.DEFAULT_ACCESS_URL,
                                                           "unittest-http",
                                                           ERepoWritable.WITHOUT_WRITE,
                                                           ERepoDeletable.WITHOUT_DELETE,
                                                           new RepoTopTocServiceRepoBasedXML ());

    final RepoStorageChain aRepoChain = RepoStorageChain.of (new CommonsArrayList <> (aRepoInMemory,
                                                                                      aRepoLocalFS,
                                                                                      aRepoHttp),
                                                             new CommonsArrayList <> (aRepoInMemory, aRepoLocalFS));
    assertTrue (aRepoChain.isCacheRemoteContent ());
    assertEquals (3, aRepoChain.internalGetAllStorages ().size ());
    assertEquals (2, aRepoChain.internalGetAllWritableStorages ().size ());

    // Ensure it does not exist locally
    assertNull (aRepoInMemory.read (aKey));
    assertNull (aRepoLocalFS.read (aKey));

    try
    {
      // Read from chain, ending up with the item from HTTP
      // This should implicitly copy the item to in-memory and local FS repo
      RepoStorageItem aItem = aRepoChain.read (aKey);
      assertNotNull (aItem);
      assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
      assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

      // Now it should be present in memory as well
      aItem = aRepoInMemory.read (aKey);
      assertNotNull (aItem);
      assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());

      // Now it should be present locally as well
      aItem = aRepoLocalFS.read (aKey);
      assertNotNull (aItem);
      assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());
    }
    finally
    {
      final File fBase = MockRepoStorageLocalFileSystem.TEST_REPO_DIR;

      // Cleanup from local FS
      File f = new File (fBase, "com/ecosio/http-only/1/http-only-1.txt");
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase, "com/ecosio/http-only/1/http-only-1.txt" + RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete ToC as well
      f = new File (fBase, "com/ecosio/http-only/" + RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase,
                    "com/ecosio/http-only/" +
                           RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML +
                           RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete Top-ToC as well
      f = new File (fBase, RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase, RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML + RepoStorageKey.SUFFIX_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);
    }
  }

  @Test
  public void testReadNoCacheAndRead ()
  {
    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-only", "1"),
                                                                       ".txt");

    final RepoStorageInMemory aInMemory = RepoStorageInMemory.createDefault ("unittest-local",
                                                                             ERepoWritable.WITH_WRITE,
                                                                             ERepoDeletable.WITHOUT_DELETE);
    final RepoStorageLocalFileSystem aLocalFS = new MockRepoStorageLocalFileSystem (ERepoWritable.WITH_WRITE,
                                                                                    ERepoDeletable.WITH_DELETE);
    final RepoStorageHttp aHttp = new RepoStorageHttp (new HttpClientManager (),
                                                       LocalJettyRunner.DEFAULT_ACCESS_URL,
                                                       "unittest-http",
                                                       ERepoWritable.WITHOUT_WRITE,
                                                       ERepoDeletable.WITHOUT_DELETE,
                                                       new RepoTopTocServiceRepoBasedXML ());
    final RepoStorageChain aChain = RepoStorageChain.of (new CommonsArrayList <> (aInMemory, aLocalFS, aHttp),
                                                         new CommonsArrayList <> (aInMemory, aLocalFS))
                                                    .setCacheRemoteContent (false);
    assertFalse (aChain.isCacheRemoteContent ());
    assertEquals (3, aChain.internalGetAllStorages ().size ());
    assertEquals (2, aChain.internalGetAllWritableStorages ().size ());

    // Ensure it does not exist locally
    assertNull (aInMemory.read (aKey));
    assertNull (aLocalFS.read (aKey));

    // Read from chain, ending up with the item from HTTP
    RepoStorageItem aItem = aChain.read (aKey);
    assertNotNull (aItem);
    assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    // Now it should be present in memory as well
    aItem = aInMemory.read (aKey);
    assertNull (aItem);

    // Now it should be present locally as well
    aItem = aLocalFS.read (aKey);
    assertNull (aItem);
  }
}
