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
package com.helger.diver.api.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for class {@link DVRPseudoVersionRegistry}
 *
 * @author Philip Helger
 */
public final class DVRPseudoVersionRegistryTest
{
  @Test
  public void testBasic ()
  {
    final DVRPseudoVersionRegistry a = DVRPseudoVersionRegistry.getInstance ();
    assertNotNull (a);

    assertEquals (3, a.size ());

    assertNotNull (a.getFromIDOrNull (DVRPseudoVersionRegistry.OLDEST.getID ()));
    assertNotNull (a.getFromIDOrNull (DVRPseudoVersionRegistry.LATEST_RELEASE.getID ()));
    assertNotNull (a.getFromIDOrNull (DVRPseudoVersionRegistry.LATEST.getID ()));
    assertNull (a.getFromIDOrNull ("hoppla"));
  }
}
