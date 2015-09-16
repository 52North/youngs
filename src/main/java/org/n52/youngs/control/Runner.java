/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.control;

import org.n52.youngs.api.Report;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.load.Sink;
import org.n52.youngs.transform.Mapper;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface Runner {

    public Runner harvest(Source source);

    public Runner transform(Mapper mapper);

    public Report load(Sink sink);

    public double getCompletedPercentage();

}
