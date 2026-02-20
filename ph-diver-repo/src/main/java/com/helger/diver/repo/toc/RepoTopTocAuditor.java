package com.helger.diver.repo.toc;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsConcurrentHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.diver.api.coord.DVRCoordinate;
import com.helger.diver.repo.IRepoStorage;
import com.helger.diver.repo.IRepoStorageAuditor;
import com.helger.diver.repo.IRepoStorageContent;
import com.helger.diver.repo.RepoStorageKey;
import com.helger.diver.repo.RepoStorageKeyOfArtefact;

/**
 * Special {@link IRepoStorageAuditor} implementation to handle {@link IRepoTopTocService}
 * instances.
 *
 * @author Philip Helger
 * @since 4.2.0
 */
public class RepoTopTocAuditor implements IRepoStorageAuditor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RepoTopTocAuditor.class);

  // Key is the repository ID
  private final ICommonsMap <String, Object> m_aTopTocServiceInitialized = new CommonsConcurrentHashMap <> ();
  private final IRepoTopTocService m_aTopTocService;

  public RepoTopTocAuditor (@NonNull final IRepoTopTocService aTopTocService)
  {
    ValueEnforcer.notNull (aTopTocService, "TopTocService");
    m_aTopTocService = aTopTocService;
  }

  @NonNull
  private IRepoTopTocService _getTopTocService (@NonNull final IRepoStorageWithToc aRepoWithToc)
  {
    final String sRepoID = aRepoWithToc.getID ();
    if (m_aTopTocServiceInitialized.put (sRepoID, Boolean.TRUE) == null)
    {
      LOGGER.info ("Begin initializing TopToc for repo '" + sRepoID + "'");
      m_aTopTocService.initForRepo (aRepoWithToc);
      LOGGER.info ("Done initializing TopToc for repo '" + sRepoID + "'");
    }

    return m_aTopTocService;
  }

  public void onWrite (@NonNull final IRepoStorage aRepo,
                       @NonNull final RepoStorageKey aKey,
                       @NonNull final IRepoStorageContent aContent,
                       @NonNull final ESuccess eSuccess)
  {
    // We only care about success
    if (eSuccess.isSuccess () &&
        aRepo instanceof final IRepoStorageWithToc aRepoWithToc &&
        aRepoWithToc.isEnableTocUpdates () &&
        aKey instanceof final RepoStorageKeyOfArtefact aArtefactKey)
    {
      // Update top-level ToC
      final DVRCoordinate aCoord = aArtefactKey.getCoordinate ();
      _getTopTocService (aRepoWithToc).registerGroupAndArtifact (aCoord.getGroupID (), aCoord.getArtifactID ());
    }
  }
}
