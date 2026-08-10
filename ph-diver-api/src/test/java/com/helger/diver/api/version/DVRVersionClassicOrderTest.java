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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;

/**
 * Test class for {@link DVRVersion#compareToClassic(DVRVersion)} - the ordering as it was up to
 * v4.2.1, where "SNAPSHOT" was the only pre-release qualifier.
 *
 * @author Philip Helger
 */
public final class DVRVersionClassicOrderTest
{
  private static final Comparator <DVRVersion> CLASSIC = DVRVersion::compareToClassic;

  @NonNull
  private static DVRVersion _parse (@NonNull final String sVersion)
  {
    final DVRVersion ret = DVRVersion.parseOrNull (sVersion);
    assertNotNull ("Failed to parse '" + sVersion + "'", ret);
    return ret;
  }

  /**
   * Assert that the provided versions are in strictly ascending order according to the classic
   * comparison. Every pair is checked in both directions, so this also covers antisymmetry and
   * transitivity.
   *
   * @param aVersions
   *        The versions in the expected ascending order.
   */
  private static void _assertStrictlyAscendingClassic (@NonNull final String... aVersions)
  {
    final ICommonsList <DVRVersion> aList = new CommonsArrayList <> ();
    for (final String s : aVersions)
      aList.add (_parse (s));

    for (int i = 0; i < aList.size (); ++i)
      for (int j = 0; j < aList.size (); ++j)
      {
        final int nCmp = aList.get (i).compareToClassic (aList.get (j));
        final String sMsg = "'" + aVersions[i] + "' <=> '" + aVersions[j] + "' resulted in " + nCmp;
        if (i < j)
          assertTrue (sMsg, nCmp < 0);
        else
          if (i > j)
            assertTrue (sMsg, nCmp > 0);
          else
            assertEquals (sMsg, 0, nCmp);
      }

    // Sorting a scrambled copy must lead to the original order
    final ICommonsList <DVRVersion> aScrambled = new CommonsArrayList <> ();
    for (int i = 0; i < aList.size (); ++i)
      aScrambled.add (aList.get (i * 17 % aList.size ()));
    assertEquals (aList.size (), aScrambled.size ());
    aScrambled.sort (CLASSIC);
    assertEquals (aList, aScrambled);

    // All versions must remain distinguishable in a sorted set
    final ICommonsSortedSet <DVRVersion> aSorted = new CommonsTreeSet <> (CLASSIC);
    aSorted.addAll (aList);
    assertEquals (aList.size (), aSorted.size ());
    assertEquals (aList, aSorted.getCopyAsList ());
  }

  @Test
  public void testSnapshotIsTheOnlyPreRelease ()
  {
    // SNAPSHOT still comes before the release ...
    _assertStrictlyAscendingClassic ("0.9.9", "1.0.0-SNAPSHOT", "1.0.0", "1.0.1");

    // ... but everything else comes after it, including all the qualifiers that
    // became pre-release qualifiers in v4.2.2
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-alpha1");
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-beta1");
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-milestone1");
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-rc1");
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-RC2");
  }

  @Test
  public void testSnapshotIsCaseSensitive ()
  {
    // In the classic ordering only the exact spelling "SNAPSHOT" is a snapshot,
    // everything else is an ordinary qualifier that comes after the release
    _assertStrictlyAscendingClassic ("1.0.0-SNAPSHOT", "1.0.0", "1.0.0-Snapshot", "1.0.0-snapshot");
  }

  @Test
  public void testQualifiersAreComparedAsStrings ()
  {
    // No numeric comparison of the trailing number
    _assertStrictlyAscendingClassic ("1.0.0", "1.0.0-rc1", "1.0.0-rc10", "1.0.0-rc2", "1.0.0-rc9");

    // Uppercase before lowercase, as always for Strings
    _assertStrictlyAscendingClassic ("1.0.0", "1.0.0-RC1", "1.0.0-rc1");

    // The zero padding rule applies to every qualifier here
    _assertStrictlyAscendingClassic ("2.0.3", "2.0.3-01", "2.0.3-09", "2.0.3-13", "2.0.3-9");
  }

  @Test
  public void testUnknownQualifiersAreUnchanged ()
  {
    // These behave identically in both orderings
    _assertStrictlyAscendingClassic ("1.3.5", "1.3.6", "1.3.6-a", "1.3.6-b", "1.3.7");
    _assertStrictlyAscendingClassic ("1.3.1", "1.4.0", "1.4.0-03", "1.4.1");
    _assertStrictlyAscendingClassic ("1.4.0", "1.4.0-hotfix01", "1.4.0-hotfix02");
  }

  @Test
  public void testNumericPartsWinOverQualifier ()
  {
    _assertStrictlyAscendingClassic ("0.9.9-zzz", "1.0.0-SNAPSHOT", "1.0.0", "1.0.0-zzz", "1.0.1-SNAPSHOT");
    _assertStrictlyAscendingClassic ("1.2.3", "1.2.4-SNAPSHOT", "1.2.4", "1.2.4-rc1");
  }

  @Test
  public void testPseudoVersionsAreUnaffected ()
  {
    // The pseudo version handling is shared by both comparisons
    final DVRVersion aOldest = DVRVersion.of (DVRPseudoVersionRegistry.OLDEST);
    final DVRVersion aLatest = DVRVersion.of (DVRPseudoVersionRegistry.LATEST);
    final DVRVersion aLatestRelease = DVRVersion.of (DVRPseudoVersionRegistry.LATEST_RELEASE);

    for (final String s : new String [] { "1.0.0-SNAPSHOT", "1.0.0-alpha1", "1.0.0-rc1", "1.0.0" })
    {
      final DVRVersion aVer = _parse (s);
      assertTrue (s, aOldest.compareToClassic (aVer) < 0);
      assertTrue (s, aVer.compareToClassic (aOldest) > 0);
      assertTrue (s, aLatest.compareToClassic (aVer) > 0);
      assertTrue (s, aVer.compareToClassic (aLatest) < 0);
      assertTrue (s, aLatestRelease.compareToClassic (aVer) > 0);
      assertTrue (s, aVer.compareToClassic (aLatestRelease) < 0);

      // Identical to the current comparison
      assertEquals (s, aOldest.compareTo (aVer), aOldest.compareToClassic (aVer));
      assertEquals (s, aVer.compareTo (aLatest), aVer.compareToClassic (aLatest));
    }

    // Pseudo version vs. pseudo version
    assertEquals (aOldest.compareTo (aLatest), aOldest.compareToClassic (aLatest));
    assertEquals (0, aLatest.compareToClassic (aLatest));
  }

  @Test
  public void testDiffersFromCurrentOnlyForPreReleases ()
  {
    // Where no pre-release qualifier other than the exact "SNAPSHOT" is
    // involved, both comparisons must agree on every pair
    final String [] aNeutral = { "0.9.9",
                                 "1.0.0-SNAPSHOT",
                                 "1.0.0",
                                 "1.0.0-01",
                                 "1.0.0-1",
                                 "1.0.0-9",
                                 "1.0.0-13",
                                 "1.0.0-a",
                                 "1.0.0-b",
                                 "1.0.0-alphax",
                                 "1.0.0-hotfix03",
                                 "1.0.0-zzz",
                                 "1.0.1",
                                 "2.0.0" };
    for (final String sLhs : aNeutral)
      for (final String sRhs : aNeutral)
      {
        final DVRVersion aLhs = _parse (sLhs);
        final DVRVersion aRhs = _parse (sRhs);
        final String sMsg = "'" + sLhs + "' <=> '" + sRhs + "'";
        assertEquals (sMsg,
                      Integer.signum (aLhs.compareTo (aRhs)),
                      Integer.signum (aLhs.compareToClassic (aRhs)));
      }
  }

  @Test
  public void testCurrentAndClassicDisagreeOnPreReleases ()
  {
    // The whole point of the classic comparison - the two orderings are
    // inverted for the pre-release qualifiers introduced in v4.2.2
    for (final String sQualifier : new String [] { "alpha", "alpha1", "beta", "beta7", "milestone", "milestone2", "rc",
                                                   "rc1", "RC2", "snapshot" })
    {
      final DVRVersion aPre = _parse ("1.0.0-" + sQualifier);
      final DVRVersion aRelease = _parse ("1.0.0");
      final String sMsg = "1.0.0-" + sQualifier;
      assertTrue (sMsg, aPre.compareTo (aRelease) < 0);
      assertTrue (sMsg, aPre.compareToClassic (aRelease) > 0);
    }

    // "rc9" vs. "rc10" is compared numerically now and as a String before
    final DVRVersion aRC9 = _parse ("1.0.0-rc9");
    final DVRVersion aRC10 = _parse ("1.0.0-rc10");
    assertTrue (aRC9.compareTo (aRC10) < 0);
    assertTrue (aRC9.compareToClassic (aRC10) > 0);

    // The exact spelling "SNAPSHOT" behaves identically in both
    final DVRVersion aSnapshot = _parse ("1.0.0-SNAPSHOT");
    final DVRVersion aRelease = _parse ("1.0.0");
    assertTrue (aSnapshot.compareTo (aRelease) < 0);
    assertTrue (aSnapshot.compareToClassic (aRelease) < 0);
  }

  @Test
  public void testClassicMatchesPreviousImplementation ()
  {
    // Cross check against the implementation of com.helger.base.version.Version,
    // which the classic comparison delegates to for all non-snapshot versions
    final String [] aVersions = { "0.9.9", "1.0.0", "1.0.0-01", "1.0.0-a", "1.0.0-rc1", "1.0.0-zzz", "1.0.1", "2.0.0" };
    for (final String sLhs : aVersions)
      for (final String sRhs : aVersions)
      {
        final DVRVersion aLhs = _parse (sLhs);
        final DVRVersion aRhs = _parse (sRhs);
        assertEquals ("'" + sLhs + "' <=> '" + sRhs + "'",
                      Integer.signum (aLhs.getStaticVersion ().compareTo (aRhs.getStaticVersion ())),
                      Integer.signum (aLhs.compareToClassic (aRhs)));
      }
  }
}
