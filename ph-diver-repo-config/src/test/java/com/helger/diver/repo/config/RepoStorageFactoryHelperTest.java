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
package com.helger.diver.repo.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.config.Config;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.impl.RepoStorageInMemory;
import com.helger.diver.repo.impl.RepoStorageLocalFileSystem;

/**
 * Test class for class {@link RepoStorageFactoryHelper} and the closing of
 * {@link ConfiguredRepoStorage}.
 *
 * @author Philip Helger
 */
public final class RepoStorageFactoryHelperTest
{
  @NonNull
  private static RepoStorageDefinition _create (@NonNull final Map <String, String> aProperties)
  {
    return RepoStorageDefinition.builder ().id ("repo").type ("test").properties (aProperties).build ();
  }

  @Test
  public void testProperties ()
  {
    final RepoStorageDefinition aDef = _create (Map.of ("a", " value ", "empty", "  ", "t", "true", "x", "maybe"));
    final ErrorList aErrorList = new ErrorList ();

    RepoStorageFactoryHelper.warnOnUnsupportedProperties (aDef, Set.of ("a", "empty", "t"), aErrorList);
    assertEquals (1, aErrorList.size ());
    assertFalse (aErrorList.containsAtLeastOneError ());
    aErrorList.clear ();

    assertEquals ("value", RepoStorageFactoryHelper.getTrimmedProperty (aDef, "a"));
    assertNull (RepoStorageFactoryHelper.getTrimmedProperty (aDef, "empty"));
    assertNull (RepoStorageFactoryHelper.getTrimmedProperty (aDef, "missing"));

    assertNull (RepoStorageFactoryHelper.getRequiredProperty (aDef, "empty", aErrorList));
    assertEquals (1, aErrorList.getErrorCount ());
    aErrorList.clear ();

    assertEquals (Boolean.TRUE, RepoStorageFactoryHelper.getBooleanProperty (aDef, "t", false, aErrorList));
    assertEquals (Boolean.FALSE, RepoStorageFactoryHelper.getBooleanProperty (aDef, "missing", false, aErrorList));
    assertTrue (aErrorList.isEmpty ());
    assertNull (RepoStorageFactoryHelper.getBooleanProperty (aDef, "x", false, aErrorList));
    assertEquals (1, aErrorList.getErrorCount ());
  }

  @Test
  public void testURLScheme ()
  {
    final RepoStorageDefinition aDef = _create (Map.of ());
    final ErrorList aErrorList = new ErrorList ();
    assertTrue (RepoStorageFactoryHelper.checkURLScheme (aDef, "url", "https://x/", false, aErrorList));
    assertTrue (RepoStorageFactoryHelper.checkURLScheme (aDef, "url", "HTTPS://x/", false, aErrorList));
    assertTrue (RepoStorageFactoryHelper.checkURLScheme (aDef, "url", "http://x/", true, aErrorList));
    assertTrue (aErrorList.isEmpty ());
    assertFalse (RepoStorageFactoryHelper.checkURLScheme (aDef, "url", "http://x/", false, aErrorList));
    assertFalse (RepoStorageFactoryHelper.checkURLScheme (aDef, "url", "file:///x/", true, aErrorList));
    assertEquals (2, aErrorList.getErrorCount ());
  }

  @Test
  public void testSecretFromConfig ()
  {
    final RepoStorageDefinition aDef = _create (Map.of ("set", "key.set", "unset", "key.unset"));
    final Map <String, String> aSecrets = Map.of ("key.set", "secret");
    final IConfig aConfig = Config.create (new MultiConfigurationValueProvider (new ConfigurationSourceFunction (aSecrets::get)));
    final ErrorList aErrorList = new ErrorList ();
    assertEquals ("secret", RepoStorageFactoryHelper.getSecretFromConfig (aDef, "set", aConfig, aErrorList));
    assertNull (RepoStorageFactoryHelper.getSecretFromConfig (aDef, "missing", aConfig, aErrorList));
    assertTrue (aErrorList.isEmpty ());
    assertNull (RepoStorageFactoryHelper.getSecretFromConfig (aDef, "unset", aConfig, aErrorList));
    assertEquals (1, aErrorList.getErrorCount ());
    assertFalse (aErrorList.toString (), aErrorList.toString ().contains ("secret"));
  }

  /**
   * A local file system storage that records whether it was closed.
   */
  private static final class CloseableRepoStorage extends RepoStorageLocalFileSystem implements AutoCloseable
  {
    private boolean m_bClosed;

    CloseableRepoStorage ()
    {
      super (new File ("target/closeable-repo"), "closeable", ERepoWritable.WITHOUT_WRITE, ERepoDeletable.WITHOUT_DELETE);
    }

    public void close ()
    {
      m_bClosed = true;
    }
  }

  @Test
  public void testClose ()
  {
    final RepoStorageDefinition aDef = _create (Map.of ());
    final CloseableRepoStorage aRepo = new CloseableRepoStorage ();
    try (final ConfiguredRepoStorage aStorage = new ConfiguredRepoStorage (aDef, aRepo, null))
    {
      assertFalse (aRepo.m_bClosed);
    }
    assertTrue (aRepo.m_bClosed);

    // Not closeable - nothing happens
    try (final ConfiguredRepoStorage aStorage = new ConfiguredRepoStorage (aDef,
                                                                          RepoStorageInMemory.createDefault ("mem",
                                                                                                             ERepoWritable.WITHOUT_WRITE,
                                                                                                             ERepoDeletable.WITHOUT_DELETE),
                                                                          null))
    {
      assertNull (aStorage.getTopTocService ());
    }
  }
}
