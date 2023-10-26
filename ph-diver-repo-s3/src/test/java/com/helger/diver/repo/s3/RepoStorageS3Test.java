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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.state.ESuccess;
import com.helger.commons.url.URLHelper;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/**
 * Test class for class {@link RepoStorageS3}.<br>
 * Use the following command to run a local S3 instance as Docker image:
 *
 * <pre>
 * docker run --pull always --name s3mock -p 9090:9090 -p 9191:9191 -e initialBuckets=test -e debug=true -t adobe/s3mock
 * </pre>
 *
 * @author Philip Helger
 */
public final class RepoStorageS3Test
{
  private static final String TEST_BUCKET_NAME = "test";
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageS3Test.class);

  @Nonnull
  private static S3Client _s3Client ()
  {
    return S3Client.builder ()
                   .region (Region.EU_WEST_1)
                   // This URL is intended for the s3Mock Docker image
                   .endpointOverride (URLHelper.getAsURI ("http://localhost:9090/" + TEST_BUCKET_NAME))
                   .credentialsProvider (AnonymousCredentialsProvider.create ())
                   .build ();
  }

  @Nonnull
  private static RepoStorageS3 _createRepoWritable ()
  {
    return new RepoStorageS3 (_s3Client (),
                              "bucket",
                              "unittest.s3",
                              ERepoWritable.WITH_WRITE,
                              ERepoDeletable.WITH_DELETE);
  }

  @Test
  public void testWritableWriteAndRead ()
  {
    final RepoStorageS3 aRepo = _createRepoWritable ();
    assertTrue (aRepo.canWrite ());

    // Existing only in "local fs" repo but not in S3
    RepoStorageItem aItem = aRepo.read (RepoStorageKey.of (new VESID ("com.ecosio", "local", "1"), ".txt"));
    assertNull (aItem);

    final RepoStorageKey aKey = RepoStorageKey.of (new VESID ("com.ecosio", "s3-written", "1"), ".txt");
    boolean bS3Available = true;

    try
    {
      // Ensure not existing
      assertNull (aRepo.read (aKey));

      final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();
      // Write
      final ESuccess eSuccess = aRepo.write (aKey, RepoStorageItem.ofUtf8 (sUploadedPayload));
      assertTrue (eSuccess.isSuccess ());

      // Read again
      aItem = aRepo.read (aKey);
      assertNotNull (aItem);
      assertEquals (sUploadedPayload, aItem.getDataAsUtf8String ());
      assertSame (ERepoHashState.VERIFIED_MATCHING, aItem.getHashState ());
    }
    catch (final SdkClientException ex)
    {
      LOGGER.warn ("No S3 instance available: " + ex.getMessage ());
      bS3Available = false;
    }
    finally
    {
      if (bS3Available)
      {
        // Cleanup if S3 is there
        final S3Client s3 = _s3Client ();
        s3.deleteObject (DeleteObjectRequest.builder ()
                                            .bucket (TEST_BUCKET_NAME)
                                            .key ("com/ecosio/s3-written/1/s3-written-1.txt")
                                            .build ());
        s3.deleteObject (DeleteObjectRequest.builder ()
                                            .bucket (TEST_BUCKET_NAME)
                                            .key ("com/ecosio/s3-written/1/s3-written-1.txt" +
                                                  RepoStorageKey.SUFFIX_SHA256)
                                            .build ());
      }
    }
  }
}
