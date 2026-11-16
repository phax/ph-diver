/*
 * Copyright (C) 2023-2026 Philip Helger & ecosio
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;

/**
 * Test class for class {@link RepoStorageDefinition}.
 *
 * @author Philip Helger
 */
public final class RepoStorageDefinitionTest
{
  @Test
  public void testBuilderDefaults ()
  {
    final RepoStorageDefinition aDef = RepoStorageDefinition.builder ()
                                                            .id ("repo")
                                                            .type (RepoStorageFactoryLocalFileSystem.TYPE)
                                                            .build ();
    assertEquals ("repo", aDef.getID ());
    assertEquals ("localfs", aDef.getType ());
    assertEquals (ERepoWritable.WITHOUT_WRITE, aDef.getWritable ());
    assertEquals (ERepoDeletable.WITHOUT_DELETE, aDef.getDeletable ());
    assertTrue (aDef.isVerifyHash ());
    assertEquals (ERepoTopTocType.NONE, aDef.getTopTocType ());
    assertTrue (aDef.getAllProperties ().isEmpty ());
    assertNull (aDef.getBaseDirectory ());
  }

  @Test
  public void testBuilderAllFields ()
  {
    final File aBaseDir = new File ("target").getAbsoluteFile ();
    final RepoStorageDefinition aDef = RepoStorageDefinition.builder ()
                                                            .id ("repo")
                                                            .type ("localfs")
                                                            .writable (true)
                                                            .deletable (ERepoDeletable.WITH_DELETE)
                                                            .verifyHash (false)
                                                            .topTocType (ERepoTopTocType.DIVER_XML)
                                                            .property ("basedir", "dir")
                                                            .property ("foo", "bar")
                                                            .baseDirectory (aBaseDir)
                                                            .build ();
    assertEquals (ERepoWritable.WITH_WRITE, aDef.getWritable ());
    assertEquals (ERepoDeletable.WITH_DELETE, aDef.getDeletable ());
    assertFalse (aDef.isVerifyHash ());
    assertEquals (ERepoTopTocType.DIVER_XML, aDef.getTopTocType ());
    assertEquals ("[basedir, foo]", aDef.getAllProperties ().keySet ().toString ());
    assertEquals ("dir", aDef.getPropertyValue ("basedir"));
    assertEquals (aBaseDir, aDef.getBaseDirectory ());
    assertEquals (new File (aBaseDir, "dir"), aDef.getResolvedFile ("dir"));

    // Copy builder
    final RepoStorageDefinition aCopy = RepoStorageDefinition.builder (aDef)
                                                             .id ("repo2")
                                                             .property ("foo", null)
                                                             .build ();
    assertNotSame (aDef, aCopy);
    assertEquals ("repo2", aCopy.getID ());
    assertEquals (ERepoWritable.WITH_WRITE, aCopy.getWritable ());
    assertEquals (ERepoTopTocType.DIVER_XML, aCopy.getTopTocType ());
    assertEquals ("[basedir]", aCopy.getAllProperties ().keySet ().toString ());
    assertEquals (aBaseDir, aCopy.getBaseDirectory ());

    // Source is unchanged
    assertEquals ("repo", aDef.getID ());
    assertTrue (aDef.hasProperty ("foo"));
  }

  @Test
  public void testBuilderProperties ()
  {
    final ICommonsOrderedMap <String, String> aProps = new CommonsLinkedHashMap <> ();
    aProps.put ("b", "2");
    aProps.put ("a", "1");
    final RepoStorageDefinition aDef = RepoStorageDefinition.builder ()
                                                            .id ("repo")
                                                            .type ("localfs")
                                                            .property ("x", "y")
                                                            .properties (aProps)
                                                            .build ();
    // Replaced and in the provided order
    assertEquals ("[b, a]", aDef.getAllProperties ().keySet ().toString ());

    // Modifying the source map does not modify the definition
    aProps.put ("c", "3");
    assertFalse (aDef.hasProperty ("c"));
  }

  @Test
  public void testBuilderMissingValues ()
  {
    try
    {
      RepoStorageDefinition.builder ().type ("localfs").build ();
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected - no ID
    }

    try
    {
      RepoStorageDefinition.builder ().id ("repo").build ();
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected - no type
    }

    try
    {
      RepoStorageDefinition.builder ().id ("repo").type ("localfs").topTocType (null).build ();
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected - no top-level ToC type
    }
  }
}
