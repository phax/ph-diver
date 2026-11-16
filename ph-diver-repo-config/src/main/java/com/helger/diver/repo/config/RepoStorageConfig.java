/*
 * Copyright (C) 2023-2026 Philip Helger
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
package com.helger.diver.repo.config;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageAuditor;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.diver.repo.toc.RepoTopTocServiceRepoBasedXML;

/**
 * The complete repository configuration: the list of all {@link RepoStorageDefinition} objects in
 * the order of declaration.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class RepoStorageConfig
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageConfig.class);

  private final ICommonsList <RepoStorageDefinition> m_aDefinitions;

  public RepoStorageConfig (@NonNull final List <RepoStorageDefinition> aDefinitions)
  {
    ValueEnforcer.notNullNoNullValue (aDefinitions, "Definitions");
    m_aDefinitions = new CommonsArrayList <> (aDefinitions);
  }

  /**
   * @return A copy of all repository definitions in the order of declaration. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <RepoStorageDefinition> getAllDefinitions ()
  {
    return m_aDefinitions.getClone ();
  }

  public int getDefinitionCount ()
  {
    return m_aDefinitions.size ();
  }

  /**
   * Get the definition with the provided repository ID.
   *
   * @param sID
   *        The repository ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such definition is present.
   */
  @Nullable
  public RepoStorageDefinition getDefinitionOfID (@Nullable final String sID)
  {
    return m_aDefinitions.findFirst (x -> x.getID ().equals (sID));
  }

  /**
   * Create and initialise the top-level ToC service of a repository.
   *
   * @return <code>null</code> if initialisation failed. In that case an error was added.
   */
  @Nullable
  private static IRepoTopTocService _createTopTocService (@NonNull final RepoStorageDefinition aDefinition,
                                                          @NonNull final AbstractRepoStorageWithToc <?> aRepoStorage,
                                                          @NonNull final ErrorList aErrorList)
  {
    final IRepoTopTocService ret = switch (aDefinition.getTopTocType ())
    {
      case DIVER_XML -> new RepoTopTocServiceRepoBasedXML ();
      default -> throw new IllegalStateException ("Unsupported top-level ToC type " + aDefinition.getTopTocType ());
    };

    try
    {
      ret.initForRepo (aRepoStorage);
    }
    catch (final IllegalStateException ex)
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (aDefinition.getID ())
                                 .errorText ("Failed to read the top-level ToC of repository '" +
                                             aDefinition.getID () +
                                             "': " +
                                             ex.getMessage ())
                                 .linkedException (ex)
                                 .build ());
      return null;
    }

    if (aDefinition.getWritable ().isWriteEnabled ())
    {
      // Keep the already initialised top-level ToC up to date on writes
      aRepoStorage.setAuditor (new IRepoStorageAuditor ()
      {
        @Override
        public void onWrite (@NonNull final IRepoStorage aRepo,
                             @NonNull final RepoStorageKey aKey,
                             @NonNull final IRepoStorageContent aContent,
                             @NonNull final ESuccess eSuccess)
        {
          if (eSuccess.isSuccess () &&
              aRepoStorage.isEnableTocUpdates () &&
              aKey instanceof final RepoStorageKeyOfArtefact aArtefactKey)
          {
            ret.registerGroupAndArtifact (aArtefactKey.getCoordinate ().getGroupID (),
                                          aArtefactKey.getCoordinate ().getArtifactID ());
          }
        }
      });
    }
    return ret;
  }

  /**
   * Create all repository storages of this configuration. For each definition the factory of its
   * type is used, then hash verification is configured and the top-level ToC service is
   * initialised.
   *
   * @param aFactoryRegistry
   *        The factory registry to use. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>. Callers must check it for
   *        errors, because the definitions with errors are not part of the result.
   * @return The list of all successfully created storages in the order of declaration. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ConfiguredRepoStorage> createAllRepoStorages (@NonNull final RepoStorageFactoryRegistry aFactoryRegistry,
                                                                     @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aFactoryRegistry, "FactoryRegistry");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    final ICommonsList <ConfiguredRepoStorage> ret = new CommonsArrayList <> ();
    for (final RepoStorageDefinition aDefinition : m_aDefinitions)
    {
      final IRepoStorageFactory aFactory = aFactoryRegistry.getFactoryOfType (aDefinition.getType ());
      if (aFactory == null)
      {
        aErrorList.add (SingleError.builderError ()
                                   .errorFieldName (aDefinition.getID ())
                                   .errorText ("Repository '" +
                                               aDefinition.getID () +
                                               "' uses the unsupported type '" +
                                               aDefinition.getType () +
                                               "'. Supported types are: " +
                                               aFactoryRegistry.getAllTypes ())
                                   .build ());
        continue;
      }

      final AbstractRepoStorageWithToc <?> aRepoStorage = aFactory.createRepoStorage (aDefinition, aErrorList);
      if (aRepoStorage == null)
        continue;

      aRepoStorage.setVerifyHashOnRead (aDefinition.isVerifyHash ());

      IRepoTopTocService aTopTocService = null;
      if (aDefinition.getTopTocType () != ERepoTopTocType.NONE)
      {
        aTopTocService = _createTopTocService (aDefinition, aRepoStorage, aErrorList);
        if (aTopTocService == null)
          continue;
      }

      LOGGER.info ("Created repository storage '" + aDefinition.getID () + "' of type '" + aDefinition.getType () + "'");
      ret.add (new ConfiguredRepoStorage (aDefinition, aRepoStorage, aTopTocService));
    }
    return ret;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Definitions", m_aDefinitions).getToString ();
  }
}
