package com.helger.diver.repo.toc;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.string.StringHelper;
import com.helger.diver.api.version.VESID;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.toptoc.jaxb.v10.ArtifactType;
import com.helger.diver.repo.toptoc.jaxb.v10.GroupType;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;

/**
 * JAXB independent top ToC representation
 *
 * @author Philip Helger
 */
public class RepoTopToc
{
  private static final class Group
  {
    private final String m_sName;
    private final ICommonsSortedMap <String, Group> m_aSubGroups = new CommonsTreeMap <> ();
    private final ICommonsSortedSet <String> m_aArtifacts = new CommonsTreeSet <> ();

    public Group (@Nonnull @Nonempty final String sName)
    {
      ValueEnforcer.notEmpty (sName, "Name");
      ValueEnforcer.isTrue ( () -> VESID.isValidPart (sName), "Name is not a valid group part");
      m_sName = sName;
    }

    public boolean deepEquals (@Nonnull final Group rhs)
    {
      // Check all fields - takes longer
      return m_sName.equals (rhs.m_sName) &&
             m_aSubGroups.equals (rhs.m_aSubGroups) &&
             m_aArtifacts.equals (rhs.m_aArtifacts);
    }
  }

  private final ICommonsSortedMap <String, Group> m_aTopLevelGroups = new CommonsTreeMap <> ();

  public RepoTopToc ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSortedSet <String> getAllTopLevelGroupNames ()
  {
    return m_aTopLevelGroups.copyOfKeySet ();
  }

  @Nullable
  private Group _getGroup (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsList <String> aGroupPart = StringHelper.getExploded (RepoStorageKey.GROUP_LEVEL_SEPARATOR, sGroupID);
    // Resolve all recursive subgroups
    Group aGroup = m_aTopLevelGroups.get (aGroupPart.removeFirst ());
    while (aGroup != null && aGroupPart.isNotEmpty ())
    {
      aGroup = aGroup.m_aSubGroups.get (aGroupPart.removeFirst ());
    }
    return aGroup;
  }

  private void _recursiveIterateExistingSubGroups (@Nonnull @Nonempty final String sAbsoluteGroupID,
                                                   @Nonnull final Group aCurGroup,
                                                   @Nonnull final BiConsumer <String, String> aGroupNameConsumer)
  {
    for (final Map.Entry <String, Group> aEntry : aCurGroup.m_aSubGroups.entrySet ())
    {
      final String sSubGroupName = aEntry.getKey ();
      final String sSubAbsName = sAbsoluteGroupID + RepoStorageKey.GROUP_LEVEL_SEPARATOR + sSubGroupName;
      aGroupNameConsumer.accept (sSubGroupName, sSubAbsName);

      // Descend
      _recursiveIterateExistingSubGroups (sSubAbsName, aEntry.getValue (), aGroupNameConsumer);
    }
  }

  public void iterateAllSubGroups (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull final BiConsumer <String, String> aGroupNameConsumer)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notNull (aGroupNameConsumer, "GroupNameConsumer");

    final Group aGroup = _getGroup (sGroupID);
    if (aGroup != null)
    {
      _recursiveIterateExistingSubGroups (sGroupID, aGroup, aGroupNameConsumer);
    }
    // else: no group, no callback
  }

  public void iterateAllArtifacts (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull final Consumer <String> aArtifactNameConsumer)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notNull (aArtifactNameConsumer, "ArtifactNameConsumer");

    final Group aGroup = _getGroup (sGroupID);
    if (aGroup != null)
    {
      for (final String sArtifactID : aGroup.m_aArtifacts)
        aArtifactNameConsumer.accept (sArtifactID);
    }
    // else: no group, no callback
  }

  @Nonnull
  private Group _getOrCreateGroup (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsList <String> aGroupPart = StringHelper.getExploded (RepoStorageKey.GROUP_LEVEL_SEPARATOR, sGroupID);
    // Resolve all recursive subgroups
    Group aGroup = m_aTopLevelGroups.computeIfAbsent (aGroupPart.removeFirst (), Group::new);
    while (aGroupPart.isNotEmpty ())
    {
      aGroup = aGroup.m_aSubGroups.computeIfAbsent (aGroupPart.removeFirst (), Group::new);
    }
    return aGroup;
  }

  public void registerGroupAndArtifact (@Nonnull @Nonempty final String sGroupID,
                                        @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    final Group aGroup = _getOrCreateGroup (sGroupID);
    // Add to artifact list of latest subgroup
    aGroup.m_aArtifacts.add (sArtifactID);
  }

  public boolean deepEquals (@Nonnull final RepoTopToc aOther)
  {
    ValueEnforcer.notNull (aOther, "Other");

    // Size must match
    if (m_aTopLevelGroups.size () != aOther.m_aTopLevelGroups.size ())
      return false;

    for (final Map.Entry <String, Group> aEntry : m_aTopLevelGroups.entrySet ())
    {
      final Group aOtherGroup = aOther.m_aTopLevelGroups.get (aEntry.getKey ());
      if (aOtherGroup == null)
      {
        // Only present in this but not in other
        return false;
      }

      final Group aThisGroup = aEntry.getValue ();
      if (!aThisGroup.deepEquals (aOtherGroup))
      {
        // Groups differ
        return false;
      }
    }
    return true;
  }

  private static void _recursiveReadGroups (@Nonnull final String sAbsoluteGroupName,
                                            @Nonnull final GroupType aSrcGroup,
                                            @Nonnull final Group aDstGroup)
  {
    // First add artifacts
    for (final ArtifactType aSrcArtifact : aSrcGroup.getArtifact ())
    {
      final String sArtifactName = aSrcArtifact.getName ();
      if (!aDstGroup.m_aArtifacts.add (sArtifactName))
        throw new IllegalStateException ("The artifact '" +
                                         sAbsoluteGroupName +
                                         ':' +
                                         sArtifactName +
                                         "' is contained more then once");
    }

    // Now all subgroups
    for (final GroupType aSrcSubGroup : aSrcGroup.getGroup ())
    {
      final String sSubGroupName = aSrcSubGroup.getName ();
      final Group aDstSubGroup = new Group (sSubGroupName);
      if (aDstGroup.m_aSubGroups.put (sSubGroupName, aDstSubGroup) != null)
        throw new IllegalArgumentException ("Another group with name '" +
                                            sAbsoluteGroupName +
                                            RepoStorageKey.GROUP_LEVEL_SEPARATOR +
                                            sSubGroupName +
                                            "' is already contained");

      // Descend recursively
      _recursiveReadGroups (sAbsoluteGroupName + RepoStorageKey.GROUP_LEVEL_SEPARATOR + sSubGroupName,
                            aSrcSubGroup,
                            aDstSubGroup);
    }
  }

  @Nonnull
  public static RepoTopToc createFromJaxbObject (@Nonnull final RepoTopTocType aRepoTopToc)
  {
    ValueEnforcer.notNull (aRepoTopToc, "RepoTopToc");

    final RepoTopToc ret = new RepoTopToc ();
    for (final GroupType aSrcGroup : aRepoTopToc.getGroup ())
    {
      final String sGroupName = aSrcGroup.getName ();
      final Group aDstGroup = new Group (sGroupName);
      if (ret.m_aTopLevelGroups.put (sGroupName, aDstGroup) != null)
        throw new IllegalArgumentException ("Another top-level group with name '" +
                                            sGroupName +
                                            "' is already contained");
      _recursiveReadGroups (sGroupName, aSrcGroup, aDstGroup);
    }
    return ret;
  }
}
