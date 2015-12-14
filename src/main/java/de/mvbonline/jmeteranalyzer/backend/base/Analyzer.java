package de.mvbonline.jmeteranalyzer.backend.base;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by mholz on 14.12.2015.
 */
public interface Analyzer {

    void analyze(Connection sqliteConnection, List<String> tables, PrintWriter resultsFileWriter, File resultDir) throws Exception;

}
