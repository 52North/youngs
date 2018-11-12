
package org.n52.youngs.harvest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.n52.youngs.api.Report;
import org.n52.youngs.exception.SourceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class FileSource implements Source {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileSource.class.getName());
    
    private final Path file;
    private DocumentBuilderFactory docBuilderFactory;

    public FileSource(Path file) {
        Objects.requireNonNull(file);
        if (!Files.exists(file)) {
            throw new IllegalStateException("File does not exist: "+ file);
        }
        this.file = file;
        this.docBuilderFactory = DocumentBuilderFactory.newInstance();
        this.docBuilderFactory.setNamespaceAware(true);
    }

    @Override
    public URL getEndpoint() {
        try {
            return this.file.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new SourceError(e, "Could not create URL from directory %s", this.file);
        }
    }

    @Override
    public long getRecordCount() {
        return 1;
    }

    @Override
    public Collection<SourceRecord> getRecords(Report report) {
        LOG.debug("Reading record from file {}", this.file);

        try {
            DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
            Charset cs = Charset.forName("utf-8");
            Document doc = documentBuilder.parse(new InputSource(new InputStreamReader(Files.newInputStream(this.file), cs)));

            Element elem = doc.getDocumentElement();
            elem.normalize();
            LOG.trace("Read document: {}", elem);

            NodeSourceRecord record = new NodeSourceRecord(elem);
            return Collections.singletonList(record);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOG.warn("Could not read file:" + ex.getMessage(), ex);
        }
        
        return Collections.emptyList();
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) {
        return getRecords(report);
    }

}
