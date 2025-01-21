/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
package com.helger.diver.api.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.string.StringHelper;
import com.helger.diver.api.coord.DVRCoordinate;

/**
 * Test class for class {@link DVRValidityHelper}.
 *
 * @author Philip Helger
 */
public final class DVRValidityHelperTest
{
  @Test
  public void testIsValid ()
  {
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("com"));
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("com.helger"));
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("01234"));
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("1.2.3.4.5"));
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("ph-as4"));
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID ("-.___"));

    assertFalse (DVRValidityHelper.isValidCoordinateGroupID (null));
    assertFalse (DVRValidityHelper.isValidCoordinateGroupID (""));
    assertFalse (DVRValidityHelper.isValidCoordinateGroupID ("ä"));
    assertFalse (DVRValidityHelper.isValidCoordinateGroupID ("a:b"));

    // Max length
    assertTrue (DVRValidityHelper.isValidCoordinateGroupID (StringHelper.getRepeated ('a',
                                                                                      DVRGlobalCoordinateSettings.DEFAULT_GROUP_ID_MAX_LEN)));
    assertFalse (DVRValidityHelper.isValidCoordinateGroupID (StringHelper.getRepeated ('a',
                                                                                       DVRGlobalCoordinateSettings.DEFAULT_GROUP_ID_MAX_LEN +
                                                                                            1)));
  }

  @Test
  public void testMaxGroupIDLen ()
  {
    assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRGlobalCoordinateSettings.getGroupIDMaxLen ();
    DVRGlobalCoordinateSettings.setGroupIDMaxLen (1);
    try
    {
      // Too long
      assertNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRCoordinate.parseOrNull ("g:artifact:1.0:classifier"));
    }
    finally
    {
      DVRGlobalCoordinateSettings.setGroupIDMaxLen (nOld);
    }
  }

  @Test
  public void testMaxArtifactIDLen ()
  {
    assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRGlobalCoordinateSettings.getArtifactIDMaxLen ();
    DVRGlobalCoordinateSettings.setArtifactIDMaxLen (1);
    try
    {
      // Too long
      assertNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRCoordinate.parseOrNull ("group:a:1.0:classifier"));
    }
    finally
    {
      DVRGlobalCoordinateSettings.setArtifactIDMaxLen (nOld);
    }
  }

  @Test
  public void testMaxVersionLen ()
  {
    assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRGlobalCoordinateSettings.getVersionMaxLen ();
    DVRGlobalCoordinateSettings.setVersionMaxLen (1);
    try
    {
      // Too long
      assertNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1:classifier"));
    }
    finally
    {
      DVRGlobalCoordinateSettings.setVersionMaxLen (nOld);
    }
  }

  @Test
  public void testMaxClassifierLen ()
  {
    assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
    final int nOld = DVRGlobalCoordinateSettings.getClassifierMaxLen ();
    DVRGlobalCoordinateSettings.setClassifierMaxLen (1);
    try
    {
      // Too long
      assertNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:classifier"));
      // Valid
      assertNotNull (DVRCoordinate.parseOrNull ("group:artifact:1.0:c"));
    }
    finally
    {
      DVRGlobalCoordinateSettings.setClassifierMaxLen (nOld);
    }
  }
}
