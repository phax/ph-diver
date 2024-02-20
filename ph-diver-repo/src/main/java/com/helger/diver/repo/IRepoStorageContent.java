package com.helger.diver.repo;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.io.IHasInputStreamAndReader;

/**
 * The main content of an {@link IRepoStorageItem}.
 *
 * @author Philip Helger
 */
public interface IRepoStorageContent extends IHasInputStreamAndReader
{
  /**
   * @return The number of bytes of the item. Must be &ge; 0.
   */
  @Nonnegative
  long getLength ();

  // TODO remove in ph-commons 11.1.5 or later
  @Nullable
  default <T> T withInputStreamDo (@Nonnull final Function <InputStream, T> aFunc) throws IOException
  {
    ValueEnforcer.notNull (aFunc, "Func");
    try (final InputStream aIS = getInputStream ())
    {
      return aFunc.apply (aIS);
    }
  }

  // Use stream based where possible
  @Deprecated
  @Nonnull
  byte [] getAllBytesNoCopy ();

  /**
   * @return The whole content as an UTF-8 String. This is merely available for
   *         testing purposes. May be <code>null</code>.
   */
  @Nullable
  String getAsUtf8String ();
}
