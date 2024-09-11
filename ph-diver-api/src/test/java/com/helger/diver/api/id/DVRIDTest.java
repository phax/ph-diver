/*
 * Copyright (C) 2023-2024 Philip Helger (www.helger.com)
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
package com.helger.diver.api.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.string.StringHelper;
import com.helger.diver.api.DVRException;

/**
 * Test class for class {@link DVRID}.
 *
 * @author Philip Helger
 */
public final class DVRIDTest
{
  @Test
  public void testIsValid ()
  {
    assertTrue (DVRID.isValidGroupID ("com"));
    assertTrue (DVRID.isValidGroupID ("com.helger"));
    assertTrue (DVRID.isValidGroupID ("01234"));
    assertTrue (DVRID.isValidGroupID ("1.2.3.4.5"));
    assertTrue (DVRID.isValidGroupID ("ph-as4"));
    assertTrue (DVRID.isValidGroupID ("-.___"));

    assertFalse (DVRID.isValidGroupID (null));
    assertFalse (DVRID.isValidGroupID (""));
    assertFalse (DVRID.isValidGroupID ("ä"));
    assertFalse (DVRID.isValidGroupID ("a:b"));

    // Max length
    assertTrue (DVRID.isValidGroupID (StringHelper.getRepeated ('a', DVRIDSettings.DEFAULT_MAX_GROUP_ID_LEN)));
    assertFalse (DVRID.isValidGroupID (StringHelper.getRepeated ('a', DVRIDSettings.DEFAULT_MAX_GROUP_ID_LEN + 1)));
  }

  @Test
  public void testBasic () throws DVRException
  {
    final DVRID aID1 = new DVRID ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());
    CommonsTestHelper.testEqualsImplementationWithEqualContentObject (aID1,
                                                                      new DVRID ("com.helger",
                                                                                 "phive",
                                                                                 "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new DVRID ("com.holger",
                                                                                     "phive",
                                                                                     "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new DVRID ("com.helger",
                                                                                     "phivengine",
                                                                                     "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new DVRID ("com.helger", "phive", "3.0.0"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new DVRID ("com.helger",
                                                                                     "phive",
                                                                                     "3.0.0.SNAPSHOT",
                                                                                     "src"));
  }

  @Test
  public void testParseID () throws DVRException
  {
    final DVRID aID1 = new DVRID ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());

    final DVRID aID2 = aID1.getWithClassifier ("test");
    assertEquals ("com.helger", aID2.getGroupID ());
    assertEquals ("phive", aID2.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID2.getVersionString ());
    assertEquals ("test", aID2.getClassifier ());

    assertEquals (aID1, DVRID.parseIDOrNull (aID1.getAsSingleID ()));
    assertEquals (aID1, DVRID.parseIDOrNull (aID1.getAsSingleID () + DVRID.ID_SEPARATOR));
    assertEquals (aID2, DVRID.parseIDOrNull (aID2.getAsSingleID ()));
    assertNull (DVRID.parseIDOrNull (null));
    assertNull (DVRID.parseIDOrNull ("a"));
    assertNull (DVRID.parseIDOrNull ("a:b"));
    assertNull (DVRID.parseIDOrNull ("a:b:c:d:e"));
    assertNull (DVRID.parseIDOrNull ("a:b:c:d:e:f"));
    assertNull (DVRID.parseIDOrNull ("::"));
    assertNull (DVRID.parseIDOrNull (":::"));
    assertNull (DVRID.parseIDOrNull ("a:b:"));
  }

  @Test
  public void testMaxGroupIDLen ()
  {
    assertNotNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRIDSettings.getMaxGroupIDLen ();
    DVRIDSettings.setMaxGroupIDLen (1);
    try
    {
      // Too long
      assertNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRID.parseIDOrNull ("g:artifact:1.0:classifier"));
    }
    finally
    {
      DVRIDSettings.setMaxGroupIDLen (nOld);
    }
  }

  @Test
  public void testMaxArtifactIDLen ()
  {
    assertNotNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRIDSettings.getMaxArtifactIDLen ();
    DVRIDSettings.setMaxArtifactIDLen (1);
    try
    {
      // Too long
      assertNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRID.parseIDOrNull ("group:a:1.0:classifier"));
    }
    finally
    {
      DVRIDSettings.setMaxArtifactIDLen (nOld);
    }
  }

  @Test
  public void testMaxVersionLen ()
  {
    assertNotNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRIDSettings.getMaxVersionLen ();
    DVRIDSettings.setMaxVersionLen (1);
    try
    {
      // Too long
      assertNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRID.parseIDOrNull ("group:artifact:1:classifier"));
    }
    finally
    {
      DVRIDSettings.setMaxVersionLen (nOld);
    }
  }

  @Test
  public void testMaxClassifierLen ()
  {
    assertNotNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRIDSettings.getMaxClassifierLen ();
    DVRIDSettings.setMaxClassifierLen (1);
    try
    {
      // Too long
      assertNull (DVRID.parseIDOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRID.parseIDOrNull ("group:artifact:1.0:c"));
    }
    finally
    {
      DVRIDSettings.setMaxClassifierLen (nOld);
    }
  }
}
