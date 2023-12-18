package com.helger.diver.repo.toc;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.state.ESuccess;

/**
 * Base interface for a top-level ToC
 *
 * @author Philip Helger
 * @since 1.1.0
 */
public interface IRepoTopToc
{
  @FunctionalInterface
  public interface IGroupNameConsumer
  {
    /**
     * Consumer callback
     *
     * @param sRelativeGroupName
     *        Relative group name. Neither <code>null</code> nor empty.
     * @param aAbsoluteGroupName
     *        Absolute group name. Neither <code>null</code> nor empty.
     */
    void accept (@Nonnull @Nonempty String sRelativeGroupName, @Nonnull @Nonempty String aAbsoluteGroupName);
  }

  void iterateAllTopLevelGroupNames (@Nonnull Consumer <String> aGroupNameConsumer);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsSortedSet <String> getAllTopLevelGroupNames ()
  {
    final ICommonsSortedSet <String> ret = new CommonsTreeSet <> ();
    iterateAllTopLevelGroupNames (ret::add);
    return ret;
  }

  void iterateAllSubGroups (@Nonnull @Nonempty String sGroupID,
                            @Nonnull IGroupNameConsumer aGroupNameConsumer,
                            boolean bRecursive);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNamesRecursive (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), true);
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllAbsoluteSubGroupNames (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllSubGroups (sGroupID, (rgn, agn) -> ret.add (agn), false);
    return ret;
  }

  void iterateAllArtifacts (@Nonnull @Nonempty String sGroupID, @Nonnull Consumer <String> aArtifactNameConsumer);

  @Nonnull
  @ReturnsMutableCopy
  default ICommonsOrderedSet <String> getAllArtefacts (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsOrderedSet <String> ret = new CommonsLinkedHashSet <> ();
    iterateAllArtifacts (sGroupID, ret::add);
    return ret;
  }

  @Nonnull
  ESuccess registerGroupAndArtifact (@Nonnull @Nonempty String sGroupID, @Nonnull @Nonempty String sArtifactID);
}
