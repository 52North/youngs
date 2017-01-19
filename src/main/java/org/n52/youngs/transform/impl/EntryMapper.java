/*
 * Copyright 2015-2017 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.transform.impl;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class EntryMapper {

    private static final Logger log = LoggerFactory.getLogger(EntryMapper.class);

    private static final Map<String, String> DEFAULT_OUTPUT_PROPERTIES = ImmutableMap.of(
            OutputKeys.OMIT_XML_DECLARATION, "no",
            OutputKeys.INDENT, "no",
            OutputKeys.ENCODING, Charsets.UTF_8.name());

    private final Optional<Transformer> stripspaceTransformer;
    private final Transformer defaultTransformer;

    public EntryMapper() {
        this(Optional.empty(), null);
    }

    public EntryMapper(Optional<Transformer> stripspaceTransformer, Transformer defaultTransformer) {
        this.stripspaceTransformer = stripspaceTransformer;
        this.defaultTransformer = defaultTransformer;
    }

    public Optional<EvalResult> mapEntry(MappingEntry entry, final Node node) {
        log.trace("Applying field mapping '{}' to node: {}", entry.getFieldName(), node);

        Optional<EvalResult> result = Optional.empty();
        // try nodeset first
        try {
            if (entry.hasCondition() && !assertCondition(node, entry.getCondition())) {
                log.info("Condition '{}' not matched, skipping", entry.getCondition());
                return Optional.empty();
            }

            Object nodesetResult = entry.getXPath().evaluate(node, XPathConstants.NODESET);

            if (entry.getChildren() != null && !entry.getChildren().isEmpty()) {
                result = mapChildren(nodesetResult, entry);
            }
            else {
                result = Optional.ofNullable(handleEvaluationResult(nodesetResult, entry.getFieldName()));
            }

            if (result.isPresent()) {
                log.trace("Found nodeset result: {}", result.get());
            }
        } catch (XPathExpressionException e) {
            log.debug("Error selecting field {} as nodeset, could be XPath 2.0 expression... trying evaluation to string."
                    + " Error was: {}", entry.getFieldName(), e.getMessage());
            log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
        }

        // try string eval if nodeset did not work
        if (!result.isPresent()) {
            try {
                String stringResult = (String) entry.getXPath().evaluate(node, XPathConstants.STRING);
                result = Optional.ofNullable(handleEvaluationResult(stringResult, entry.getFieldName()));
                if (result.isPresent()) {
                    log.trace("Found string result: {}", result.get());
                }
            } catch (XPathExpressionException e) {
                log.warn("Error selecting field {} as string: {}", entry.getFieldName(), e.getMessage());
                log.trace("Error selecting field {} as string", entry.getFieldName(), e);
            }
        }

        if (result.isPresent()) {
            EvalResult er = result.get();
            if (entry.hasReplacements()) {
                er = handleReplacements(entry, er);
            }
            if (entry.hasSplit()) {
                er = handleSplit(entry, er);
            }

            result = Optional.of(er);
        }

        return result;
    }

    private Optional<EvalResult> mapChildren(Object nodesetResult, MappingEntry entry) {
        Optional<EvalResult> result;

        // special case: children --> nested object
        List<Node> nodes;
        if (nodesetResult instanceof NodeList) {
            NodeList tmp = (NodeList) nodesetResult;
            nodes = new ArrayList<>(tmp.getLength());
            for (int i = 0; i < tmp.getLength(); i++) {
                nodes.add(tmp.item(i));
            }
        }
        else if (nodesetResult instanceof Node) {
            nodes = Collections.singletonList((Node) nodesetResult);
        }
        else {
            log.warn("nodesetResult type {} not supported", nodesetResult.getClass());
            nodes = Collections.emptyList();
        }

        //for each parent node, parse the children
        List<Map<String, Object>> value = nodes.stream()
                .map(n -> entry.getChildren().stream()
                    //use the common mapEntry method
                    .map(me -> mapEntry(me, n).orElse(null))
                    .filter(me -> me != null)
                    //but map to a default Map<String, Object>
                    .collect(Collectors.toMap(EvalResult::getName, EvalResult::getValue)))
                .collect(Collectors.toList());

        //set singleton not as array result
        if (value.size() == 1) {
            result = Optional.of(new EvalResult(entry.getFieldName(), value.get(0)));
        }
        else if (value.isEmpty()) {
            return Optional.empty();
        }
        else {
            result = Optional.of(new EvalResult(entry.getFieldName(), value));
        }

        return result;
    }

    private EvalResult handleReplacements(MappingEntry entry, EvalResult er) {
        EvalResult result = null;
        Map<String, String> replacements = entry.getReplacements();
        log.trace("Applying replacements in {}: {}", er.name, Arrays.toString(replacements.entrySet().toArray()));
        if (er.value instanceof String) {
            result = new EvalResult(er.name, applyReplacements(replacements, (String) er.value));
        } else if (er.value instanceof String[]) {
            String[] val = (String[]) er.value;
            result = new EvalResult(er.name, Arrays.stream(val).map(currentVal -> {
                return applyReplacements(replacements, currentVal);
            }).collect(Collectors.toList()).toArray());
        } else if (er.value instanceof Object[]) {
            Object[] val = (Object[]) er.value;
            result = new EvalResult(er.name, Arrays.stream(val)
                    .filter(v -> v instanceof String)
                    .filter(Objects::nonNull)
                    .map(v -> (String) v)
                    .map(currentVal -> {
                        return applyReplacements(replacements, currentVal);
                    }).collect(Collectors.toList()).toArray());
        }

        log.trace("Result after replacements: {} (if null, then the original is returned)", result);
        if (result != null) {
            return result;
        }

        log.debug("No handling of replacement for given result implemented, returning input again: {}", er);
        return er;
    }

    private String applyReplacements(Map<String, String> replacements, String in) {
        String out = in;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            out = out.replace(replacement.getKey(), replacement.getValue());
        }
        return out;
    }

    private EvalResult handleSplit(MappingEntry entry, EvalResult er) {
        if (er.value instanceof String) {
            String value = (String) er.value;
            log.trace("Applying split in field {} with '{}' on {}", entry.getFieldName(), entry.getSplit(), value);
            String[] split = value.split(entry.getSplit());

            List<String> list = Arrays.asList(split).stream().map((String t) -> {
                return t.trim();
            }).collect(Collectors.toList());

            log.trace("Split resulted in {} items: {}", list.size(), Arrays.toString(list.toArray()));
            return new EvalResult(er.name, list);
        } else {
            log.trace("Split can only be applied to string value, but result was {} ({})", er.value, er.value.getClass());
            return er;
        }
    }

    private boolean assertCondition(Node node, XPathExpression condition) throws XPathExpressionException {
        Object nodesetResult = condition.evaluate(node, XPathConstants.NODESET);
        Optional<EvalResult> result = Optional.ofNullable(handleEvaluationResult(nodesetResult, "condition"));
        if (!result.isPresent()) {
            log.info("condition did not fulfill: {}", condition);
        }
        else {
            log.info("conditiion fulfilled! {}", condition);
        }
        return result.isPresent();
    }


    private EvalResult handleEvaluationResult(final Object evalutationResult, final String name) {
        if (evalutationResult instanceof String) {
            String valueString = (String) evalutationResult;
            if (!valueString.isEmpty()) {
                log.trace("Found field {} = {}", name, valueString);
                return new EvalResult(name, valueString);
            } else {
                log.debug("Evaluation returned empty string for entry {}", name);
            }
        } else if (evalutationResult instanceof NodeList) {
            NodeList nodeList = (NodeList) evalutationResult;
            Optional<Object> value = Optional.empty();

            if (nodeList.getLength() < 1) {
                log.debug("Evaluation returned no results for entry {}", name);
            } else {
                Set<String> contents = Sets.newHashSet();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node n = nodeList.item(i);
                    String textContent = n.getTextContent();
                    if (textContent != null && !textContent.isEmpty()) {
                        contents.add(textContent);
                    }
                }

                log.trace("{} evaluation result(s): {} = {}", contents.size(),
                        name, Arrays.toString(contents.toArray()));

                if (contents.size() == 1) {
                    value = Optional.of(contents.iterator().next());
                    log.trace("Adding field {} = '{}'", name, value.get());
                }
                if (contents.size() > 1) {
                    value = Optional.of(contents.toArray());
                    log.trace("Adding array field ({} values) {} = {}", contents.size(), name,
                            Arrays.toString(contents.toArray()));
                }
            }

            if (value.isPresent()) {
                return new EvalResult(name, value.get());
            } else {
                log.trace("No result found for field {}", name);
            }
        } else {
            log.debug("Unsupported evalutation result: {}", evalutationResult);
        }

        return null;
    }


    public String mapRawEntry(MappingEntry entry, Node node) throws XPathExpressionException {
        // handle full xml
        Node nodesetResult = (Node) entry.getXPath().evaluate(node, XPathConstants.NODE);
        Map<String, String> outputProperties = Maps.newHashMap();
        outputProperties.putAll(DEFAULT_OUTPUT_PROPERTIES);
        if (entry.hasOutputProperties()) {
            outputProperties.putAll(entry.getOutputProperties());
        }
        String xmldoc = asString(nodesetResult, outputProperties);
        log.trace("Storing full XML to field {} starting with {}", entry.getFieldName(),
                xmldoc.substring(0, Math.min(xmldoc.length(), 120)));
        return xmldoc;
    }

    private String asString(Node node, Map<String, String> outputProperties) {
        log.debug("Converting node {} to string using properties {}", node, Arrays.toString(outputProperties.entrySet().toArray()));

        StringWriter sw = new StringWriter();
        try {
            Transformer t = null;
            if (outputProperties.get("indent").equals("no") && stripspaceTransformer.isPresent()) {
                t = stripspaceTransformer.get();
                log.trace("Will apply stripspace XSLT.");
            } else {
                t = defaultTransformer;
            }

            for (Map.Entry<String, String> op : outputProperties.entrySet()) {
                t.setOutputProperty(op.getKey(), op.getValue());
            }

            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException e) {
            log.warn("Problem getting node {} as string", node, e);
        }

        return sw.toString();
    }

    public static class EvalResult {

        protected final String name;

        protected final Object value;

        public EvalResult(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("value", value)
                    .omitNullValues()
                    .toString();
        }

    }

}
