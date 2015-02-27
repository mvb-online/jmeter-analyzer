

package de.mvbonline.jmeteranalyzer;

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
class CalculatingAggregatesPerLabel implements ProgressingRunnable {
    private String status = "starting";

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
    public String getProgress() {
        return status;
    }

    @Override
    public void run() {
        try {
            for(String table : tables) {

                out.println();
                out.println(table);

                status = table + ": getting labels";

                Statement s = c.createStatement();

                Set<String> names = new HashSet<>();

                ResultSet result = s.executeQuery("select distinct name from " + table);

                while(result.next()) {
                    names.add(result.getString("name"));
                }
                result.close();
                s.close();

                for(String name : names) {
                    status = table + ":" + name;

                    out.println("\t");
                    out.println("\t" + name);
                    out.flush();

                    for(Map.Entry<String, String> agg : aggregate.entrySet()) {
                        Statement statement = c.createStatement();
                        String query = agg.getValue().replace("%name%", "name = '" + name + "'").replace("%table%", table);
                        ResultSet rs = statement.executeQuery(query);
                        if(!rs.next()) {
                            System.err.println("Did not get result from query: " + agg.getKey());
                            return;
                        }
                        String res = rs.getString("value");

                        out.println("\t\t" + agg.getKey() + ": " + res);

                        rs.close();
                        statement.close();
                    }
                }
            }

            status = "OK";
        } catch (SQLException e) {
            status = "FAIL";
        }
    }

    @Override
    public void end() {
        if(!status.equals("FAIL")) {
            out.flush();
            output.println(rawOut);
        }
    }
}
