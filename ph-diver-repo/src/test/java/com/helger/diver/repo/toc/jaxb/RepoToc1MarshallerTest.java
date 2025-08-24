/*
 * Copyright (C) 2023-2025 Philip Helger & ecosio
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

import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.diver.repo.toc.jaxb.v10.RTVersionListType;
import com.helger.diver.repo.toc.jaxb.v10.RTVersionType;
import com.helger.diver.repo.toc.jaxb.v10.RTVersioningType;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;
import com.helger.io.resource.ClassPathResource;

/**
 * Test class for class {@link RepoToc1Marshaller}.
 *
 * @author Philip Helger
 */
public final class RepoToc1MarshallerTest
{
  @Test
  public void testRepoToc1 ()
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    assertEquals ("com.ecosio", aRepoToc1.getGroupId ());
    assertEquals ("artifact1", aRepoToc1.getArtifactId ());

    final RTVersioningType aVersioning = aRepoToc1.getVersioning ();
    assertNotNull (aVersioning);

    assertEquals ("11.1.3-SNAPSHOT", aVersioning.getLatest ());
    assertEquals ("11.1.2", aVersioning.getLatestRelease ());

    final RTVersionListType aVersions = aVersioning.getVersions ();
    assertNotNull (aVersions);
    assertEquals (4, aVersions.getVersionCount ());

    final RTVersionType v0 = aVersions.getVersionAtIndex (0);
    assertEquals ("10.0.0", v0.getValue ());
    assertEquals ("2023-07-19T14:37:00Z", PDTWebDateHelper.getAsStringXSD (v0.getPublished ()));

    final RTVersionType v1 = aVersions.getVersionAtIndex (1);
    assertEquals ("10.1.4", v1.getValue ());
    assertEquals ("2023-08-19T15:37:00Z", PDTWebDateHelper.getAsStringXSD (v1.getPublished ()));

    final RTVersionType v2 = aVersions.getVersionAtIndex (2);
    assertEquals ("11.1.2", v2.getValue ());
    assertEquals ("2023-09-19T16:37:00.123Z", PDTWebDateHelper.getAsStringXSD (v2.getPublished ()));

    final RTVersionType v3 = aVersions.getVersionAtIndex (3);
    assertEquals ("11.1.3-SNAPSHOT", v3.getValue ());
    assertEquals ("2023-09-19T17:45:12.456Z", PDTWebDateHelper.getAsStringXSD (v3.getPublished ()));
  }
}
