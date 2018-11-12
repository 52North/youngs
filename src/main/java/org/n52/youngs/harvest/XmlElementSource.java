
package org.n52.youngs.harvest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.n52.youngs.api.Report;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class XmlElementSource implements Source {
    
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(XmlElementSource.class.getName());
    
    private final Element element;

    public XmlElementSource(Element element) {
        Objects.requireNonNull(element);
        this.element = element;
    }
    
    @Override
    public URL getEndpoint() {
        try {
            return new URL("inmemory://xml");
        } catch (MalformedURLException ex) {
            LOG.warn("invalid URL", ex);
        }
        return null;
    }

    @Override
    public long getRecordCount() {
        return 1;
    }

    @Override
    public Collection<SourceRecord> getRecords(Report report) {
        return Collections.singleton(new NodeSourceRecord(this.element));
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) {
        return Collections.singleton(new NodeSourceRecord(this.element));
    }

}
