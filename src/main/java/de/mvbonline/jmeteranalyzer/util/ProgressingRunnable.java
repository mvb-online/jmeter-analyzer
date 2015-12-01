package de.mvbonline.jmeteranalyzer.util;

/**
 * Interface for some action that can report on its status asynchronously
 */
public interface ProgressingRunnable {

    /**
     * Do something that has a progress and takes a long time
     */
    void run(ProgressingRunnableLogger context) throws Exception;

}
