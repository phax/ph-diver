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
package com.helger.diver.api.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;

/**
 * Test class for class {@link EDVRPreReleaseQualifier}.
 *
 * @author Philip Helger
 */
public final class EDVRPreReleaseQualifierTest
{
  @Test
  public void testGetID ()
  {
    for (final EDVRPreReleaseQualifier e : EDVRPreReleaseQualifier.values ())
    {
      assertNotNull (e.getID ());
      assertTrue (e.getID ().length () > 0);
      // All IDs are lower case, because the matching lower cases the input
      assertEquals (e.getID (), e.getID ().toLowerCase (Locale.ROOT));
    }

    assertEquals ("snapshot", EDVRPreReleaseQualifier.SNAPSHOT.getID ());
    assertEquals ("alpha", EDVRPreReleaseQualifier.ALPHA.getID ());
    assertEquals ("beta", EDVRPreReleaseQualifier.BETA.getID ());
    assertEquals ("milestone", EDVRPreReleaseQualifier.MILESTONE.getID ());
    assertEquals ("rc", EDVRPreReleaseQualifier.RC.getID ());
  }

  @Test
  public void testRank ()
  {
    // The rank defines the ordering
    final EDVRPreReleaseQualifier [] aExpected = { EDVRPreReleaseQualifier.SNAPSHOT,
                                                   EDVRPreReleaseQualifier.ALPHA,
                                                   EDVRPreReleaseQualifier.BETA,
                                                   EDVRPreReleaseQualifier.MILESTONE,
                                                   EDVRPreReleaseQualifier.RC };
    assertEquals (aExpected.length, EDVRPreReleaseQualifier.values ().length);

    // All ranks must be strictly ascending in the expected order
    for (int i = 1; i < aExpected.length; ++i)
      assertTrue (aExpected[i - 1].getID () + " vs. " + aExpected[i].getID (),
                  aExpected[i - 1].getRank () < aExpected[i].getRank ());

    // All ranks must be distinct and positive
    final ICommonsSet <Integer> aRanks = new CommonsHashSet <> ();
    for (final EDVRPreReleaseQualifier e : EDVRPreReleaseQualifier.values ())
    {
      assertTrue (e.getID (), e.getRank () > 0);
      assertTrue ("Rank " + e.getRank () + " is used more than once", aRanks.add (Integer.valueOf (e.getRank ())));
    }
  }

  @Test
  public void testMaxRank ()
  {
    // MAX_RANK must be the highest rank of all qualifiers
    for (final EDVRPreReleaseQualifier e : EDVRPreReleaseQualifier.values ())
      assertTrue (e.getID (), e.getRank () <= EDVRPreReleaseQualifier.MAX_RANK);

    // ... and it must be reached by exactly one of them
    int nCount = 0;
    for (final EDVRPreReleaseQualifier e : EDVRPreReleaseQualifier.values ())
      if (e.getRank () == EDVRPreReleaseQualifier.MAX_RANK)
        nCount++;
    assertEquals (1, nCount);

    // The last pre-release qualifier before the release is the release
    // candidate
    assertEquals (EDVRPreReleaseQualifier.RC.getRank (), EDVRPreReleaseQualifier.MAX_RANK);
  }

  @Test
  public void testNumberSupported ()
  {
    // Only SNAPSHOT must be used standalone
    assertFalseNumber (EDVRPreReleaseQualifier.SNAPSHOT);
    assertTrue (EDVRPreReleaseQualifier.ALPHA.isNumberSupported ());
    assertTrue (EDVRPreReleaseQualifier.BETA.isNumberSupported ());
    assertTrue (EDVRPreReleaseQualifier.MILESTONE.isNumberSupported ());
    assertTrue (EDVRPreReleaseQualifier.RC.isNumberSupported ());
  }

  private static void assertFalseNumber (final EDVRPreReleaseQualifier e)
  {
    assertFalse (e.getID (), e.isNumberSupported ());
  }

  @Test
  public void testGetFromQualifierExactMatch ()
  {
    assertSame (EDVRPreReleaseQualifier.SNAPSHOT, EDVRPreReleaseQualifier.getFromQualifierOrNull ("snapshot"));
    assertSame (EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha"));
    assertSame (EDVRPreReleaseQualifier.BETA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("beta"));
    assertSame (EDVRPreReleaseQualifier.MILESTONE, EDVRPreReleaseQualifier.getFromQualifierOrNull ("milestone"));
    assertSame (EDVRPreReleaseQualifier.RC, EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc"));
  }

  @Test
  public void testGetFromQualifierCaseInsensitive ()
  {
    for (final String s : new String [] { "SNAPSHOT", "snapshot", "Snapshot", "SnApShOt", "sNAPSHOT" })
      assertSame (s, EDVRPreReleaseQualifier.SNAPSHOT, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));

    for (final String s : new String [] { "RC", "rc", "Rc", "rC" })
      assertSame (s, EDVRPreReleaseQualifier.RC, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));

    for (final String s : new String [] { "RC2", "rc2", "Rc2", "rC2" })
      assertSame (s, EDVRPreReleaseQualifier.RC, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));

    for (final String s : new String [] { "ALPHA1", "alpha1", "Alpha1" })
      assertSame (s, EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));

    for (final String s : new String [] { "BETA", "beta", "BeTa" })
      assertSame (s, EDVRPreReleaseQualifier.BETA, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));

    for (final String s : new String [] { "MILESTONE", "milestone", "MileStone" })
      assertSame (s, EDVRPreReleaseQualifier.MILESTONE, EDVRPreReleaseQualifier.getFromQualifierOrNull (s));
  }

  @Test
  public void testGetFromQualifierCaseInsensitiveInTurkishLocale ()
  {
    // "MILESTONE" contains an "I". In the Turkish locale the lower case of "I"
    // is the dotless "i", so the matching must not use the default locale
    final Locale aOldDefault = Locale.getDefault ();
    try
    {
      Locale.setDefault (Locale.forLanguageTag ("tr"));
      assertSame (EDVRPreReleaseQualifier.MILESTONE, EDVRPreReleaseQualifier.getFromQualifierOrNull ("MILESTONE"));
      assertSame (EDVRPreReleaseQualifier.MILESTONE, EDVRPreReleaseQualifier.getFromQualifierOrNull ("MILESTONE3"));
      assertEquals (3, EDVRPreReleaseQualifier.MILESTONE.getNumber ("MILESTONE3"));
      assertTrue (DVRVersion.isStaticSnapshotVersion ("SNAPSHOT"));
    }
    finally
    {
      Locale.setDefault (aOldDefault);
    }
  }

  @Test
  public void testGetFromQualifierWithNumber ()
  {
    assertSame (EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha0"));
    assertSame (EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha1"));
    assertSame (EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha10"));
    assertSame (EDVRPreReleaseQualifier.ALPHA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha007"));
    assertSame (EDVRPreReleaseQualifier.BETA, EDVRPreReleaseQualifier.getFromQualifierOrNull ("beta99"));
    assertSame (EDVRPreReleaseQualifier.MILESTONE, EDVRPreReleaseQualifier.getFromQualifierOrNull ("milestone3"));
    assertSame (EDVRPreReleaseQualifier.RC, EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc123"));
  }

  @Test
  public void testGetFromQualifierNoMatch ()
  {
    // Empty
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull (null));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull (""));

    // Single letter abbreviations are deliberately not supported, because they
    // collide with revision letters like in "1.3.6-a"
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("a"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("b"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("m"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("a1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("b2"));

    // Partial keywords
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("al"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alph"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("mile"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("r"));

    // The keyword must match as a whole, no prefix or substring matching
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alphax"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("ALPHAX"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alphax1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha1x"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alphabet"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("betamax"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rcx"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc1a"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("xrc"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("prerc1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("snapshotty"));

    // Only digits may follow, no separator
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc-1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc.1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc_1"));

    // SNAPSHOT does not support a number
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("snapshot1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("SNAPSHOT2"));

    // A number that does not fit into an int
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("rc99999999999999"));

    // Plain qualifiers used in the wild
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("03"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("3"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("hotfix03"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("bla"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("4.5.6.7.8"));
  }

  @Test
  public void testGetNumber ()
  {
    // No number present
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber ("rc"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber ("RC"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.ALPHA.getNumber ("alpha"));

    // Number present
    assertEquals (0, EDVRPreReleaseQualifier.RC.getNumber ("rc0"));
    assertEquals (1, EDVRPreReleaseQualifier.RC.getNumber ("rc1"));
    assertEquals (2, EDVRPreReleaseQualifier.RC.getNumber ("RC2"));
    assertEquals (10, EDVRPreReleaseQualifier.RC.getNumber ("Rc10"));
    assertEquals (123, EDVRPreReleaseQualifier.RC.getNumber ("rc123"));
    assertEquals (7, EDVRPreReleaseQualifier.ALPHA.getNumber ("alpha007"));
    assertEquals (99, EDVRPreReleaseQualifier.BETA.getNumber ("BETA99"));
    assertEquals (3, EDVRPreReleaseQualifier.MILESTONE.getNumber ("milestone3"));

    // Leading zeroes are irrelevant for the number itself
    assertEquals (EDVRPreReleaseQualifier.RC.getNumber ("rc1"), EDVRPreReleaseQualifier.RC.getNumber ("rc01"));

    // Qualifier of a different pre-release qualifier
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber ("alpha1"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.ALPHA.getNumber ("rc1"));

    // SNAPSHOT never has a number
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.SNAPSHOT.getNumber ("snapshot"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.SNAPSHOT.getNumber ("snapshot1"));

    // Unparsable
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber (null));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber (""));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber ("rcx"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.RC.getNumber ("rc99999999999999"));
  }

  @Test
  public void testNoQualifierIsPrefixOfAnother ()
  {
    // The matching relies on the fact that no keyword is a prefix of another
    // one - otherwise the iteration order would matter
    for (final EDVRPreReleaseQualifier e1 : EDVRPreReleaseQualifier.values ())
      for (final EDVRPreReleaseQualifier e2 : EDVRPreReleaseQualifier.values ())
        if (e1 != e2)
          assertFalse (e1.getID () + " vs. " + e2.getID (), e1.getID ().startsWith (e2.getID ()));
  }
}
