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
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.state.ESuccess;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageContentHelper;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.http.mock.LocalJettyRunner;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;
import com.helger.httpclient.HttpClientManager;

/**
 * Test class for class {@link RepoStorageHttp}.
 *
 * @author Philip Helger
 */
public final class RepoStorageHttpTest
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

  @Nonnull
  private static RepoStorageHttp _createRepoReadOnly ()
  {
    return new RepoStorageHttp (new HttpClientManager (),
                                LocalJettyRunner.DEFAULT_ACCESS_URL,
                                "unittest",
                                ERepoWritable.WITHOUT_WRITE,
                                ERepoDeletable.WITHOUT_DELETE,
                                new RepoTopTocServiceRepoBasedXML ());
  }

  @Test
  public void testReadOnlyRead ()
  {
    final RepoStorageHttp aRepo = _createRepoReadOnly ();
    assertFalse (aRepo.canWrite ());

    // Existing only in "local fs" repo but not in http repo
    IRepoStorageReadItem aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "local", "1"),
                                                                          ".txt"));
    assertNull (aItem);

    // This one exists
    aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-only", "1"), ".txt"));
    assertNotNull (aItem);
    assertEquals ("This file is on HTTP native", RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());
  }

  @Nonnull
  private static RepoStorageHttp _createRepoWritable ()
  {
    return new RepoStorageHttp (new HttpClientManager (),
                                LocalJettyRunner.DEFAULT_ACCESS_URL,
                                "unittest",
                                ERepoWritable.WITH_WRITE,
                                ERepoDeletable.WITH_DELETE,
                                new RepoTopTocServiceRepoBasedXML ());
  }

  @Test
  public void testWritableRead ()
  {
    final RepoStorageHttp aRepo = _createRepoWritable ();
    assertTrue (aRepo.canWrite ());

    IRepoStorageReadItem aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-only", "1"),
                                                                          ".txt"));
    assertNotNull (aItem);
    assertEquals ("This file is on HTTP native", RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    // Ensure the one written below, is not existing
    aItem = aRepo.read (RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-written", "1"), ".txt"));
    assertNull (aItem);
  }

  @Test
  public void testWritableWriteAndRead ()
  {
    final RepoStorageHttp aRepoHttp = _createRepoWritable ();
    assertTrue (aRepoHttp.canWrite ());

    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (new VESID ("com.ecosio", "http-written", "1"),
                                                                       ".txt");

    try
    {
      // Ensure not existing
      assertNull (aRepoHttp.read (aKey));

      final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();

      // Write
      final ESuccess eSuccess = aRepoHttp.write (aKey, RepoStorageContentByteArray.ofUtf8 (sUploadedPayload));
      assertTrue (eSuccess.isSuccess ());

      // Read again
      final IRepoStorageReadItem aItem = aRepoHttp.read (aKey);
      assertNotNull (aItem);
      assertEquals (sUploadedPayload, RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());
    }
    finally
    {
      final File fBase = LocalJettyRunner.DEFAULT_TEST_RESOURCE_BASE;

      // Cleanup
      File f = new File (fBase, "com/ecosio/http-written/1/http-written-1.txt");
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase, "com/ecosio/http-written/1/http-written-1.txt" + RepoStorageKey.FILE_EXT_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete ToC as well
      f = new File (fBase, "com/ecosio/http-written/" + RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase,
                    "com/ecosio/http-written/" +
                           RepoStorageKeyOfArtefact.FILENAME_TOC_DIVER_XML +
                           RepoStorageKey.FILE_EXT_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);

      // Delete Top-ToC as well
      f = new File (fBase, RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML);
      FileOperationManager.INSTANCE.deleteFile (f);

      f = new File (fBase, RepoTopTocServiceRepoBasedXML.FILENAME_TOP_TOC_DIVER_XML + RepoStorageKey.FILE_EXT_SHA256);
      FileOperationManager.INSTANCE.deleteFile (f);
    }
  }
}
