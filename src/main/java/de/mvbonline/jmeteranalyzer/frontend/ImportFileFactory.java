package de.mvbonline.jmeteranalyzer.frontend;

import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;

import java.io.File;
import java.sql.Connection;

/**
 * Created by mholz on 24.11.2015.
 */
public class ImportFileFactory {

    public static ImportFileJob buildImportFileJob(File importFile, boolean createTable, Connection connection) {
        if(importFile.getName().endsWith(".jtl")) {
            return new ImportJtlFile(importFile, createTable, connection);
        } else if(importFile.getName().endsWith(".csv")) {
            return new ImportCsvFile(importFile, createTable, connection);
        }

        throw new IllegalArgumentException("Can't import file " + importFile + ". Unknown format");
    }

}
