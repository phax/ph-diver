/*
 * Copyright (C) 2023-2024 Philip Helger & ecosio
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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.api.version.DVRVersion;

/**
 * This is a specific storage key that not just contains a path but also a DVR
 * Coordinate to uniquely identify the object.
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
  private static final DVRVersion TOC_VERSION = DVRVersion.parseOrNull ("0");

  private final DVRCoordinate m_aCoord;

  private RepoStorageKeyOfArtefact (@Nonnull final DVRCoordinate aCoord, @Nonnull @Nonempty final String sPath)
  {
    super (sPath);
    ValueEnforcer.notNull (aCoord, "Coord");
    ValueEnforcer.isTrue (aCoord.getVersionObj ().isStaticVersion (),
                          "DVR Coordinate must use a static version to access a repository item");

    m_aCoord = aCoord;
  }

  @Nonnull
  public final DVRCoordinate getCoordinate ()
  {
    return m_aCoord;
  }

  @Override
  @Nonnull
  @ReturnsMutableCopy
  public RepoStorageKeyOfArtefact getKeyHashSha256 ()
  {
    final String sPath = getPath ();
    if (sPath.endsWith (FILE_EXT_SHA256))
    {
      // Seems like a doubled hash key
      LOGGER.warn ("You are trying to create a RepoStorageKey SHA-256 of something that already seems to be a SHA-256 key: '" +
                   sPath +
                   "'");
    }
    return new RepoStorageKeyOfArtefact (m_aCoord, sPath + FILE_EXT_SHA256);
  }

  @Nonnull
  @ReturnsMutableCopy
  public RepoStorageKeyOfArtefact getKeyToc ()
  {
    return ofToc (m_aCoord.getGroupID (), m_aCoord.getArtifactID ());
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !super.equals (o))
      return false;
    final RepoStorageKeyOfArtefact rhs = (RepoStorageKeyOfArtefact) o;
    return m_aCoord.equals (rhs.m_aCoord);
  }

  @Override
  public int hashCode ()
  {
    return HashCodeGenerator.getDerived (super.hashCode ()).append (m_aCoord).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("Coord", m_aCoord).getToString ();
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
   * Create the relative path for the passed DVR Coordinate elements and the
   * file extension. The algorithm is like this:
   * <code>sGroupID.replace ('.', '/') + "/" + sArtifactID + "/" + sVersion + "/" + sArtifactID + "-" + sVersion [+ "-" + sClassifier] + sFileExt</code>
   * which is basically
   * <code>group/artifact/version/artifact-version[-classifier].fileExtension</code>
   *
   * @param sGroupID
   *        The DVR Coordinate group ID. May neither be <code>null</code> nor
   *        empty.
   * @param sArtifactID
   *        The DVR Coordinate artifact ID. May neither be <code>null</code> nor
   *        empty.
   * @param sVersion
   *        The DVR Coordinate version. May neither be <code>null</code> nor
   *        empty.
   * @param sClassifier
   *        The DVR Coordinate classifier. If present, MUST NOT start with "-".
   *        May be <code>null</code>.
   * @param sFileExt
   *        The file extension to use. Must start with ".". May not be
   *        <code>null</code>.
   * @return Never <code>null</code>.
   * @see #getPathOfGroupIDAndArtifactID(String, String)
   */
  @Nonnull
  @Nonempty
  public static String getFilename (@Nonnull @Nonempty final String sGroupID,
                                    @Nonnull @Nonempty final String sArtifactID,
                                    @Nonnull @Nonempty final String sVersion,
                                    @Nullable final String sClassifier,
                                    @Nonnull @Nonempty final String sFileExt)
  {
    ValueEnforcer.notEmpty (sVersion, "Version");
    ValueEnforcer.notEmpty (sFileExt, "FileExt");
    ValueEnforcer.isTrue ( () -> sFileExt.startsWith ("."), "FileExt must start with a dot");

    final String sRealClassifier = StringHelper.hasText (sClassifier) ? DVRVersion.DEFAULT_CLASSIFIER_SEPARATOR +
                                                                        sClassifier : "";
    return getPathOfGroupIDAndArtifactID (sGroupID, sArtifactID) +
           sVersion +
           "/" +
           sArtifactID +
           "-" +
           sVersion +
           sRealClassifier +
           sFileExt;
  }

  /**
   * Create a {@link RepoStorageKey} from the passed DVR Coordinate and the file
   * extension.
   *
   * @param aCoord
   *        The DVR Coordinate to convert. Considers an optionally present
   *        classifier. May not be <code>null</code>.
   * @param sFileExt
   *        The file extension to use. Must start with ".". May not be
   *        <code>null</code>.
   * @return Never <code>null</code>.
   * @see #getFilename(String, String, String, String, String)
   */
  @Nonnull
  public static RepoStorageKeyOfArtefact of (@Nonnull final DVRCoordinate aCoord,
                                             @Nonnull @Nonempty final String sFileExt)
  {
    ValueEnforcer.notNull (aCoord, "Coord");
    ValueEnforcer.isTrue (aCoord.getVersionObj ().isStaticVersion (),
                          "DVR Coordinate must use a static version to access a repository item");

    return new RepoStorageKeyOfArtefact (aCoord,
                                         getFilename (aCoord.getGroupID (),
                                                      aCoord.getArtifactID (),
                                                      aCoord.getVersionString (),
                                                      aCoord.getClassifier (),
                                                      sFileExt));
  }

  @Nonnull
  public static RepoStorageKeyOfArtefact ofToc (@Nonnull @Nonempty final String sGroupID,
                                                @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    // ToC per group and artifact
    return new RepoStorageKeyOfArtefact (new DVRCoordinate (sGroupID, sArtifactID, TOC_VERSION),
                                         getPathOfGroupIDAndArtifactID (sGroupID, sArtifactID) +
                                                                                                 FILENAME_TOC_DIVER_XML);
  }
}
