

package de.mvbonline.jmeteranalyzer.backend;

import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ProgressingAction to calculate the aggregates for every label
 */
public class CalculatingAggregatesPerLabel implements ProgressingRunnable {

    private ByteArrayOutputStream rawOut;

    private PrintStream out;

    private PrintWriter output;

    private Connection c;

    private Map<String, String> aggregate;

    private List<String> tables;

    public CalculatingAggregatesPerLabel(Connection c, List<String> tables, Map<String, String> aggregate, PrintWriter output) {
        this.rawOut = new ByteArrayOutputStream();
        this.out = new PrintStream(rawOut);

        this.c = c;
        this.aggregate = aggregate;
        this.output = output;
        this.tables = tables;
    }

    @Override
    public void run(ProgressingRunnableLogger context) throws SQLException {
        try {
            for(String table : tables) {

                out.println();
                out.println(table);

                context.setStatus(table + ": getting labels");

                Statement s = c.createStatement();

                Set<String> names = new HashSet<>();

                ResultSet result = s.executeQuery("select distinct name from '" + table + "'");

                while(result.next()) {
                    names.add(result.getString("name"));
                }
                result.close();
                s.close();

                for(String name : names) {
                    context.setStatus(table + ":" + name);

                    out.println("\t");
                    out.println("\t" + name);
                    out.flush();

                    for(Map.Entry<String, String> agg : aggregate.entrySet()) {
                        try (Statement statement = c.createStatement()) {
                            String query = agg.getValue().replace("%name%", "name = '" + name + "'").replace("%table%", "'" + table + "'");
                            try (ResultSet rs = statement.executeQuery(query)) {
                                if (!rs.next()) {
                                    out.println("\t\t" + agg.getKey() + ": No value could be calculated (Probably because of too many error responses)");
                                    continue;
                                }
                                String res = rs.getString("value");

                                out.println("\t\t" + agg.getKey() + ": " + res);
                            }
                        }
                    }
                }
            }

            context.setStatus("OK");
        } finally {
            out.flush();
            output.println(rawOut);
        }
    }
}
