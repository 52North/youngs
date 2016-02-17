/*
 * Copyright 2015-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.util;

import com.jayway.jsonassert.JsonAssert;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 *
 * @author Alistair Todd, via https://cleantestcode.wordpress.com/2014/05/10/hamcrest-json-matchers/
 */
public class JsonMatchers {

    public static <T> Matcher<String> hasJsonPath(
            final String jsonPath, final Matcher<T> matches) {

        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String json) {
                try {
                    JsonAssert.with(json).assertThat(jsonPath, matches);
                    return true;
                } catch (AssertionError | Exception e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText(" JSON object with a value at node ")
                        .appendValue(jsonPath)
                        .appendText(" that is ")
                        .appendDescriptionOf(matches);
            }
        };
    }
}
