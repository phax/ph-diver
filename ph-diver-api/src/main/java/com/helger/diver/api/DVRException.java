package com.helger.diver.api;

import javax.annotation.Nonnull;

/**
 * A specific exception for DVRID handling
 *
 * @author Philip Helger
 * @since 2.0.0
 */
public class DVRException extends Exception
{
  public DVRException (@Nonnull final String sMsg)
  {
    super (sMsg);
  }
}
