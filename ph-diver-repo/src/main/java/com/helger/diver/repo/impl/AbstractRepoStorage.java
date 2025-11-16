/*
 * Copyright (C) 2023-2025 Philip Helger & ecosio
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.array.ArrayHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.base.trait.IGenericImplTrait;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoHashState;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageAuditor;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.IRepoStorageReadItem;
import com.helger.diver.repo.IRepoStorageType;
import com.helger.diver.repo.RepoStorageContentByteArray;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.RepoStorageReadItem;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;

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

  private final IRepoStorageType m_aRepoStorageType;
  private final String m_sID;
  // Currently constant
  private final EMessageDigestAlgorithm m_eMDAlgo = DEFAULT_MD_ALGORITHM;
  private final ERepoWritable m_eWriteEnabled;
  private final ERepoDeletable m_eDeleteEnabled;
  // Verify hash value on read
  private boolean m_bVerifyHashOnRead = DEFAULT_VERIFY_HASH_VALUE;
  private IRepoStorageAuditor m_aAuditor = IRepoStorageAuditor.DO_NOTHING_AUDITOR;

  protected AbstractRepoStorage (@NonNull final IRepoStorageType aRepoStorageType,
                                 @NonNull @Nonempty final String sID,
                                 @NonNull final ERepoWritable eWriteEnabled,
                                 @NonNull final ERepoDeletable eDeleteEnabled)
  {
    ValueEnforcer.notNull (aRepoStorageType, "RepoStorageType");
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (eWriteEnabled, "WriteEnabled");
    ValueEnforcer.notNull (eDeleteEnabled, "DeleteEnabled");
    m_aRepoStorageType = aRepoStorageType;
    m_sID = sID;
    m_eWriteEnabled = eWriteEnabled;
    m_eDeleteEnabled = eDeleteEnabled;
  }

  @NonNull
  public final IRepoStorageType getRepoType ()
  {
    return m_aRepoStorageType;
  }

  @NonNull
  @Nonempty
  public final String getID ()
  {
    return m_sID;
  }

  public final boolean isVerifyHashOnRead ()
  {
    return m_bVerifyHashOnRead;
  }

  @NonNull
  public final IMPLTYPE setVerifyHashOnRead (final boolean b)
  {
    m_bVerifyHashOnRead = b;
    LOGGER.info ("RepoStorage[" +
                 m_aRepoStorageType.getID () +
                 "]: hash verification on read is now: " +
                 (b ? "enabled" : "disabled"));
    return thisAsT ();
  }

  @NonNull
  public final IRepoStorageAuditor getAuditor ()
  {
    return m_aAuditor;
  }

  @NonNull
  public final IMPLTYPE setAuditor (@NonNull final IRepoStorageAuditor aAuditor)
  {
    ValueEnforcer.notNull (aAuditor, "Auditor");
    m_aAuditor = aAuditor;
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
  protected abstract InputStream getInputStream (@NonNull final RepoStorageKey aKey);

  @Nullable
  private InputStream _getInputStreamWithAudit (@NonNull final RepoStorageKey aKey)
  {
    final InputStream ret = getInputStream (aKey);
    m_aAuditor.onRead (thisAsT (), aKey, ESuccess.valueOf (ret != null));
    return ret;
  }

  @Nullable
  public final IRepoStorageReadItem read (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    try
    {
      IRepoStorageContent aRepoContent = null;
      byte [] aRepoDigest = null;
      byte [] aCalculatedDigest = null;
      ERepoHashState eHashState = ERepoHashState.NOT_VERIFIED;

      if (isVerifyHashOnRead ())
      {
        // Read the expected hash digest
        aRepoDigest = StreamHelper.getAllBytes (_getInputStreamWithAudit (aKey.getKeyHashSha256 ()));
        if (aRepoDigest == null)
        {
          // Should already be logged in getInputStream
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("Failed to read digest value from " + aKey.getPath ());
        }

        // The message digest to be calculated while reading
        final MessageDigest aMD = m_eMDAlgo.createMessageDigest ();
        try (final InputStream aContentIS = _getInputStreamWithAudit (aKey))
        {
          if (aContentIS != null)
            try (final DigestInputStream aMDIS = new DigestInputStream (aContentIS, aMD))
            {
              // read data and calculate digest
              final byte [] aContentBytes = StreamHelper.getAllBytes (aMDIS);
              if (aRepoDigest == null)
              {
                // Error in reading
              }
              else
              {
                // We have read a digest value
                aCalculatedDigest = aMD.digest ();
                if (ArrayHelper.isArrayEquals (aRepoDigest, aCalculatedDigest))
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
              aRepoContent = RepoStorageContentByteArray.of (aContentBytes);
            }
        }
      }
      else
      {
        // No verify
        try (final InputStream aIS = _getInputStreamWithAudit (aKey))
        {
          if (aIS != null)
          {
            final byte [] aContentBytes = StreamHelper.getAllBytes (aIS);
            aRepoContent = RepoStorageContentByteArray.of (aContentBytes);
          }
        }
      }

      if (aRepoContent != null)
        return new RepoStorageReadItem (aRepoContent, aRepoDigest, aCalculatedDigest, eHashState);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to read RepoStorage[" + m_aRepoStorageType.getID () + "] item '" + aKey.getPath () + "'",
                    ex);
    }

    return null;
  }

  public final boolean canWrite ()
  {
    return m_eWriteEnabled.isWriteEnabled ();
  }

  @NonNull
  protected abstract ESuccess writeObject (@NonNull final RepoStorageKey aKey,
                                           @NonNull final IRepoStorageContent aContent);

  @Nullable
  private ESuccess _writeObjectWithAudit (@NonNull final RepoStorageKey aKey,
                                          @NonNull final IRepoStorageContent aContent)
  {
    final ESuccess ret = writeObject (aKey, aContent);
    m_aAuditor.onWrite (thisAsT (), aKey, ret);
    return ret;
  }

  @NonNull
  protected final ESuccess doWriteRepoStorageItem (@NonNull final RepoStorageKey aKey,
                                                   @NonNull final IRepoStorageContent aContent)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aContent, "Content");

    LOGGER.info ("Writing item '" +
                 aKey.getPath () +
                 "' with " +
                 aContent.getLength () +
                 " bytes to RepoStorage[" +
                 m_aRepoStorageType.getID () +
                 "]");

    final MessageDigest aMD = m_eMDAlgo.createMessageDigest ();
    final IRepoStorageContent aMDContent = new IRepoStorageContent ()
    {
      public boolean isReadMultiple ()
      {
        return aContent.isReadMultiple ();
      }

      public InputStream getInputStream ()
      {
        return new DigestInputStream (aContent.getInputStream (), aMD);
      }

      public long getLength ()
      {
        return aContent.getLength ();
      }
    };

    // Store the main data
    if (_writeObjectWithAudit (aKey, aMDContent).isFailure ())
      return ESuccess.FAILURE;

    // Get the digest bytes
    final byte [] aDigestBytes = aMD.digest ();

    // Store the hash value
    if (_writeObjectWithAudit (aKey.getKeyHashSha256 (), RepoStorageContentByteArray.of (aDigestBytes)).isFailure ())
      return ESuccess.FAILURE;

    return ESuccess.SUCCESS;
  }

  /**
   * Callback invoked after successful writing.
   *
   * @param aKey
   *        The key of the created repo item. Never <code>null</code>.
   * @param aContent
   *        The created repo item content. Never <code>null</code>.
   * @param aPublicationDT
   *        The publication date time of the item. May be <code>null</code>.
   * @return {@link ESuccess}
   */
  @OverrideOnDemand
  @NonNull
  protected ESuccess onAfterWrite (@NonNull final RepoStorageKeyOfArtefact aKey,
                                   @NonNull final IRepoStorageContent aContent,
                                   @Nullable final OffsetDateTime aPublicationDT)
  {
    return ESuccess.SUCCESS;
  }

  @NonNull
  public final ESuccess write (@NonNull final RepoStorageKey aKey,
                               @NonNull final IRepoStorageContent aContent,
                               @Nullable final OffsetDateTime aPublicationDT)
  {
    ValueEnforcer.notNull (aKey, "Key");
    ValueEnforcer.notNull (aContent, "Content");

    if (!canWrite ())
    {
      LOGGER.error ("Trying to write on a RepoStorage[" + m_aRepoStorageType.getID () + "] with write disabled");
      throw new UnsupportedOperationException ("write is not enabled");
    }

    if (doWriteRepoStorageItem (aKey, aContent).isFailure ())
      return ESuccess.FAILURE;

    if (aKey instanceof RepoStorageKeyOfArtefact)
      if (onAfterWrite ((RepoStorageKeyOfArtefact) aKey, aContent, aPublicationDT).isFailure ())
        return ESuccess.FAILURE;

    return ESuccess.SUCCESS;
  }

  public final boolean canDelete ()
  {
    return m_eDeleteEnabled.isDeleteEnabled ();
  }

  @NonNull
  protected abstract ESuccess deleteObject (@NonNull final RepoStorageKey aKey);

  @Nullable
  private ESuccess _deleteObjectWithAudit (@NonNull final RepoStorageKey aKey)
  {
    final ESuccess ret = deleteObject (aKey);
    m_aAuditor.onDelete (thisAsT (), aKey, ret);
    return ret;
  }

  @NonNull
  private ESuccess _doDeleteRepoStorageItem (@NonNull final RepoStorageKey aKey)
  {
    LOGGER.info ("Deleting item '" + aKey.getPath () + "' from RepoStorage[" + m_aRepoStorageType.getID () + "]");

    // Delete the main data
    if (_deleteObjectWithAudit (aKey).isFailure ())
      return ESuccess.FAILURE;

    // Delete the hash value
    if (_deleteObjectWithAudit (aKey.getKeyHashSha256 ()).isFailure ())
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
  @NonNull
  @OverrideOnDemand
  protected ESuccess onAfterDelete (@NonNull final RepoStorageKeyOfArtefact aKey)
  {
    return ESuccess.SUCCESS;
  }

  @NonNull
  public final ESuccess delete (@NonNull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    if (!canDelete ())
    {
      LOGGER.error ("Trying to delete on a RepoStorage[" + m_aRepoStorageType.getID () + "] with delete disabled");
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
    return new ToStringGenerator (null).append ("Type", m_aRepoStorageType)
                                       .append ("ID", m_sID)
                                       .append ("MDAlgo", m_eMDAlgo)
                                       .append ("WriteEnabled", m_eWriteEnabled)
                                       .append ("DeleteEnabled", m_eDeleteEnabled)
                                       .append ("VerifyHashOnRead", m_bVerifyHashOnRead)
                                       .getToString ();
  }
}
