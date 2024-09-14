/*
 * Copyright (C) 2023-2024 Philip Helger & ecosio
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
package com.helger.diver.repo.toc.jaxb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.diver.repo.toptoc.jaxb.v10.GroupType;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;

/**
 * Test class for class {@link RepoTopToc1Marshaller}.
 *
 * @author Philip Helger
 */
public final class RepoTopToc1MarshallerTest
{
  @Test
  public void testRepoToc1 ()
  {
    final RepoTopToc1Marshaller m = new RepoTopToc1Marshaller ();
    final RepoTopTocType aRepoTopToc1 = m.read (new ClassPathResource ("repotoptoc/repotoptoc-1.xml"));
    assertNotNull (aRepoTopToc1);

    assertEquals (2, aRepoTopToc1.getGroupCount ());
    assertEquals ("com", aRepoTopToc1.getGroupAtIndex (0).getName ());
    assertEquals ("org", aRepoTopToc1.getGroupAtIndex (1).getName ());

    final GroupType aCom = aRepoTopToc1.getGroupAtIndex (0);
    assertEquals (3, aCom.getGroupCount ());
    assertEquals ("ecosio", aCom.getGroupAtIndex (0).getName ());
    assertEquals ("helger", aCom.getGroupAtIndex (1).getName ());
    assertEquals ("rest", aCom.getGroupAtIndex (2).getName ());

    assertEquals (0, aCom.getGroupAtIndex (0).getGroupCount ());
    assertEquals (2, aCom.getGroupAtIndex (0).getArtifactCount ());
    assertEquals (0, aCom.getGroupAtIndex (1).getGroupCount ());
    assertEquals (1, aCom.getGroupAtIndex (1).getArtifactCount ());
    assertEquals (1, aCom.getGroupAtIndex (2).getGroupCount ());
    assertEquals (0, aCom.getGroupAtIndex (2).getArtifactCount ());
  }
}
