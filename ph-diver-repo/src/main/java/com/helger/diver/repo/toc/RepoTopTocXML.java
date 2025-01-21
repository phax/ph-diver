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
package com.helger.diver.repo.toc;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.diver.api.settings.DVRValidityHelper;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;
import com.helger.diver.repo.toptoc.jaxb.v10.ArtifactType;
import com.helger.diver.repo.toptoc.jaxb.v10.GroupType;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;

/**
 * JAXB independent top ToC representation
 *
 * @author Philip Helger
 */
public class RepoTopTocXML
{
  /**
   * Represents a single group with a name, a list of sub groups and a list of
   * artifacts.
   *
   * @author Philip Helger
   */
  private static final class Group
  {
    private final String m_sName;
    private final ICommonsSortedMap <String, Group> m_aSubGroups = new CommonsTreeMap <> ();
    private final ICommonsSortedSet <String> m_aArtifacts = new CommonsTreeSet <> ();

    public Group (@Nonnull @Nonempty final String sName)
    {
      ValueEnforcer.notEmpty (sName, "Name");
      ValueEnforcer.isTrue ( () -> DVRValidityHelper.isValidCoordinateGroupID (sName),
                             "Name is not a valid group part");
      m_sName = sName;
    }

    public boolean deepEquals (@Nonnull final Group rhs)
    {
      // Check all fields - takes longer
      return m_sName.equals (rhs.m_sName) &&
             m_aSubGroups.equals (rhs.m_aSubGroups) &&
             m_aArtifacts.equals (rhs.m_aArtifacts);
    }

    @Nonnull
    public GroupType getAsJaxbObject ()
    {
      final GroupType ret = new GroupType ();
      ret.setName (m_sName);

      // Hack: Make sure the internal lists are created (important for equals)
      ret.getArtifact ();
      ret.getGroup ();

      for (final String sArtifactID : m_aArtifacts)
      {
        final ArtifactType aJaxbArtifact = new ArtifactType ();
        aJaxbArtifact.setName (sArtifactID);
        ret.addArtifact (aJaxbArtifact);
      }

      // recursive descend
      for (final Group aSubGroup : m_aSubGroups.values ())
        ret.addGroup (aSubGroup.getAsJaxbObject ());
      return ret;
    }
  }

  private final ICommonsSortedMap <String, Group> m_aTopLevelGroups = new CommonsTreeMap <> ();

  public RepoTopTocXML ()
  {}

  public int getTopLevelGroupCount ()
  {
    return m_aTopLevelGroups.size ();
  }

  @Nullable
  private Group _getGroup (@Nonnull @Nonempty final String sGroupID)
  {
    final ICommonsList <String> aGroupPart = StringHelper.getExploded (RepoStorageKeyOfArtefact.GROUP_LEVEL_SEPARATOR,
                                                                       sGroupID);
    // Resolve all recursive subgroups
    Group aGroup = m_aTopLevelGroups.get (aGroupPart.removeFirstOrNull ());
    while (aGroup != null && aGroupPart.isNotEmpty ())
    {
      aGroup = aGroup.m_aSubGroups.get (aGroupPart.removeFirstOrNull ());
    }
    return aGroup;
  }

  public boolean containsGroupAndArtifact (@Nonnull @Nonempty final String sGroupID,
                                           @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    final Group aGroup = _getGroup (sGroupID);
    if (aGroup == null)
      return false;

    return aGroup.m_aArtifacts.contains (sArtifactID);
  }

  public void iterateAllTopLevelGroupNames (@Nonnull final Consumer <String> aGroupNameConsumer)
  {
    ValueEnforcer.notNull (aGroupNameConsumer, "GroupNameConsumer");

    m_aTopLevelGroups.keySet ().forEach (aGroupNameConsumer);
  }

  private void _recursiveIterateExistingSubGroups (@Nonnull @Nonempty final String sAbsoluteGroupID,
                                                   @Nonnull final Group aCurGroup,
                                                   @Nonnull final IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                                                   final boolean bRecursive)
  {
    for (final Map.Entry <String, Group> aEntry : aCurGroup.m_aSubGroups.entrySet ())
    {
      final String sSubGroupName = aEntry.getKey ();
      final String sSubGroupAbsoluteName = sAbsoluteGroupID +
                                           RepoStorageKeyOfArtefact.GROUP_LEVEL_SEPARATOR +
                                           sSubGroupName;
      aGroupNameConsumer.accept (sSubGroupName, sSubGroupAbsoluteName);

      // Descend always or never
      if (bRecursive)
        _recursiveIterateExistingSubGroups (sSubGroupAbsoluteName, aEntry.getValue (), aGroupNameConsumer, true);
    }
  }

  public void iterateAllSubGroups (@Nonnull @Nonempty final String sGroupID,
                                   @Nonnull final IRepoTopTocGroupNameConsumer aGroupNameConsumer,
                                   final boolean bRecursive)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notNull (aGroupNameConsumer, "GroupNameConsumer");

    final Group aGroup = _getGroup (sGroupID);
    if (aGroup != null)
    {
      _recursiveIterateExistingSubGroups (sGroupID, aGroup, aGroupNameConsumer, bRecursive);
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
    final ICommonsList <String> aGroupPart = StringHelper.getExploded (RepoStorageKeyOfArtefact.GROUP_LEVEL_SEPARATOR,
                                                                       sGroupID);
    // Resolve all recursive subgroups
    Group aGroup = m_aTopLevelGroups.computeIfAbsent (aGroupPart.removeFirstOrNull (), Group::new);
    while (aGroupPart.isNotEmpty ())
    {
      aGroup = aGroup.m_aSubGroups.computeIfAbsent (aGroupPart.removeFirstOrNull (), Group::new);
    }
    return aGroup;
  }

  @Nonnull
  public EChange registerGroupAndArtifact (@Nonnull @Nonempty final String sGroupID,
                                           @Nonnull @Nonempty final String sArtifactID)
  {
    ValueEnforcer.notEmpty (sGroupID, "GroupID");
    ValueEnforcer.notEmpty (sArtifactID, "ArtifactID");

    final Group aGroup = _getOrCreateGroup (sGroupID);

    // Add to artifact list of latest subgroup
    final boolean bAdded = aGroup.m_aArtifacts.add (sArtifactID);

    return EChange.valueOf (bAdded);
  }

  public boolean deepEquals (@Nonnull final RepoTopTocXML aOther)
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

  @Nonnull
  public RepoTopTocType getAsJaxbObject ()
  {
    final RepoTopTocType ret = new RepoTopTocType ();
    for (final Map.Entry <String, Group> aEntry : m_aTopLevelGroups.entrySet ())
      ret.addGroup (aEntry.getValue ().getAsJaxbObject ());
    return ret;
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
                                            RepoStorageKeyOfArtefact.GROUP_LEVEL_SEPARATOR +
                                            sSubGroupName +
                                            "' is already contained");

      // Descend recursively
      _recursiveReadGroups (sAbsoluteGroupName + RepoStorageKeyOfArtefact.GROUP_LEVEL_SEPARATOR + sSubGroupName,
                            aSrcSubGroup,
                            aDstSubGroup);
    }
  }

  @Nonnull
  public static RepoTopTocXML createFromJaxbObject (@Nonnull final RepoTopTocType aRepoTopToc)
  {
    ValueEnforcer.notNull (aRepoTopToc, "RepoTopToc");

    final RepoTopTocXML ret = new RepoTopTocXML ();
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
