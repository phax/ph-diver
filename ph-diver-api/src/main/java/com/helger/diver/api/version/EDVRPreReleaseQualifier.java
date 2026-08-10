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

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.id.IHasID;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringParser;

/**
 * This enum contains the well known "pre-release" version qualifiers. A version using one of these
 * qualifiers is ordered BEFORE the respective final release version, in the order defined by
 * {@link #getRank()}:
 * <ol>
 * <li>1.0.0-SNAPSHOT</li>
 * <li>1.0.0-alpha1</li>
 * <li>1.0.0-beta1</li>
 * <li>1.0.0-milestone1</li>
 * <li>1.0.0-rc1</li>
 * <li>1.0.0 (the final release)</li>
 * </ol>
 * All qualifiers except {@link #SNAPSHOT} may be followed by a number, as in <code>rc2</code>. The
 * number is compared numerically, so that <code>rc9</code> is correctly ordered before
 * <code>rc10</code>. A qualifier without a number is ordered before the same qualifier with a
 * number, so <code>rc</code> comes before <code>rc1</code>.
 * <p>
 * The matching of these qualifiers is case <b>insensitive</b>, so <code>RC1</code>,
 * <code>rc1</code> and <code>Rc1</code> all have the same rank. This affects the <b>ordering</b>
 * only - the version itself stays case sensitive, hence <code>1.0.0-RC1</code> and
 * <code>1.0.0-rc1</code> are still two different versions.
 * <p>
 * A qualifier that is none of the constants below - like the revision letter in
 * <code>1.3.6-a</code> or the hotfix number in <code>1.4.0-03</code> - is not a pre-release
 * qualifier and is therefore ordered AFTER the respective final release version. Single letter
 * abbreviations like <code>a</code> for "alpha" or <code>b</code> for "beta" are deliberately NOT
 * supported, because they collide with such revision letters.
 *
 * @author Philip Helger
 * @since 4.2.2
 */
public enum EDVRPreReleaseQualifier implements IHasID <String>
{
  /** Work in progress, before anything else */
  SNAPSHOT ("snapshot", 10, false),
  /** Alpha version */
  ALPHA ("alpha", 20, true),
  /** Beta version */
  BETA ("beta", 30, true),
  /** Milestone version */
  MILESTONE ("milestone", 40, true),
  /** Release candidate */
  RC ("rc", 50, true);

  /** The number returned if a pre-release qualifier has no trailing number */
  public static final int NO_NUMBER = CGlobal.ILLEGAL_UINT;

  /**
   * The highest rank of all pre-release qualifiers. Every version that is not a pre-release version
   * must be ordered after this rank.
   *
   * @see #getRank()
   */
  public static final int MAX_RANK = RC.m_nRank;

  private final String m_sID;
  private final int m_nRank;
  private final boolean m_bNumberSupported;

  EDVRPreReleaseQualifier (@NonNull @Nonempty final String sID, final int nRank, final boolean bNumberSupported)
  {
    m_sID = sID;
    m_nRank = nRank;
    m_bNumberSupported = bNumberSupported;
  }

  /**
   * @return The lower case ID of this qualifier. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The sort rank of this qualifier. The lower the value, the earlier the version is
   *         ordered. All ranks are distinct and are lower than the rank of the final release
   *         version. The ranks are spread out, so that further qualifiers can be inserted in
   *         between later on, without changing the existing values.
   */
  public int getRank ()
  {
    return m_nRank;
  }

  /**
   * @return <code>true</code> if this qualifier may be followed by a number, <code>false</code> if
   *         it must be used standalone.
   */
  public boolean isNumberSupported ()
  {
    return m_bNumberSupported;
  }

  /**
   * @param s
   *        The string to check. May not be <code>null</code>.
   * @return <code>true</code> if the provided string consists of digits only and fits into an
   *         <code>int</code>.
   */
  private static boolean _isUnsignedIntDigitsOnly (@NonNull final String s)
  {
    if (s.isEmpty ())
      return false;
    for (int i = 0; i < s.length (); ++i)
    {
      final char c = s.charAt (i);
      if (c < '0' || c > '9')
        return false;
    }
    // Also ensure it fits into an int - "99999999999999" does not
    return StringParser.parseInt (s, NO_NUMBER) >= 0;
  }

  private boolean _matches (@NonNull final String sLowerCaseQualifier)
  {
    if (sLowerCaseQualifier.equals (m_sID))
      return true;
    if (!m_bNumberSupported)
      return false;
    if (!sLowerCaseQualifier.startsWith (m_sID))
      return false;
    return _isUnsignedIntDigitsOnly (sLowerCaseQualifier.substring (m_sID.length ()));
  }

  /**
   * Get the number of the provided version qualifier, assuming it belongs to this pre-release
   * qualifier.
   *
   * @param sQualifier
   *        The version qualifier to evaluate. May be <code>null</code>.
   * @return {@link #NO_NUMBER} if the provided qualifier does not belong to this pre-release
   *         qualifier or if it has no trailing number. The contained number otherwise.
   * @see #getFromQualifierOrNull(String)
   */
  public int getNumber (@Nullable final String sQualifier)
  {
    if (!m_bNumberSupported || StringHelper.isEmpty (sQualifier))
      return NO_NUMBER;

    final String sLC = sQualifier.toLowerCase (Locale.ROOT);
    if (!sLC.startsWith (m_sID))
      return NO_NUMBER;

    final String sRest = sLC.substring (m_sID.length ());
    if (!_isUnsignedIntDigitsOnly (sRest))
      return NO_NUMBER;
    return StringParser.parseInt (sRest, NO_NUMBER);
  }

  /**
   * Try to resolve the provided version qualifier to a well known pre-release qualifier. The
   * matching is case insensitive and the qualifier must match as a whole - optionally followed by a
   * number, if {@link #isNumberSupported()} is <code>true</code>.
   *
   * @param sQualifier
   *        The version qualifier to resolve. May be <code>null</code>.
   * @return <code>null</code> if the provided qualifier is empty or is not a well known pre-release
   *         qualifier.
   */
  @Nullable
  public static EDVRPreReleaseQualifier getFromQualifierOrNull (@Nullable final String sQualifier)
  {
    if (StringHelper.isEmpty (sQualifier))
      return null;

    final String sLC = sQualifier.toLowerCase (Locale.ROOT);
    for (final EDVRPreReleaseQualifier e : values ())
      if (e._matches (sLC))
        return e;
    return null;
  }
}
