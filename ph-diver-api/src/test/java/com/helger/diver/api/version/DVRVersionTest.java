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
package com.helger.diver.api.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.base.version.Version;
import com.helger.unittest.support.TestHelper;

/**
 * Test class for class {@link DVRVersion}.
 *
 * @author Philip Helger
 */
public final class DVRVersionTest
{
  @Test
  public void testBasic ()
  {
    // Valid static
    DVRVersion ver = DVRVersion.parseOrNull ("1.2.3");
    assertNotNull (ver);
    assertTrue (ver.isStaticVersion ());
    assertNotNull (ver.getStaticVersion ());
    assertFalse (ver.isPseudoVersion ());
    assertNull (ver.getPseudoVersion ());
    assertEquals ("1.2.3", ver.getAsString ());

    // Valid static
    ver = DVRVersion.parseOrNull ("1.2.3.a");
    assertNotNull (ver);
    assertTrue (ver.isStaticVersion ());
    assertNotNull (ver.getStaticVersion ());
    assertFalse (ver.isPseudoVersion ());
    assertNull (ver.getPseudoVersion ());
    assertEquals ("1.2.3-a", ver.getAsString ());

    // Valid static
    ver = DVRVersion.parseOrNull ("1.2.3.4.5.6.7.8");
    assertNotNull (ver);
    assertTrue (ver.isStaticVersion ());
    assertNotNull (ver.getStaticVersion ());
    assertFalse (ver.isPseudoVersion ());
    assertNull (ver.getPseudoVersion ());
    assertEquals ("1.2.3-4.5.6.7.8", ver.getAsString ());

    // Valid static
    ver = DVRVersion.parseOrNull ("1.2.3-a");
    assertNotNull (ver);
    assertTrue (ver.isStaticVersion ());
    assertNotNull (ver.getStaticVersion ());
    assertFalse (ver.isPseudoVersion ());
    assertNull (ver.getPseudoVersion ());
    assertEquals ("1.2.3-a", ver.getAsString ());

    // Invalid static
    ver = DVRVersion.parseOrNull ("0.09.5");
    assertNull (ver);

    // Valid pseudo version
    ver = DVRVersion.parseOrNull ("latest");
    assertNotNull (ver);
    assertFalse (ver.isStaticVersion ());
    assertNull (ver.getStaticVersion ());
    assertTrue (ver.isPseudoVersion ());
    assertNotNull (ver.getPseudoVersion ());
    assertEquals ("latest", ver.getAsString ());

    // Invalid pseudo version
    ver = DVRVersion.parseOrNull ("blafoo");
    assertNotNull (ver);
    assertTrue (ver.isStaticVersion ());
    assertNotNull (ver.getStaticVersion ());
    assertFalse (ver.isPseudoVersion ());
    assertNull (ver.getPseudoVersion ());
    assertEquals ("blafoo", ver.getAsString ());

    final Version aVer = ver.getStaticVersion ();
    assertEquals (0, aVer.getMajor ());
    assertEquals (0, aVer.getMinor ());
    assertEquals (0, aVer.getMicro ());
    assertEquals ("blafoo", aVer.getQualifier ());
  }

  @Test
  public void testGetAsString ()
  {
    assertEquals ("1.2.3-bla", DVRVersion.parseOrNull ("1.2.3.bla").getAsString ());
    assertEquals ("1.2.3", DVRVersion.parseOrNull ("1.2.3").getAsString ());
    assertEquals ("1.2", DVRVersion.parseOrNull ("1.2").getAsString ());
    assertEquals ("1", DVRVersion.parseOrNull ("1").getAsString ());
    assertEquals ("1", DVRVersion.parseOrNull ("1.0").getAsString ());
    assertEquals ("1", DVRVersion.parseOrNull ("1.0.0").getAsString ());
    assertEquals ("0.1", DVRVersion.parseOrNull ("0.1").getAsString ());
  }

  @Test
  public void testZero ()
  {
    assertEquals ("0", DVRVersion.parseOrNull ("0").getAsString ());
    assertEquals ("0", DVRVersion.parseOrNull ("0.0").getAsString ());
    assertEquals ("0", DVRVersion.parseOrNull ("0.0.0").getAsString ());
  }

  @Test
  public void testEqualsHashcode ()
  {
    final DVRVersion ver = DVRVersion.parseOrNull ("1.2.3");
    TestHelper.testEqualsImplementationWithEqualContentObject (ver, DVRVersion.parseOrNull ("1.2.3"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (ver, DVRVersion.parseOrNull ("1.2.4"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (ver, DVRVersion.parseOrNull ("1.1.3"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (ver, DVRVersion.parseOrNull ("2.2.3"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (ver, DVRVersion.parseOrNull ("1.2"));
    TestHelper.testEqualsImplementationWithDifferentContentObject (ver, DVRVersion.parseOrNull ("1.2.3.bla"));
  }

  @Test
  public void testCompare ()
  {
    final DVRVersion ver0 = DVRVersion.of (DVRPseudoVersionRegistry.OLDEST);
    final DVRVersion ver1 = DVRVersion.parseOrNull ("1.2");
    final DVRVersion ver2 = DVRVersion.parseOrNull ("1.2.3");
    final DVRVersion ver3 = DVRVersion.parseOrNull ("1.2.4");
    final DVRVersion ver4 = DVRVersion.parseOrNull ("1.3");
    final DVRVersion ver5 = DVRVersion.parseOrNull ("2023.5");
    final DVRVersion ver6 = DVRVersion.of (DVRPseudoVersionRegistry.LATEST_RELEASE);
    final DVRVersion ver7 = DVRVersion.of (DVRPseudoVersionRegistry.LATEST);

    assertEquals (0, ver0.compareTo (ver0));
    assertTrue (ver0.compareTo (ver1) < 0);
    assertTrue (ver0.compareTo (ver2) < 0);
    assertTrue (ver0.compareTo (ver3) < 0);
    assertTrue (ver0.compareTo (ver4) < 0);
    assertTrue (ver0.compareTo (ver5) < 0);
    assertTrue (ver0.compareTo (ver6) < 0);
    assertTrue (ver0.compareTo (ver7) < 0);

    assertTrue (ver1.compareTo (ver0) > 0);
    assertEquals (0, ver1.compareTo (ver1));
    assertTrue (ver1.compareTo (ver2) < 0);
    assertTrue (ver1.compareTo (ver3) < 0);
    assertTrue (ver1.compareTo (ver4) < 0);
    assertTrue (ver1.compareTo (ver5) < 0);
    assertTrue (ver1.compareTo (ver6) < 0);
    assertTrue (ver1.compareTo (ver7) < 0);

    assertTrue (ver2.compareTo (ver0) > 0);
    assertTrue (ver2.compareTo (ver1) > 0);
    assertEquals (0, ver2.compareTo (ver2));
    assertTrue (ver2.compareTo (ver3) < 0);
    assertTrue (ver2.compareTo (ver4) < 0);
    assertTrue (ver2.compareTo (ver5) < 0);
    assertTrue (ver2.compareTo (ver6) < 0);
    assertTrue (ver2.compareTo (ver7) < 0);

    assertTrue (ver3.compareTo (ver0) > 0);
    assertTrue (ver3.compareTo (ver1) > 0);
    assertTrue (ver3.compareTo (ver2) > 0);
    assertEquals (0, ver3.compareTo (ver3));
    assertTrue (ver3.compareTo (ver4) < 0);
    assertTrue (ver3.compareTo (ver5) < 0);
    assertTrue (ver3.compareTo (ver6) < 0);
    assertTrue (ver3.compareTo (ver7) < 0);

    assertTrue (ver4.compareTo (ver0) > 0);
    assertTrue (ver4.compareTo (ver1) > 0);
    assertTrue (ver4.compareTo (ver2) > 0);
    assertTrue (ver4.compareTo (ver3) > 0);
    assertEquals (0, ver4.compareTo (ver4));
    assertTrue (ver4.compareTo (ver5) < 0);
    assertTrue (ver4.compareTo (ver6) < 0);
    assertTrue (ver4.compareTo (ver7) < 0);

    assertTrue (ver5.compareTo (ver0) > 0);
    assertTrue (ver5.compareTo (ver1) > 0);
    assertTrue (ver5.compareTo (ver2) > 0);
    assertTrue (ver5.compareTo (ver3) > 0);
    assertTrue (ver5.compareTo (ver4) > 0);
    assertEquals (0, ver5.compareTo (ver5));
    assertTrue (ver5.compareTo (ver6) < 0);
    assertTrue (ver5.compareTo (ver7) < 0);

    assertTrue (ver6.compareTo (ver0) > 0);
    assertTrue (ver6.compareTo (ver1) > 0);
    assertTrue (ver6.compareTo (ver2) > 0);
    assertTrue (ver6.compareTo (ver3) > 0);
    assertTrue (ver6.compareTo (ver4) > 0);
    assertTrue (ver6.compareTo (ver5) > 0);
    assertEquals (0, ver6.compareTo (ver6));
    assertTrue (ver6.compareTo (ver7) < 0);

    assertTrue (ver7.compareTo (ver0) > 0);
    assertTrue (ver7.compareTo (ver1) > 0);
    assertTrue (ver7.compareTo (ver2) > 0);
    assertTrue (ver7.compareTo (ver3) > 0);
    assertTrue (ver7.compareTo (ver4) > 0);
    assertTrue (ver7.compareTo (ver5) > 0);
    assertTrue (ver7.compareTo (ver6) > 0);
    assertEquals (0, ver7.compareTo (ver7));
  }

  @Test
  public void testSnapshot ()
  {
    final DVRVersion ver1 = DVRVersion.parseOrNull ("0.9.9");
    final DVRVersion ver2 = DVRVersion.parseOrNull ("1.0.0-SNAPSHOT");
    final DVRVersion ver3 = DVRVersion.parseOrNull ("1.0.0");
    final DVRVersion ver4 = DVRVersion.parseOrNull ("1.0.1");

    assertTrue (ver1.compareTo (ver1) == 0);
    assertTrue (ver1.compareTo (ver2) < 0);
    assertTrue (ver1.compareTo (ver3) < 0);
    assertTrue (ver1.compareTo (ver4) < 0);

    assertTrue (ver2.compareTo (ver1) > 0);
    assertTrue (ver2.compareTo (ver2) == 0);
    assertTrue (ver2.compareTo (ver3) < 0);
    assertTrue (ver2.compareTo (ver4) < 0);

    assertTrue (ver3.compareTo (ver1) > 0);
    assertTrue (ver3.compareTo (ver2) > 0);
    assertTrue (ver3.compareTo (ver3) == 0);
    assertTrue (ver3.compareTo (ver4) < 0);

    assertTrue (ver4.compareTo (ver1) > 0);
    assertTrue (ver4.compareTo (ver2) > 0);
    assertTrue (ver4.compareTo (ver3) > 0);
    assertTrue (ver4.compareTo (ver4) == 0);
  }
}
