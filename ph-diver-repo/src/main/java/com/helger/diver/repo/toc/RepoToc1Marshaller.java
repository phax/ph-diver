package com.helger.diver.repo.toc;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.diver.repo.toc.jaxb.v10.ObjectFactory;
import com.helger.diver.repo.toc.jaxb.v10.RepoTocType;
import com.helger.jaxb.GenericJAXBMarshaller;

/**
 * JAXB marshaller for {@link RepoTocType} objects.
 *
 * @author Philip Helger
 */
public class RepoToc1Marshaller extends GenericJAXBMarshaller <RepoTocType>
{
  /**
   * The namespace URI of the RepoToc1 XML data model
   */
  public static final String NAMESPACE_URI = "urn:com:helger:diver:repotoc:v1.0";

  /**
   * The XML Schema path to validate against.
   */
  public static final ClassPathResource XSD_RES = new ClassPathResource ("schemas/repotoc/repotoc-1.0.xsd",
                                                                         RepoToc1Marshaller.class.getClassLoader ());

  public RepoToc1Marshaller ()
  {
    super (RepoTocType.class, new CommonsArrayList <> (XSD_RES), new ObjectFactory ()::createRepotoc);
  }
}
