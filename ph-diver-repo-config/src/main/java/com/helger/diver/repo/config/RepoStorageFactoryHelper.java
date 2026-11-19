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

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;

/**
 * Helper methods for {@link IRepoStorageFactory} implementations to validate the repository type
 * specific properties of a {@link RepoStorageDefinition}.
 *
 * @author Philip Helger
 * @since 4.4.0
 */
@Immutable
public final class RepoStorageFactoryHelper
{
  /**
   * The name of the property that allows insecure (non-TLS) connections, e.g. <code>http://</code>
   * URLs. Shared by all network based repository types.
   */
  public static final String PROPERTY_ALLOW_INSECURE = "allow-insecure";

  private RepoStorageFactoryHelper ()
  {}

  /**
   * Add a warning for each property of the definition that is not supported by the repository
   * type.
   *
   * @param aDefinition
   *        The definition to check. May not be <code>null</code>.
   * @param aSupportedProperties
   *        The names of all supported properties. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to add the warnings to. May not be <code>null</code>.
   */
  public static void warnOnUnsupportedProperties (@NonNull final RepoStorageDefinition aDefinition,
                                                  @NonNull final Set <String> aSupportedProperties,
                                                  @NonNull final ErrorList aErrorList)
  {
    final String sRepoID = aDefinition.getID ();
    for (final String sName : aDefinition.getAllProperties ().keySet ())
      if (!aSupportedProperties.contains (sName))
        aErrorList.add (SingleError.builderWarn ()
                                   .errorFieldName (sRepoID)
                                   .errorText ("Repository '" +
                                               sRepoID +
                                               "' of type '" +
                                               aDefinition.getType () +
                                               "' contains the unsupported property '" +
                                               sName +
                                               "' - it is ignored")
                                   .build ());
  }

  /**
   * Get the trimmed value of a property.
   *
   * @param aDefinition
   *        The definition to use. May not be <code>null</code>.
   * @param sName
   *        The property name. May neither be <code>null</code> nor empty.
   * @return <code>null</code> if the property is not present or empty after trimming.
   */
  @Nullable
  public static String getTrimmedProperty (@NonNull final RepoStorageDefinition aDefinition,
                                           @NonNull @Nonempty final String sName)
  {
    final String ret = StringHelper.trim (aDefinition.getPropertyValue (sName));
    return StringHelper.isEmpty (ret) ? null : ret;
  }

  /**
   * Get the value of a required property, or add an error if it is missing.
   *
   * @param aDefinition
   *        The definition to use. May not be <code>null</code>.
   * @param sName
   *        The property name. May neither be <code>null</code> nor empty.
   * @param aErrorList
   *        The error list to add the error to. May not be <code>null</code>.
   * @return <code>null</code> if the property is missing or empty. In that case an error was
   *         added.
   */
  @Nullable
  public static String getRequiredProperty (@NonNull final RepoStorageDefinition aDefinition,
                                            @NonNull @Nonempty final String sName,
                                            @NonNull final ErrorList aErrorList)
  {
    final String ret = getTrimmedProperty (aDefinition, sName);
    if (ret == null)
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (aDefinition.getID ())
                                 .errorText ("Repository '" +
                                             aDefinition.getID () +
                                             "' of type '" +
                                             aDefinition.getType () +
                                             "' is missing the required property '" +
                                             sName +
                                             "'")
                                 .build ());
    return ret;
  }

  /**
   * Get the value of a boolean property.
   *
   * @param aDefinition
   *        The definition to use. May not be <code>null</code>.
   * @param sName
   *        The property name. May neither be <code>null</code> nor empty.
   * @param bDefault
   *        The value to use if the property is not present.
   * @param aErrorList
   *        The error list to add the error to. May not be <code>null</code>.
   * @return <code>null</code> if the value is neither <code>true</code> nor <code>false</code>. In
   *         that case an error was added.
   */
  @Nullable
  public static Boolean getBooleanProperty (@NonNull final RepoStorageDefinition aDefinition,
                                            @NonNull @Nonempty final String sName,
                                            final boolean bDefault,
                                            @NonNull final ErrorList aErrorList)
  {
    final String sValue = getTrimmedProperty (aDefinition, sName);
    if (sValue == null)
      return Boolean.valueOf (bDefault);
    if ("true".equals (sValue))
      return Boolean.TRUE;
    if ("false".equals (sValue))
      return Boolean.FALSE;

    aErrorList.add (SingleError.builderError ()
                               .errorFieldName (aDefinition.getID ())
                               .errorText ("The property '" +
                                           sName +
                                           "' of repository '" +
                                           aDefinition.getID () +
                                           "' must be 'true' or 'false' but is '" +
                                           sValue +
                                           "'")
                               .build ());
    return null;
  }

  /**
   * Check that a URL uses <code>https://</code>, or <code>http://</code> if insecure connections
   * are allowed.
   *
   * @param aDefinition
   *        The definition the URL belongs to. May not be <code>null</code>.
   * @param sName
   *        The name of the property containing the URL. May neither be <code>null</code> nor
   *        empty.
   * @param sURL
   *        The URL to check. May not be <code>null</code>.
   * @param bAllowInsecure
   *        <code>true</code> if <code>http://</code> is allowed.
   * @param aErrorList
   *        The error list to add the error to. May not be <code>null</code>.
   * @return <code>true</code> if the URL is acceptable, <code>false</code> if an error was added.
   */
  public static boolean checkURLScheme (@NonNull final RepoStorageDefinition aDefinition,
                                        @NonNull @Nonempty final String sName,
                                        @NonNull final String sURL,
                                        final boolean bAllowInsecure,
                                        @NonNull final ErrorList aErrorList)
  {
    final String sLowerURL = sURL.toLowerCase (Locale.ROOT);
    if (sLowerURL.startsWith ("https://"))
      return true;

    final String sErrorText;
    if (sLowerURL.startsWith ("http://"))
    {
      if (bAllowInsecure)
        return true;
      sErrorText = "The property '" +
                   sName +
                   "' of repository '" +
                   aDefinition.getID () +
                   "' uses an insecure 'http://' URL - use 'https://' or set the property '" +
                   PROPERTY_ALLOW_INSECURE +
                   "' to 'true'";
    }
    else
      sErrorText = "The property '" +
                   sName +
                   "' of repository '" +
                   aDefinition.getID () +
                   "' must be an 'https://' URL but is '" +
                   sURL +
                   "'";

    aErrorList.add (SingleError.builderError ().errorFieldName (aDefinition.getID ()).errorText (sErrorText).build ());
    return false;
  }

  /**
   * Resolve a secret from the ph-config configuration, using the key given in a property. With the
   * default configuration a key such as <code>diver.repo.password</code> is resolved from the
   * system property of that name, the environment variable <code>DIVER_REPO_PASSWORD</code> or the
   * application properties files. The secret itself is never part of the repository configuration
   * and never part of an error message.
   *
   * @param aDefinition
   *        The definition to use. May not be <code>null</code>.
   * @param sName
   *        The name of the property containing the configuration key. May neither be
   *        <code>null</code> nor empty.
   * @param aConfig
   *        The configuration to resolve the key in. May not be <code>null</code>.
   * @param aErrorList
   *        The error list to add the error to. May not be <code>null</code>.
   * @return <code>null</code> if the property is not present, or if the configuration key has no
   *         or an empty value (in that case an error was added).
   */
  @Nullable
  public static String getSecretFromConfig (@NonNull final RepoStorageDefinition aDefinition,
                                            @NonNull @Nonempty final String sName,
                                            @NonNull final IConfig aConfig,
                                            @NonNull final ErrorList aErrorList)
  {
    final String sConfigKey = getTrimmedProperty (aDefinition, sName);
    if (sConfigKey == null)
      return null;

    final String ret = aConfig.getAsString (sConfigKey);
    if (StringHelper.isEmpty (ret))
    {
      aErrorList.add (SingleError.builderError ()
                                 .errorFieldName (aDefinition.getID ())
                                 .errorText ("The configuration key '" +
                                             sConfigKey +
                                             "' referenced by the property '" +
                                             sName +
                                             "' of repository '" +
                                             aDefinition.getID () +
                                             "' has no value")
                                 .build ());
      return null;
    }
    return ret;
  }
}
