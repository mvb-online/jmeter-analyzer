
package de.mvbonline.jmeteranalyzer.backend.aggregate.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class holds configuration values. In this case all the aggregate functions
 */
public class Config {

    // aggregation queries for all requests
    // these don't really make sense to apply to a single request
    // requests per second would be really low for example, since most requests won't be executed all the time
    public final static Map<String, String> TOTAL_AGGREGATES = new LinkedHashMap<>();
    static {
        TOTAL_AGGREGATES.put("Start time", "select datetime(min(cast(timestamp / 1000 as int)), \"unixepoch\") || ' UTC' as value from %table%\n");
        TOTAL_AGGREGATES.put("End time", "select datetime(max(cast(timestamp / 1000 as int)), \"unixepoch\") || ' UTC' as value from %table%\n");
        TOTAL_AGGREGATES.put("Ran for (seconds)", "select (max(timestamp) - min(timestamp)) / 1000.0 as value from %table%\n");
        TOTAL_AGGREGATES.put("Max requests per second", "with persecond as (select count(*) as ps from %table% group by cast(timestamp / 1000 as int))\n" +
                "select max(ps) as value from persecond");
        TOTAL_AGGREGATES.put("Avg requests per second", "with persecond as (select count(*) as ps from %table% group by cast(timestamp / 1000 as int))\n" +
                "select avg(ps) as value from persecond");
        TOTAL_AGGREGATES.put("Median requests per second", "with persecond as (select count(*) as ps from %table% group by cast(timestamp / 1000 as int))\n" +
                "select avg(ps) as value from (select ps from persecond order by ps limit 2 - (select count(*) from persecond) % 2 offset (select (count(*) - 1) / 2 from persecond))");
        TOTAL_AGGREGATES.put("90% of the time there were more than x requests per second", "with persecond as (select count(*) as ps from %table% group by cast(timestamp / 1000 as int))\n" +
                "select ps as value from persecond order by ps desc limit 1 offset (select count(*) from persecond) * 90 / 100 - 1");
        TOTAL_AGGREGATES.put("99% of the time there were more than x requests per second", "with persecond as (select count(*) as ps from %table% group by cast(timestamp / 1000 as int))\n" +
                "select ps as value from persecond order by ps desc limit 1 offset (select count(*) from persecond) * 99 / 100 - 1");

        TOTAL_AGGREGATES.put("Max requests per hour", "with perhour as (select count(*) as ps from %table% group by cast(timestamp / 3600 / 1000 as int))\n" +
                "select max(ps) as value from perhour");
        TOTAL_AGGREGATES.put("Avg requests per hour", "with perhour as (select count(*) as ps from %table% group by cast(timestamp / 3600 / 1000 as int))\n" +
                "select avg(ps) as value from perhour");
        TOTAL_AGGREGATES.put("Median requests per hour", "with perhour as (select count(*) as ps from %table% group by cast(timestamp / 3600 / 1000 as int))\n" +
                "select avg(ps) as value from (select ps from perhour order by ps limit 2 - (select count(*) from perhour) % 2 offset (select (count(*) - 1) / 2 from perhour))");
        TOTAL_AGGREGATES.put("90% of the time there were more than x requests per hour", "with perhour as (select count(*) as ps from %table% group by cast(timestamp / 3600 / 1000 as int))\n" +
                "select ps as value from perhour order by ps desc limit 1 offset (select count(*) from perhour) * 90 / 100 - 1");
        TOTAL_AGGREGATES.put("99% of the time there were more than x requests per hour", "with perhour as (select count(*) as ps from %table% group by cast(timestamp / 3600 / 1000 as int))\n" +
                "select ps as value from perhour order by ps desc limit 1 offset (select count(*) from perhour) * 99 / 100 - 1");
    }

    // aggregation queries for all queries
    public final static Map<String, String> PER_LABEL_AGGREGATES = new LinkedHashMap<>();
    static {
        PER_LABEL_AGGREGATES.put("Requests", "select count(*) as value from %table% where %name%");
        PER_LABEL_AGGREGATES.put("Errors", "select count(*) as value from %table% where success = 0 and %name%");
        PER_LABEL_AGGREGATES.put("Error %", "select ((1 - avg(success)) * 100) as value from %table% where %name%");
        PER_LABEL_AGGREGATES.put("Error Response Codes", "select group_concat(distinct responseCode) as value from %table% where success = 0 and %name%");
        PER_LABEL_AGGREGATES.put("Error Response Messages", "select group_concat(distinct responseMessage) as value from %table% where success = 0 and %name%");
        PER_LABEL_AGGREGATES.put("Successes", "select count(*) as value from %table% where success = '1' and %name%");
        PER_LABEL_AGGREGATES.put("Success %", "select (avg(success) * 100) as value from %table% where %name%");
        PER_LABEL_AGGREGATES.put("Min Response Time (ms)", "select (duration || ' (' || datetime(cast(timestamp / 1000 as int), \"unixepoch\") || ')') || ' UTC' as value from %table% where %name% and duration not null order by duration ASC limit 1");
        PER_LABEL_AGGREGATES.put("Max Response Time (ms)", "select (duration || ' (' || datetime(cast(timestamp / 1000 as int), \"unixepoch\") || ')') || ' UTC' as value from %table% where %name% and duration not null order by duration DESC limit 1");
        PER_LABEL_AGGREGATES.put("Average Response Time (ms)", "select avg(duration) as value from %table% where %name%");
        PER_LABEL_AGGREGATES.put("Median Response Time (ms)", "SELECT AVG(duration) as value\n" +
                "FROM (SELECT duration\n" +
                "      FROM %table%\n" +
                "      WHERE %name%\n" +
                "AND duration not null\n" +
                "      ORDER BY duration\n" +
                "      LIMIT 2 - (SELECT COUNT(*) FROM %table% where %name% AND duration not null) % 2    -- odd 1, even 2\n" +
                "      OFFSET (SELECT (COUNT(*) - 1) / 2\n" +
                "              FROM %table% where %name% AND duration not null))");
        PER_LABEL_AGGREGATES.put("90% quicker than (ms)", "SELECT\n" +
                "  duration AS value\n" +
                "  FROM %table%\n" +
                "  WHERE %name%\n" +
                "AND duration not null\n" +
                "  ORDER BY duration ASC\n" +
                "  LIMIT 1\n" +
                "  OFFSET (select count(*) from %table% where %name% AND duration not null)*9/10-1;");
        PER_LABEL_AGGREGATES.put("99% quicker than (ms)", "SELECT\n" +
                "  duration AS value\n" +
                "  FROM %table%\n" +
                "  WHERE %name%\n" +
                "AND duration not null\n" +
                "  ORDER BY duration ASC\n" +
                "  LIMIT 1\n" +
                "  OFFSET (select count(*) from %table% where %name% AND duration not null)*99/100-1;");
        PER_LABEL_AGGREGATES.put("90% slower than (ms)", "SELECT\n" +
                "  duration AS value\n" +
                "  FROM %table%\n" +
                "  WHERE %name%\n" +
                "AND duration not null\n" +
                "  ORDER BY duration DESC\n" +
                "  LIMIT 1\n" +
                "  OFFSET (select count(*) from %table% where %name% AND duration not null)*9/10-1;");
        PER_LABEL_AGGREGATES.put("99% slower than (ms)", "SELECT\n" +
                "  duration AS value\n" +
                "  FROM %table%\n" +
                "  WHERE %name%\n" +
                "AND duration not null\n" +
                "  ORDER BY duration DESC\n" +
                "  LIMIT 1\n" +
                "  OFFSET (select count(*) from %table% where %name% AND duration not null)*99/100-1;");
        PER_LABEL_AGGREGATES.put("% faster than 1000ms", "SELECT\n" +
                "(count(*) * 100.0 / (SELECT count(*) FROM %table% WHERE %name%)) || '% (' || count(*) || ')' as value\n" +
                "FROM %table%\n" +
                "WHERE %name%\n" +
                "AND duration <= 1000\n");
        PER_LABEL_AGGREGATES.put("% faster than 500ms", "SELECT\n" +
                "(count(*) * 100.0 / (SELECT count(*) FROM %table% WHERE %name%)) || '% (' || count(*) || ')' as value\n" +
                "FROM %table%\n" +
                "WHERE %name%\n" +
                "AND duration <= 500\n");
        PER_LABEL_AGGREGATES.put("% faster than 100ms", "SELECT\n" +
                "(count(*) * 100.0 / (SELECT count(*) FROM %table% WHERE %name%)) || '% (' || count(*) || ')' as value\n" +
                "FROM %table%\n" +
                "WHERE %name%\n" +
                "AND duration <= 100\n");
        PER_LABEL_AGGREGATES.put("% faster than 50ms", "SELECT\n" +
                "(count(*) * 100.0 / (SELECT count(*) FROM %table% WHERE %name%)) || '% (' || count(*) || ')' as value\n" +
                "FROM %table%\n" +
                "WHERE %name%\n" +
                "AND duration <= 50\n");
    }
}
