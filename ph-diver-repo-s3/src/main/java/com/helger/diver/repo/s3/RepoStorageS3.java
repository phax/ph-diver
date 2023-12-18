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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ESuccess;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Base implementation of {@link IRepoStorage} for Amazon AWS S3.
 *
 * @author Philip Helger
 */
public class RepoStorageS3 extends AbstractRepoStorageWithToc <RepoStorageS3>
{
  public static final RepoStorageType AWS_S3 = new RepoStorageType ("aws-s3", true, true);

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageS3.class);

  private final S3Client m_aS3Client;
  private final String m_sBucketName;

  public RepoStorageS3 (@Nonnull final S3Client aS3Client,
                        @Nonnull @Nonempty final String sBucketName,
                        @Nonnull @Nonempty final String sID,
                        @Nonnull final ERepoWritable eWriteEnabled,
                        @Nonnull final ERepoDeletable eDeleteEnabled)
  {
    super (AWS_S3, sID, eWriteEnabled, eDeleteEnabled);
    ValueEnforcer.notNull (aS3Client, "S3Client");
    ValueEnforcer.notEmpty (sBucketName, "BucketName");
    ValueEnforcer.isEqual (sBucketName, sBucketName.trim (), "BucketName must be trimmed");
    m_aS3Client = aS3Client;
    m_sBucketName = sBucketName;
  }

  public boolean exists (@Nonnull final RepoStorageKeyOfArtefact aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final String sRealKey = aKey.getPath ();
    if (sRealKey.startsWith ("/"))
      throw new IllegalArgumentException ("RepoStorageKey may not start with a leading slash");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Reading from S3 '" + m_sBucketName + "' / '" + sRealKey + "'");

    try
    {
      // If this API does not throw an exception
      m_aS3Client.headObject (HeadObjectRequest.builder ().bucket (m_sBucketName).key (sRealKey).build ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Found on HTTP '" + m_sBucketName + "' / '" + sRealKey + "'");
      return true;
    }
    catch (final Exception ex)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to read from S3 '" + m_sBucketName + "' / '" + sRealKey + "': " + ex.getMessage ());
      return false;
    }
  }

  @Override
  @Nullable
  protected ResponseInputStream <GetObjectResponse> getInputStream (@Nonnull final RepoStorageKey aKey)
  {
    final String sRealKey = aKey.getPath ();
    if (sRealKey.startsWith ("/"))
      throw new IllegalArgumentException ("RepoStorageKey may not start with a leading slash");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Reading from S3 '" + m_sBucketName + "' / '" + sRealKey + "'");

    try
    {
      final ResponseInputStream <GetObjectResponse> ret = m_aS3Client.getObject (GetObjectRequest.builder ()
                                                                                                 .bucket (m_sBucketName)
                                                                                                 .key (sRealKey)
                                                                                                 .build ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Found on HTTP '" + m_sBucketName + "' / '" + sRealKey + "'");
      return ret;
    }
    catch (final Exception ex)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to read from S3 '" + m_sBucketName + "' / '" + sRealKey + "': " + ex.getMessage ());
      return null;
    }
  }

  @Override
  @Nonnull
  protected ESuccess writeObject (@Nonnull final RepoStorageKeyOfArtefact aKey, @Nonnull final byte [] aPayload)
  {
    final String sRealKey = aKey.getPath ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Writing to S3 '" + m_sBucketName + "' / '" + sRealKey + "'");

    // Use the source payload
    final PutObjectResponse aPutResponse = m_aS3Client.putObject (PutObjectRequest.builder ()
                                                                                  .bucket (m_sBucketName)
                                                                                  .key (sRealKey)
                                                                                  .build (),
                                                                  RequestBody.fromContentProvider ( () -> new NonBlockingByteArrayInputStream (aPayload),
                                                                                                    aPayload.length,
                                                                                                    CMimeType.APPLICATION_OCTET_STREAM.getAsString ()));
    if (!aPutResponse.sdkHttpResponse ().isSuccessful ())
    {
      LOGGER.error ("Failed to put S3 object '" + m_sBucketName + "' / '" + sRealKey + "'");
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully wrote to S3 '" + m_sBucketName + "' / '" + sRealKey + "'");
    return ESuccess.SUCCESS;
  }

  @Override
  @Nonnull
  protected ESuccess deleteObject (@Nonnull final RepoStorageKeyOfArtefact aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final String sRealKey = aKey.getPath ();
    LOGGER.info ("Deleting from S3 '" + m_sBucketName + "' / '" + sRealKey + "'");

    final DeleteObjectResponse aDeleteResponse = m_aS3Client.deleteObject (DeleteObjectRequest.builder ()
                                                                                              .bucket (m_sBucketName)
                                                                                              .key (sRealKey)
                                                                                              .build ());
    if (!aDeleteResponse.sdkHttpResponse ().isSuccessful ())
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Failed to delete S3 object '" + m_sBucketName + "' / '" + sRealKey + "'");
      return ESuccess.FAILURE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully deleted S3 object '" + m_sBucketName + "' / '" + sRealKey + "'");
    return ESuccess.SUCCESS;
  }
}
