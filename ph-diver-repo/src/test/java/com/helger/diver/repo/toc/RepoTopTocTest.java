/*
 * Copyright (C) 2023 Philip Helger & ecosio
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;

/**
 * Test class for class {@link RepoTopToc}.
 *
 * @author Philip Helger
 */
public final class RepoTopTocTest
{
  @Test
  public void testBasic ()
  {}

  @Test
  public void testCreateFromJaxb ()
  {
    final RepoTopToc1Marshaller m = new RepoTopToc1Marshaller ();
    final RepoTopTocType aRepoTopToc1 = m.read (new ClassPathResource ("repotoptoc/repotoptoc-1.xml"));
    assertNotNull (aRepoTopToc1);

    final RepoTopToc aToC = RepoTopToc.createFromJaxbObject (aRepoTopToc1);
    assertNotNull (aToC);

    // Check top level groups
    final ICommonsSortedSet <String> aTLGroups = aToC.getAllTopLevelGroupNames ();
    assertNotNull (aTLGroups);
    assertEquals (2, aTLGroups.size ());
    assertTrue (aTLGroups.contains ("com"));
    assertTrue (aTLGroups.contains ("org"));

    // Check all subgroups from a specific start
    final ICommonsList <String> aAllRelSubgroups = new CommonsArrayList <> ();
    final ICommonsSet <String> aAllAbsSubgroups = new CommonsHashSet <> ();
    aToC.iterateAllSubGroups ("com", (relgn, absgn) -> {
      aAllRelSubgroups.add (relgn);
      aAllAbsSubgroups.add (absgn);
    });
    assertEquals (6, aAllAbsSubgroups.size ());
    assertTrue (aAllAbsSubgroups.contains ("com.ecosio"));
    assertTrue (aAllAbsSubgroups.contains ("com.helger"));
    assertTrue (aAllAbsSubgroups.contains ("com.rest"));
    assertTrue (aAllAbsSubgroups.contains ("com.rest.of"));
    assertTrue (aAllAbsSubgroups.contains ("com.rest.of.the"));
    assertTrue (aAllAbsSubgroups.contains ("com.rest.of.the.fest"));

    assertEquals (6, aAllRelSubgroups.size ());
    assertEquals ("ecosio", aAllRelSubgroups.get (0));
    assertEquals ("helger", aAllRelSubgroups.get (1));
    assertEquals ("rest", aAllRelSubgroups.get (2));
    assertEquals ("of", aAllRelSubgroups.get (3));
    assertEquals ("the", aAllRelSubgroups.get (4));
    assertEquals ("fest", aAllRelSubgroups.get (5));

    final ICommonsSet <String> aAllArtifacts = new CommonsHashSet <> ();
    aToC.iterateAllArtifacts ("com", artifactID -> { aAllArtifacts.add (artifactID); });
  }
}
