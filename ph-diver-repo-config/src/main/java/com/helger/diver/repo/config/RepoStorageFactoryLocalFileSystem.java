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

import java.io.File;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.diver.repo.impl.RepoStorageLocalFileSystem;

/**
 * {@link IRepoStorageFactory} for {@link RepoStorageLocalFileSystem}. The only supported property
 * is {@link #PROPERTY_BASEDIR}, which is required. A relative base directory is resolved against
 * {@link RepoStorageDefinition#getBaseDirectory()}.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
public class RepoStorageFactoryLocalFileSystem implements IRepoStorageFactory
{
  /** The repository type ID */
  public static final String TYPE = RepoStorageType.LOCAL_FILE_SYSTEM.getID ();
  /** The name of the property containing the base directory of the repository */
  public static final String PROPERTY_BASEDIR = "basedir";

  @NonNull
  @Nonempty
  public String getType ()
  {
    return TYPE;
  }

  @Nullable
  public AbstractRepoStorageWithToc <?> createRepoStorage (@NonNull final RepoStorageDefinition aDefinition,
                                                           @NonNull final ErrorList aErrorList)
  {
    final String sRepoID = aDefinition.getID ();

    for (final String sName : aDefinition.getAllProperties ().keySet ())
      if (!PROPERTY_BASEDIR.equals (sName))
        aErrorList.add (SingleError.builderWarn ()
                                   .errorFieldName (sRepoID)
                                   .errorText ("Repository '" +
                                               sRepoID +
                                               "' of type '" +
                                               TYPE +
                                               "' contains the unsupported property '" +
                                               sName +
                                               "' - it is ignored")
                                   .build ());

    final String sBaseDir = StringHelper.trim (aDefinition.getPropertyValue (PROPERTY_BASEDIR));
    if (StringHelper.isEmpty (sBaseDir))
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText ("Repository '" +
                                             sRepoID +
                                             "' of type '" +
                                             TYPE +
                                             "' is missing the required property '" +
                                             PROPERTY_BASEDIR +
                                             "'")
                                 .build ());
      return null;
    }

    final File aBaseDir = aDefinition.getResolvedFile (sBaseDir);
    if (aBaseDir.isFile ())
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText ("The base directory '" +
                                             aBaseDir.getAbsolutePath () +
                                             "' of repository '" +
                                             sRepoID +
                                             "' is an existing file")
                                 .build ());
      return null;
    }

    // A read-only repository is never created implicitly
    if (aDefinition.getWritable ().isWriteDisabled () && !aBaseDir.isDirectory ())
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (sRepoID)
                                 .errorText ("The base directory '" +
                                             aBaseDir.getAbsolutePath () +
                                             "' of the read-only repository '" +
                                             sRepoID +
                                             "' does not exist")
                                 .build ());
      return null;
    }

    return new RepoStorageLocalFileSystem (aBaseDir, sRepoID, aDefinition.getWritable (), aDefinition.getDeletable ());
  }
}
