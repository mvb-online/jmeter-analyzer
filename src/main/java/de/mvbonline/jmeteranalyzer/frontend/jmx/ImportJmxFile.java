package de.mvbonline.jmeteranalyzer.frontend.jmx;

import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Created by mholz on 23.11.2015.
 */
public class ImportJmxFile implements ProgressingRunnable {

    private File file;

    private boolean createTable;

    private Connection connection;

    public ImportJmxFile(File file, boolean createTable, Connection connection) {
        this.file = file;
        this.createTable = createTable;
        this.connection = connection;
    }

    @Override
    public void run(ProgressingRunnableLogger context) throws Exception {
        context.setStatus("creating table");
        if(createTable) {
            Statement s = connection.createStatement();
            s.executeUpdate("create table jmx_data (" +
                    "timestamp int not null," +
                    "name text not null," +
                    "value real not null" +
                    ")");
            s.close();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNo = 0;
            Statement insertBatch = connection.createStatement();
            while((line = reader.readLine()) != null) {
                lineNo++;
                context.setStatus("" + lineNo);

                String[] parts = line.split(",");
                if(parts.length != 17) {
                    throw new RuntimeException("Could not read " + file + ". " + lineNo + " does not have 18 values");
                }

                long timestamp = Long.parseLong(parts[0]);
                String name = parts[2];
                double value = Double.parseDouble(parts[4]);

                insertBatch.addBatch("insert into jmx_data values ('" + timestamp + "', '" + name + "', '" + value + "')");
            }

            insertBatch.executeBatch();
            insertBatch.close();
        }

        context.setStatus("OK");
    }
}
