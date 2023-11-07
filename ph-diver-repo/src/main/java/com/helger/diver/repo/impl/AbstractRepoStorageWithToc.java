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
package com.helger.diver.repo.impl;

import java.time.OffsetDateTime;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.toc.IRepoStorageWithToc;
import com.helger.diver.repo.toc.RepoToc;
import com.helger.diver.repo.toc.RepoToc1Marshaller;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;

/**
 * Abstract implementation of a repository storage with table of contents
 * support.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The real implementation type.
 */
public abstract class AbstractRepoStorageWithToc <IMPLTYPE extends AbstractRepoStorageWithToc <IMPLTYPE>> extends
                                                 AbstractRepoStorage <IMPLTYPE> implements
                                                 IRepoStorageWithToc
{
  public static final boolean DEFAULT_ENABLE_TOC_UPDATES = true;

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractRepoStorageWithToc.class);

  // Enable ToC updates on write and delete
  private boolean m_bEnableTocUpdates = DEFAULT_ENABLE_TOC_UPDATES;

  protected AbstractRepoStorageWithToc (@Nonnull final RepoStorageType aType,
                                        @Nonnull @Nonempty final String sID,
                                        @Nonnull final ERepoWritable eWriteEnabled,
                                        @Nonnull final ERepoDeletable eDeleteEnabled)
  {
    super (aType, sID, eWriteEnabled, eDeleteEnabled);
  }

  public final boolean isEnableTocUpdates ()
  {
    return m_bEnableTocUpdates;
  }

  @Nonnull
  public final IMPLTYPE setEnableTocUpdates (final boolean b)
  {
    m_bEnableTocUpdates = b;
    LOGGER.info ("RepoStorage[" + getRepoType ().getID () + "]: ToC updates are now: " + (b ? "enabled" : "disabled"));
    return thisAsT ();
  }

  /**
   * Update the table of contents for a specific group ID and artifact ID.
   *
   * @param aKeyToc
   *        The ToC key with group ID and artifact ID. Never <code>null</code>.
   * @param aTocConsumer
   *        The main activities are done in the callback. Never
   *        <code>null</code>.
   * @return {@link ESuccess#SUCCESS} if updating was successful. Never
   *         <code>null</code>.
   */
  @Nonnull
  @OverrideOnDemand
  protected ESuccess updateToc (@Nonnull final RepoStorageKey aKeyToc,
                                @Nonnull final Consumer <? super RepoToc> aTocConsumer)
  {
    // Read existing ToC
    final RepoStorageItem aTocItem = read (aKeyToc);
    final RepoToc aToc;
    if (aTocItem == null)
    {
      // Create a new one
      aToc = new RepoToc (aKeyToc.getVESID ().getGroupID (), aKeyToc.getVESID ().getArtifactID ());
    }
    else
    {
      final RepoTocType aJaxbToc = new RepoToc1Marshaller ().read (aTocItem.data ());
      if (aJaxbToc == null)
        throw new IllegalStateException ("Invalid TOC found in '" + aKeyToc.getPath () + "'");
      aToc = RepoToc.createFromJaxbObject (aJaxbToc);
    }

    try
    {
      // Make modifications
      aTocConsumer.accept (aToc);
    }
    catch (final RuntimeException ex)
    {
      LOGGER.error ("Error invoking ToC consumer", ex);
      return ESuccess.FAILURE;
    }

    // Write ToC again
    // Don't check if enabled or not
    return doWriteRepoStorageItem (aKeyToc,
                                   RepoStorageItem.of (new RepoToc1Marshaller ().setFormattedOutput (true)
                                                                                .getAsBytes (aToc.getAsJaxbObject ())));
  }

  @Override
  @OverrideOnDemand
  @Nonnull
  protected final ESuccess onAfterWrite (@Nonnull final RepoStorageKey aKey,
                                         @Nonnull final RepoStorageItem aItem,
                                         @Nullable final OffsetDateTime aPublicationDT)
  {
    if (isEnableTocUpdates ())
    {
      // Update ToC
      return updateToc (aKey.getKeyToc (), toc -> {
        final VESID aVESID = aKey.getVESID ();
        // Make sure a publication DT is present and always UTC
        final OffsetDateTime aRealPubDT = aPublicationDT != null ? aPublicationDT : PDTFactory
                                                                                              .getCurrentOffsetDateTimeUTC ();

        // Add new version
        if (toc.addVersion (aVESID.getVersionObj (), aRealPubDT).isUnchanged ())
        {
          LOGGER.warn ("Failed to add version '" +
                       aVESID.getAsSingleID () +
                       "' to ToC of because it is already contained");
        }
        else
        {
          LOGGER.info ("Successfully added version '" + aVESID.getAsSingleID () + "' to ToC");
        }
      });
    }

    return ESuccess.SUCCESS;
  }

  @Override
  @Nonnull
  protected final ESuccess onAfterDelete (@Nonnull final RepoStorageKey aKey)
  {
    if (isEnableTocUpdates ())
    {
      // Update ToC
      return updateToc (aKey.getKeyToc (), toc -> {
        final VESID aVESID = aKey.getVESID ();
        // Remove deleted version
        if (toc.removeVersion (aVESID.getVersionObj ()).isUnchanged ())
        {
          LOGGER.warn ("Failed to delete version '" +
                       aVESID.getAsSingleID () +
                       "' from ToC because it is not contained");
        }
        else
        {
          LOGGER.info ("Successfully deleted version '" + aVESID.getAsSingleID () + "' from ToC");
        }
      });
    }

    return ESuccess.SUCCESS;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("EnableTocUpdates", m_bEnableTocUpdates)
                            .getToString ();
  }
}