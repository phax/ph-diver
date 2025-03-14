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
package com.helger.diver.repo.toc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.diver.api.DVRException;
import com.helger.diver.api.version.DVRVersion;
import com.helger.diver.repo.toc.jaxb.RepoToc1Marshaller;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;

/**
 * Test class for class {@link RepoToc}.
 *
 * @author Philip Helger
 */
public final class RepoTocTest
{
  @Test
  public void testBasic () throws DVRException
  {
    final RepoToc aToC = new RepoToc ("g", "a");
    assertEquals ("g", aToC.getGroupID ());
    assertEquals ("a", aToC.getArtifactID ());

    // No version
    ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertTrue (aVersions.isEmpty ());

    assertNull (aToC.getLatestVersion ());
    assertNull (aToC.getLatestVersionAsString ());
    assertNull (aToC.getLatestReleaseVersion ());
    assertNull (aToC.getLatestReleaseVersionAsString ());

    // 1 release version
    assertFalse (aToC.containsVersion (null));
    assertFalse (aToC.containsVersion (DVRVersion.parseOrThrow ("1.0")));
    assertTrue (aToC.addVersion (DVRVersion.parseOrThrow ("1.0"), PDTFactory.getCurrentOffsetDateTime ()).isChanged ());
    assertFalse (aToC.containsVersion (null));
    assertTrue (aToC.containsVersion (DVRVersion.parseOrThrow ("1.0")));

    aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (1, aVersions.size ());

    assertNotNull (aToC.getLatestVersion ());
    assertEquals ("1", aToC.getLatestVersionAsString ());
    assertNotNull (aToC.getLatestReleaseVersion ());
    assertEquals ("1", aToC.getLatestReleaseVersionAsString ());

    // 2 release versions
    assertTrue (aToC.addVersion (DVRVersion.parseOrThrow ("1.1"), PDTFactory.getCurrentOffsetDateTime ()).isChanged ());

    aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (2, aVersions.size ());

    assertNotNull (aToC.getLatestVersion ());
    assertEquals ("1.1", aToC.getLatestVersionAsString ());
    assertNotNull (aToC.getLatestReleaseVersion ());
    assertEquals ("1.1", aToC.getLatestReleaseVersionAsString ());

    // 2 release versions, 1 SNAPSHOT version
    assertTrue (aToC.addVersion (DVRVersion.parseOrThrow ("1.2-SNAPSHOT"), PDTFactory.getCurrentOffsetDateTime ())
                    .isChanged ());

    aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (3, aVersions.size ());

    assertNotNull (aToC.getLatestVersion ());
    assertEquals ("1.2-SNAPSHOT", aToC.getLatestVersionAsString ());
    assertNotNull (aToC.getLatestReleaseVersion ());
    assertEquals ("1.1", aToC.getLatestReleaseVersionAsString ());

    // 3 release versions, 1 SNAPSHOT version
    assertTrue (aToC.addVersion (DVRVersion.parseOrThrow ("1.2"), PDTFactory.getCurrentOffsetDateTime ()).isChanged ());

    aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (4, aVersions.size ());

    assertNotNull (aToC.getLatestVersion ());
    assertEquals ("1.2", aToC.getLatestVersionAsString ());
    assertNotNull (aToC.getLatestReleaseVersion ());
    assertEquals ("1.2", aToC.getLatestReleaseVersionAsString ());
  }

  @Test
  public void testStandardMethods () throws DVRException
  {
    final OffsetDateTime aODT = PDTFactory.getCurrentOffsetDateTime ();

    final ICommonsMap <DVRVersion, OffsetDateTime> aVersions = new CommonsHashMap <> ();
    aVersions.put (DVRVersion.parseOrThrow ("1.0"), aODT);
    aVersions.put (DVRVersion.parseOrThrow ("1.1"), aODT);
    final RepoToc aToC = new RepoToc ("g", "a", aVersions);

    final ICommonsMap <DVRVersion, OffsetDateTime> aVersions2 = aVersions.getClone ();
    aVersions2.put (DVRVersion.parseOrThrow ("1.2"), aODT);

    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (aToC, new RepoToc ("g", "a", aVersions));
    CommonsTestHelper.testDefaultImplementationWithDifferentContentObject (aToC, new RepoToc ("g2", "a", aVersions));
    CommonsTestHelper.testDefaultImplementationWithDifferentContentObject (aToC, new RepoToc ("g", "a2", aVersions));
    CommonsTestHelper.testDefaultImplementationWithDifferentContentObject (aToC, new RepoToc ("g", "a", aVersions2));
  }

  @Test
  public void testCreateFromJaxb () throws DVRException
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    final RepoToc aToC = RepoToc.createFromJaxbObject (aRepoToc1);
    assertNotNull (aToC);

    assertEquals ("com.ecosio", aToC.getGroupID ());
    assertEquals ("artifact1", aToC.getArtifactID ());

    assertEquals ("11.1.3-SNAPSHOT", aToC.getLatestVersionAsString ());
    assertEquals ("11.1.2", aToC.getLatestReleaseVersionAsString ());

    assertEquals (PDTFactory.createOffsetDateTimeUTC (2023, Month.SEPTEMBER, 19, 17, 45, 12)
                            .plus (456, ChronoUnit.MILLIS), aToC.getLatestVersionPublicationDateTime ());
    assertEquals (PDTFactory.createOffsetDateTimeUTC (2023, Month.SEPTEMBER, 19, 16, 37, 00)
                            .plus (123, ChronoUnit.MILLIS), aToC.getLatestReleaseVersionPublicationDateTime ());

    assertEquals (4, aToC.getVersionCount ());
    final ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (4, aVersions.size ());

    OffsetDateTime aDT = aVersions.get (DVRVersion.parseOrThrow ("10.0.0"));
    assertNotNull (aDT);
    assertEquals ("2023-07-19T14:37:00Z", PDTWebDateHelper.getAsStringXSD (aDT));

    aDT = aVersions.get (DVRVersion.parseOrThrow ("10.1.4"));
    assertNotNull (aDT);
    assertEquals ("2023-08-19T15:37:00Z", PDTWebDateHelper.getAsStringXSD (aDT));

    aDT = aVersions.get (DVRVersion.parseOrThrow ("11.1.2"));
    assertNotNull (aDT);
    assertEquals ("2023-09-19T16:37:00.123Z", PDTWebDateHelper.getAsStringXSD (aDT));

    aDT = aVersions.get (DVRVersion.parseOrThrow ("11.1.3-SNAPSHOT"));
    assertNotNull (aDT);
    assertEquals ("2023-09-19T17:45:12.456Z", PDTWebDateHelper.getAsStringXSD (aDT));

    // Make sure it is convertible
    assertNotNull (new RepoToc1Marshaller ().getAsDocument (aToC.getAsJaxbObject ()));
  }

  @Test
  public void testDeleteVersionOldestRelease () throws DVRException
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    final RepoToc aToC = RepoToc.createFromJaxbObject (aRepoToc1);
    assertNotNull (aToC);

    // removed oldest release version
    aToC.removeVersion (DVRVersion.parseOrThrow ("10.0.0"));

    assertEquals ("11.1.3-SNAPSHOT", aToC.getLatestVersionAsString ());
    assertEquals ("11.1.2", aToC.getLatestReleaseVersionAsString ());

    assertEquals (3, aToC.getVersionCount ());
    final ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (3, aVersions.size ());

    // Make sure it is convertible
    assertNotNull (new RepoToc1Marshaller ().getAsDocument (aToC.getAsJaxbObject ()));
  }

  @Test
  public void testDeleteVersionNewestRelease () throws DVRException
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    final RepoToc aToC = RepoToc.createFromJaxbObject (aRepoToc1);
    assertNotNull (aToC);

    // removed newest release version
    aToC.removeVersion (DVRVersion.parseOrThrow ("11.1.2"));

    assertEquals ("11.1.3-SNAPSHOT", aToC.getLatestVersionAsString ());
    assertEquals ("10.1.4", aToC.getLatestReleaseVersionAsString ());

    assertEquals (3, aToC.getVersionCount ());
    final ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (3, aVersions.size ());

    // Make sure it is convertible
    assertNotNull (new RepoToc1Marshaller ().getAsDocument (aToC.getAsJaxbObject ()));
  }

  @Test
  public void testDeleteVersionNewestSnapshot () throws DVRException
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    final RepoToc aToC = RepoToc.createFromJaxbObject (aRepoToc1);
    assertNotNull (aToC);

    // removed newest release version
    aToC.removeVersion (DVRVersion.parseOrThrow ("11.1.3-SNAPSHOT"));

    assertEquals ("11.1.2", aToC.getLatestVersionAsString ());
    assertEquals ("11.1.2", aToC.getLatestReleaseVersionAsString ());

    assertEquals (3, aToC.getVersionCount ());
    final ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (3, aVersions.size ());

    // Make sure it is convertible
    assertNotNull (new RepoToc1Marshaller ().getAsDocument (aToC.getAsJaxbObject ()));
  }

  @Test
  public void testDeleteVersionAll () throws DVRException
  {
    final RepoToc1Marshaller m = new RepoToc1Marshaller ();
    final RepoTocType aRepoToc1 = m.read (new ClassPathResource ("repotoc/repotoc-1.xml"));
    assertNotNull (aRepoToc1);

    final RepoToc aToC = RepoToc.createFromJaxbObject (aRepoToc1);
    assertNotNull (aToC);

    // removed newest release version
    aToC.removeVersion (DVRVersion.parseOrThrow ("10.0.0"));
    aToC.removeVersion (DVRVersion.parseOrThrow ("10.1.4"));
    aToC.removeVersion (DVRVersion.parseOrThrow ("11.1.2"));
    aToC.removeVersion (DVRVersion.parseOrThrow ("11.1.3-SNAPSHOT"));

    assertNull (aToC.getLatestVersionAsString ());
    assertNull (aToC.getLatestReleaseVersionAsString ());

    assertEquals (0, aToC.getVersionCount ());
    final ICommonsSortedMap <DVRVersion, OffsetDateTime> aVersions = aToC.getAllVersions ();
    assertNotNull (aVersions);
    assertEquals (0, aVersions.size ());

    // Make sure it is convertible
    assertNotNull (new RepoToc1Marshaller ().getAsDocument (aToC.getAsJaxbObject ()));
  }
}
