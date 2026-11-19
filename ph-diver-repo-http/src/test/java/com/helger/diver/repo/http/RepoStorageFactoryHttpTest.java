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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.helger.config.Config;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.config.ConfiguredRepoStorage;
import com.helger.diver.repo.config.RepoStorageConfig;
import com.helger.diver.repo.config.RepoStorageDefinition;
import com.helger.diver.repo.config.RepoStorageDefinition.RepoStorageDefinitionBuilder;
import com.helger.diver.repo.config.RepoStorageFactoryHelper;
import com.helger.diver.repo.config.RepoStorageFactoryRegistry;
import com.helger.diver.repo.http.mock.LocalJettyRunner;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.http.bearerauth.BearerAuthClientCredentials;

/**
 * Test class for class {@link RepoStorageFactoryHttp}.
 *
 * @author Philip Helger
 */
public final class RepoStorageFactoryHttpTest
{
  private static final int PORT = LocalJettyRunner.DEFAULT_PORT + 2;
  private static final String URL = "http://localhost:" + PORT + "/";
  private static final Map <String, String> SECRETS = Map.of ("test.password", "secret", "test.token", "token123");
  private static final IConfig CONFIG = Config.create (new MultiConfigurationValueProvider (new ConfigurationSourceFunction (SECRETS::get)));

  @Rule
  public final TemporaryFolder m_aTempFolder = new TemporaryFolder ();

  @NonNull
  private static RepoStorageDefinitionBuilder _builder ()
  {
    return RepoStorageDefinition.builder ()
                                .id ("http-repo")
                                .type (RepoStorageFactoryHttp.TYPE)
                                .writable (true)
                                .deletable (ERepoDeletable.WITHOUT_DELETE);
  }

  @NonNull
  private static ErrorList _create (@NonNull final RepoStorageDefinition aDefinition) throws Exception
  {
    final ErrorList aErrorList = new ErrorList ();
    final AbstractRepoStorageWithToc <?> aRepo = new RepoStorageFactoryHttp (CONFIG).createRepoStorage (aDefinition,
                                                                                                         aErrorList);
    if (aRepo != null)
    {
      // The created storage owns its HTTP client
      if (aRepo instanceof final AutoCloseable aCloseable)
        aCloseable.close ();
      else
        fail ("The created storage must be AutoCloseable");
    }
    else
      assertTrue (aErrorList.containsAtLeastOneError ());
    return aErrorList;
  }

  @Test
  public void testSupportedPropertyNames ()
  {
    assertEquals (List.of ("url", "username", "password-key", "bearertoken-key", "allow-insecure"),
                  RepoStorageFactoryHttp.getAllSupportedPropertyNames ().getCopyAsList ());
    // A copy is returned
    RepoStorageFactoryHttp.getAllSupportedPropertyNames ().clear ();
    assertEquals (5, RepoStorageFactoryHttp.getAllSupportedPropertyNames ().size ());
  }

  @Test
  public void testInvalidDefinitions () throws Exception
  {
    // Missing URL
    ErrorList aErrorList = _create (_builder ().build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    // Insecure URL
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL).build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());
    assertTrue (aErrorList.toString (), aErrorList.getFirstOrNull ().getErrorText (Locale.ROOT).contains ("allow-insecure"));

    // Other scheme
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, "ftp://localhost/").build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    // Invalid boolean
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL)
                                     .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE, "yes")
                                     .build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    // User name without password
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, "https://localhost/")
                                     .property (RepoStorageFactoryHttp.PROPERTY_USERNAME, "user")
                                     .build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    // Configuration key without value - the error names the key only
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, "https://localhost/")
                                     .property (RepoStorageFactoryHttp.PROPERTY_USERNAME, "user")
                                     .property (RepoStorageFactoryHttp.PROPERTY_PASSWORD_KEY, "not.set")
                                     .build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());
    assertTrue (aErrorList.toString (), aErrorList.getFirstOrNull ().getErrorText (Locale.ROOT).contains ("not.set"));

    // Basic and Bearer
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, "https://localhost/")
                                     .property (RepoStorageFactoryHttp.PROPERTY_USERNAME, "user")
                                     .property (RepoStorageFactoryHttp.PROPERTY_PASSWORD_KEY, "test.password")
                                     .property (RepoStorageFactoryHttp.PROPERTY_BEARER_TOKEN_KEY, "test.token")
                                     .build ());
    assertTrue (aErrorList.toString (), aErrorList.containsAtLeastOneError ());

    // Valid with https and an unknown property - only a warning
    aErrorList = _create (_builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, "https://localhost/")
                                     .property ("foo", "bar")
                                     .build ());
    assertFalse (aErrorList.toString (), aErrorList.containsAtLeastOneError ());
    assertEquals (aErrorList.toString (), 1, aErrorList.size ());
  }

  private void _testWithAuthorization (@NonNull final String sRequiredAuthorization,
                                       @NonNull final RepoStorageDefinition aDefinition,
                                       @NonNull final RepoStorageDefinition aUnauthenticatedDefinition) throws Exception
  {
    final LocalJettyRunner aRunner = new LocalJettyRunner (PORT,
                                                           m_aTempFolder.newFolder (),
                                                           ERepoWritable.WITH_WRITE,
                                                           ERepoDeletable.WITHOUT_DELETE);
    aRunner.setRequiredAuthorization (sRequiredAuthorization);
    aRunner.startJetty ();
    try
    {
      final RepoStorageFactoryRegistry aRegistry = RepoStorageFactoryRegistry.createDefault ();
      aRegistry.registerFactory (new RepoStorageFactoryHttp (CONFIG));
      final RepoStorageKeyOfArtefact aKey = RepoStorageKeyOfArtefact.of (DVRCoordinate.create ("com.helger",
                                                                                             "auth",
                                                                                             "1"), ".txt");

      // Authenticated
      ErrorList aErrorList = new ErrorList ();
      try (final ConfiguredRepoStorage aStorage = new RepoStorageConfig (List.of (aDefinition)).createAllRepoStorages (aRegistry,
                                                                                                                              aErrorList)
                                                                                                         .getFirstOrNull ())
      {
        assertNotNull (aErrorList.toString (), aStorage);
        assertFalse (aStorage.getRepoStorage ().exists (aKey));
        assertTrue (aStorage.getRepoStorage ()
                            .write (aKey, RepoStorageContentByteArray.of ("Hello".getBytes (StandardCharsets.UTF_8)))
                            .isSuccess ());
        // The existence check uses HTTP HEAD, which must be authenticated as well
        assertTrue (aStorage.getRepoStorage ().exists (aKey));
        final IRepoStorageReadItem aItem = aStorage.getRepoStorage ().read (aKey);
        assertNotNull (aItem);
      }

      // Not authenticated
      aErrorList = new ErrorList ();
      try (final ConfiguredRepoStorage aStorage = new RepoStorageConfig (List.of (aUnauthenticatedDefinition)).createAllRepoStorages (aRegistry,
                                                                                                                                             aErrorList)
                                                                                                                        .getFirstOrNull ())
      {
        assertNotNull (aErrorList.toString (), aStorage);
        assertFalse (aStorage.getRepoStorage ().exists (aKey));
        assertNull (aStorage.getRepoStorage ().read (aKey));
        assertTrue (aStorage.getRepoStorage ()
                            .write (aKey, RepoStorageContentByteArray.of ("Evil".getBytes (StandardCharsets.UTF_8)))
                            .isFailure ());
      }
    }
    finally
    {
      aRunner.stopJetty ();
    }
  }

  @Test
  public void testBasicAuth () throws Exception
  {
    final RepoStorageDefinition aDefinition = _builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL)
                                                         .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                    "true")
                                                         .property (RepoStorageFactoryHttp.PROPERTY_USERNAME, "user")
                                                         .property (RepoStorageFactoryHttp.PROPERTY_PASSWORD_KEY,
                                                                    "test.password")
                                                         .build ();
    final RepoStorageDefinition aUnauthenticated = _builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL)
                                                              .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                         "true")
                                                              .build ();
    _testWithAuthorization (new BasicAuthClientCredentials ("user", "secret").getRequestValue (),
                            aDefinition,
                            aUnauthenticated);
  }

  @Test
  public void testBearerAuth () throws Exception
  {
    final RepoStorageDefinition aDefinition = _builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL)
                                                         .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                    "true")
                                                         .property (RepoStorageFactoryHttp.PROPERTY_BEARER_TOKEN_KEY,
                                                                    "test.token")
                                                         .build ();
    final RepoStorageDefinition aUnauthenticated = _builder ().property (RepoStorageFactoryHttp.PROPERTY_URL, URL)
                                                              .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                         "true")
                                                              .property (RepoStorageFactoryHttp.PROPERTY_USERNAME,
                                                                         "user")
                                                              .property (RepoStorageFactoryHttp.PROPERTY_PASSWORD_KEY,
                                                                         "test.password")
                                                              .build ();
    _testWithAuthorization (new BearerAuthClientCredentials ("token123").getRequestValue (),
                            aDefinition,
                            aUnauthenticated);
  }
}
