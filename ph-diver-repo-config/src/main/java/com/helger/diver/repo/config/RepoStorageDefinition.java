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
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.builder.IBuilder;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.config.jaxb.v10.RCPropertyType;
import com.helger.diver.repo.config.jaxb.v10.RCRepositoryType;
import com.helger.diver.repo.impl.AbstractRepoStorage;

/**
 * The definition of a single repository, as read from the repository configuration. It contains
 * everything that is needed to create the repository storage, but not the storage itself. Use
 * {@link #builder()} to create new instances.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class RepoStorageDefinition
{
  private final String m_sID;
  private final String m_sType;
  private final ERepoWritable m_eWritable;
  private final ERepoDeletable m_eDeletable;
  private final boolean m_bVerifyHash;
  private final ERepoTopTocType m_eTopTocType;
  private final ICommonsOrderedMap <String, String> m_aProperties;
  private final File m_aBaseDir;

  /**
   * Constructor - use the builder.
   *
   * @param sID
   *        The unique repository ID. May neither be <code>null</code> nor empty.
   * @param sType
   *        The repository type ID. May neither be <code>null</code> nor empty.
   * @param eWritable
   *        Is writing allowed? May not be <code>null</code>.
   * @param eDeletable
   *        Is deletion allowed? May not be <code>null</code>.
   * @param bVerifyHash
   *        <code>true</code> to verify the hash values on reading
   * @param eTopTocType
   *        The kind of top-level ToC to use. May not be <code>null</code>.
   * @param aProperties
   *        The repository type specific properties. May not be <code>null</code>.
   * @param aBaseDir
   *        The base directory against which relative paths are resolved. This is usually the
   *        directory of the configuration file. May be <code>null</code>.
   */
  private RepoStorageDefinition (@NonNull @Nonempty final String sID,
                                 @NonNull @Nonempty final String sType,
                                 @NonNull final ERepoWritable eWritable,
                                 @NonNull final ERepoDeletable eDeletable,
                                 final boolean bVerifyHash,
                                 @NonNull final ERepoTopTocType eTopTocType,
                                 @NonNull final Map <String, String> aProperties,
                                 @Nullable final File aBaseDir)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notEmpty (sType, "Type");
    ValueEnforcer.notNull (eWritable, "Writable");
    ValueEnforcer.notNull (eDeletable, "Deletable");
    ValueEnforcer.notNull (eTopTocType, "TopTocType");
    ValueEnforcer.notNull (aProperties, "Properties");
    m_sID = sID;
    m_sType = sType;
    m_eWritable = eWritable;
    m_eDeletable = eDeletable;
    m_bVerifyHash = bVerifyHash;
    m_eTopTocType = eTopTocType;
    m_aProperties = new CommonsLinkedHashMap <> (aProperties);
    m_aBaseDir = aBaseDir;
  }

  /**
   * @return The unique repository ID. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return The repository type ID. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getType ()
  {
    return m_sType;
  }

  @NonNull
  public ERepoWritable getWritable ()
  {
    return m_eWritable;
  }

  @NonNull
  public ERepoDeletable getDeletable ()
  {
    return m_eDeletable;
  }

  public boolean isVerifyHash ()
  {
    return m_bVerifyHash;
  }

  @NonNull
  public ERepoTopTocType getTopTocType ()
  {
    return m_eTopTocType;
  }

  /**
   * @return A copy of all repository type specific properties in the order of declaration. Never
   *         <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsOrderedMap <String, String> getAllProperties ()
  {
    return m_aProperties.getClone ();
  }

  public boolean hasProperty (@Nullable final String sName)
  {
    return m_aProperties.containsKey (sName);
  }

  /**
   * Get the value of the property with the provided name.
   *
   * @param sName
   *        The name of the property. May be <code>null</code>.
   * @return <code>null</code> if no such property is present.
   */
  @Nullable
  public String getPropertyValue (@Nullable final String sName)
  {
    return m_aProperties.get (sName);
  }

  /**
   * @return The base directory against which relative paths are resolved. May be <code>null</code>.
   */
  @Nullable
  public File getBaseDirectory ()
  {
    return m_aBaseDir;
  }

  /**
   * Resolve the provided path against the base directory, if the path is relative and a base
   * directory is present.
   *
   * @param sPath
   *        The path to resolve. May neither be <code>null</code> nor empty.
   * @return The resolved, absolute file. Never <code>null</code>.
   */
  @NonNull
  public File getResolvedFile (@NonNull @Nonempty final String sPath)
  {
    ValueEnforcer.notEmpty (sPath, "Path");
    final File aFile = new File (sPath);
    if (aFile.isAbsolute () || m_aBaseDir == null)
      return aFile.getAbsoluteFile ();
    return new File (m_aBaseDir, sPath).getAbsoluteFile ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ID", m_sID)
                                       .append ("Type", m_sType)
                                       .append ("Writable", m_eWritable)
                                       .append ("Deletable", m_eDeletable)
                                       .append ("VerifyHash", m_bVerifyHash)
                                       .append ("TopTocType", m_eTopTocType)
                                       .append ("Properties", m_aProperties)
                                       .appendIfNotNull ("BaseDir", m_aBaseDir)
                                       .getToString ();
  }

  /**
   * Builder class for {@link RepoStorageDefinition} objects. The defaults match the defaults of the
   * XML Schema: not writable, not deletable, hash verification enabled and no top-level ToC.
   *
   * @author Philip Helger
   * @since 4.3.0
   */
  @NotThreadSafe
  public static class RepoStorageDefinitionBuilder implements IBuilder <RepoStorageDefinition>
  {
    private String m_sID;
    private String m_sType;
    private ERepoWritable m_eWritable = ERepoWritable.WITHOUT_WRITE;
    private ERepoDeletable m_eDeletable = ERepoDeletable.WITHOUT_DELETE;
    private boolean m_bVerifyHash = AbstractRepoStorage.DEFAULT_VERIFY_HASH_VALUE;
    private ERepoTopTocType m_eTopTocType = ERepoTopTocType.NONE;
    private final ICommonsOrderedMap <String, String> m_aProperties = new CommonsLinkedHashMap <> ();
    private File m_aBaseDir;

    public RepoStorageDefinitionBuilder ()
    {}

    /**
     * Copy constructor from an existing {@link RepoStorageDefinition}.
     *
     * @param aSrc
     *        The source definition to copy from. May not be <code>null</code>.
     */
    public RepoStorageDefinitionBuilder (@NonNull final RepoStorageDefinition aSrc)
    {
      ValueEnforcer.notNull (aSrc, "Src");
      m_sID = aSrc.m_sID;
      m_sType = aSrc.m_sType;
      m_eWritable = aSrc.m_eWritable;
      m_eDeletable = aSrc.m_eDeletable;
      m_bVerifyHash = aSrc.m_bVerifyHash;
      m_eTopTocType = aSrc.m_eTopTocType;
      m_aProperties.putAll (aSrc.m_aProperties);
      m_aBaseDir = aSrc.m_aBaseDir;
    }

    /**
     * Set the unique repository ID.
     *
     * @param s
     *        The repository ID. May be <code>null</code>, but must be set before building.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder id (@Nullable final String s)
    {
      m_sID = s;
      return this;
    }

    /**
     * Set the repository type ID, e.g. {@link RepoStorageFactoryLocalFileSystem#TYPE}.
     *
     * @param s
     *        The repository type ID. May be <code>null</code>, but must be set before building.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder type (@Nullable final String s)
    {
      m_sType = s;
      return this;
    }

    /**
     * Define whether writing is allowed.
     *
     * @param e
     *        The writable state. May be <code>null</code>, but must be set before building.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder writable (@Nullable final ERepoWritable e)
    {
      m_eWritable = e;
      return this;
    }

    /**
     * Define whether writing is allowed.
     *
     * @param b
     *        <code>true</code> if writing is allowed
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder writable (final boolean b)
    {
      return writable (ERepoWritable.valueOf (b));
    }

    /**
     * Define whether deletion is allowed.
     *
     * @param e
     *        The deletable state. May be <code>null</code>, but must be set before building.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder deletable (@Nullable final ERepoDeletable e)
    {
      m_eDeletable = e;
      return this;
    }

    /**
     * Define whether deletion is allowed.
     *
     * @param b
     *        <code>true</code> if deletion is allowed
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder deletable (final boolean b)
    {
      return deletable (ERepoDeletable.valueOf (b));
    }

    /**
     * Define whether hash values should be verified on reading.
     *
     * @param b
     *        <code>true</code> to verify the hash values
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder verifyHash (final boolean b)
    {
      m_bVerifyHash = b;
      return this;
    }

    /**
     * Set the kind of top-level ToC of the repository.
     *
     * @param e
     *        The top-level ToC type. May be <code>null</code>, but must be set before building.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder topTocType (@Nullable final ERepoTopTocType e)
    {
      m_eTopTocType = e;
      return this;
    }

    /**
     * Add or replace a single repository type specific property.
     *
     * @param sName
     *        The property name. May neither be <code>null</code> nor empty.
     * @param sValue
     *        The property value. May be <code>null</code> to remove the property.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder property (@NonNull @Nonempty final String sName,
                                                        @Nullable final String sValue)
    {
      ValueEnforcer.notEmpty (sName, "Name");
      if (sValue == null)
        m_aProperties.remove (sName);
      else
        m_aProperties.put (sName, sValue);
      return this;
    }

    /**
     * Replace all repository type specific properties with the provided ones.
     *
     * @param aProperties
     *        The properties to use. May be <code>null</code> to remove all properties.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder properties (@Nullable final Map <String, String> aProperties)
    {
      m_aProperties.clear ();
      if (aProperties != null)
        for (final Map.Entry <String, String> aEntry : aProperties.entrySet ())
          property (aEntry.getKey (), aEntry.getValue ());
      return this;
    }

    /**
     * Set the base directory against which relative paths are resolved. This is usually the
     * directory of the configuration file.
     *
     * @param a
     *        The base directory. May be <code>null</code>.
     * @return this for chaining
     */
    @NonNull
    public final RepoStorageDefinitionBuilder baseDirectory (@Nullable final File a)
    {
      m_aBaseDir = a;
      return this;
    }

    @NonNull
    public RepoStorageDefinition build ()
    {
      if (StringHelper.isEmpty (m_sID))
        throw new IllegalStateException ("The repository ID is missing");
      if (StringHelper.isEmpty (m_sType))
        throw new IllegalStateException ("The repository type of '" + m_sID + "' is missing");
      if (m_eWritable == null)
        throw new IllegalStateException ("The writable state of repository '" + m_sID + "' is missing");
      if (m_eDeletable == null)
        throw new IllegalStateException ("The deletable state of repository '" + m_sID + "' is missing");
      if (m_eTopTocType == null)
        throw new IllegalStateException ("The top-level ToC type of repository '" + m_sID + "' is missing");

      return new RepoStorageDefinition (m_sID,
                                        m_sType,
                                        m_eWritable,
                                        m_eDeletable,
                                        m_bVerifyHash,
                                        m_eTopTocType,
                                        m_aProperties,
                                        m_aBaseDir);
    }
  }

  /**
   * @return A new builder for {@link RepoStorageDefinition} objects. Never <code>null</code>.
   */
  @NonNull
  public static RepoStorageDefinitionBuilder builder ()
  {
    return new RepoStorageDefinitionBuilder ();
  }

  /**
   * Create a new builder that is pre-filled with the values of the provided definition.
   *
   * @param aSrc
   *        The source definition to copy from. May not be <code>null</code>.
   * @return A new builder for {@link RepoStorageDefinition} objects. Never <code>null</code>.
   */
  @NonNull
  public static RepoStorageDefinitionBuilder builder (@NonNull final RepoStorageDefinition aSrc)
  {
    return new RepoStorageDefinitionBuilder (aSrc);
  }

  /**
   * Create a new definition from the provided JAXB object.
   *
   * @param aRepository
   *        The JAXB object as read from the XML. May not be <code>null</code>.
   * @param aBaseDir
   *        The base directory against which relative paths are resolved. May be <code>null</code>.
   * @return The new definition. Never <code>null</code>.
   */
  @NonNull
  public static RepoStorageDefinition createFromJaxbObject (@NonNull final RCRepositoryType aRepository,
                                                            @Nullable final File aBaseDir)
  {
    ValueEnforcer.notNull (aRepository, "Repository");

    final ERepoTopTocType eTopTocType = ERepoTopTocType.getFromIDOrNull (aRepository.getToptoc ().value ());
    if (eTopTocType == null)
      throw new IllegalStateException ("Unsupported top-level ToC type '" + aRepository.getToptoc ().value () + "'");

    final RepoStorageDefinitionBuilder aBuilder = builder ().id (aRepository.getId ())
                                                            .type (aRepository.getType ())
                                                            .writable (aRepository.isWritable ())
                                                            .deletable (aRepository.isDeletable ())
                                                            .verifyHash (aRepository.isVerifyHash ())
                                                            .topTocType (eTopTocType)
                                                            .baseDirectory (aBaseDir);
    for (final RCPropertyType aProperty : aRepository.getProperty ())
      aBuilder.property (aProperty.getName (), aProperty.getValue ());
    return aBuilder.build ();
  }
}
