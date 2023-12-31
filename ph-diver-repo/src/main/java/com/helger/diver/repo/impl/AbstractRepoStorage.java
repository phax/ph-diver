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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.commons.traits.IGenericImplTrait;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.RepoStorageItem;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.util.MessageDigestInputStream;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.security.messagedigest.MessageDigestValue;

/**
 * Abstract implementation of a repository storage. It supports the verification
 * of hash values upon reading.
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The real implementation type.
 */
public abstract class AbstractRepoStorage <IMPLTYPE extends AbstractRepoStorage <IMPLTYPE>> implements
                                          IRepoStorage,
                                          IGenericImplTrait <IMPLTYPE>
{
  public static final boolean DEFAULT_VERIFY_HASH_VALUE = true;

  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractRepoStorage.class);

  private final RepoStorageType m_aType;
  private final String m_sID;
  // Currently constant
  private final EMessageDigestAlgorithm m_eMDAlgo = DEFAULT_MD_ALGORITHM;
  private final ERepoWritable m_eWriteEnabled;
  private final ERepoDeletable m_eDeleteEnabled;
  // Verify hash value on read
  private boolean m_bVerifyHash = DEFAULT_VERIFY_HASH_VALUE;

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
  protected abstract ESuccess writeObject (@Nonnull final RepoStorageKey aKey, @Nonnull final byte [] aPayload);

  @Nonnull
  protected final ESuccess doWriteRepoStorageItem (@Nonnull final RepoStorageKey aKey,
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

  /**
   * Callback invoked after successful writing.
   *
   * @param aKey
   *        The key of the created repo item. Never <code>null</code>.
   * @param aItem
   *        The created repo item content. Never <code>null</code>.
   * @param aPublicationDT
   *        The publication date time of the item. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @OverrideOnDemand
  @Nonnull
  protected ESuccess onAfterWrite (@Nonnull final RepoStorageKeyOfArtefact aKey,
                                   @Nonnull final RepoStorageItem aItem,
                                   @Nullable final OffsetDateTime aPublicationDT)
  {
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

    if (doWriteRepoStorageItem (aKey, aItem).isFailure ())
      return ESuccess.FAILURE;

    if (aKey instanceof RepoStorageKeyOfArtefact)
      if (onAfterWrite ((RepoStorageKeyOfArtefact) aKey, aItem, aPublicationDT).isFailure ())
        return ESuccess.FAILURE;

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

  /**
   * Callback invoked after successful deletion
   *
   * @param aKey
   *        The key of the deleted repo item. Never <code>null</code>.
   * @return {@link ESuccess}
   */
  @Nonnull
  @OverrideOnDemand
  protected ESuccess onAfterDelete (@Nonnull final RepoStorageKeyOfArtefact aKey)
  {
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

    if (aKey instanceof RepoStorageKeyOfArtefact)
      if (onAfterDelete ((RepoStorageKeyOfArtefact) aKey).isFailure ())
        return ESuccess.FAILURE;

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
                                       .getToString ();
  }
}
