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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.version.VESID;

/**
 * This is a specific storage key that not just contains a path but also a VESID
 * to uniquely identify the object.
 *
 * @author Philip Helger
 */
public class RepoStorageKeyOfArtefact extends RepoStorageKey
{
  /**
   * The default filename for the table of contents per group ID and artifact
   * ID.
   */
  public static final String FILENAME_TOC_DIVER_XML = "toc-diver.xml";

  public static final char GROUP_LEVEL_SEPARATOR = '.';

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageKeyOfArtefact.class);

  // Special fake version to be used by the ToC where we don't need any version
  private static final String TOC_VERSION = "0";

  private final VESID m_aVESID;

  private RepoStorageKeyOfArtefact (@Nonnull final VESID aVESID, @Nonnull @Nonempty final String sPath)
  {
    super (sPath);
    ValueEnforcer.notNull (aVESID, "VESID");
    ValueEnforcer.isTrue (aVESID.getVersionObj ().isStaticVersion (),
                          "VESID must use a static version to access a repository item");

    m_aVESID = aVESID;
  }

  @Nonnull
  public final VESID getVESID ()
  {
    return m_aVESID;
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public RepoStorageKeyOfArtefact getKeyHashSha256 ()
  {
    final String sPath = getPath ();
    if (sPath.endsWith (SUFFIX_SHA256))
    {
      // Seems like a doubled hash key
      LOGGER.warn ("You are trying to create a RepoStorageKey SHA-256 of something that already seems to be a SHA-256 key: '" +
                   sPath +
                   "'");
    }
    return new RepoStorageKeyOfArtefact (m_aVESID, sPath + SUFFIX_SHA256);
  }

  @Nonnull
  @ReturnsMutableCopy
  public RepoStorageKeyOfArtefact getKeyToc ()
  {
    return ofToc (m_aVESID.getGroupID (), m_aVESID.getArtifactID ());
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !super.equals (o))
      return false;
    final RepoStorageKeyOfArtefact rhs = (RepoStorageKeyOfArtefact) o;
    return m_aVESID.equals (rhs.m_aVESID);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ()).append (m_aVESID).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("VESID", m_aVESID).getToString ();
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

    return sGroupID.replace (GROUP_LEVEL_SEPARATOR, '/') + "/" + sArtifactID + "/";
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
  public static RepoStorageKeyOfArtefact of (@Nonnull final VESID aVESID, @Nonnull @Nonempty final String sFileExt)
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
    return new RepoStorageKeyOfArtefact (aVESID,
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
  public static RepoStorageKeyOfArtefact ofToc (@Nonnull @Nonempty final String sGroupID,
                                                @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    // ToC per group and artifact
    return new RepoStorageKeyOfArtefact (new VESID (sGroupID, sArtifactID, TOC_VERSION),
                                         getPathOfGroupIDAndArtifactID (sGroupID, sArtifactID) +
                                                                                         FILENAME_TOC_DIVER_XML);
  }

}
