package de.mvbonline.jmeteranalyzer;

/**
 * Interface for some action that can report on its status asynchronously
 */
interface ProgressingRunnable extends Runnable {

    /**
     * Get the progress
     * @return Current progress of this action
     */
    public String getProgress();

    /**
     * Do something that has a progress and takes a long time
     */
    public void run();

    /**
     * Is called once the action is done
     */
    public void end();

}
