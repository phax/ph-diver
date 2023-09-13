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
package com.helger.diver.repo.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.junit.Test;

import com.helger.commons.state.ESuccess;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;

import software.amazon.awssdk.services.s3.S3Client;

/**
 * Test class for class {@link RepoStorageS3}.
 *
 * @author Philip Helger
 */
public final class RepoStorageS3Test
{
  @Nonnull
  private static RepoStorageS3 _createRepoReadOnly ()
  {
    return new RepoStorageS3 (S3Client.builder ().build (),
                              "bucket",
                              "unittest.s3",
                              ERepoWritable.WITHOUT_WRITE,
                              ERepoDeletable.WITHOUT_DELETE);
  }

  @Test
  public void testReadOnlyRead ()
  {
    final RepoStorageS3 aRepo = _createRepoReadOnly ();
    assertFalse (aRepo.canWrite ());

    // Existing only in "local fs" repo
    RepoStorageItem aItem = aRepo.read (RepoStorageKey.of ("com/ecosio/test/http.txt"));
    assertNull (aItem);

    // This one exists
    aItem = aRepo.read (RepoStorageKey.of ("com/ecosio/http-only/http-only.txt"));
    assertNotNull (aItem);
    assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());
  }

  @Nonnull
  private static RepoStorageS3 _createRepoWritable ()
  {
    return new RepoStorageS3 (S3Client.builder ().build (),
                              "bucket",
                              "unittest.s3",
                              ERepoWritable.WITH_WRITE,
                              ERepoDeletable.WITH_DELETE);
  }

  @Test
  public void testWritableRead ()
  {
    final RepoStorageS3 aRepo = _createRepoWritable ();
    assertTrue (aRepo.canWrite ());

    RepoStorageItem aItem = aRepo.read (RepoStorageKey.of ("com/ecosio/http-only/http-only.txt"));
    assertNotNull (aItem);
    assertEquals ("This file is on HTTP native", aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.NOT_VERIFIED, aItem.getHashState ());

    // Ensure the one written below, is not existing
    aItem = aRepo.read (RepoStorageKey.of ("com/ecosio/written/http-write.txt"));
    assertNull (aItem);
  }

  @Test
  public void testWritableWriteAndRead ()
  {
    final RepoStorageS3 aRepo = _createRepoWritable ();
    assertTrue (aRepo.canWrite ());

    final RepoStorageKey aKey = RepoStorageKey.of ("com/ecosio/written/http-write.txt");

    // Ensure not existing
    assertNull (aRepo.read (aKey));

    final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();
    // Write
    final ESuccess eSuccess = aRepo.write (aKey, RepoStorageItem.ofUtf8 (sUploadedPayload));
    assertTrue (eSuccess.isSuccess ());

    // Read again
    final RepoStorageItem aItem = aRepo.read (aKey);
    assertNotNull (aItem);
    assertEquals (sUploadedPayload, aItem.getDataAsUtf8String ());
    assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());
  }
}
