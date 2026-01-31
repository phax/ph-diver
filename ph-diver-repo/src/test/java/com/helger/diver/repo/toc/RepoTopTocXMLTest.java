/*
 * Copyright (C) 2023-2026 Philip Helger & ecosio
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

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.CommonsLinkedHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.diver.repo.toc.jaxb.RepoTopToc1Marshaller;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;
import com.helger.io.resource.ClassPathResource;

/**
 * Test class for class {@link RepoTopTocXML}.
 *
 * @author Philip Helger
 */
public final class RepoTopTocXMLTest
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

    // Read
    final RepoTopTocXML aToC = RepoTopTocXML.createFromJaxbObject (aRepoTopToc1);
    assertNotNull (aToC);

    // Convert to JAXB again
    final RepoTopTocType aRepoTopToc2 = aToC.getAsJaxbObject ();
    assertNotNull (aRepoTopToc2);
    assertEquals (aRepoTopToc1, aRepoTopToc2);

    // Check top level groups
    {
      final ICommonsOrderedSet <String> aTLGroups = new CommonsLinkedHashSet <> ();
      aToC.iterateAllTopLevelGroupNames (aTLGroups::add);
      assertNotNull (aTLGroups);
      assertEquals (2, aTLGroups.size ());
      assertTrue (aTLGroups.contains ("com"));
      assertTrue (aTLGroups.contains ("org"));
    }

    // Check all subgroups from a specific start
    {
      final ICommonsList <String> aAllRelSubgroups = new CommonsArrayList <> ();
      final ICommonsSet <String> aAllAbsSubgroups = new CommonsHashSet <> ();
      aToC.iterateAllSubGroups ("com", (relGroupName, absGroupName) -> {
        aAllRelSubgroups.add (relGroupName);
        aAllAbsSubgroups.add (absGroupName);
      }, true);
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
    }

    // Check all subgroups from a specific start
    {
      final ICommonsList <String> aAllRelSubgroups = new CommonsArrayList <> ();
      final ICommonsSet <String> aAllAbsSubgroups = new CommonsHashSet <> ();
      aToC.iterateAllSubGroups ("com", (relGroupName, absGroupName) -> {
        aAllRelSubgroups.add (relGroupName);
        aAllAbsSubgroups.add (absGroupName);
      }, false);
      assertEquals (3, aAllAbsSubgroups.size ());
      assertTrue (aAllAbsSubgroups.contains ("com.ecosio"));
      assertTrue (aAllAbsSubgroups.contains ("com.helger"));
      assertTrue (aAllAbsSubgroups.contains ("com.rest"));

      assertEquals (3, aAllRelSubgroups.size ());
      assertEquals ("ecosio", aAllRelSubgroups.get (0));
      assertEquals ("helger", aAllRelSubgroups.get (1));
      assertEquals ("rest", aAllRelSubgroups.get (2));
    }

    // Check all artifacts of a group
    {
      final ICommonsList <String> aAllArtifacts = new CommonsArrayList <> ();
      aToC.iterateAllArtifacts ("com", artifactID -> { aAllArtifacts.add (artifactID); });
      assertEquals (0, aAllArtifacts.size ());
    }

    {
      final ICommonsList <String> aAllArtifacts = new CommonsArrayList <> ();
      aToC.iterateAllArtifacts ("com.ecosio", artifactID -> { aAllArtifacts.add (artifactID); });
      assertEquals (2, aAllArtifacts.size ());
      assertEquals ("artifact1", aAllArtifacts.get (0));
      assertEquals ("artifact2", aAllArtifacts.get (1));
    }

    {
      final ICommonsList <String> aAllArtifacts = new CommonsArrayList <> ();
      aToC.iterateAllArtifacts ("com.helger", artifactID -> { aAllArtifacts.add (artifactID); });
      assertEquals (1, aAllArtifacts.size ());
      assertEquals ("artifact3", aAllArtifacts.get (0));
    }

    {
      final ICommonsList <String> aAllArtifacts = new CommonsArrayList <> ();
      aToC.iterateAllArtifacts ("org", artifactID -> { aAllArtifacts.add (artifactID); });
      assertEquals (0, aAllArtifacts.size ());
    }

    {
      final ICommonsList <String> aAllArtifacts = new CommonsArrayList <> ();
      aToC.iterateAllArtifacts ("org.example", artifactID -> { aAllArtifacts.add (artifactID); });
      assertEquals (1, aAllArtifacts.size ());
      assertEquals ("artifact4", aAllArtifacts.get (0));
    }
  }
}
