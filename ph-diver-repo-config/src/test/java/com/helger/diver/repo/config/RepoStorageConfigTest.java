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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.impl.RepoStorageLocalFileSystem;

/**
 * Test class for class {@link RepoStorageConfig}.
 *
 * @author Philip Helger
 */
public final class RepoStorageConfigTest
{
  @Test
  public void testCreateValid ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (new File (RepoStorageConfigReaderTest.BASE_DIR,
                                                                              "repoconfig-valid.xml"),
                                                                    aErrorList);
    assertNotNull (aConfig);

    final ICommonsList <ConfiguredRepoStorage> aStorages = aConfig.createAllRepoStorages (RepoStorageFactoryRegistry.createDefault (),
                                                                                          aErrorList);
    assertTrue (aErrorList.toString (), aErrorList.isEmpty ());
    assertEquals (2, aStorages.size ());

    final ConfiguredRepoStorage aStorage0 = aStorages.get (0);
    assertEquals ("test-repo", aStorage0.getID ());
    assertTrue (aStorage0.getRepoStorage () instanceof RepoStorageLocalFileSystem);
    assertEquals ("test-repo", aStorage0.getRepoStorage ().getID ());
    assertFalse (aStorage0.getRepoStorage ().canWrite ());
    assertFalse (aStorage0.getRepoStorage ().canDelete ());
    assertTrue (aStorage0.hasTopTocService ());
    assertTrue (aStorage0.getTopTocService ().containsGroupAndArtifact ("com.helger", "demo"));
    assertFalse (aStorage0.getTopTocService ().containsGroupAndArtifact ("com.helger", "other"));

    final ConfiguredRepoStorage aStorage1 = aStorages.get (1);
    assertEquals ("empty-repo", aStorage1.getID ());
    assertFalse (aStorage1.hasTopTocService ());
    assertNull (aStorage1.getTopTocService ());
    assertFalse (((RepoStorageLocalFileSystem) aStorage1.getRepoStorage ()).isVerifyHashOnRead ());
  }

  @Test
  public void testCreateWithErrors ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (new File (RepoStorageConfigReaderTest.BASE_DIR,
                                                                              "repoconfig-semantic-errors.xml"),
                                                                    aErrorList);
    assertNotNull (aConfig);
    assertTrue (aErrorList.isEmpty ());
    assertEquals (4, aConfig.getDefinitionCount ());

    final ICommonsList <ConfiguredRepoStorage> aStorages = aConfig.createAllRepoStorages (RepoStorageFactoryRegistry.createDefault (),
                                                                                          aErrorList);
    // Only the one with the unknown property is created
    assertEquals (1, aStorages.size ());
    assertEquals ("unknown-property", aStorages.get (0).getID ());

    // 3 errors and 1 warning
    assertEquals (aErrorList.toString (), 3, aErrorList.getAllErrors ().size ());
    assertEquals (aErrorList.toString (), 4, aErrorList.getAllFailures ().size ());
    assertTrue (aErrorList.containsAny (x -> x.isError () && "unknown-type".equals (x.getErrorFieldName ())));
    assertTrue (aErrorList.containsAny (x -> x.isError () && "missing-basedir".equals (x.getErrorFieldName ())));
    assertTrue (aErrorList.containsAny (x -> x.isError () && "non-existing-basedir".equals (x.getErrorFieldName ())));
    assertTrue (aErrorList.containsAny (x -> !x.isError () && "unknown-property".equals (x.getErrorFieldName ())));
  }

  @Test
  public void testFactoryRegistry ()
  {
    final RepoStorageFactoryRegistry aRegistry = RepoStorageFactoryRegistry.createDefault ();
    assertEquals (1, aRegistry.getAllTypes ().size ());
    assertTrue (aRegistry.getAllTypes ().contains (RepoStorageFactoryLocalFileSystem.TYPE));

    final IRepoStorageFactory aFactory = aRegistry.getFactoryOfType (RepoStorageFactoryLocalFileSystem.TYPE);
    assertNotNull (aFactory);
    assertSame (EChange.UNCHANGED, aRegistry.registerFactory (new RepoStorageFactoryLocalFileSystem ()));
    assertSame (aFactory, aRegistry.getFactoryOfType (RepoStorageFactoryLocalFileSystem.TYPE));

    assertSame (EChange.CHANGED, aRegistry.unregisterFactory (RepoStorageFactoryLocalFileSystem.TYPE));
    assertSame (EChange.UNCHANGED, aRegistry.unregisterFactory (RepoStorageFactoryLocalFileSystem.TYPE));
    assertNull (aRegistry.getFactoryOfType (RepoStorageFactoryLocalFileSystem.TYPE));
    assertTrue (aRegistry.getAllTypes ().isEmpty ());
  }
}
