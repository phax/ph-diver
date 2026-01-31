/*
 * Copyright (C) 2023-2026 Philip Helger (www.helger.com)
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
package com.helger.diver.api.coord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.helger.diver.api.version.DVRVersionException;
import com.helger.unittest.support.TestHelper;

/**
 * Test class for class {@link DVRCoordinate}.
 *
 * @author Philip Helger
 */
public final class DVRCoordinateTest
{
  @Test
  public void testBasic () throws DVRVersionException
  {
    final DVRCoordinate aID1 = DVRCoordinate.create ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());
    TestHelper.testEqualsImplementationWithEqualContentObject (aID1,
                                                               DVRCoordinate.create ("com.helger",
                                                                                     "phive",
                                                                                     "3.0.0.SNAPSHOT"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                   DVRCoordinate.create ("com.holger",
                                                                                         "phive",
                                                                                         "3.0.0.SNAPSHOT"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                   DVRCoordinate.create ("com.helger",
                                                                                         "phivengine",
                                                                                         "3.0.0.SNAPSHOT"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                   DVRCoordinate.create ("com.helger",
                                                                                         "phive",
                                                                                         "3.0.0"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (aID1,
                                                                   DVRCoordinate.create ("com.helger",
                                                                                         "phive",
                                                                                         "3.0.0.SNAPSHOT",
                                                                                         "src"));
  }

  @Test
  public void testParseID () throws DVRVersionException
  {
    final DVRCoordinate aID1 = DVRCoordinate.create ("com.helger", "phive", "3.0.0.SNAPSHOT");
    assertEquals ("com.helger", aID1.getGroupID ());
    assertEquals ("phive", aID1.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID1.getVersionString ());
    assertNull (aID1.getClassifier ());

    final DVRCoordinate aID2 = aID1.getWithClassifier ("test");
    assertEquals ("com.helger", aID2.getGroupID ());
    assertEquals ("phive", aID2.getArtifactID ());
    assertEquals ("3-SNAPSHOT", aID2.getVersionString ());
    assertEquals ("test", aID2.getClassifier ());

    assertEquals (aID1, DVRCoordinate.parseOrNull (aID1.getAsSingleID ()));
    assertEquals (aID1, DVRCoordinate.parseOrNull (aID1.getAsSingleID () + DVRCoordinate.PART_SEPARATOR));
    assertEquals (aID2, DVRCoordinate.parseOrNull (aID2.getAsSingleID ()));
    assertNull (DVRCoordinate.parseOrNull (null));
    assertNull (DVRCoordinate.parseOrNull ("a"));
    assertNull (DVRCoordinate.parseOrNull ("a:b"));
    assertNull (DVRCoordinate.parseOrNull ("a:b:c:d:e"));
    assertNull (DVRCoordinate.parseOrNull ("a:b:c:d:e:f"));
    assertNull (DVRCoordinate.parseOrNull ("::"));
    assertNull (DVRCoordinate.parseOrNull (":::"));
    assertNull (DVRCoordinate.parseOrNull ("a:b:"));
  }
}
