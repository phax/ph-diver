package com.helger.diver.api.version;

import javax.annotation.Nonnull;

import com.helger.commons.version.Version;

public interface IPseudoVersionComparable
{
  int compareToPseudoVersion (@Nonnull IVESPseudoVersion aOtherPseudoVersion);

  int compareToVersion (@Nonnull Version aStaticVersion);
}
