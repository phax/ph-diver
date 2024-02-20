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
package com.helger.diver.repo.toc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.error.list.ErrorList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;

/**
 * XML based RepoTopToc model persisted in the repo itself. Each repository
 * requires its own instance of this service and MUST NOT be re-used.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class RepoTopTocServiceRepoBasedXML implements IRepoTopTocService
{
  /** The default filename for the top-level table of contents. */
  public static final String FILENAME_TOP_TOC_DIVER_XML = "toptoc-diver.xml";

  private static final Logger LOGGER = LoggerFactory.getLogger (RepoTopTocServiceRepoBasedXML.class);

  private static final RepoStorageKey RSK_TOP_TOC = new RepoStorageKey (FILENAME_TOP_TOC_DIVER_XML);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final AtomicBoolean m_aInitialized = new AtomicBoolean (false);
  private IRepoStorageWithToc m_aRepo;
  @GuardedBy ("m_aRWLock")
  private RepoTopTocXML m_aTopToc;

  public RepoTopTocServiceRepoBasedXML ()
  {}

  @Nonnull
  private RepoTopTocXML _readTopToc (final boolean bVerbose)
  {
    final RepoTopTocXML ret;

    // Read existing Top ToC from repository
    final IRepoStorageReadItem aItem = m_aRepo.read (RSK_TOP_TOC);
    if (aItem != null)
    {
      final ErrorList aErrorList = new ErrorList ();
      final RepoTopTocType aRepoTopToc = new RepoTopToc1Marshaller ().setCollectErrors (aErrorList)
                                                                     .read (aItem.getContent ()
                                                                                 .getBufferedInputStream ());
      if (aRepoTopToc == null)
        throw new IllegalStateException ("Failed to parse XML Top-ToC from Repository. Details: " + aErrorList);

      try
      {
        ret = RepoTopTocXML.createFromJaxbObject (aRepoTopToc);

        if (bVerbose)
          LOGGER.info ("Successfully read Repository Top-ToC with " +
                       ret.getTopLevelGroupCount () +
                       " top-level group(s)");
      }
      catch (final RuntimeException ex)
      {
        throw new IllegalStateException ("Internal inconsistency in Top-ToC", ex);
      }
    }
    else
    {
      if (bVerbose)
        LOGGER.info ("Repository does not have a Top-ToC yet");
      ret = new RepoTopTocXML ();
    }
    return ret;
  }

  @Nonnull
  private ESuccess _writeTopToc (@Nonnull final RepoTopTocXML aTopToc)
  {
    ValueEnforcer.notNull (aTopToc, "TopToc");

    final ErrorList aErrorList = new ErrorList ();
    final byte [] aTopTocBytes = new RepoTopToc1Marshaller ().setCollectErrors (aErrorList)
                                                             .setFormattedOutput (true)
                                                             .getAsBytes (aTopToc.getAsJaxbObject ());
    if (aTopTocBytes == null)
      throw new IllegalStateException ("Failed to serialize XML Top-ToC. Details: " + aErrorList);

    return m_aRepo.write (RSK_TOP_TOC, RepoStorageContentByteArray.of (aTopTocBytes));
  }

  public void initForRepo (@Nonnull final IRepoStorageWithToc aRepo)
  {
    ValueEnforcer.notNull (aRepo, "Repo");

    if (m_aInitialized.getAndSet (true))
      throw new IllegalStateException ("This service is already initialized - can't do it again, sorry.");
    m_aRepo = aRepo;

    // Initial read
    final RepoTopTocXML aTmpTopToc = _readTopToc (true);
    m_aRWLock.writeLocked ( () -> m_aTopToc = aTmpTopToc);
  }

  private void _checkInited ()
  {
    if (!m_aInitialized.get ())
      throw new IllegalStateException ("This service was not properly initialized");
  }

  public boolean containsGroupAndArtifact (@Nullable final String sGroupID, @Nullable final String sArtifactID)
  {
    _checkInited ();

    if (StringHelper.hasNoText (sGroupID))
      return false;
    if (StringHelper.hasNoText (sArtifactID))
      return false;

    return m_aRWLock.readLockedBoolean ( () -> m_aTopToc.containsGroupAndArtifact (sGroupID, sArtifactID));
  }

  public void iterateAllTopLevelGroupNames (@Nonnull final Consumer <String> aGroupNameConsumer)
  {
    ValueEnforcer.notNull (aGroupNameConsumer, "GroupNameConsumer");
    _checkInited ();

    m_aRWLock.readLocked ( () -> m_aTopToc.iterateAllTopLevelGroupNames (aGroupNameConsumer));
  }

  public void iterateAllSubGroups (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull final IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                                   final boolean bRecursive)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notNull (aGroupNameConsumer, "GroupNameConsumer");
    _checkInited ();

    m_aRWLock.readLocked ( () -> m_aTopToc.iterateAllSubGroups (sGroupID, aGroupNameConsumer, bRecursive));
  }

  public void iterateAllArtifacts (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull final Consumer <String> aArtifactNameConsumer)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notNull (aArtifactNameConsumer, "ArtifactNameConsumer");
    _checkInited ();

    m_aRWLock.readLocked ( () -> m_aTopToc.iterateAllArtifacts (sGroupID, aArtifactNameConsumer));
  }

  @Nonnull
  public ESuccess registerGroupAndArtifact (@Nonnull @Nonempty final String sGroupID,
                                            @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");
    _checkInited ();

    return m_aRWLock.writeLockedGet ( () -> {
      if (m_aTopToc.registerGroupAndArtifact (sGroupID, sArtifactID).isChanged ())
      {
        try
        {
          // Read the data again, to make sure to have the latest version
          m_aTopToc = _readTopToc (false);
        }
        catch (final RuntimeException ex)
        {
          LOGGER.error ("Failed to read Top-ToC", ex);
          return ESuccess.FAILURE;
        }

        // Register again, because data structure changed
        m_aTopToc.registerGroupAndArtifact (sGroupID, sArtifactID);

        // Write updated version on server
        // Pass instance to avoid it changes while writing
        if (_writeTopToc (m_aTopToc).isFailure ())
          return ESuccess.FAILURE;
      }

      return ESuccess.SUCCESS;
    });
  }
}
