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

import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class NamespaceContextImpl implements NamespaceContext {

    private final BiMap<String, String> prefixToNamespace = HashBiMap.create();

    /**
     * @param namespaceMap a map containing namespace prefixes as keys and namespaces as values.
     */
    public NamespaceContextImpl(Map<String, String> namespaceMap) {
        prefixToNamespace.putAll(namespaceMap);
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefixToNamespace.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return prefixToNamespace.inverse().get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return Collections.singleton(getPrefix(namespaceURI)).iterator();
    }

    public static NamespaceContext create() {
        return new NamespaceContextImpl(new ImmutableMap.Builder<String, String>().
                put("gmd", "http://www.isotc211.org/2005/gmd").
                put("gco", "http://www.isotc211.org/2005/gco").
                put("eum", "http://www.eumetsat.int/2008/gmi").
                put("gmi", "http://www.isotc211.org/2005/gmi").
                put("ogc", "http://www.opengis.net/ogc").
                put("xlink", "http://www.w3.org/1999/xlink").
                put("fn", "http://www.w3.org/TR/xpath-functions").
                put("str", "http://exslt.org/strings").
                put("xdt", "http://www.w3.org/2005/02/xpath-datatypes").
                put("xsi", "http://www.w3.org/2001/XMLSchema-instance").
                put("xs", "http://www.w3.org/2001/XMLSchema").
                put("ows", "http://www.opengis.net/ows").
                put("csw", "http://www.opengis.net/cat/csw/2.0.2").
                put("srv", "http://www.isotc211.org/2005/srv").
                put("dc", "http://purl.org/dc/elements/1.1/").
                put("dct", "http://purl.org/dc/terms/").
                put("inspire_ds", "http://inspire.ec.europa.eu/schemas/inspire_ds/1.0").
                put("inspire_c", "http://inspire.ec.europa.eu/schemas/common/1.0").build());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("namespaces", Arrays.deepToString(prefixToNamespace.entrySet().toArray()))
                .toString();
    }

}
