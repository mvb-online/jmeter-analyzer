package de.mvbonline.jmeteranalyzer.frontend.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Created by mholz on 24.11.2015.
 */
public class SqlImport {

    private Connection connection;

    public SqlImport(Connection connection) {
        this.connection = connection;
    }

    public void createSampleTable(String threadName) {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("create table '" + threadName + "' (" +
                    "timestamp int not null," +
                    "name text not null," +
                    "success int not null," +
                    "responseCode int not null," +
                    "responseMessage text not null," +
                    "duration int null," +
                    "responseSize int not null" +
                    ")");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private StringBuilder appendInsertString(Sample sample, StringBuilder ret) {
        ret.append("(");

        ret.append("'").append(sample.getTimestamp()).append("', ");
        ret.append("'").append(sample.getName()).append("', ");
        ret.append("'").append(sample.getSuccess()).append("', ");
        ret.append("'").append(sample.getResponseCode()).append("', ");
        ret.append("'").append(sample.getResponseMessage()).append("', ");

        String duration = "NULL";
        if(sample.getDuration() != Integer.MAX_VALUE) {
            duration = "'" + sample.getDuration() + "'";
        }

        ret.append(duration).append(", ");
        ret.append("'").append(sample.getResponseSize()).append("'");

        ret.append(")");

        return ret;
    }

    public void insertSamples(List<Sample> samples, String threadName) {
        StringBuilder sql = new StringBuilder();

        sql
                .append("insert into '")
                .append(threadName)
                .append("' (")
                .append("timestamp, ")
                .append("name, ")
                .append("success, ")
                .append("responseCode, ")
                .append("responseMessage, ")
                .append("duration, ")
                .append("responseSize")
                .append(") VALUES ");

        String delim = "";

        for (Sample sample : samples) {
            sql.append(delim);
            appendInsertString(sample, sql);
            delim = ",";
        }

        try (Statement s = connection.createStatement()) {
            s.executeUpdate(sql.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
