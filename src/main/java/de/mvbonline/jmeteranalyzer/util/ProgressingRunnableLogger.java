
package de.mvbonline.jmeteranalyzer.util;

/**
 * Helper class for pretty output. Logs the action of a ProgressingAction in a pretty way
 */
public class ProgressingRunnableLogger {

    private static final int WIDTH = 80;
    private static final String DOTDOTDOT = "...";


    private ProgressingRunnable action;

    private String status;
    private Exception exception;

    private String name;

    /**
     * Default constructor
     * @param name Name that will be displayed
     * @param action Action to be watched
     */
    public ProgressingRunnableLogger(String name, ProgressingRunnable action) {
        this.name = name;
        this.action = action;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Overwrite the whole line with empty spaces
     */
    private void overwrite() {
        for(int i = 0; i < WIDTH; i++) {
            System.out.print(" ");
        }
        System.out.print("\r");
    }

    /**
     * Print the name
     */
    private void printName() {
        System.out.print(name + DOTDOTDOT);
    }

    /**
     * Print the spaces between the name and the status
     * @param statusLength Length of the status that will be displayed
     */
    private void printEmpty(int statusLength) {
        int x = WIDTH - this.name.length() - statusLength - DOTDOTDOT.length() - "[  ]".length();
        for(int i = 0; i < x; i++) {
            System.out.print(" ");
        }
    }

    /**
     * Print the status and the spaces before it to align everything
     * @param status Status message that will be printed
     */
    private void printStatus(String status) {
        printEmpty(status.length());
        System.out.print("[ " + status + " ]");
    }

    /**
     * Print the whole line
     * @param status Status message that will be printed
     */
    private void printProgressLine(String status) {
        overwrite();
        printName();
        printStatus(status);
        //System.out.print("\r");
    }

    /**
     * Run the action and display the output
     */
    public void doAction() {
        status = "starting";
        exception = null;

        // create the thread that will run the action
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run(ProgressingRunnableLogger.this);
                } catch (Exception e) {
                    exception = e;
                }
            }
        });
        t.start();

        // wait for the thread to finish
        while(t.isAlive()) {
            // print the progress and sleep
            printProgressLine(status);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // nothing
            }
        }
        if(exception != null) {
            status = "ERROR";
        }

        // print the last status again and go to the next line
        overwrite();
        printName();
        printStatus(status);
        System.out.println();

        if(exception != null) {
            exception.printStackTrace();
        }
    }

}
