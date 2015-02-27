package de.mvbonline.jmeteranalyzer;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;


/**
 * Main class
 */
public class Analyze {

    private void run(String... args) throws SQLException, IOException, FileNotFoundException {
        if (args.length < 1) {
            throw new IllegalArgumentException("First parameter needs to be a JTL file!");
        }

        String source = args[0];
        String aggregateDest = "result";
        if(args.length > 1) {
            aggregateDest = args[1];
        }

        File resultDir = new File(aggregateDest);
        if(!resultDir.exists()) {
            if(!resultDir.mkdirs()) {
                throw new IOException("Could not create result directory " + resultDir);
            }
        }
        File aggreateFile = new File(resultDir, "results.txt");
        PrintWriter fileStream;
        fileStream = new PrintWriter(new FileOutputStream(aggreateFile));

        String dest = new File(resultDir, "data.sqlite").getAbsolutePath();
        boolean createTable = !(new File(dest).exists());

        if(!createTable) {
            System.out.println("The database file already exists. Will use that instead of importing values again.");
        }

        analyze(source, dest, createTable, fileStream);
    }

    public void analyze(String source, String dest, boolean createTable, PrintWriter resultWriter) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dest);

        ImportFile importFile = new ImportFile(source, createTable, connection);
        ProgressingRunnableLogger pal = new ProgressingRunnableLogger((createTable) ? "Importing JTL file" : "Reading JTL file", importFile);
        pal.doAction();

        List<String> tables = importFile.getTables();

        pal = new ProgressingRunnableLogger("Calculating aggregates", new CalculatingTotalAggregates(connection, tables, Config.TOTAL_AGGREGATES, Config.PER_LABEL_AGGREGATES, resultWriter));
        pal.doAction();

        pal = new ProgressingRunnableLogger("Calculating aggregates per label", new CalculatingAggregatesPerLabel(connection, tables, Config.PER_LABEL_AGGREGATES, resultWriter));
        pal.doAction();

        connection.close();
        resultWriter.flush();
        resultWriter.close();

    }

    public static void main(String... args) {
        Analyze analyze = new Analyze();
        try {
            analyze.run(args);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
