package de.mvbonline.jmeteranalyzer.backend.aggregate;

import de.mvbonline.jmeteranalyzer.Analyze;
import de.mvbonline.jmeteranalyzer.backend.aggregate.config.Config;
import de.mvbonline.jmeteranalyzer.backend.base.Analyzer;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;

/**
 * Created by mholz on 14.12.2015.
 */
public class AggregateAnalyzer implements Analyzer {

    @Override
    public void analyze(Connection sqliteConnection, List<String> tables, PrintWriter resultsFileWriter, File resultDir) throws Exception {
        ProgressingRunnableLogger pal = new ProgressingRunnableLogger("Calculating aggregates", new CalculatingTotalAggregates(sqliteConnection, tables, Config.TOTAL_AGGREGATES, Config.PER_LABEL_AGGREGATES, resultsFileWriter));
        pal.doAction();

        pal = new ProgressingRunnableLogger("Calculating aggregates per label", new CalculatingAggregatesPerLabel(sqliteConnection, tables, Config.PER_LABEL_AGGREGATES, resultsFileWriter));
        pal.doAction();
    }
}
