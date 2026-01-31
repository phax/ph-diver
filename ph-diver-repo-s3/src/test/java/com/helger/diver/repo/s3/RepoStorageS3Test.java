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
package com.helger.diver.repo.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ESuccess;
import com.helger.base.url.URLHelper;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.api.version.DVRVersionException;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageContentHelper;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.toc.IRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocGroupNameConsumer;
import com.helger.diver.repo.toc.IRepoTopTocService;

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

  @NonNull
  private static S3Client _s3Client ()
  {
    return S3Client.builder ()
                   .region (Region.EU_WEST_1)
                   // This URL is intended for the s3Mock Docker image
                   .endpointOverride (URLHelper.getAsURI ("http://localhost:9090/" + TEST_BUCKET_NAME))
                   .credentialsProvider (AnonymousCredentialsProvider.create ())
                   .build ();
  }

  @NonNull
  private static RepoStorageS3 _createRepoWritable ()
  {
    return new RepoStorageS3 (_s3Client (),
                              "bucket",
                              "unittest.s3",
                              ERepoWritable.WITH_WRITE,
                              ERepoDeletable.WITH_DELETE,
                              new IRepoTopTocService ()
                              {
                                @NonNull
                                public ESuccess registerGroupAndArtifact (final String sGroupID,
                                                                          final String sArtifactID)
                                {
                                  return ESuccess.SUCCESS;
                                }

                                public void iterateAllTopLevelGroupNames (final Consumer <String> aGroupNameConsumer)
                                {}

                                public void iterateAllSubGroups (final String sGroupID,
                                                                 final IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                                                                 final boolean bRecursive)
                                {}

                                public void iterateAllArtifacts (final String sGroupID,
                                                                 final Consumer <String> aArtifactNameConsumer)
                                {}

                                public void initForRepo (final IRepoStorageWithToc aRepo)
                                {}

                                public boolean containsGroupAndArtifact (final String sGroupID,
                                                                         final String sArtifactID)
                                {
                                  return false;
                                }
                              });
  }

  @Test
  public void testWritableWriteAndRead () throws DVRVersionException
  {
    final RepoStorageS3 aRepo = _createRepoWritable ();
    assertTrue (aRepo.canWrite ());

    // Existing only in "local fs" repo but not in S3
    IRepoStorageReadItem aItem = aRepo.read (RepoStorageKeyOfArtefact.of (DVRCoordinate.create ("com.ecosio",
                                                                                                "local",
                                                                                                "1"), ".txt"));
    assertNull (aItem);

    final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (DVRCoordinate.create ("com.ecosio",
                                                                                             "s3-written",
                                                                                             "1"), ".txt");
    boolean bS3Available = true;

    try
    {
      // Ensure not existing
      assertNull (aRepo.read (aKey));

      final String sUploadedPayload = "bla-" + ThreadLocalRandom.current ().nextInt ();
      // Write
      final ESuccess eSuccess = aRepo.write (aKey, RepoStorageContentByteArray.ofUtf8 (sUploadedPayload));
      assertTrue (eSuccess.isSuccess ());

      // Read again
      aItem = aRepo.read (aKey);
      assertNotNull (aItem);
      assertEquals (sUploadedPayload, RepoStorageContentHelper.getAsUtf8String (aItem.getContent ()));
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
                                                  RepoStorageKey.FILE_EXT_SHA256)
                                            .build ());
      }
    }
  }
}
