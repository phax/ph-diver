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
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;

/**
 * Test class for the pre-release version qualifier ordering of {@link DVRVersion}.
 *
 * @author Philip Helger
 */
public final class DVRVersionPreReleaseOrderTest
{
  @NonNull
  private static DVRVersion _parse (@NonNull final String sVersion)
  {
    final DVRVersion ret = DVRVersion.parseOrNull (sVersion);
    assertNotNull ("Failed to parse '" + sVersion + "'", ret);
    return ret;
  }

  @NonNull
  private static ICommonsList <DVRVersion> _parseAll (@NonNull final String... aVersions)
  {
    final ICommonsList <DVRVersion> ret = new CommonsArrayList <> ();
    for (final String s : aVersions)
      ret.add (_parse (s));
    return ret;
  }

  /**
   * Assert that the provided versions are in strictly ascending order. Every pair is checked in
   * both directions, so this also covers antisymmetry and transitivity.
   *
   * @param aVersions
   *        The versions in the expected ascending order.
   */
  private static void _assertStrictlyAscending (@NonNull final String... aVersions)
  {
    final ICommonsList <DVRVersion> aList = _parseAll (aVersions);
    for (int i = 0; i < aList.size (); ++i)
      for (int j = 0; j < aList.size (); ++j)
      {
        final int nCmp = aList.get (i).compareTo (aList.get (j));
        final String sMsg = "'" + aVersions[i] + "' <=> '" + aVersions[j] + "' resulted in " + nCmp;
        if (i < j)
          assertTrue (sMsg, nCmp < 0);
        else
          if (i > j)
            assertTrue (sMsg, nCmp > 0);
          else
            assertEquals (sMsg, 0, nCmp);
      }

    // Sorting a scrambled copy must lead to the original order. Using a
    // deterministic permutation - gcd (17, size) is 1 for all sizes used here,
    // so this really is a permutation
    final ICommonsList <DVRVersion> aScrambled = new CommonsArrayList <> ();
    for (int i = 0; i < aList.size (); ++i)
      aScrambled.add (aList.get (i * 17 % aList.size ()));
    assertEquals (aList.size (), aScrambled.size ());
    Collections.sort (aScrambled);
    assertEquals (aList, aScrambled);

    // All versions must remain distinguishable in a sorted set - if two of them
    // compared as equal, one would silently disappear
    final ICommonsSortedSet <DVRVersion> aSorted = new CommonsTreeSet <> (aList);
    assertEquals (aList.size (), aSorted.size ());
    assertEquals (aList, aSorted.getCopyAsList ());
  }

  @Test
  public void testFullQualifierChain ()
  {
    // The complete ordering of one and the same numeric version
    _assertStrictlyAscending ("1.0.0-SNAPSHOT",
                              "1.0.0-alpha",
                              "1.0.0-alpha0",
                              "1.0.0-alpha1",
                              "1.0.0-alpha2",
                              "1.0.0-alpha9",
                              "1.0.0-alpha10",
                              "1.0.0-beta",
                              "1.0.0-beta1",
                              "1.0.0-beta20",
                              "1.0.0-milestone",
                              "1.0.0-milestone1",
                              "1.0.0-milestone2",
                              "1.0.0-rc",
                              "1.0.0-rc1",
                              "1.0.0-rc2",
                              "1.0.0-rc9",
                              "1.0.0-rc10",
                              "1.0.0-rc100",
                              // The final release
                              "1.0.0",
                              // Everything unknown comes after the release and
                              // keeps being compared as a String
                              "1.0.0-01",
                              "1.0.0-1",
                              "1.0.0-a",
                              "1.0.0-alphax",
                              "1.0.0-hotfix03",
                              "1.0.0-zzz");
  }

  @Test
  public void testNumericPartsWinOverQualifier ()
  {
    _assertStrictlyAscending ("0.9.9-zzz", "1.0.0-SNAPSHOT", "1.0.0-rc1", "1.0.0", "1.0.0-zzz", "1.0.1-SNAPSHOT");

    // A release candidate of the next version is still newer than the previous
    // release
    _assertStrictlyAscending ("1.2.3", "1.2.4-alpha1", "1.2.4-rc1", "1.2.4");

    _assertStrictlyAscending ("0.9.0-rc", "1.0.0-alpha");
  }

  @Test
  public void testRcBeforeRelease ()
  {
    // The actual reason for this feature
    _assertStrictlyAscending ("1.0.0-RC2", "1.0.0");
    _assertStrictlyAscending ("1.15.0-rc", "1.15.0");
    _assertStrictlyAscending ("1.15.0-rc", "1.15.0", "1.15.1");
  }

  @Test
  public void testCaseInsensitiveRank ()
  {
    // Different spellings must all have the same rank, hence they are all
    // sorted between "beta9" and the release
    for (final String sRC : new String [] { "RC1", "rc1", "Rc1", "rC1" })
      _assertStrictlyAscending ("1.0.0-beta9", "1.0.0-" + sRC, "1.0.0");

    for (final String sSnapshot : new String [] { "SNAPSHOT", "snapshot", "Snapshot", "SnApShOt" })
      _assertStrictlyAscending ("0.9.9", "1.0.0-" + sSnapshot, "1.0.0-alpha", "1.0.0");

    for (final String sAlpha : new String [] { "ALPHA", "alpha", "Alpha" })
      _assertStrictlyAscending ("1.0.0-SNAPSHOT", "1.0.0-" + sAlpha, "1.0.0-beta", "1.0.0");

    for (final String sMilestone : new String [] { "MILESTONE2", "milestone2", "MileStone2" })
      _assertStrictlyAscending ("1.0.0-beta", "1.0.0-" + sMilestone, "1.0.0-rc", "1.0.0");
  }

  @Test
  public void testDifferentSpellingsAreNeverEqual ()
  {
    // Same rank and same number, but different Strings. They must not compare
    // as equal, otherwise they would collapse in a sorted set or map
    _assertStrictlyAscending ("1.0.0-RC1", "1.0.0-Rc1", "1.0.0-rC1", "1.0.0-rc1");
    _assertStrictlyAscending ("1.0.0-SNAPSHOT", "1.0.0-Snapshot", "1.0.0-snapshot");

    // Leading zeroes are irrelevant for the rank, but the versions are still
    // different
    _assertStrictlyAscending ("1.0.0-rc01", "1.0.0-rc1");
    _assertStrictlyAscending ("1.0.0-rc001", "1.0.0-rc01", "1.0.0-rc1");

    // Cross check: the versions really are not equal
    assertFalse (_parse ("1.0.0-RC1").equals (_parse ("1.0.0-rc1")));
    assertFalse (_parse ("1.0.0-rc01").equals (_parse ("1.0.0-rc1")));
  }

  @Test
  public void testUnknownQualifiersKeepStringOrder ()
  {
    // These are real world coordinates from phive-rules that must not be
    // reinterpreted as pre-release versions

    // "a" is a revision letter, not "alpha"
    _assertStrictlyAscending ("1.3.6", "1.3.6-a");
    _assertStrictlyAscending ("1.3.5", "1.3.6", "1.3.6-a", "1.3.7");

    // "b" is a revision letter, not "beta"
    _assertStrictlyAscending ("1.3.6", "1.3.6-a", "1.3.6-b");

    // A numeric hotfix number supersedes the release
    _assertStrictlyAscending ("1.4.0", "1.4.0-03");
    _assertStrictlyAscending ("1.3.1", "1.4.0", "1.4.0-03", "1.4.1");

    // Zero padded numeric classifiers - see phax/phive-rules#80
    _assertStrictlyAscending ("2.0.3", "2.0.3-01", "2.0.3-02", "2.0.3-09", "2.0.3-10", "2.0.3-13");

    // Unpadded numeric classifiers are still compared as Strings, which is
    // exactly why the padding is needed
    _assertStrictlyAscending ("2.0.3-13", "2.0.3-9");

    // Other qualifiers seen in the wild
    _assertStrictlyAscending ("1.4.0", "1.4.0-hotfix01", "1.4.0-hotfix02");
  }

  @Test
  public void testAlphaXIsNotAnAlpha ()
  {
    // "alphax" is not a pre-release qualifier. The keyword must match as a
    // whole, optionally followed by digits only - any other trailing text makes
    // it an ordinary qualifier
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alphax"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("ALPHAX"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("AlphaX"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alphax1"));
    assertNull (EDVRPreReleaseQualifier.getFromQualifierOrNull ("alpha1x"));
    assertEquals (EDVRPreReleaseQualifier.NO_NUMBER, EDVRPreReleaseQualifier.ALPHA.getNumber ("alphax"));

    // Therefore "1.0.0-alphax" is ordered AFTER the release "1.0.0" ...
    _assertStrictlyAscending ("1.0.0", "1.0.0-alphax");

    // ... whereas a real "1.0.0-alpha" is ordered BEFORE the release
    _assertStrictlyAscending ("1.0.0-alpha", "1.0.0");

    // Both side by side
    _assertStrictlyAscending ("1.0.0-alpha", "1.0.0-alpha1", "1.0.0", "1.0.0-alphax");

    // The case insensitive matching must not change that
    _assertStrictlyAscending ("1.0.0-ALPHA", "1.0.0", "1.0.0-ALPHAX");

    // It is not a SNAPSHOT either, so it is treated like any other release and
    // is accepted by the "latest-release" pseudo version
    final DVRVersion aVer = _parse ("1.0.0-alphax");
    assertFalse (aVer.isStaticSnapshotVersion ());
    assertTrue (DVRVersion.getStaticVersionAcceptor (null, false).test (aVer));

    // Being an ordinary qualifier, it keeps being compared as a String
    _assertStrictlyAscending ("1.0.0-alphax", "1.0.0-alphay", "1.0.0-alphaz");
  }

  @Test
  public void testPartialKeywordsAreNotPreReleases ()
  {
    // None of these is a pre-release qualifier, hence they all sort after the
    // release, in String order
    _assertStrictlyAscending ("1.0.0", "1.0.0-alphabet");
    _assertStrictlyAscending ("1.0.0", "1.0.0-betamax");
    _assertStrictlyAscending ("1.0.0", "1.0.0-rcx");
    _assertStrictlyAscending ("1.0.0", "1.0.0-snapshot1");
    _assertStrictlyAscending ("1.0.0", "1.0.0-rc1a");

    // A separated number is not supported
    _assertStrictlyAscending ("1.0.0", "1.0.0-rc.1");

    // But the plain keyword still is
    _assertStrictlyAscending ("1.0.0-rc", "1.0.0", "1.0.0-rc.1");
  }

  @Test
  public void testMixedLargeSetSorting ()
  {
    // A larger set that spans several numeric versions, so that the sorting
    // really exercises the merge path of the sort algorithm
    _assertStrictlyAscending ("1.0.0-SNAPSHOT",
                              "1.0.0-alpha1",
                              "1.0.0-alpha10",
                              "1.0.0-beta1",
                              "1.0.0-milestone1",
                              "1.0.0-rc1",
                              "1.0.0-rc10",
                              "1.0.0",
                              "1.0.0-01",
                              "1.0.0-a",
                              "1.0.1-SNAPSHOT",
                              "1.0.1-alpha1",
                              "1.0.1-alpha10",
                              "1.0.1-beta1",
                              "1.0.1-milestone1",
                              "1.0.1-rc1",
                              "1.0.1-rc10",
                              "1.0.1",
                              "1.0.1-01",
                              "1.0.1-a",
                              "1.1.0-SNAPSHOT",
                              "1.1.0-alpha1",
                              "1.1.0-alpha10",
                              "1.1.0-beta1",
                              "1.1.0-milestone1",
                              "1.1.0-rc1",
                              "1.1.0-rc10",
                              "1.1.0",
                              "1.1.0-01",
                              "1.1.0-a",
                              "2.0.0-SNAPSHOT",
                              "2.0.0-alpha1",
                              "2.0.0-alpha10",
                              "2.0.0-beta1",
                              "2.0.0-milestone1",
                              "2.0.0-rc1",
                              "2.0.0-rc10",
                              "2.0.0",
                              "2.0.0-01",
                              "2.0.0-a");
  }

  @Test
  public void testPseudoVersionsUnaffected ()
  {
    // A pre-release is still a static version, so all pseudo version rules
    // continue to apply
    final DVRVersion aOldest = DVRVersion.of (DVRPseudoVersionRegistry.OLDEST);
    final DVRVersion aLatest = DVRVersion.of (DVRPseudoVersionRegistry.LATEST);
    final DVRVersion aLatestRelease = DVRVersion.of (DVRPseudoVersionRegistry.LATEST_RELEASE);

    for (final String s : new String [] { "1.0.0-SNAPSHOT", "1.0.0-alpha1", "1.0.0-rc1", "1.0.0" })
    {
      final DVRVersion aVer = _parse (s);
      assertTrue (s, aOldest.compareTo (aVer) < 0);
      assertTrue (s, aVer.compareTo (aOldest) > 0);
      assertTrue (s, aLatest.compareTo (aVer) > 0);
      assertTrue (s, aVer.compareTo (aLatest) < 0);
      assertTrue (s, aLatestRelease.compareTo (aVer) > 0);
      assertTrue (s, aVer.compareTo (aLatestRelease) < 0);
    }
  }

  @Test
  public void testIsStaticSnapshotVersion ()
  {
    // The SNAPSHOT detection is case insensitive as well
    assertTrue (_parse ("1.0.0-SNAPSHOT").isStaticSnapshotVersion ());
    assertTrue (_parse ("1.0.0-snapshot").isStaticSnapshotVersion ());
    assertTrue (_parse ("1.0.0-Snapshot").isStaticSnapshotVersion ());
    assertTrue (DVRVersion.isStaticSnapshotVersion ("SNAPSHOT"));
    assertTrue (DVRVersion.isStaticSnapshotVersion ("snapshot"));

    // Everything else is not a snapshot
    assertFalse (_parse ("1.0.0").isStaticSnapshotVersion ());
    assertFalse (_parse ("1.0.0-alpha1").isStaticSnapshotVersion ());
    assertFalse (_parse ("1.0.0-rc1").isStaticSnapshotVersion ());
    assertFalse (_parse ("1.0.0-snapshot1").isStaticSnapshotVersion ());
    assertFalse (DVRVersion.isStaticSnapshotVersion ((String) null));
    assertFalse (DVRVersion.isStaticSnapshotVersion (""));
  }

  @Test
  public void testStaticVersionAcceptorOnlyFiltersSnapshots ()
  {
    // Note: "latest-release" excludes SNAPSHOT versions only. The other
    // pre-release qualifiers are deliberately NOT excluded
    final var aAcceptor = DVRVersion.getStaticVersionAcceptor (null, false);
    assertFalse (aAcceptor.test (_parse ("1.0.0-SNAPSHOT")));
    assertFalse (aAcceptor.test (_parse ("1.0.0-snapshot")));
    assertTrue (aAcceptor.test (_parse ("1.0.0-alpha1")));
    assertTrue (aAcceptor.test (_parse ("1.0.0-rc1")));
    assertTrue (aAcceptor.test (_parse ("1.0.0")));

    final var aAcceptorAll = DVRVersion.getStaticVersionAcceptor (null, true);
    assertTrue (aAcceptorAll.test (_parse ("1.0.0-SNAPSHOT")));
    assertTrue (aAcceptorAll.test (_parse ("1.0.0-rc1")));
  }

  @Test
  public void testStringRepresentationIsUnchanged ()
  {
    // The ordering is case insensitive, but the version itself is not touched -
    // the original spelling must be preserved and stay round trip safe
    for (final String sQualifier : new String [] { "SNAPSHOT",
                                                   "snapshot",
                                                   "RC1",
                                                   "rc1",
                                                   "Alpha7",
                                                   "milestone2",
                                                   "03",
                                                   "a" })
    {
      final DVRVersion aVer = _parse ("1.4.0-" + sQualifier);
      assertEquals ("1.4-" + sQualifier, aVer.getAsString ());
      assertEquals (aVer, _parse (aVer.getAsString ()));
    }
  }
}
