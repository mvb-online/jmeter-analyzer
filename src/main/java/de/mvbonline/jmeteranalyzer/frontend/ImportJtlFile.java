
package de.mvbonline.jmeteranalyzer.frontend;

import de.mvbonline.jmeteranalyzer.frontend.sql.SqlImport;
import de.mvbonline.jmeteranalyzer.frontend.sql.Sample;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progressing action that imports the jtl file into a sqlite database
 */
public class ImportJtlFile implements ImportFileJob {

    private int counter = 0;
    private Map<String, List<Sample>> statements = new HashMap<>();

    private boolean sample = false;

    private File jtlSourceFile;
    private boolean createTable;

    private SqlImport sql;

    private List<String> tables = new ArrayList<>();

    public ImportJtlFile(File jtlSourceFile, boolean createTable, Connection connection) {
        this.jtlSourceFile = jtlSourceFile;
        this.createTable = createTable;

        this.sql = new SqlImport(connection);
    }

    @Override
    public List<String> getTables() {
        return tables;
    }

    @Override
    public void run(final ProgressingRunnableLogger context) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jtlSourceFile)));

            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            DefaultHandler dh = new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (("httpSample".equals(localName) || "httpSample".equals(qName) || "sample".equals(localName) || "sample".equals(qName)) && !sample) {
                        if("sample".equals(localName) || "sample".equals(qName)) {
                            sample = true;
                        }

                        String threadName = attributes.getValue("tn").split(" ")[0];

                        if(!tables.contains(threadName)) {
                            if(createTable) {
                                sql.createSampleTable(threadName);
                            }

                            statements.put(threadName, new ArrayList<Sample>());

                            tables.add(threadName);
                        }

                        String strSuccess = attributes.getValue("s");
                        int success;
                        if ("true".equals(strSuccess)) {
                            success = 1;
                        } else {
                            success = 0;
                        }

                        Sample sample = new Sample();
                        sample.setTimestamp(Long.parseLong(attributes.getValue("ts")));
                        sample.setName(attributes.getValue("lb"));
                        sample.setSuccess(success);
                        sample.setResponseCode(attributes.getValue("rc"));
                        sample.setResponseMessage(attributes.getValue("rm"));
                        sample.setDuration(Integer.parseInt(attributes.getValue("t")));
                        sample.setResponseSize(Integer.parseInt(attributes.getValue("by")));

                        statements.get(threadName).add(sample);

                        counter++;
                        context.setStatus(Integer.toString(counter));

                        if (counter % 500 == 0 && createTable) {
                            for(String table : tables) {
                                if(statements.get(table).isEmpty()) {
                                    continue;
                                }

                                sql.insertSamples(statements.get(table), table);

                                statements.remove(table);
                                statements.put(table, new ArrayList<Sample>());
                            }
                        }
                    }

                    super.startElement(uri, localName, qName, attributes);
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if("sample".equals(localName) || "sample".equals(qName)) {
                        sample = false;
                    }
                    super.endElement(uri, localName, qName);
                }

                @Override
                public void endDocument() throws SAXException {
                    super.endDocument();

                    if(createTable) {
                        for(String table : tables) {
                            if(statements.get(table).isEmpty()) {
                                continue;
                            }

                            sql.insertSamples(statements.get(table), table);
                        }
                    }

                    context.setStatus("OK");
                }
            };

            saxParser.parse(new InputSource(reader), dh);

        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
