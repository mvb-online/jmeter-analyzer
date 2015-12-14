package de.mvbonline.jmeteranalyzer.frontend;

import de.mvbonline.jmeteranalyzer.frontend.sql.Sample;
import de.mvbonline.jmeteranalyzer.frontend.sql.SqlImport;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;
import de.mvbonline.jmeteranalyzer.util.ProgressingRunnableLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.sql.Connection;
import java.util.*;

/**
 * Created by mholz on 24.11.2015.
 */
public class ImportCsvFile implements ImportFileJob {

    private SqlImport sqlImport;

    private File input;

    private boolean createTables;

    private Map<String, List<Sample>> threadNameSampleBuffer = new HashMap<>();

    private Map<String, Boolean> createdTables = new HashMap<>();

    public ImportCsvFile(File input, boolean createTables, Connection connection) {
        this.input = input;
        this.createTables = createTables;

        this.sqlImport = new SqlImport(connection);
    }

    @Override
    public void run(ProgressingRunnableLogger context) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(input))) {

            boolean first = true;
            long counter = 0;

            String line;
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if(first) {
                    try {
                        int test = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        continue;
                    } finally {
                        first = false;
                    }
                }
                context.setStatus("" + counter);


                Sample s = new Sample();
                s.setResponseSize(Integer.parseInt(parts[8]));
                s.setDuration(Integer.parseInt(parts[1]));
                s.setResponseMessage(parts[4]);
                s.setName(parts[2]);
                s.setResponseCode(parts[3]);
                s.setTimestamp(Long.parseLong(parts[0]));
                s.setSuccess("false".equals(parts[7]) ? 0 : 1);

                writeToBuffer(parts[5].split(" ")[0], s);
                counter++;
            }

            writeCompleteBuffer(threadNameSampleBuffer);
        }
    }

    private void writeToBuffer(String threadName, Sample s) {
        List<Sample> buffer = threadNameSampleBuffer.get(threadName);
        if(buffer == null) {
            buffer = new ArrayList<>();
            threadNameSampleBuffer.put(threadName, buffer);
        }

        buffer.add(s);
        if(buffer.size() == 500) {
            writeSampleBuffer(buffer, threadName);

            buffer.clear();
        }
    }

    private void writeSampleBuffer(List<Sample> buffer, String threadName) {
        if(createTables && createdTables.get(threadName) == null) {
            sqlImport.createSampleTable(threadName);
            createdTables.put(threadName, true);
        }

        if(createTables) {
            sqlImport.insertSamples(buffer, threadName);
        }
    }

    private void writeCompleteBuffer(Map<String, List<Sample>> threadNameSampleBuffer) {
        for (Map.Entry<String, List<Sample>> threadNameBufferEntry : threadNameSampleBuffer.entrySet()) {
            String threadName = threadNameBufferEntry.getKey();
            List<Sample> buffer = threadNameBufferEntry.getValue();

            writeSampleBuffer(buffer, threadName);
        }
    }

    @Override
    public List<String> getTables() {
        Set<String> ret = new HashSet<>();

        for (String s : threadNameSampleBuffer.keySet()) {
            ret.add(s.split(" ")[0]);
        }

        return new ArrayList<>(ret);
    }
}
