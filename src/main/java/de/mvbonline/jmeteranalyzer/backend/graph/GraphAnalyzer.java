package de.mvbonline.jmeteranalyzer.backend.graph;

import de.mvbonline.jmeteranalyzer.backend.base.Analyzer;
import de.mvbonline.jmeteranalyzer.util.Average;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mholz on 14.12.2015.
 */
public class GraphAnalyzer implements Analyzer {

    private static final int PAGE_SIZE = 100_000;

    private void writeJSDataToAnyFile(String jsData, String fileName, File dir) throws Exception {
        jsData = jsData.substring(1);

        try(InputStream is = GraphAnalyzer.class.getResourceAsStream("/graphs/graph.html")) {
            String file = IOUtils.toString(is);
            file = file.replace("$RESULT_DATA$", jsData);

            if(!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Could not create dir " + dir);
            }

            File resultFile = new File(dir, fileName);
            try(OutputStream os = new FileOutputStream(resultFile)) {
                IOUtils.write(file, os);
            }
        }
    }

    private void writeJSDataToResultDir(String jsData, String fileName, File resultDir, String table, String sampleName) throws Exception {
        File subResultDir = new File(resultDir, "graphs/" + table + "/" + sampleName);

        writeJSDataToAnyFile(jsData, fileName, subResultDir);
    }

    private void writeJSDataMapToResultDir(Map<String, StringBuilder> jsData, Date start, Date end, File resultDir, String table) throws Exception {
        for (String sampleName : jsData.keySet()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
            String startString = sdf.format(start);
            String endString = sdf.format(end);

            writeJSDataToResultDir(jsData.get(sampleName).toString(), startString + "-" + endString + ".duration-graph.html", resultDir, table, sampleName);
        }
    }

    private void appendSampleToStringBuilder(StringBuilder builder, long duration, boolean success, long timestamp) {
        builder.append(",{date: ");
        builder.append(timestamp).append(", \n");

        if(success) {
            builder.append("lineColor: '#5fb503',\n");
        } else {
            builder.append("lineColor: '#ff0000',\n");
        }

        builder.append("duration: ");

        builder.append(duration).append("}");
    }

    private boolean writeDetailedFiles(Connection sqliteConnection, String table, File resultDir) throws Exception {
        long results = 0;
        boolean written = false;

        while(true) {
            Map<String, StringBuilder> jsData = new HashMap<>();

            Date start = null;
            Date end = null;

            try(Statement statement = sqliteConnection.createStatement()) {
                String sql = "SELECT name, timestamp, duration, success FROM '" + table + "' ORDER BY timestamp ASC LIMIT " + PAGE_SIZE + " OFFSET " + results;
                try(ResultSet resultSet = statement.executeQuery(sql)) {
                    boolean foundAny = false;
                    while(resultSet.next()) {
                        foundAny = true;
                        String name = resultSet.getString("name");
                        if(jsData.get(name) == null) {
                            jsData.put(name, new StringBuilder());
                        }

                        long timestamp = resultSet.getLong("timestamp");
                        if(start == null) {
                            start = new Date(timestamp);
                        }
                        end = new Date(timestamp);

                        appendSampleToStringBuilder(jsData.get(name), resultSet.getLong("duration"), resultSet.getBoolean("success"), timestamp);

                        results++;
                    }

                    if(!foundAny) {
                        break;
                    }
                }
            }

            System.out.println("Writing page...");
            writeJSDataMapToResultDir(jsData, start, end, resultDir, table);
            written = true;
        }

        return written;
    }

    private void extractJSLibrary(File resultDir) throws Exception {
        File zipFile = new File(resultDir, "amcharts.zip");
        try(InputStream is = GraphAnalyzer.class.getResourceAsStream("/graphs/amcharts.zip");
            OutputStream os = new FileOutputStream(zipFile)) {
            IOUtils.copyLarge(is, os);
        }

        ZipFile zip = new ZipFile(zipFile);
        zip.extractAll(resultDir.getAbsolutePath());

        zipFile.delete();
    }

    @Override
    public void analyze(Connection sqliteConnection, List<String> tables, PrintWriter resultsFileWriter, File resultDir) throws Exception {
        boolean written = false;

        for (String table : tables) {
            System.out.println("Creating graph files for " + table + "...");
            written = writeDetailedFiles(sqliteConnection, table, resultDir) || written;
            System.out.println("Done");

            System.out.println("Creating average graph file...");
            written = writeAverage(sqliteConnection, table, resultDir) || written;
            System.out.println("Done");
        }

        if(written) {
            System.out.println("Extracting JS libraries to result folder...");
            extractJSLibrary(resultDir);
            System.out.println("Done");
        }
    }

    private boolean writeAverage(Connection sqliteConnection, String table, File resultDir) throws Exception {
        long resultCounter = 0;

        long count = 0;
        try(Statement statement = sqliteConnection.createStatement()) {
            String sql = "SELECT count(*) FROM '" + table + "'";
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                count = resultSet.getLong(1);
            }
        }
        if(count == 0) {
            return false;
        }

        int averagingSamples = ((int) ((float) count / (float) PAGE_SIZE)) + 1;

        long createdSamples = 0;

        Map<String, AtomicLong> startTimestamps = new HashMap<>();
        Map<String, Average> average = new HashMap<>();
        Map<String, StringBuilder> jsData = new HashMap<>();
        Map<String, AtomicBoolean> avgSuccess = new HashMap<>();

        boolean foundSomething;
        do {
            foundSomething = false;
            try(Statement statement = sqliteConnection.createStatement()) {
                String sql = "SELECT name, timestamp, duration, success FROM '" + table + "' ORDER BY timestamp ASC LIMIT " + PAGE_SIZE + " OFFSET " + resultCounter;
                try(ResultSet resultSet = statement.executeQuery(sql)) {
                    while(resultSet.next()) {
                        resultCounter++;
                        foundSomething = true;

                        String name = resultSet.getString("name");
                        if(jsData.get(name) == null) {
                            jsData.put(name, new StringBuilder());
                            average.put(name, new Average());
                            startTimestamps.put(name, new AtomicLong());
                            startTimestamps.get(name).set(resultSet.getLong("timestamp"));
                            avgSuccess.put(name, new AtomicBoolean());
                        }

                        if(average.get(name).count() >= averagingSamples) {
                            createdSamples++;

                            appendSampleToStringBuilder(jsData.get(name), (long) average.get(name).average(), avgSuccess.get(name).get(), startTimestamps.get(name).get());

                            average.get(name).reset();
                            startTimestamps.get(name).set(resultSet.getLong("timestamp"));
                            avgSuccess.get(name).set(true);
                        }

                        long duration = resultSet.getLong("duration");
                        average.get(name).add(duration);

                        boolean success = resultSet.getBoolean("success");
                        avgSuccess.get(name).set(success && avgSuccess.get(name).get());
                    }
                }
            }

        } while(foundSomething);

        System.out.println("Writing results to disk...");
        for(String sampleName: jsData.keySet()) {
            if(average.get(sampleName).count() > 0) {
                appendSampleToStringBuilder(jsData.get(sampleName), (long) average.get(sampleName).average(), avgSuccess.get(sampleName).get(), startTimestamps.get(sampleName).get());
            }

            writeJSDataToResultDir(jsData.get(sampleName).toString(), "average.duration.html", resultDir, table, sampleName);
        }
        System.out.println("Done");

        return createdSamples > 0;
    }
}
