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
package com.helger.diver.repo.config.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.config.jaxb.v10.RCRepositoryType;
import com.helger.diver.repo.config.jaxb.v10.RCTopTocType;
import com.helger.diver.repo.config.jaxb.v10.RepoConfigType;
import com.helger.io.resource.ClassPathResource;

/**
 * Test class for class {@link RepoConfig1Marshaller}.
 *
 * @author Philip Helger
 */
public final class RepoConfig1MarshallerTest
{
  @Test
  public void testValid ()
  {
    final RepoConfig1Marshaller m = new RepoConfig1Marshaller ();
    final RepoConfigType aRepoConfig = m.read (new ClassPathResource ("repoconfig/repoconfig-valid.xml"));
    assertNotNull (aRepoConfig);
    assertEquals (2, aRepoConfig.getRepositoryCount ());

    final RCRepositoryType aRepo0 = aRepoConfig.getRepositoryAtIndex (0);
    assertEquals ("test-repo", aRepo0.getId ());
    assertEquals ("localfs", aRepo0.getType ());
    assertFalse (aRepo0.isWritable ());
    assertFalse (aRepo0.isDeletable ());
    assertTrue (aRepo0.isVerifyHash ());
    assertEquals (RCTopTocType.DIVER_XML, aRepo0.getToptoc ());
    assertEquals (1, aRepo0.getPropertyCount ());
    assertEquals ("basedir", aRepo0.getPropertyAtIndex (0).getName ());
    assertEquals ("test-repo", aRepo0.getPropertyAtIndex (0).getValue ());

    final RCRepositoryType aRepo1 = aRepoConfig.getRepositoryAtIndex (1);
    assertEquals ("empty-repo", aRepo1.getId ());
    assertFalse (aRepo1.isVerifyHash ());
    assertEquals (RCTopTocType.NONE, aRepo1.getToptoc ());
  }

  @Test
  public void testEmpty ()
  {
    final RepoConfigType aRepoConfig = new RepoConfig1Marshaller ().read (new ClassPathResource ("repoconfig/repoconfig-empty.xml"));
    assertNotNull (aRepoConfig);
    assertEquals (0, aRepoConfig.getRepositoryCount ());
  }

  @Test
  public void testInvalid ()
  {
    for (final String sFilename : new String [] { "repoconfig-duplicate-id.xml",
                                                  "repoconfig-duplicate-property.xml",
                                                  "repoconfig-invalid-toptoc.xml" })
    {
      final ErrorList aErrorList = new ErrorList ();
      final RepoConfigType aRepoConfig = new RepoConfig1Marshaller ().setCollectErrors (aErrorList)
                                                                     .read (new ClassPathResource ("repoconfig/" +
                                                                                                   sFilename));
      assertNull (sFilename, aRepoConfig);
      assertTrue (sFilename, aErrorList.containsAtLeastOneError ());
    }
  }
}
