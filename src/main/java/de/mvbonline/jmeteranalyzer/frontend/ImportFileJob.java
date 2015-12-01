package de.mvbonline.jmeteranalyzer.frontend;

import de.mvbonline.jmeteranalyzer.util.ProgressingRunnable;

import java.util.List;

/**
 * Created by mholz on 01.12.2015.
 */
public interface ImportFileJob extends ProgressingRunnable {

    List<String> getTables();

}
