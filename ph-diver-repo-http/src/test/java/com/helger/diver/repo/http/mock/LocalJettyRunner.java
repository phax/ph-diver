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
import java.io.IOException;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.diver.repo.ERepoDeletable;
import com.helger.diver.repo.ERepoWritable;

import jakarta.servlet.http.HttpServletRequest;
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

    final AbstractHandler putHandler = new AbstractHandler ()
    {
      @Override
      public void handle (final String target,
                          final Request baseRequest,
                          final HttpServletRequest request,
                          final HttpServletResponse response) throws IOException
      {
        if (request.getMethod ().equals (EHttpMethod.PUT.getName ()))
        {
          final File targetFile = new File (aResourceBase, target);
          LOGGER.info ("Jetty PUT '" + targetFile.getAbsolutePath () + "'");

          FileHelper.ensureParentDirectoryIsPresent (targetFile);
          try (FileOutputStream outputStream = new FileOutputStream (targetFile, false))
          {
            int nBytesWritten = 0;
            int nBytesRead;
            final byte [] aBuffer = new byte [8192];
            while ((nBytesRead = baseRequest.getInputStream ().read (aBuffer)) != -1)
            {
              outputStream.write (aBuffer, 0, nBytesRead);
              nBytesWritten += nBytesRead;
            }
            LOGGER.info ("Jetty successfully PUT " +
                         nBytesWritten +
                         " bytes to file '" +
                         targetFile.getAbsolutePath () +
                         "'");
          }

          response.setStatus (HttpServletResponse.SC_OK);
          baseRequest.setHandled (true);
        }
      }
    };

    final AbstractHandler deleteHandler = new AbstractHandler ()
    {
      @Override
      public void handle (final String target,
                          final Request baseRequest,
                          final HttpServletRequest request,
                          final HttpServletResponse response) throws IOException
      {
        if (request.getMethod ().equals (EHttpMethod.DELETE.getName ()))
        {
          final File targetFile = new File (aResourceBase, target);
          LOGGER.info ("Jetty DELETE '" + targetFile.getAbsolutePath () + "'");
          FileOperationManager.INSTANCE.deleteFileIfExisting (targetFile);

          response.setStatus (HttpServletResponse.SC_OK);
          baseRequest.setHandled (true);
        }
      }
    };

    final ResourceHandler resourceHandler = new ResourceHandler ();
    resourceHandler.setBaseResource (Resource.newResource (aResourceBase));

    final ICommonsList <Handler> aHandlers = new CommonsArrayList <> ();
    if (eWriteEnabled.isWriteEnabled ())
      aHandlers.add (putHandler);
    if (eDeleteEnabled.isDeleteEnabled ())
      aHandlers.add (deleteHandler);
    aHandlers.addAll (resourceHandler, new DefaultHandler ());

    final HandlerList handlers = new HandlerList (aHandlers.toArray (new Handler [0]));
    m_aServer.setHandler (handlers);
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
