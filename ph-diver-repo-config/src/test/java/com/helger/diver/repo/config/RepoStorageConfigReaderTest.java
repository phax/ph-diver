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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;

/**
 * Test class for class {@link RepoStorageConfigReader}.
 *
 * @author Philip Helger
 */
public final class RepoStorageConfigReaderTest
{
  static final File BASE_DIR = new File ("src/test/resources/repoconfig");

  @Test
  public void testReadValid ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (new File (BASE_DIR, "repoconfig-valid.xml"),
                                                                    aErrorList);
    assertNotNull (aConfig);
    assertTrue (aErrorList.toString (), aErrorList.isEmpty ());
    assertEquals (2, aConfig.getDefinitionCount ());

    final RepoStorageDefinition aDef0 = aConfig.getDefinitionOfID ("test-repo");
    assertNotNull (aDef0);
    assertEquals ("localfs", aDef0.getType ());
    assertEquals (ERepoWritable.WITHOUT_WRITE, aDef0.getWritable ());
    assertEquals (ERepoDeletable.WITHOUT_DELETE, aDef0.getDeletable ());
    assertTrue (aDef0.isVerifyHash ());
    assertEquals (ERepoTopTocType.DIVER_XML, aDef0.getTopTocType ());
    assertTrue (aDef0.hasProperty ("basedir"));
    assertEquals ("test-repo", aDef0.getPropertyValue ("basedir"));
    assertEquals (BASE_DIR.getAbsoluteFile (), aDef0.getBaseDirectory ());
    assertEquals (new File (BASE_DIR, "test-repo").getAbsoluteFile (), aDef0.getResolvedFile ("test-repo"));

    final RepoStorageDefinition aDef1 = aConfig.getDefinitionOfID ("empty-repo");
    assertNotNull (aDef1);
    assertFalse (aDef1.isVerifyHash ());
    assertEquals (ERepoTopTocType.NONE, aDef1.getTopTocType ());

    assertNull (aConfig.getDefinitionOfID ("other"));
  }

  @Test
  public void testResolveAbsolutePath ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (new File (BASE_DIR, "repoconfig-valid.xml"),
                                                                    aErrorList);
    assertNotNull (aConfig);
    final File aAbsDir = new File ("target").getAbsoluteFile ();
    assertEquals (aAbsDir, aConfig.getDefinitionOfID ("test-repo").getResolvedFile (aAbsDir.getAbsolutePath ()));
  }

  @Test
  public void testReadEmpty ()
  {
    final ErrorList aErrorList = new ErrorList ();
    final RepoStorageConfig aConfig = RepoStorageConfigReader.read (new File (BASE_DIR, "repoconfig-empty.xml"),
                                                                    aErrorList);
    assertNotNull (aConfig);
    assertTrue (aErrorList.isEmpty ());
    assertEquals (0, aConfig.getDefinitionCount ());
  }

  @Test
  public void testReadInvalid ()
  {
    for (final String sFilename : new String [] { "repoconfig-duplicate-id.xml",
                                                  "repoconfig-duplicate-property.xml",
                                                  "repoconfig-invalid-toptoc.xml",
                                                  "does-not-exist.xml" })
    {
      final ErrorList aErrorList = new ErrorList ();
      assertNull (sFilename, RepoStorageConfigReader.read (new File (BASE_DIR, sFilename), aErrorList));
      assertTrue (sFilename, aErrorList.containsAtLeastOneError ());
    }
  }
}
