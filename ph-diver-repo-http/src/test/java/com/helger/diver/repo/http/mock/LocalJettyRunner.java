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
package com.helger.diver.repo.http.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;
import com.helger.http.EHttpMethod;
import com.helger.io.file.FileHelper;
import com.helger.io.file.FileOperationManager;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletResponse;

public class LocalJettyRunner
{
  public static final int DEFAULT_PORT = 8282;
  public static final String DEFAULT_ACCESS_URL = "http://localhost:" + DEFAULT_PORT + "/";

  public static final File DEFAULT_TEST_RESOURCE_BASE = new File ("src/test/resources/test-http");

  private static final Logger LOGGER = LoggerFactory.getLogger (LocalJettyRunner.class);

  private final Server m_aServer;

  public LocalJettyRunner (@Nonnegative final int nPort,
                           @Nonnull final File aResourceBase,
                           @Nonnull final ERepoWritable eWriteEnabled,
                           @Nonnull final ERepoDeletable eDeleteEnabled)
  {
    LOGGER.info ("Starting Jetty with resource base dir '" + aResourceBase.getAbsolutePath () + "'");
    m_aServer = new Server (nPort);

    final Handler.Abstract putHandler = new Handler.Abstract ()
    {
      public boolean handle (final Request request, final Response response, final Callback callback) throws Exception
      {
        if (request.getMethod ().equals (EHttpMethod.PUT.getName ()))
        {
          final File targetFile = new File (aResourceBase, request.getHttpURI ().getPath ());
          LOGGER.info ("Jetty PUT '" +
                       targetFile.getAbsolutePath () +
                       "' for " +
                       request.getHeaders ().getLongField (HttpHeader.CONTENT_LENGTH) +
                       " bytes");

          FileHelper.ensureParentDirectoryIsPresent (targetFile);
          try (final FileOutputStream outputStream = new FileOutputStream (targetFile, false))
          {
            final InputStream aIS = Content.Source.asInputStream (request);
            final MutableLong aCounter = new MutableLong (0);
            StreamHelper.copyByteStream ()
                        .from (aIS)
                        .closeFrom (false)
                        .to (outputStream)
                        .closeTo (true)
                        .copyByteCount (aCounter)
                        .build ();
            LOGGER.info ("Jetty successfully PUT " +
                         aCounter.longValue () +
                         " bytes to file '" +
                         targetFile.getAbsolutePath () +
                         "'");
          }

          // Status before payload!
          response.setStatus (HttpServletResponse.SC_NO_CONTENT);

          // Important to write something empty
          response.write (true, StandardCharsets.UTF_8.encode (""), callback);
          return true;
        }
        return false;
      }
    };

    final Handler.Abstract deleteHandler = new Handler.Abstract ()
    {
      public boolean handle (final Request request, final Response response, final Callback callback) throws Exception
      {
        if (request.getMethod ().equals (EHttpMethod.DELETE.getName ()))
        {
          final File targetFile = new File (aResourceBase, request.getHttpURI ().getPath ());
          LOGGER.info ("Jetty DELETE '" + targetFile.getAbsolutePath () + "'");
          FileOperationManager.INSTANCE.deleteFileIfExisting (targetFile);

          // Status before payload!
          response.setStatus (HttpServletResponse.SC_NO_CONTENT);

          // Important to write something empty
          response.write (true, StandardCharsets.UTF_8.encode (""), callback);
          return true;
        }
        return false;
      }
    };

    final Handler.Abstract getHandler = new Handler.Abstract ()
    {
      public boolean handle (final Request request, final Response response, final Callback callback) throws Exception
      {
        if (request.getMethod ().equals (EHttpMethod.GET.getName ()))
        {
          final File targetFile = new File (aResourceBase, request.getHttpURI ().getPath ());
          if (targetFile.isFile ())
          {
            LOGGER.info ("Jetty GET [found] '" + targetFile.getAbsolutePath () + "'");

            // Status before payload!
            response.setStatus (HttpServletResponse.SC_OK);
            response.write (true, ByteBuffer.wrap (Files.readAllBytes (targetFile.toPath ())), callback);
          }
          else
          {
            // Important to write something empty
            LOGGER.info ("Jetty GET [not found] '" + targetFile.getAbsolutePath () + "'");

            // Status before payload!
            response.setStatus (HttpServletResponse.SC_NOT_FOUND);
            response.write (true, StandardCharsets.UTF_8.encode (""), callback);
          }
          return true;
        }
        return false;
      }
    };

    final Handler.Sequence sequence = new Handler.Sequence ();

    // Write handler
    if (eWriteEnabled.isWriteEnabled ())
      sequence.addHandler (putHandler);

    // Delete handler
    if (eDeleteEnabled.isDeleteEnabled ())
      sequence.addHandler (deleteHandler);

    // Read handler
    sequence.addHandler (getHandler);

    // Built-in read handler
    if (false)
    {
      final ResourceHandler resourceHandler = new ResourceHandler ();
      final ResourceFactory m_aRF = new PathResourceFactory ();
      final var aBase = m_aRF.newResource (aResourceBase.getAbsoluteFile ().toURI ());
      LOGGER.info ("Using base path " + aBase);
      resourceHandler.setBaseResource (aBase);
      sequence.addHandler (resourceHandler);
    }

    // Rest handler
    if (false)
      sequence.addHandler (new DefaultHandler ());

    m_aServer.setHandler (sequence);
  }

  public void startJetty () throws Exception
  {
    m_aServer.start ();
  }

  public void stopJetty () throws Exception
  {
    m_aServer.stop ();
  }

  @Nonnull
  public static LocalJettyRunner createDefaultTestInstance (@Nonnull final ERepoWritable eWriteEnabled,
                                                            @Nonnull final ERepoDeletable eDeleteEnabled)
  {
    return new LocalJettyRunner (DEFAULT_PORT, DEFAULT_TEST_RESOURCE_BASE, eWriteEnabled, eDeleteEnabled);
  }
}
