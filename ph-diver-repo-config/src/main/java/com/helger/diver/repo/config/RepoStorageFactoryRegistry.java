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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.collection.commons.ICommonsSortedSet;

/**
 * Registry of all {@link IRepoStorageFactory} instances, indexed by their repository type. Factories
 * are registered explicitly (and not via SPI), so that factories with external dependencies (like
 * an HTTP client) can be constructed by the application.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@ThreadSafe
public class RepoStorageFactoryRegistry
{
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsSortedMap <String, IRepoStorageFactory> m_aMap = new CommonsTreeMap <> ();

  public RepoStorageFactoryRegistry ()
  {}

  /**
   * Register a new factory.
   *
   * @param aFactory
   *        The factory to register. May not be <code>null</code>.
   * @return {@link EChange#UNCHANGED} if another factory for the same type is already registered.
   */
  @NonNull
  public EChange registerFactory (@NonNull final IRepoStorageFactory aFactory)
  {
    ValueEnforcer.notNull (aFactory, "Factory");

    final String sType = aFactory.getType ();
    return m_aRWLock.writeLockedGet ( () -> {
      if (m_aMap.containsKey (sType))
        return EChange.UNCHANGED;
      m_aMap.put (sType, aFactory);
      return EChange.CHANGED;
    });
  }

  /**
   * Unregister the factory of the provided type.
   *
   * @param sType
   *        The repository type. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if a factory was removed.
   */
  @NonNull
  public EChange unregisterFactory (@Nullable final String sType)
  {
    return m_aRWLock.writeLockedGet ( () -> m_aMap.removeObject (sType));
  }

  /**
   * Get the factory of the provided repository type.
   *
   * @param sType
   *        The repository type. May be <code>null</code>.
   * @return <code>null</code> if no factory is registered for that type.
   */
  @Nullable
  public IRepoStorageFactory getFactoryOfType (@Nullable final String sType)
  {
    return m_aRWLock.readLockedGet ( () -> m_aMap.get (sType));
  }

  /**
   * @return A sorted copy of all registered repository types. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSortedSet <String> getAllTypes ()
  {
    return m_aRWLock.readLockedGet (m_aMap::copyOfKeySet);
  }

  /**
   * @return A new registry with all factories of this module registered. Currently this is only
   *         {@link RepoStorageFactoryLocalFileSystem}. Never <code>null</code>.
   */
  @NonNull
  public static RepoStorageFactoryRegistry createDefault ()
  {
    final RepoStorageFactoryRegistry ret = new RepoStorageFactoryRegistry ();
    ret.registerFactory (new RepoStorageFactoryLocalFileSystem ());
    return ret;
  }
}
