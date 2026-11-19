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
package com.helger.diver.repo.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.diver.repo.config.RepoStorageDefinition;
import com.helger.diver.repo.config.RepoStorageDefinition.RepoStorageDefinitionBuilder;
import com.helger.diver.repo.config.RepoStorageFactoryHelper;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;

/**
 * Test class for class {@link RepoStorageFactoryS3}. No S3 service is needed, because creating the
 * client does not connect.
 *
 * @author Philip Helger
 */
public final class RepoStorageFactoryS3Test
{
  @NonNull
  private static RepoStorageDefinitionBuilder _builder ()
  {
    return RepoStorageDefinition.builder ()
                                .id ("s3-repo")
                                .type (RepoStorageFactoryS3.TYPE)
                                .writable (true)
                                .property (RepoStorageFactoryS3.PROPERTY_REGION, "eu-west-1");
  }

  @NonNull
  private static ErrorList _createWithErrors (@NonNull final RepoStorageDefinition aDefinition)
  {
    final ErrorList aErrorList = new ErrorList ();
    assertNull (new RepoStorageFactoryS3 ().createRepoStorage (aDefinition, aErrorList));
    assertTrue (aErrorList.containsAtLeastOneError ());
    return aErrorList;
  }

  @Test
  public void testSupportedPropertyNames ()
  {
    assertEquals (List.of ("bucket", "prefix", "region", "endpoint", "path-style", "profile", "allow-insecure"),
                  RepoStorageFactoryS3.getAllSupportedPropertyNames ().getCopyAsList ());
    // A copy is returned
    RepoStorageFactoryS3.getAllSupportedPropertyNames ().clear ();
    assertEquals (7, RepoStorageFactoryS3.getAllSupportedPropertyNames ().size ());
  }

  @Test
  public void testInvalidDefinitions ()
  {
    // Missing bucket
    _createWithErrors (_builder ().build ());

    // Invalid prefixes
    _createWithErrors (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET, "bucket")
                                  .property (RepoStorageFactoryS3.PROPERTY_PREFIX, "/abc/")
                                  .build ());
    _createWithErrors (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET, "bucket")
                                  .property (RepoStorageFactoryS3.PROPERTY_PREFIX, "abc")
                                  .build ());
    _createWithErrors (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET, "bucket")
                                  .property (RepoStorageFactoryS3.PROPERTY_PREFIX, "/")
                                  .build ());

    // Insecure endpoint
    final ErrorList aErrorList = _createWithErrors (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET,
                                                                          "bucket")
                                                              .property (RepoStorageFactoryS3.PROPERTY_ENDPOINT,
                                                                         "http://localhost:9090")
                                                              .build ());
    assertTrue (aErrorList.toString (),
                aErrorList.getFirstOrNull ().getErrorText (Locale.ROOT).contains ("allow-insecure"));

    // Invalid boolean
    _createWithErrors (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET, "bucket")
                                  .property (RepoStorageFactoryS3.PROPERTY_PATH_STYLE, "1")
                                  .build ());
  }

  @Test
  public void testCreate () throws Exception
  {
    final ErrorList aErrorList = new ErrorList ();
    final AbstractRepoStorageWithToc <?> aRepo = new RepoStorageFactoryS3 ().createRepoStorage (_builder ().property (RepoStorageFactoryS3.PROPERTY_BUCKET,
                                                                                                                       " bucket ")
                                                                                                             .property (RepoStorageFactoryS3.PROPERTY_PREFIX,
                                                                                                                        "repo/")
                                                                                                             .property (RepoStorageFactoryS3.PROPERTY_ENDPOINT,
                                                                                                                        "http://localhost:9090")
                                                                                                             .property (RepoStorageFactoryHelper.PROPERTY_ALLOW_INSECURE,
                                                                                                                        "true")
                                                                                                             .property (RepoStorageFactoryS3.PROPERTY_PATH_STYLE,
                                                                                                                        "true")
                                                                                                             .build (),
                                                                                                aErrorList);
    assertNotNull (aErrorList.toString (), aRepo);
    assertFalse (aErrorList.toString (), aErrorList.containsAtLeastOneFailure ());

    if (!(aRepo instanceof final RepoStorageS3 aS3Repo))
      throw new AssertionError ("Expected a RepoStorageS3 but got " + aRepo);
    assertEquals ("bucket", aS3Repo.getBucketName ());
    assertEquals ("repo/", aS3Repo.getDefaultKeyPrefix ());
    assertTrue (aS3Repo.canWrite ());
    assertFalse (aS3Repo.canDelete ());

    // The created storage owns its S3 client
    if (aRepo instanceof final AutoCloseable aCloseable)
      aCloseable.close ();
    else
      fail ("The created storage must be AutoCloseable");
  }
}
