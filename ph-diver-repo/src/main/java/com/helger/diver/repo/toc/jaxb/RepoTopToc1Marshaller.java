/*
 * Copyright (C) 2023-2024 Philip Helger & ecosio
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
package com.helger.diver.repo.toc.jaxb;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.diver.repo.toptoc.jaxb.v10.ObjectFactory;
import com.helger.diver.repo.toptoc.jaxb.v10.RepoTopTocType;
import com.helger.jaxb.GenericJAXBMarshaller;

/**
 * JAXB marshaller for {@link RepoTopTocType} objects.
 *
 * @author Philip Helger
 */
public class RepoTopToc1Marshaller extends GenericJAXBMarshaller <RepoTopTocType>
{
  /**
   * The namespace URI of the RepoToc1 XML data model
   */
  public static final String NAMESPACE_URI = "urn:com:helger:diver:repotoptoc:v1.0";

  /**
   * The XML Schema path to validate against.
   */
  public static final ClassPathResource XSD_RES = new ClassPathResource ("schemas/repotoptoc-1.0.xsd",
                                                                         RepoTopToc1Marshaller.class.getClassLoader ());

  public RepoTopToc1Marshaller ()
  {
    super (RepoTopTocType.class, new CommonsArrayList <> (XSD_RES), new ObjectFactory ()::createRepotoptoc);
  }
}
