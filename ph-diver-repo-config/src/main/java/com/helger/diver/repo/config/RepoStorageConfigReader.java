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

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.config.jaxb.RepoConfig1Marshaller;
import com.helger.diver.repo.config.jaxb.v10.RCRepositoryType;
import com.helger.diver.repo.config.jaxb.v10.RepoConfigType;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;

/**
 * Reader for the repository configuration XML files. The XML is validated against the XML Schema
 * {@link RepoConfig1Marshaller#XSD_RES}.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class RepoStorageConfigReader
{
  private RepoStorageConfigReader ()
  {}

  /**
   * Read the repository configuration from the provided resource.
   *
   * @param aRes
   *        The resource to read from. May not be <code>null</code>.
   * @param aBaseDir
   *        The base directory against which relative paths in the configuration are resolved. May
   *        be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return <code>null</code> if the resource does not exist or could not be read. In that case
   *         at least one error was added to the error list.
   */
  @Nullable
  public static RepoStorageConfig read (@NonNull final IReadableResource aRes,
                                        @Nullable final File aBaseDir,
                                        @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aRes, "Resource");
    ValueEnforcer.notNull (aErrorList, "ErrorList");

    if (!aRes.exists ())
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorText ("The repository configuration '" + aRes.getPath () + "' does not exist")
                                 .build ());
      return null;
    }

    final RepoConfigType aRepoConfig = new RepoConfig1Marshaller ().setCollectErrors (aErrorList).read (aRes);
    if (aRepoConfig == null)
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorText ("Failed to read the repository configuration '" + aRes.getPath () + "'")
                                 .build ());
      return null;
    }

    final ICommonsList <RepoStorageDefinition> aDefinitions = new CommonsArrayList <> ();
    for (final RCRepositoryType aRepository : aRepoConfig.getRepository ())
      aDefinitions.add (RepoStorageDefinition.createFromJaxbObject (aRepository, aBaseDir));
    return new RepoStorageConfig (aDefinitions);
  }

  /**
   * Read the repository configuration from the provided file. Relative paths in the configuration
   * are resolved against the directory of the file.
   *
   * @param aFile
   *        The file to read from. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to be filled. May not be <code>null</code>.
   * @return <code>null</code> if the file does not exist or could not be read. In that case at
   *         least one error was added to the error list.
   */
  @Nullable
  public static RepoStorageConfig read (@NonNull final File aFile, @NonNull final ErrorList aErrorList)
  {
    ValueEnforcer.notNull (aFile, "File");

    final File aAbsFile = aFile.getAbsoluteFile ();
    return read (new FileSystemResource (aAbsFile), aAbsFile.getParentFile (), aErrorList);
  }
}
