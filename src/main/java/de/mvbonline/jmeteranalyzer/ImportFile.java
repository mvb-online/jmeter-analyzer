
package de.mvbonline.jmeteranalyzer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progressing action that imports the jtl file into a sqlite database
 */
class ImportFile implements ProgressingRunnable {

    private int counter = 0;
    private Map<String, StringBuilder> statements = new HashMap<>();
    private Map<String, String> delim = new HashMap<>();

    private String status = "";

    private boolean sample = false;

    private String source;
    private boolean createTable;
    private Connection c;

    private List<String> tables = new ArrayList<>();

    public ImportFile(String source, boolean createTable, Connection c) {
        this.source = source;
        this.createTable = createTable;
        this.c = c;
    }

    @Override
    public String getProgress() {
        return status;
    }

    public List<String> getTables() {
        return tables;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source)));

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
                            try {
                                if(createTable) {
                                    Statement s = c.createStatement();
                                    s.executeUpdate("create table " + threadName + " (" +
                                            "timestamp int not null," +
                                            "name text not null," +
                                            "success int not null," +
                                            "responseCode int not null," +
                                            "responseMessage text not null," +
                                            "duration int not null," +
                                            "responseSize int not null" +
                                            ")");
                                    s.close();
                                }

                                statements.put(threadName, new StringBuilder("insert into " + threadName + " (" +
                                        "timestamp, " +
                                        "name, " +
                                        "success, " +
                                        "responseCode, " +
                                        "responseMessage, " +
                                        "duration, " +
                                        "responseSize" +
                                        ") VALUES "));
                                delim.put(threadName, "");
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }

                            tables.add(threadName);
                        }

                        String success = attributes.getValue("s");
                        if ("true".equals(success)) {
                            success = "1";
                        } else {
                            success = "0";
                        }

                        try {
                            statements.get(threadName).append(delim.get(threadName)).append("('").append(
                                    attributes.getValue("ts")).append("','").append(
                                    attributes.getValue("lb")).append("','").append(
                                    success).append("','").append(
                                    attributes.getValue("rc")).append("','").append(
                                    attributes.getValue("rm")).append("','").append(
                                    attributes.getValue("t")).append("','").append(
                                    attributes.getValue("by")).append(
                                    "')");
                            delim.put(threadName, ", ");

                            counter++;
                            status = Integer.toString(counter);

                            if (counter % 500 == 0 && createTable) {
                                for(String table : tables) {
                                    String update = statements.get(table).toString();
                                    Statement s = c.createStatement();

                                    s.executeUpdate(update);
                                    s.close();

                                    statements.remove(table);
                                    statements.put(table, new StringBuilder("insert into " + table + " (" +
                                            "timestamp, " +
                                            "name, " +
                                            "success, " +
                                            "responseCode, " +
                                            "responseMessage, " +
                                            "duration, " +
                                            "responseSize" +
                                            ") VALUES "));
                                    delim.put(table, "");
                                }
                            }
                        } catch (SQLException e) {
                            throw new SAXException(e);
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

                    try {
                        if(createTable) {
                            for(String table : tables) {
                                Statement s = c.createStatement();
                                s.executeUpdate(statements.get(table).toString());
                                s.close();
                            }
                        }
                    } catch (SQLException e) {
                        throw new SAXException(e);
                    }

                    status = "OK";
                }
            };

            saxParser.parse(new InputSource(reader), dh);

        } catch (SAXException | ParserConfigurationException e) {
            status = "FAIL";
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            status = "FAIL";
            System.exit(-1);
        } catch (IOException e) {
            status = "FAIL";
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void end() {

    }
}
