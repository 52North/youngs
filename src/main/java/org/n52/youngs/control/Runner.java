/*
 * Copyright 2015-2020 52°North Initiative for Geospatial Open Source
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

import java.util.List;
import org.n52.youngs.api.Report;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.load.Sink;
import org.n52.youngs.postprocess.PostProcessor;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.validation.XmlSchemaValidator;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface Runner {

    Runner harvest(Source source);

    Runner transform(Mapper mapper);

    Runner postTransformProcess(PostProcessor postProcessor);

    Runner withValidators(List<XmlSchemaValidator> vals);

    Report load(Sink sink);

    double getCompletedPercentage();
}
