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

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorageWithToc;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.toc.RepoToc;
import com.helger.diver.repo.toc.RepoToc1Marshaller;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;
import com.helger.diver.repo.util.MessageDigestInputStream;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.security.messagedigest.MessageDigestValue;

/**
 * Abstract implementation of a repository storage. It supports the verification
 * of hash values upon reading and the update of the table of contents on
 * writing.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The real implementation type.
 */
public abstract class AbstractRepoStorage <IMPLTYPE extends AbstractRepoStorage <IMPLTYPE>> implements
                                          IRepoStorageWithToc,
                                          IGenericImplTrait <IMPLTYPE>
{
  public static final boolean DEFAULT_VERIFY_HASH_VALUE = true;
  public static final boolean DEFAULT_ENABLE_TOC_UPDATES = true;

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractRepoStorage.class);

  private final RepoStorageType m_aType;
  private final String m_sID;
  // Currently constant
  private final EMessageDigestAlgorithm m_eMDAlgo = DEFAULT_MD_ALGORITHM;
  private final ERepoWritable m_eWriteEnabled;
  private final ERepoDeletable m_eDeleteEnabled;
  // Verify hash value on read
  private boolean m_bVerifyHash = DEFAULT_VERIFY_HASH_VALUE;
  // Enable ToC updates on write and delete
  private boolean m_bEnableTocUpdates = DEFAULT_ENABLE_TOC_UPDATES;

  protected AbstractRepoStorage (@Nonnull final RepoStorageType aType,
                                 @Nonnull @Nonempty final String sID,
                                 @Nonnull final ERepoWritable eWriteEnabled,
                                 @Nonnull final ERepoDeletable eDeleteEnabled)
  {
    ValueEnforcer.notNull (aType, "Type");
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (eWriteEnabled, "WriteEnabled");
    ValueEnforcer.notNull (eDeleteEnabled, "DeleteEnabled");
    m_aType = aType;
    m_sID = sID;
    m_eWriteEnabled = eWriteEnabled;
    m_eDeleteEnabled = eDeleteEnabled;
  }

  @Nonnull
  public final RepoStorageType getRepoType ()
  {
    return m_aType;
  }

  @Nonnull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  public final boolean isVerifyHash ()
  {
    return m_bVerifyHash;
  }

  @Nonnull
  public final IMPLTYPE setVerifyHash (final boolean b)
  {
    m_bVerifyHash = b;
    LOGGER.info ("RepoStorage[" + m_aType.getID () + "]: hash verification is now: " + (b ? "enabled" : "disabled"));
    return thisAsT ();
  }

  public final boolean isEnableTocUpdates ()
  {
    return m_bEnableTocUpdates;
  }

  @Nonnull
  public final IMPLTYPE setEnableTocUpdates (final boolean b)
  {
    m_bEnableTocUpdates = b;
    LOGGER.info ("RepoStorage[" + m_aType.getID () + "]: ToC updates are now: " + (b ? "enabled" : "disabled"));
    return thisAsT ();
  }

  /**
   * Get the input stream (locally or remote) to the provided key. Any failure
   * to open, should be logged inside.
   *
   * @param aKey
   *        The key to open. May not be <code>null</code>.
   * @return <code>null</code> in case open fails (why so ever)
   */
  @Nullable
  protected abstract InputStream getInputStream (@Nonnull final RepoStorageKey aKey);

  @Nullable
  public final RepoStorageItem readToc (@Nonnull @Nonempty final String sGroupID,
                                        @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    return read (RepoStorageKey.ofToc (sGroupID, sArtifactID));
  }

  @Nullable
  public final RepoStorageItem read (@Nonnull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    try
    {
      if (isVerifyHash ())
      {
        // Read the expected hash digest
        final byte [] aExpectedDigest = StreamHelper.getAllBytes (getInputStream (aKey.getKeyHashSha256 ()));
        if (aExpectedDigest == null)
        {
          // Should already be logged in getInputStream
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Failed to read digest value from " + aKey.getPath ());
        }

        // The message digest to be calculated while reading
        final MessageDigest aMD = m_eMDAlgo.createMessageDigest ();
        try (final InputStream aIS = getInputStream (aKey))
        {
          if (aIS != null)
            try (final MessageDigestInputStream aMDIS = new MessageDigestInputStream (aIS, aMD))
            {
              final byte [] aData = StreamHelper.getAllBytes (aMDIS);

              final ERepoHashState eHashState;
              if (aExpectedDigest == null)
              {
                // Error in reading
                eHashState = ERepoHashState.NOT_VERIFIED;
              }
              else
              {
                // We have read a digest value
                final byte [] aDigest = aMD.digest ();
                if (ArrayHelper.isArrayEquals (aExpectedDigest, aDigest))
                {
                  // Digest match
                  eHashState = ERepoHashState.VERIFIED_MATCHING;
                  if (LOGGER.isDebugEnabled ())
                    LOGGER.debug ("Hash values are identical for '" + aKey.getPath () + "'");
                }
                else
                {
                  // Digest mismatch
                  eHashState = ERepoHashState.VERIFIED_NON_MATCHING;
                  if (LOGGER.isDebugEnabled ())
                    LOGGER.debug ("Hash value mismatch for '" + aKey.getPath () + "'");
                }
              }

              // We're good to go
              return RepoStorageItem.of (aData, eHashState);
            }
        }
      }
      else
      {
        // No verify
        try (final InputStream aIS = getInputStream (aKey))
        {
          if (aIS != null)
          {
            final byte [] aData = StreamHelper.getAllBytes (aIS);
            return RepoStorageItem.of (aData, ERepoHashState.NOT_VERIFIED);
          }
        }
      }
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to read RepoStorage[" + m_aType.getID () + "] item '" + aKey.getPath () + "'", ex);
    }

    return null;
  }

  public final boolean canWrite ()
  {
    return m_eWriteEnabled.isWriteEnabled ();
  }

  @Nonnull
  private ESuccess _updateToc (@Nonnull final RepoStorageKey aKeyToc,
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

    // Make modifications
    aTocConsumer.accept (aToc);

    // Write ToC again
    // Don't check if enabled or not
    return _doWriteRepoStorageItem (aKeyToc,
                                    RepoStorageItem.of (new RepoToc1Marshaller ().getAsBytes (aToc.getAsJaxbObject ())));
  }

  @Nonnull
  protected abstract ESuccess writeObject (@Nonnull final RepoStorageKey aKey, @Nonnull final byte [] aPayload);

  @Nonnull
  private final ESuccess _doWriteRepoStorageItem (@Nonnull final RepoStorageKey aKey,
                                                  @Nonnull final RepoStorageItem aItem)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aItem, "Item");

    LOGGER.info ("Writing item '" +
                 aKey.getPath () +
                 "' with " +
                 aItem.data ().size () +
                 " bytes to RepoStorage[" +
                 m_aType.getID () +
                 "]");

    // Create the message digest up front
    final byte [] aDigest = MessageDigestValue.create (aItem.data ().bytes (), m_eMDAlgo).bytes ();

    // Store the main data
    if (writeObject (aKey, aItem.data ().bytes ()).isFailure ())
      return ESuccess.FAILURE;

    // Store the hash value
    if (writeObject (aKey.getKeyHashSha256 (), aDigest).isFailure ())
      return ESuccess.FAILURE;

    return ESuccess.SUCCESS;
  }

  @Nonnull
  public final ESuccess write (@Nonnull final RepoStorageKey aKey,
                               @Nonnull final RepoStorageItem aItem,
                               @Nullable final OffsetDateTime aPublicationDT)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aItem, "Item");

    if (!canWrite ())
    {
      LOGGER.error ("Trying to write on a RepoStorage[" + m_aType.getID () + "] with write disabled");
      throw new UnsupportedOperationException ("write is not enabled");
    }

    if (_doWriteRepoStorageItem (aKey, aItem).isFailure ())
      return ESuccess.FAILURE;

    if (isEnableTocUpdates ())
    {
      // Update ToC
      if (_updateToc (aKey.getKeyToc (), toc -> {
        // Make sure a publication DT is present and always UTC
        final OffsetDateTime aRealPubDT = aPublicationDT != null ? aPublicationDT : PDTFactory
                                                                                              .getCurrentOffsetDateTimeUTC ();

        // Add new version
        if (toc.addVersion (aKey.getVESID ().getVersionObj (), aRealPubDT).isUnchanged ())
        {
          LOGGER.warn ("Failed to add version '" +
                       aKey.getVESID ().getVersionString () +
                       "' to ToC because it is already contained");
        }
        else
        {
          LOGGER.info ("Successfully added version '" + aKey.getVESID ().getVersionString () + "' to ToC");
        }
      }).isFailure ())
        return ESuccess.FAILURE;
    }

    return ESuccess.SUCCESS;
  }

  public final boolean canDelete ()
  {
    return m_eDeleteEnabled.isDeleteEnabled ();
  }

  @Nonnull
  protected abstract ESuccess deleteObject (@Nonnull final RepoStorageKey aKey);

  @Nonnull
  private ESuccess _doDeleteRepoStorageItem (@Nonnull final RepoStorageKey aKey)
  {
    LOGGER.info ("Deleting item '" + aKey.getPath () + "' from RepoStorage[" + m_aType.getID () + "]");

    // Delete the main data
    if (deleteObject (aKey).isFailure ())
      return ESuccess.FAILURE;

    // Delete the hash value
    if (deleteObject (aKey.getKeyHashSha256 ()).isFailure ())
      return ESuccess.FAILURE;

    return ESuccess.SUCCESS;
  }

  @Nonnull
  public final ESuccess delete (@Nonnull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    if (!canDelete ())
    {
      LOGGER.error ("Trying to delete on a RepoStorage[" + m_aType.getID () + "] with delete disabled");
      throw new UnsupportedOperationException ("delete is not enabled");
    }

    if (_doDeleteRepoStorageItem (aKey).isFailure ())
      return ESuccess.FAILURE;

    if (isEnableTocUpdates ())
    {
      // Update ToC
      if (_updateToc (aKey.getKeyToc (), toc -> {
        // Remove deleted version
        if (toc.removeVersion (aKey.getVESID ().getVersionObj ()).isUnchanged ())
        {
          LOGGER.warn ("Failed to delete version '" +
                       aKey.getVESID ().getVersionString () +
                       "' from ToC because it is not contained");
        }
        else
        {
          LOGGER.info ("Successfully deleted version '" + aKey.getVESID ().getVersionString () + "' from ToC");
        }
      }).isFailure ())
        return ESuccess.FAILURE;
    }

    return ESuccess.SUCCESS;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Type", m_aType)
                                       .append ("ID", m_sID)
                                       .append ("MDAlgo", m_eMDAlgo)
                                       .append ("WriteEnabled", m_eWriteEnabled)
                                       .append ("DeleteEnabled", m_eDeleteEnabled)
                                       .append ("VerifyHash", m_bVerifyHash)
                                       .append ("EnableTocUpdates", m_bEnableTocUpdates)
                                       .getToString ();
  }
}
