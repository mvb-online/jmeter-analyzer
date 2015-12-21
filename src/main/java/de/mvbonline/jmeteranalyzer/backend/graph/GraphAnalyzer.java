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
import java.util.*;

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
                Map<String, StringBuilder> jsData = new HashMap<>();
                String delim = "";

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

                            jsData.get(name).append(delim);
                            delim = ",\n";

                            jsData.get(name).append("{date: ");
                            jsData.get(name).append(timestamp).append(", \n");

                            boolean success = resultSet.getBoolean("success");
                            if(success) {
                                jsData.get(name).append("lineColor: '#5fb503',\n");
                            } else {
                                jsData.get(name).append("lineColor: '#ff0000',\n");
                            }

                            jsData.get(name).append("duration: ");

                            long duration = resultSet.getLong("duration");
                            jsData.get(name).append(duration).append("}");

                            results++;
                        }

                        if(!foundAny) {
                            break;
                        }
                    }
                }

                System.out.println("Writing page...");
                for (String s : jsData.keySet()) {
                    try(InputStream is = GraphAnalyzer.class.getResourceAsStream("/graphs/graph.html")) {
                        String file = IOUtils.toString(is);
                        file = file.replace("$RESULT_DATA$", jsData.get(s).toString());

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmssSSS");
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
                        String startString = sdf.format(start);
                        String endString = sdf.format(end);

                        File subResultDir = new File(resultDir, "graphs/" + table + "/" + s);
                        if(!subResultDir.exists() && !subResultDir.mkdirs()) {
                            throw new IOException("Could not create dir " + subResultDir);
                        }

                        File resultFile = new File(subResultDir, startString + "-" + endString + ".duration-graph.html");
                        try(OutputStream os = new FileOutputStream(resultFile)) {
                            IOUtils.write(file, os);
                        }
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
