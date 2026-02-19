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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.IRepoStorageType;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.mime.CMimeType;

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
  public static final IRepoStorageType AWS_S3 = new RepoStorageType ("aws-s3", true, true);

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageS3.class);

  private final S3Client m_aS3Client;
  private final String m_sBucketName;
  private final String m_sDefaultKeyPrefix;

  /**
   * Constructor
   *
   * @param aS3Client
   *        The AWS S3 client to use. May not be <code>null</code>.
   * @param sBucketName
   *        The S3 bucket name to use. May neither be <code>null</code> nor empty.
   * @param sDefaultKeyPrefix
   *        An optional "default key prefix" that is automatically added on the beginning of each
   *        path. May be <code>null</code> or an empty string. If the value is a non-empty String,
   *        it must not start with a slash, but must end with a slash.
   * @param sID
   *        The internal repository ID. May neither be <code>null</code> nor empty.
   * @param eWriteEnabled
   *        Is the repository writable? May not be <code>null</code>.
   * @param eDeleteEnabled
   *        Is the repository deletable? May not be <code>null</code>.
   * @param aTopTocService
   *        The top-level Table of Content service to be used. May not be <code>null</code>.
   */
  public RepoStorageS3 (@NonNull final S3Client aS3Client,
                        @NonNull @Nonempty final String sBucketName,
                        @Nullable final String sDefaultKeyPrefix,
                        @NonNull @Nonempty final String sID,
                        @NonNull final ERepoWritable eWriteEnabled,
                        @NonNull final ERepoDeletable eDeleteEnabled,
                        @NonNull final IRepoTopTocService aTopTocService)
  {
    super (AWS_S3, sID, eWriteEnabled, eDeleteEnabled, aTopTocService);
    ValueEnforcer.notNull (aS3Client, "S3Client");
    ValueEnforcer.notEmpty (sBucketName, "BucketName");
    ValueEnforcer.isEqual (sBucketName, sBucketName.trim (), "BucketName must be trimmed");
    if (StringHelper.isNotEmpty (sDefaultKeyPrefix))
    {
      ValueEnforcer.isFalse ( () -> sDefaultKeyPrefix.startsWith ("/"),
                              () -> "The default key prefix ('" +
                                    sDefaultKeyPrefix +
                                    "') must not start with a slash ('/')");
      ValueEnforcer.isTrue ( () -> sDefaultKeyPrefix.endsWith ("/"),
                             () -> "The default key prefix ('" + sDefaultKeyPrefix + "') must end with a slash ('/')");
      ValueEnforcer.isFalse ( () -> sDefaultKeyPrefix.equals ("/"),
                              () -> "The default key prefix must not be a single slash ('/')");
    }
    m_aS3Client = aS3Client;
    m_sBucketName = sBucketName;
    m_sDefaultKeyPrefix = sDefaultKeyPrefix == null ? "" : sDefaultKeyPrefix;
  }

  @NonNull
  private String _getRealKey (@NonNull final RepoStorageKey aKey)
  {
    final String sRealKey = m_sDefaultKeyPrefix + aKey.getPath ();
    if (sRealKey.startsWith ("/"))
      throw new IllegalArgumentException ("RepoStorageKey ('" + sRealKey + "') may not start with a leading slash");
    return sRealKey;
  }

  public boolean exists (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final String sRealKey = _getRealKey (aKey);

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
  protected ResponseInputStream <GetObjectResponse> getInputStream (@NonNull final RepoStorageKey aKey)
  {
    final String sRealKey = _getRealKey (aKey);

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
  @NonNull
  protected ESuccess writeObject (@NonNull final RepoStorageKey aKey, @NonNull final IRepoStorageContent aContent)
  {
    final String sRealKey = _getRealKey (aKey);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Writing to S3 '" + m_sBucketName + "' / '" + sRealKey + "'");

    // Use the source payload
    final PutObjectResponse aPutResponse = m_aS3Client.putObject (PutObjectRequest.builder ()
                                                                                  .bucket (m_sBucketName)
                                                                                  .key (sRealKey)
                                                                                  .build (),
                                                                  RequestBody.fromContentProvider (aContent::getBufferedInputStream,
                                                                                                   aContent.getLength (),
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
  @NonNull
  protected ESuccess deleteObject (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    final String sRealKey = _getRealKey (aKey);

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
