/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
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
package org.n52.youngs.test;

import javax.xml.xpath.XPathFactory;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import org.n52.youngs.impl.XPathHelper;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class XPathHelperTest {

    @Test
    public void defaultFactory() {
        XPathHelper helper = new XPathHelper();
        XPathFactory factory = XPathFactory.newInstance();
        assertThat("default factory supports 1.0", helper.isXPath10Supported(factory));
        assertThat("default factory does not support 2.0", not(helper.isXPath20Supported(factory)));
    }

    @Test
    public void factoryFromHelper() {
        XPathHelper helper = new XPathHelper();
        XPathFactory factory = helper.newXPathFactory();
        assertThat("default factory supports 1.0", helper.isXPath10Supported(factory));
        assertThat("default factory supports 2.0", helper.isXPath20Supported(factory));
    }

}
