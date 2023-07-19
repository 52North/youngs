/*
 * Copyright 2015-2023 52°North Spatial Information Research GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.youngs.impl;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ContextHelper {

    private static final String DEFAULT_NAMESPACE = "http://www.opengis.net/cat/csw/2.0.2";

    private static final Map<String, String> namespaceToContextPath = ImmutableMap.of(
            DEFAULT_NAMESPACE, "net.opengis.csw.v_2_0_2",
            "http://www.isotc211.org/2005/gmd", "net.opengis.csw.v_2_0_2"); // just need context to harvest response, not the content

    public static JAXBContext getContextForNamespace(String namespace) throws JAXBException {
        String contextPath = namespaceToContextPath.get(namespace);
        if(contextPath == null) {
            // return default context
            contextPath = namespaceToContextPath.get(DEFAULT_NAMESPACE);
        }
        return JAXBContext.newInstance(contextPath);
    }

}
