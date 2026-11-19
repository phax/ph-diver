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
package com.helger.diver.repo.http;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillCloseWhenClosed;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.config.IRepoStorageFactory;
import com.helger.diver.repo.config.RepoStorageDefinition;
import com.helger.diver.repo.config.RepoStorageFactoryHelper;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.http.CHttpHeader;
import com.helger.http.IHttpClientCredentials;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.http.bearerauth.BearerAuthClientCredentials;
import com.helger.httpclient.HttpClientManager;

/**
 * {@link IRepoStorageFactory} for {@link RepoStorageHttp}. Supported properties:
 * <ul>
 * <li>{@link #PROPERTY_URL} (required) - the URL prefix of the repository. Must use
 * <code>https://</code> unless {@link RepoStorageFactoryHelper#PROPERTY_ALLOW_INSECURE} is
 * <code>true</code>.</li>
 * <li>{@link #PROPERTY_USERNAME} and {@link #PROPERTY_PASSWORD_KEY} (optional, both or none) - HTTP
 * Basic authentication. The password is read from the ph-config configuration key.</li>
 * <li>{@link #PROPERTY_BEARER_TOKEN_KEY} (optional) - HTTP Bearer authentication. The token is read
 * from the ph-config configuration key. Cannot be combined with Basic authentication.</li>
 * <li>{@link RepoStorageFactoryHelper#PROPERTY_ALLOW_INSECURE} (optional, default
 * <code>false</code>) - allow <code>http://</code> URLs.</li>
 * </ul>
 * Secrets are never part of the repository configuration - only ph-config keys that are resolved
 * via
 * {@link RepoStorageFactoryHelper#getSecretFromConfig(RepoStorageDefinition, String, IConfig, ErrorList)}.
 * The created repository storage owns its HTTP client and implements {@link AutoCloseable}.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
public class RepoStorageFactoryHttp implements IRepoStorageFactory
{
  /** The repository type ID */
  public static final String TYPE = RepoStorageType.HTTP.getID ();
  /** The name of the property containing the URL prefix of the repository */
  public static final String PROPERTY_URL = "url";
  /** The name of the property containing the user name for HTTP Basic authentication */
  public static final String PROPERTY_USERNAME = "username";
  /**
   * The name of the property containing the ph-config key of the password for HTTP Basic
   * authentication
   */
  public static final String PROPERTY_PASSWORD_KEY = "password-key";
  /**
   * The name of the property containing the ph-config key of the token for HTTP Bearer
   * authentication
   */
  public static final String PROPERTY_BEARER_TOKEN_KEY = "bearertoken-key";

  private static final ICommonsOrderedSet <String> SUPPORTED_PROPERTIES = new CommonsLinkedHashSet <> (PROPERTY_URL,
                                                                                                       PROPERTY_USERNAME,
                                                                                                       PROPERTY_PASSWORD_KEY,
                                                                                                       PROPERTY_BEARER_TOKEN_KEY,
                                                                                                       RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE);

  /**
   * A {@link RepoStorageHttp} that owns its HTTP client and closes it.
   *
   * @author Philip Helger
   */
  private static final class OwningRepoStorageHttp extends RepoStorageHttp implements AutoCloseable
  {
    OwningRepoStorageHttp (@NonNull @WillCloseWhenClosed final HttpClientManager aHttpClient,
                           @NonNull @Nonempty final String sURLPrefix,
                           @NonNull @Nonempty final String sID,
                           @NonNull final ERepoWritable eWriteEnabled,
                           @NonNull final ERepoDeletable eDeleteEnabled)
    {
      super (aHttpClient, sURLPrefix, sID, eWriteEnabled, eDeleteEnabled);
    }

    public void close ()
    {
      m_aHttpClient.close ();
    }
  }

  private final IConfig m_aConfig;

  /**
   * Constructor resolving secrets from {@link ConfigFactory#getDefaultConfig()}.
   */
  public RepoStorageFactoryHttp ()
  {
    this (ConfigFactory.getDefaultConfig ());
  }

  /**
   * Constructor with a custom configuration to resolve secrets from.
   *
   * @param aConfig
   *        The configuration to resolve the secret keys in. May not be <code>null</code>.
   */
  public RepoStorageFactoryHttp (@NonNull final IConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    m_aConfig = aConfig;
  }

  /**
   * @return The configuration secrets are resolved from. Never <code>null</code>.
   */
  @NonNull
  public final IConfig getConfig ()
  {
    return m_aConfig;
  }

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
   * Determine the credentials from the definition.
   *
   * @param aDefinition
   *        The definition to use. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to add errors to. May not be <code>null</code>.
   * @return <code>null</code> if no authentication is configured or if an error was added.
   */
  @Nullable
  private IHttpClientCredentials _getCredentials (@NonNull final RepoStorageDefinition aDefinition,
                                                  @NonNull final ErrorList aErrorList)
  {
    final String sRepoID = aDefinition.getID ();
    final String sUserName = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition, PROPERTY_USERNAME);
    final boolean bHasPasswordKey = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition,
                                                                                PROPERTY_PASSWORD_KEY) != null;
    final boolean bHasBearerTokenKey = RepoStorageFactoryHelper.getPropertyOrNull (aDefinition,
                                                                                   PROPERTY_BEARER_TOKEN_KEY) != null;

    if ((sUserName != null || bHasPasswordKey) && bHasBearerTokenKey)
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText ("Repository '" +
                                             sRepoID +
                                             "' cannot combine Basic authentication ('" +
                                             PROPERTY_USERNAME +
                                             "', '" +
                                             PROPERTY_PASSWORD_KEY +
                                             "') with Bearer authentication ('" +
                                             PROPERTY_BEARER_TOKEN_KEY +
                                             "')")
                                 .build ());
      return null;
    }

    if (bHasBearerTokenKey)
    {
      final String sToken = RepoStorageFactoryHelper.getSecretFromConfig (aDefinition,
                                                                          PROPERTY_BEARER_TOKEN_KEY,
                                                                          m_aConfig,
                                                                          aErrorList);
      return sToken == null ? null : new BearerAuthClientCredentials (sToken);
    }

    if (sUserName != null || bHasPasswordKey)
    {
      if (sUserName == null || !bHasPasswordKey)
      {
        aErrorList.add (SingleError.builderError ()
                                   .errorFieldName (sRepoID)
                                   .errorText ("Repository '" +
                                               sRepoID +
                                               "' requires both properties '" +
                                               PROPERTY_USERNAME +
                                               "' and '" +
                                               PROPERTY_PASSWORD_KEY +
                                               "' for Basic authentication")
                                   .build ());
        return null;
      }
      final String sPassword = RepoStorageFactoryHelper.getSecretFromConfig (aDefinition,
                                                                             PROPERTY_PASSWORD_KEY,
                                                                             m_aConfig,
                                                                             aErrorList);
      return sPassword == null ? null : new BasicAuthClientCredentials (sUserName, sPassword);
    }

    // No authentication
    return null;
  }

  @Nullable
  public AbstractRepoStorageWithToc <?> createRepoStorage (@NonNull final RepoStorageDefinition aDefinition,
                                                           @NonNull final ErrorList aErrorList)
  {
    RepoStorageFactoryHelper.warnOnUnsupportedProperties (aDefinition, SUPPORTED_PROPERTIES, aErrorList);

    final int nErrorsBefore = aErrorList.getErrorCount ();
    final String sURL = RepoStorageFactoryHelper.getRequiredProperty (aDefinition, PROPERTY_URL, aErrorList);
    final Boolean aAllowInsecure = RepoStorageFactoryHelper.getBooleanProperty (aDefinition,
                                                                                RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                                false,
                                                                                aErrorList);
    if (sURL != null && aAllowInsecure != null)
      RepoStorageFactoryHelper.checkUrlScheme (aDefinition, PROPERTY_URL, sURL, aAllowInsecure.booleanValue (), aErrorList);
    final IHttpClientCredentials aCredentials = _getCredentials (aDefinition, aErrorList);
    if (aErrorList.getErrorCount () > nErrorsBefore)
      return null;

    final OwningRepoStorageHttp ret = new OwningRepoStorageHttp (new HttpClientManager (),
                                                                 sURL,
                                                                 aDefinition.getID (),
                                                                 aDefinition.getWritable (),
                                                                 aDefinition.getDeletable ());
    if (aCredentials != null)
    {
      // Authenticate all requests, including the existence check
      final String sAuthorization = aCredentials.getRequestValue ();
      ret.setHeadCustomizer (x -> x.setHeader (CHttpHeader.AUTHORIZATION, sAuthorization));
      ret.setReadCusomizer (x -> x.setHeader (CHttpHeader.AUTHORIZATION, sAuthorization));
      ret.setWriteCusomizer (x -> x.setHeader (CHttpHeader.AUTHORIZATION, sAuthorization));
      ret.setDeleteCusomizer (x -> x.setHeader (CHttpHeader.AUTHORIZATION, sAuthorization));
    }
    return ret;
  }
}
