package de.mvbonline.jmeteranalyzer.backend.graph;

import de.mvbonline.jmeteranalyzer.backend.base.Analyzer;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by mholz on 14.12.2015.
 */
public class GraphAnalyzer implements Analyzer {

    private static final int PAGE_SIZE = 100_000;

    @Override
    public void analyze(Connection sqliteConnection, List<String> tables, PrintWriter resultsFileWriter, File resultDir) throws Exception {

        boolean written = false;

        for (String table : tables) {
            System.out.println("Creating graph files for " + table + "...");
            long results = 0;

            while(true) {
                StringBuilder jsData = new StringBuilder();
                String delim = "";

                Date start = null;
                Date end = null;

                try(Statement statement = sqliteConnection.createStatement()) {
                    String sql = "SELECT timestamp, duration, success FROM '" + table + "' ORDER BY timestamp ASC LIMIT " + PAGE_SIZE + " OFFSET " + results;
                    try(ResultSet resultSet = statement.executeQuery(sql)) {
                        boolean foundAny = false;
                        while(resultSet.next()) {
                            foundAny = true;

                            long timestamp = resultSet.getLong("timestamp");
                            if(start == null) {
                                start = new Date(timestamp);
                            }
                            end = new Date(timestamp);

                            jsData.append(delim);
                            delim = ",\n";

                            jsData.append("{date: ");
                            jsData.append(timestamp).append(", \n");

                            boolean success = resultSet.getBoolean("success");
                            if(success) {
                                jsData.append("lineColor: '#5fb503',\n");
                            } else {
                                jsData.append("lineColor: '#ff0000',\n");
                            }

                            jsData.append("duration: ");

                            long duration = resultSet.getLong("duration");
                            jsData.append(duration).append("}");

                            results++;
                        }

                        if(!foundAny) {
                            break;
                        }
                    }
                }

                System.out.println("Writing page...");
                try(InputStream is = GraphAnalyzer.class.getResourceAsStream("/graphs/graph.html")) {
                    String file = IOUtils.toString(is);
                    file = file.replace("$RESULT_DATA$", jsData.toString());

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
                    String startString = sdf.format(start);
                    String endString = sdf.format(end);

                    File resultFile = new File(resultDir, table + "-" + startString + "-" + endString + ".duration-graph.html");
                    try(OutputStream os = new FileOutputStream(resultFile)) {
                        IOUtils.write(file, os);
                    }
                }

                written = true;
            }
            System.out.println("Done");
        }

        if(written) {
            System.out.println("Extracting JS libraries to result folder...");
            File zipFile = new File(resultDir, "amcharts.zip");
            try(InputStream is = GraphAnalyzer.class.getResourceAsStream("/graphs/amcharts.zip");
                OutputStream os = new FileOutputStream(zipFile)) {
                IOUtils.copyLarge(is, os);
            }

            ZipFile zip = new ZipFile(zipFile);
            zip.extractAll(resultDir.getAbsolutePath());

            boolean test = zipFile.delete();
            System.out.println("Done");
        }
    }
}
