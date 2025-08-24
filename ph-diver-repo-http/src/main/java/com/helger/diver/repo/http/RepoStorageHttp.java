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
package com.helger.diver.repo.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.WillNotClose;
import com.helger.base.array.ArrayHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.log.ConditionalLogger;
import com.helger.base.state.ESuccess;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageType;
import com.helger.diver.repo.impl.AbstractRepoStorageWithToc;
import com.helger.diver.repo.toc.IRepoTopTocService;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.io.file.FilenameHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Base implementation of {@link IRepoStorage} for arbitrary HTTP connections. Supports HTTP GET,
 * PUT and DELETE.
 *
 * @author Philip Helger
 */
public class RepoStorageHttp extends AbstractRepoStorageWithToc <RepoStorageHttp>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoStorageHttp.class);
  private static final ConditionalLogger CONDLOG = new ConditionalLogger (LOGGER);

  protected final HttpClientManager m_aHttpClient;
  protected final String m_sURLPrefix;

  private Consumer <? super HttpGet> m_aReadCustomizer;
  private Consumer <? super HttpPut> m_aWriteCustomizer;
  private Consumer <? super HttpDelete> m_aDeleteCustomizer;

  public RepoStorageHttp (@Nonnull @WillNotClose final HttpClientManager aHttpClient,
                          @Nonnull @Nonempty final String sURLPrefix,
                          @Nonnull @Nonempty final String sID,
                          @Nonnull final ERepoWritable eWriteEnabled,
                          @Nonnull final ERepoDeletable eDeleteEnabled,
                          @Nonnull final IRepoTopTocService aTopTocService)
  {
    super (RepoStorageType.HTTP, sID, eWriteEnabled, eDeleteEnabled, aTopTocService);
    ValueEnforcer.notNull (aHttpClient, "HttpClient");
    ValueEnforcer.notEmpty (sURLPrefix, "URLPrefix");
    m_aHttpClient = aHttpClient;
    m_sURLPrefix = sURLPrefix;
  }

  /**
   * @return <code>true</code> if logging is disabled, <code>false</code> if it is enabled.
   * @since 4.0.0
   */
  public static boolean isSilentMode ()
  {
    return CONDLOG.isDisabled ();
  }

  /**
   * Enable or disable certain regular log messages.
   *
   * @param bSilentMode
   *        <code>true</code> to disable logging, <code>false</code> to enable logging
   * @return The previous value of the silent mode.
   * @since 4.0.0
   */
  public static boolean setSilentMode (final boolean bSilentMode)
  {
    return !CONDLOG.setEnabled (!bSilentMode);
  }

  @Nullable
  public final Consumer <? super HttpGet> getReadCusomizer ()
  {
    return m_aReadCustomizer;
  }

  @Nonnull
  public final RepoStorageHttp setReadCusomizer (@Nullable final Consumer <? super HttpGet> aReadCustomizer)
  {
    m_aReadCustomizer = aReadCustomizer;
    return this;
  }

  @Nullable
  public final Consumer <? super HttpPut> getWriteCusomizer ()
  {
    return m_aWriteCustomizer;
  }

  @Nonnull
  public final RepoStorageHttp setWriteCusomizer (@Nullable final Consumer <? super HttpPut> aWriteCustomizer)
  {
    m_aWriteCustomizer = aWriteCustomizer;
    return this;
  }

  @Nullable
  public final Consumer <? super HttpDelete> getDeleteCusomizer ()
  {
    return m_aDeleteCustomizer;
  }

  @Nonnull
  public final RepoStorageHttp setDeleteCusomizer (@Nullable final Consumer <? super HttpDelete> aDeleteCustomizer)
  {
    m_aDeleteCustomizer = aDeleteCustomizer;
    return this;
  }

  public boolean exists (@Nonnull final RepoStorageKey aKey)
  {
    ValueEnforcer.notNull (aKey, "Key");

    // Not ideal, but what shall we do :)
    return read (aKey) != null;
  }

  @Override
  @Nullable
  protected InputStream getInputStream (@Nonnull final RepoStorageKey aKey)
  {
    final String sURL = FilenameHelper.getCleanConcatenatedUrlPath (m_sURLPrefix, aKey.getPath ());
    CONDLOG.info ( () -> "Reading from HTTP '" + sURL + "'");

    try
    {
      final HttpGet aGet = new HttpGet (sURL);
      if (m_aReadCustomizer != null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Now customizing HttpGet");
        m_aReadCustomizer.accept (aGet);
      }

      final byte [] aResponse = m_aHttpClient.execute (aGet, new ResponseHandlerByteArray ());

      CONDLOG.info ( () -> "Found on HTTP '" + sURL + "' for " + ArrayHelper.getSize (aResponse) + " bytes");
      return new NonBlockingByteArrayInputStream (aResponse);
    }
    catch (final IOException ex)
    {
      CONDLOG.warn ( () -> "Failed to read from HTTP '" + sURL + "': " + ex.getMessage ());
      return null;
    }
  }

  @Override
  @Nonnull
  protected ESuccess writeObject (@Nonnull final RepoStorageKey aKey, @Nonnull final IRepoStorageContent aContent)
  {
    final String sURL = FilenameHelper.getCleanConcatenatedUrlPath (m_sURLPrefix, aKey.getPath ());
    CONDLOG.info ( () -> "Writing to HTTP '" + sURL + "'");

    try
    {
      final HttpPut aPut = new HttpPut (sURL);
      aPut.setEntity (new InputStreamEntity (aContent.getBufferedInputStream (),
                                             aContent.getLength (),
                                             ContentType.APPLICATION_OCTET_STREAM));
      if (m_aWriteCustomizer != null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Now customizing HttpPut");
        m_aWriteCustomizer.accept (aPut);
      }

      final byte [] aResponse = m_aHttpClient.execute (aPut, new ResponseHandlerByteArray ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("HTTP PUT result: " +
                      (aResponse == null ? null : new String (aResponse, StandardCharsets.UTF_8)));
      // Ignore the response
    }
    catch (final IOException ex)
    {
      CONDLOG.warn ( () -> "Failed to write to HTTP '" + sURL + "': " + ex.getMessage ());
      return ESuccess.FAILURE;
    }

    CONDLOG.info ( () -> "Successfully wrote to HTTP '" + sURL + "'");
    return ESuccess.SUCCESS;
  }

  @Override
  @Nonnull
  protected ESuccess deleteObject (@Nonnull final RepoStorageKey aKey)
  {
    final String sURL = FilenameHelper.getCleanConcatenatedUrlPath (m_sURLPrefix, aKey.getPath ());
    CONDLOG.info ( () -> "Deleting from HTTP '" + sURL + "'");

    try
    {
      final HttpDelete aDelete = new HttpDelete (sURL);
      if (m_aDeleteCustomizer != null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Now customizing HttpDelete");
        m_aDeleteCustomizer.accept (aDelete);
      }

      final byte [] aResponse = m_aHttpClient.execute (aDelete, new ResponseHandlerByteArray ());
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("HTTP DELETE result: " +
                      (aResponse == null ? null : new String (aResponse, StandardCharsets.UTF_8)));
      // Ignore the response
    }
    catch (final IOException ex)
    {
      CONDLOG.warn ( () -> "Failed to delete from HTTP '" + sURL + "': " + ex.getMessage ());
      return ESuccess.FAILURE;
    }

    CONDLOG.info ( () -> "Successfully deleted from HTTP '" + sURL + "'");
    return ESuccess.SUCCESS;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("HttpClient", m_aHttpClient)
                            .append ("UrlPrefix", m_sURLPrefix)
                            .append ("ReadCustomizer", m_aReadCustomizer)
                            .append ("WriteCustomizer", m_aWriteCustomizer)
                            .append ("DeleteCustomizer", m_aDeleteCustomizer)
                            .getToString ();
  }
}
