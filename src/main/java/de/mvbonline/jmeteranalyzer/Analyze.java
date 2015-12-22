package de.mvbonline.jmeteranalyzer;

import de.mvbonline.jmeteranalyzer.backend.aggregate.CalculatingAggregatesPerLabel;
import de.mvbonline.jmeteranalyzer.backend.aggregate.CalculatingTotalAggregates;
import de.mvbonline.jmeteranalyzer.backend.aggregate.config.Config;
import de.mvbonline.jmeteranalyzer.backend.base.Analyzer;
import de.mvbonline.jmeteranalyzer.backend.base.IgnoreAnalyzer;
import de.mvbonline.jmeteranalyzer.frontend.ImportFileFactory;
import de.mvbonline.jmeteranalyzer.frontend.ImportFileJob;
import de.mvbonline.jmeteranalyzer.frontend.jmx.ImportJmxFile;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;
import org.reflections.Reflections;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Main class
 */
public class Analyze {

    private String findArg(String flag, String defaultValue, String... args) {
        for (int i = 0; i < args.length; i++) {
            if(flag.equals(args[i])) {
                if(i + 1 < args.length) {
                    String arg = args[i + 1];
                    if(arg.startsWith("-")) {
                        throw new IllegalArgumentException("Missing argument after " + flag);
                    }
                    return arg;
                } else {
                    throw new IllegalArgumentException("Missing argument after " + flag);
                }
            }
        }

        if(defaultValue == null) {
            throw new IllegalArgumentException("Missing argument " + flag);
        }

        return defaultValue;
    }

    private void run(String... args) {

        File source;
        String aggregateDest;
        File jmxSourceFile;

        if (args.length < 1) {
            throw new IllegalArgumentException("First parameter needs to be a JTL file!");
        } else if(args.length == 1) {
            source = new File(args[0]);
            aggregateDest = "result";
            jmxSourceFile = null;
        } else if(args.length == 2 && !args[0].startsWith("-")) {
            source = new File(args[0]);
            aggregateDest = args[1];
            jmxSourceFile = null;
        } else {
            String jmxFile = findArg("-jmx", "", args);
            if(!jmxFile.isEmpty()) {
                jmxSourceFile = new File(jmxFile);
            } else {
                jmxSourceFile = null;
            }

            source = new File(findArg("-in", null, args));
            aggregateDest = findArg("-out", "result", args);
        }

        File resultDir = new File(aggregateDest);
        if(!resultDir.exists()) {
            if(!resultDir.mkdirs()) {
                throw new RuntimeException(new IOException("Could not create result directory " + resultDir));
            }
        }

        File aggregateFile = new File(resultDir, "results.txt");

        try(PrintWriter fileStream = new PrintWriter(new FileOutputStream(aggregateFile))) {
            File dest = new File(resultDir, "data.sqlite");
            boolean createTable = !(dest.exists());

            if(!createTable) {
                System.out.println("The database file already exists. Will use that instead of importing values again.");
            }

            if(jmxSourceFile == null) {
                analyze(source, dest, createTable, fileStream);
            } else {
                analyzeWithJmx(source, jmxSourceFile, dest, createTable, fileStream);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void doImportAndAnalyze(File source, Connection connection, boolean createTable, PrintWriter resultFileWriter, File resultDir) {
        ImportFileJob importFile = ImportFileFactory.buildImportFileJob(source, createTable, connection);

        ProgressingRunnableLogger pal = new ProgressingRunnableLogger((createTable) ? "Importing JTL file" : "Reading JTL file", importFile);
        pal.doAction();

        List<String> tables = importFile.getTables();

        Reflections reflections = new Reflections();
        Set<Class<? extends Analyzer>> analyzers = reflections.getSubTypesOf(Analyzer.class);

        for (Class<? extends Analyzer> analyzerClass : analyzers) {
            try {
                if(analyzerClass.getAnnotation(IgnoreAnalyzer.class) == null) {
                    Analyzer analyzer = analyzerClass.newInstance();
                    analyzer.analyze(connection, tables, resultFileWriter, resultDir);
                } else {
                    System.out.println("Skipping " + analyzerClass.getSimpleName() + "...");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void analyzeWithJmx(File jtlSource, File jmxSource, File destDatabase, boolean createTable, PrintWriter resultFileWriter) {
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + destDatabase.getAbsolutePath())) {
            doImportAndAnalyze(jtlSource, connection, createTable, resultFileWriter, destDatabase.getParentFile());

            ProgressingRunnableLogger pal = new ProgressingRunnableLogger("Importing JMX data", new ImportJmxFile(jmxSource, createTable, connection));
            pal.doAction();


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void analyze(File jtlSource, File destDatabase, boolean createTable, PrintWriter resultFileWriter) {
        try(Connection connection = DriverManager.getConnection("jdbc:sqlite:" + destDatabase.getAbsolutePath())) {
            doImportAndAnalyze(jtlSource, connection, createTable, resultFileWriter, destDatabase.getParentFile());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        System.setProperty("java.awt.headless", "true");

        Analyze analyze = new Analyze();

        analyze.run(args);
    }

}
