/*
 * Copyright (C) 2023 Philip Helger (www.helger.com)
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
package com.helger.diver.api.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.string.StringHelper;

/**
 * Test class for class {@link VESID}.
 *
 * @author Philip Helger
 */
public final class VESIDTest
{
  @Test
  public void testIsValid ()
  {
    assertTrue (VESID.isValidGroupID ("com"));
    assertTrue (VESID.isValidGroupID ("com.helger"));
    assertTrue (VESID.isValidGroupID ("01234"));
    assertTrue (VESID.isValidGroupID ("1.2.3.4.5"));
    assertTrue (VESID.isValidGroupID ("ph-as4"));
    assertTrue (VESID.isValidGroupID ("-.___"));

    assertFalse (VESID.isValidGroupID (null));
    assertFalse (VESID.isValidGroupID (""));
    assertFalse (VESID.isValidGroupID ("ä"));
    assertFalse (VESID.isValidGroupID ("a:b"));

    // Max length
    assertTrue (VESID.isValidGroupID (StringHelper.getRepeated ('a', VESIDSettings.DEFAULT_MAX_GROUP_ID_LEN)));
    assertFalse (VESID.isValidGroupID (StringHelper.getRepeated ('a', VESIDSettings.DEFAULT_MAX_GROUP_ID_LEN + 1)));
  }

  @Test
  public void testBasic ()
  {
    final VESID aID1 = new VESID ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());
    CommonsTestHelper.testEqualsImplementationWithEqualContentObject (aID1,
                                                                      new VESID ("com.helger",
                                                                                 "phive",
                                                                                 "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new VESID ("com.holger",
                                                                                     "phive",
                                                                                     "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new VESID ("com.helger",
                                                                                     "phivengine",
                                                                                     "3.0.0.SNAPSHOT"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new VESID ("com.helger", "phive", "3.0.0"));
    CommonsTestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                          new VESID ("com.helger",
                                                                                     "phive",
                                                                                     "3.0.0.SNAPSHOT",
                                                                                     "src"));
  }

  @Test
  public void testParseID ()
  {
    final VESID aID1 = new VESID ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());

    final VESID aID2 = aID1.getWithClassifier ("test");
    assertEquals ("com.helger", aID2.getGroupID ());
    assertEquals ("phive", aID2.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID2.getVersionString ());
    assertEquals ("test", aID2.getClassifier ());

    assertEquals (aID1, VESID.parseIDOrNull (aID1.getAsSingleID ()));
    assertEquals (aID1, VESID.parseIDOrNull (aID1.getAsSingleID () + VESID.ID_SEPARATOR));
    assertEquals (aID2, VESID.parseIDOrNull (aID2.getAsSingleID ()));
    assertNull (VESID.parseIDOrNull (null));
    assertNull (VESID.parseIDOrNull ("a"));
    assertNull (VESID.parseIDOrNull ("a:b"));
    assertNull (VESID.parseIDOrNull ("a:b:c:d:e"));
    assertNull (VESID.parseIDOrNull ("a:b:c:d:e:f"));
    assertNull (VESID.parseIDOrNull ("::"));
    assertNull (VESID.parseIDOrNull (":::"));
    assertNull (VESID.parseIDOrNull ("a:b:"));
  }
}
