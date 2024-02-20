package com.helger.diver.repo;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.io.stream.StreamHelper;

/**
 * Utility class
 *
 * @author Philip Helger
 */
@Immutable
public final class RepoStorageContentHelper
{
  private RepoStorageContentHelper ()
  {}

  @Nullable
  public static String getAsUtf8String (@Nullable final IRepoStorageContent aContent)
  {
    if (aContent == null)
      return null;

    // Shortcut?
    if (aContent instanceof RepoStorageContentByteArray)
      return ((RepoStorageContentByteArray) aContent).getAsUtf8String ();

    // Generic way
    final byte [] aBytes = StreamHelper.getAllBytes (aContent);
    return new String (aBytes, StandardCharsets.UTF_8);
  }
}
