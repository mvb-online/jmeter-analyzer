

package de.mvbonline.jmeteranalyzer.backend.aggregate;

import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * Progressing action that calculates the totals
 */
public class CalculatingTotalAggregates implements ProgressingRunnable {

    private int status = 0;

    private int max = 10;

    private ByteArrayOutputStream rawOut;

    private PrintStream out;

    private PrintWriter output;

    private Connection c;

    private Map<String, String> aggregate;

    private Map<String, String> totalAggregate;

    private List<String> tables;

    private int counter;

    public CalculatingTotalAggregates(Connection c, List<String> tables, Map<String, String> totalAggregate, Map<String, String> aggregate, PrintWriter output) {
        this.c = c;
        this.aggregate = aggregate;
        this.output = output;
        this.totalAggregate = totalAggregate;
        this.tables = tables;

        this.rawOut = new ByteArrayOutputStream();
        this.out = new PrintStream(rawOut);
    }

    public String getProgress() {
        if(status >= max) {
            return "OK";
        }
        if(status == -1) {
            return "FAIL";
        }

        StringBuilder ret = new StringBuilder("");
        for(int i = 0; i < status; i++) {
            ret.append("=");
        }
        for(int i = status; i < max; i++) {
            ret.append(" ");
        }

        return ret.toString();
    }

    private void runQueries(ProgressingRunnableLogger context, String table, Map<String, String> queries) throws SQLException {
        for(Map.Entry<String, String> aggregation : queries.entrySet()) {
            try (Statement s = c.createStatement()) {
                String query = aggregation.getValue().replace("%name%", "'1'").replace("%table%", "'" + table + "'");
                try (ResultSet rs = s.executeQuery(query)) {
                    if (!rs.next()) {
                        out.println("\t" + aggregation.getKey() + ": No value could be calculated (Probably because of too many error responses)");
                        counter++;
                        continue;
                    }
                    String res = rs.getString("value");

                    out.println("\t" + aggregation.getKey() + ": " + res);

                    counter++;
                    double total = totalAggregate.size() + aggregate.size();
                    double current = counter;
                    double maximum = max;
                    status = (int) (current / total * maximum + 0.5d);
                    context.setStatus(getProgress());
                }
            }
        }
    }

    @Override
    public void run(ProgressingRunnableLogger context) throws SQLException {
        try {
            for(String table : tables) {
                out.println();
                out.println(table);
                out.println();

                this.counter = 0;

                runQueries(context, table, totalAggregate);
                runQueries(context, table, aggregate);
            }
        } finally {
            out.flush();
            output.println(rawOut);
        }
    }
}
