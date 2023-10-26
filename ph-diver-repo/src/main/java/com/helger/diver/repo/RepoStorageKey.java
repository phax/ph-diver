/*
 * Copyright (C) 2023 Philip Helger & ecosio
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
package com.helger.diver.repo;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.version.VESID;

/**
 * A key that identifies a single item to be exchanged. It is an abstract
 * interpretation of a combination of folder and filename.
 *
 * @author Philip Helger
 */
@Immutable
public final class RepoStorageKey
{
  public static final String FILENAME_DIVER_TOC_XML = "diver-toc.xml";
  public static final String SUFFIX_SHA256 = ".sha256";
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageKey.class);

  private final VESID m_aVESID;
  private final String m_sPath;

  private RepoStorageKey (@Nonnull final VESID aVESID, @Nonnull @Nonempty final String sPath)
  {
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.isTrue (aVESID.getVersionObj ().isStaticVersion (),
                          "VESID must use a static version to access a repository item");
    ValueEnforcer.notEmpty (sPath, "Path");
    ValueEnforcer.isFalse (sPath.startsWith ("/"), "Path should not start with a Slash");
    ValueEnforcer.isFalse (sPath.endsWith ("/"), "Path should not end with a Slash");

    m_aVESID = aVESID;
    m_sPath = sPath;
  }

  @Nonnull
  public VESID getVESID ()
  {
    return m_aVESID;
  }

  @Nonnull
  @Nonempty
  public String getPath ()
  {
    return m_sPath;
  }

  @Nonnull
  @ReturnsMutableCopy
  public RepoStorageKey getKeyHashSha256 ()
  {
    if (m_sPath.endsWith (SUFFIX_SHA256))
    {
      // Seems like a doubled hash key
      LOGGER.warn ("You are trying to create a RepoStorageKey SHA-256 of something that already seems to be a SHA-256 key: '" +
                   m_sPath +
                   "'");
    }
    return new RepoStorageKey (m_aVESID, m_sPath + SUFFIX_SHA256);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final RepoStorageKey rhs = (RepoStorageKey) o;
    return m_sPath.equals (rhs.m_sPath);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sPath).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Path", m_sPath).getToString ();
  }

  /**
   * Get the path representation of group ID and artifact ID only.
   *
   * @param sGroupID
   *        Group ID. May neither be <code>null</code> nor empty.
   * @param sArtifactID
   *        Artifact ID. May neither be <code>null</code> nor empty.
   * @return A path from group ID and artifact ID, ending with a trailing slash.
   *         Can never be <code>null</code> or empty.
   */
  @Nonnull
  @Nonempty
  public static String getPathOfGroupIDAndArtifactID (@Nonnull @Nonempty final String sGroupID,
                                                      @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    return sGroupID.replace ('.', '/') + "/" + sArtifactID + "/";
  }

  /**
   * Create a {@link RepoStorageKey} from the passed VESID and the file
   * extension. The algorithm is like this:
   * <code>sGroupID.replace ('.', '/') + "/" + sArtifactID + "/" + sVersion + "/" + sArtifactID + "-" + sVersion [+ "-" + sClassifier] + sFileExt</code>
   * which is basically
   * <code>group/artifact/version/artifact-version[-classifier].fileExtension</code>
   *
   * @param aVESID
   *        The VESID to convert. Considers an optionally present classifier.
   *        May not be <code>null</code>.
   * @param sFileExt
   *        The file extension to use. Must start with ".". May not be
   *        <code>null</code>.
   * @return Never <code>null</code>.
   */
  @Nonnull
  public static RepoStorageKey of (@Nonnull final VESID aVESID, @Nonnull @Nonempty final String sFileExt)
  {
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.isTrue (aVESID.getVersionObj ().isStaticVersion (),
                          "VESID must use a static version to access a repository item");
    ValueEnforcer.notEmpty (sFileExt, "FileExt");
    ValueEnforcer.isTrue ( () -> sFileExt.startsWith ("."), "FileExt must start with a dot");

    final String sGroupID = aVESID.getGroupID ();
    final String sArtifactID = aVESID.getArtifactID ();
    final String sVersion = aVESID.getVersionString ();
    final String sClassifier = aVESID.hasClassifier () ? "-" + aVESID.getClassifier () : "";
    return new RepoStorageKey (aVESID,
                               getPathOfGroupIDAndArtifactID (sGroupID, sArtifactID) +
                                       sVersion +
                                       "/" +
                                       sArtifactID +
                                       "-" +
                                       sVersion +
                                       sClassifier +
                                       sFileExt);
  }

  @Nonnull
  public static RepoStorageKey ofToc (@Nonnull @Nonempty final String sGroupID,
                                      @Nonnull @Nonempty final String sArtifactID)
  {
    // ToC per group and artifact
    return new RepoStorageKey (new VESID (sGroupID, sArtifactID, "0"),
                               getPathOfGroupIDAndArtifactID (sGroupID, sArtifactID) + FILENAME_DIVER_TOC_XML);
  }
}
