/*
 * Copyright (C) 2023-2026 Philip Helger
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

import java.net.URI;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillCloseWhenClosed;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.config.IRepoStorageFactory;
import com.helger.diver.repo.config.RepoStorageDefinition;
import com.helger.diver.repo.config.RepoStorageFactoryHelper;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * {@link IRepoStorageFactory} for {@link RepoStorageS3}. Supported properties:
 * <ul>
 * <li>{@link #PROPERTY_BUCKET} (required) - the S3 bucket name.</li>
 * <li>{@link #PROPERTY_PREFIX} (optional) - the key prefix within the bucket. Must not start with a
 * slash, but must end with a slash.</li>
 * <li>{@link #PROPERTY_REGION} (optional) - the AWS region. If absent, the default region provider
 * chain of the AWS SDK is used.</li>
 * <li>{@link #PROPERTY_ENDPOINT} (optional) - an endpoint override for S3 compatible services. Must
 * use <code>https://</code> unless {@link RepoStorageFactoryHelper#PROPERTY_ALLOW_INSECURE} is
 * <code>true</code>.</li>
 * <li>{@link #PROPERTY_PATH_STYLE} (optional, default <code>false</code>) - use path style
 * access.</li>
 * <li>{@link #PROPERTY_PROFILE} (optional) - the AWS profile to read the credentials from. If
 * absent, the default credentials provider chain of the AWS SDK is used (environment variables,
 * system properties, profile files, container and instance credentials).</li>
 * <li>{@link RepoStorageFactoryHelper#PROPERTY_ALLOW_INSECURE} (optional, default
 * <code>false</code>) - allow an <code>http://</code> endpoint.</li>
 * </ul>
 * Credentials are never part of the configuration. The created repository storage owns its S3
 * client and implements {@link AutoCloseable}.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public class RepoStorageFactoryS3 implements IRepoStorageFactory
{
  /** The repository type ID */
  public static final String TYPE = RepoStorageS3.AWS_S3.getID ();
  /** The name of the property containing the bucket name */
  public static final String PROPERTY_BUCKET = "bucket";
  /** The name of the property containing the key prefix */
  public static final String PROPERTY_PREFIX = "prefix";
  /** The name of the property containing the AWS region */
  public static final String PROPERTY_REGION = "region";
  /** The name of the property containing the endpoint override */
  public static final String PROPERTY_ENDPOINT = "endpoint";
  /** The name of the property enabling path style access */
  public static final String PROPERTY_PATH_STYLE = "path-style";
  /** The name of the property containing the AWS profile name */
  public static final String PROPERTY_PROFILE = "profile";

  private static final ICommonsOrderedSet <String> SUPPORTED_PROPERTIES = new CommonsLinkedHashSet <> (PROPERTY_BUCKET,
                                                                                                       PROPERTY_PREFIX,
                                                                                                       PROPERTY_REGION,
                                                                                                       PROPERTY_ENDPOINT,
                                                                                                       PROPERTY_PATH_STYLE,
                                                                                                       PROPERTY_PROFILE,
                                                                                                       RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE);

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageFactoryS3.class);

  /**
   * A {@link RepoStorageS3} that owns its S3 client and credentials provider and closes them.
   *
   * @author Philip Helger
   */
  private static final class OwningRepoStorageS3 extends RepoStorageS3 implements AutoCloseable
  {
    private final AwsCredentialsProvider m_aCredentialsProvider;

    OwningRepoStorageS3 (@NonNull @WillCloseWhenClosed final S3Client aS3Client,
                         @NonNull @WillCloseWhenClosed final AwsCredentialsProvider aCredentialsProvider,
                         @NonNull @Nonempty final String sBucketName,
                         @Nullable final String sDefaultKeyPrefix,
                         @NonNull @Nonempty final String sID,
                         @NonNull final ERepoWritable eWriteEnabled,
                         @NonNull final ERepoDeletable eDeleteEnabled)
    {
      super (aS3Client, sBucketName, sDefaultKeyPrefix, sID, eWriteEnabled, eDeleteEnabled);
      m_aCredentialsProvider = aCredentialsProvider;
    }

    public void close () throws Exception
    {
      // Explicitly provided credentials providers are not closed by the client
      try
      {
        getS3Client ().close ();
      }
      finally
      {
        if (m_aCredentialsProvider instanceof final AutoCloseable aCloseable)
          aCloseable.close ();
      }
    }
  }

  /** Constructor. */
  public RepoStorageFactoryS3 ()
  {}

  @NonNull
  @Nonempty
  public String getType ()
  {
    return TYPE;
  }

  /**
   * @return The names of all repository type specific properties supported by this factory, in a
   *         stable order. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsOrderedSet <String> getAllSupportedPropertyNames ()
  {
    return SUPPORTED_PROPERTIES.getClone ();
  }

  /**
   * Check the key prefix with the same rules as the {@link RepoStorageS3} constructor, so that a
   * configuration error becomes an error message instead of an exception.
   */
  private static void _checkPrefix (@NonNull final RepoStorageDefinition aDefinition,
                                    @Nullable final String sPrefix,
                                    @NonNull final ErrorList aErrorList)
  {
    if (sPrefix != null && (sPrefix.startsWith ("/") || !sPrefix.endsWith ("/")))
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (aDefinition.getID ())
                                 .errorText ("The property '" +
                                             PROPERTY_PREFIX +
                                             "' of repository '" +
                                             aDefinition.getID () +
                                             "' must not start with a slash but must end with a slash, and must not be a single slash. Value: '" +
                                             sPrefix +
                                             "'")
                                 .build ());
  }

  @Nullable
  public AbstractRepoStorageWithToc <?> createRepoStorage (@NonNull final RepoStorageDefinition aDefinition,
                                                           @NonNull final ErrorList aErrorList)
  {
    final String sRepoID = aDefinition.getID ();
    RepoStorageFactoryHelper.warnOnUnsupportedProperties (aDefinition, SUPPORTED_PROPERTIES, aErrorList);

    final int nErrorsBefore = aErrorList.getErrorCount ();
    final String sBucket = RepoStorageFactoryHelper.getRequiredProperty (aDefinition, PROPERTY_BUCKET, aErrorList);
    final String sPrefix = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition, PROPERTY_PREFIX);
    _checkPrefix (aDefinition, sPrefix, aErrorList);
    final String sRegion = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition, PROPERTY_REGION);
    final String sEndpoint = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition, PROPERTY_ENDPOINT);
    final String sProfile = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition, PROPERTY_PROFILE);
    final Boolean aPathStyle = RepoStorageFactoryHelper.getBooleanProperty (aDefinition,
                                                                            PROPERTY_PATH_STYLE,
                                                                            false,
                                                                            aErrorList);
    final Boolean aAllowInsecure = RepoStorageFactoryHelper.getBooleanProperty (aDefinition,
                                                                                RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                                false,
                                                                                aErrorList);
    URI aEndpointURI = null;
    if (sEndpoint != null && aAllowInsecure != null)
    {
      if (RepoStorageFactoryHelper.checkUrlScheme (aDefinition,
                                                   PROPERTY_ENDPOINT,
                                                   sEndpoint,
                                                   aAllowInsecure.booleanValue (),
                                                   aErrorList))
        try
        {
          aEndpointURI = new URI (sEndpoint);
        }
        catch (final Exception ex)
        {
          aErrorList.add (SingleError.builderError ()
                                     .errorFieldName (sRepoID)
                                     .errorText ("The property '" +
                                                 PROPERTY_ENDPOINT +
                                                 "' of repository '" +
                                                 sRepoID +
                                                 "' is not a valid URI: " +
                                                 ex.getMessage ())
                                     .build ());
        }
    }
    if (aErrorList.getErrorCount () > nErrorsBefore)
      return null;

    final AwsCredentialsProvider aCredentialsProvider = sProfile != null ? ProfileCredentialsProvider.create (sProfile)
                                                                         : DefaultCredentialsProvider.builder ()
                                                                                                     .build ();
    final S3Client aS3Client;
    try
    {
      final S3ClientBuilder aBuilder = S3Client.builder ()
                                               .credentialsProvider (aCredentialsProvider)
                                               .forcePathStyle (aPathStyle);
      if (sRegion != null)
        aBuilder.region (Region.of (sRegion));
      if (aEndpointURI != null)
        aBuilder.endpointOverride (aEndpointURI);
      aS3Client = aBuilder.build ();
    }
    catch (final RuntimeException ex)
    {
      // E.g. no region could be determined
      if (aCredentialsProvider instanceof final AutoCloseable aCloseable)
        try
        {
          aCloseable.close ();
        }
        catch (final Exception ex2)
        {
          LOGGER.warn ("Failed to close credentials provider", ex2);
        }
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText ("Failed to create the S3 client of repository '" +
                                             sRepoID +
                                             "': " +
                                             ex.getMessage ())
                                 .linkedException (ex)
                                 .build ());
      return null;
    }

    return new OwningRepoStorageS3 (aS3Client,
                                    aCredentialsProvider,
                                    sBucket,
                                    sPrefix,
                                    sRepoID,
                                    aDefinition.getWritable (),
                                    aDefinition.getDeletable ());
  }
}
