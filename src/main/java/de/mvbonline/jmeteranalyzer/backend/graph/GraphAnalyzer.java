package de.mvbonline.jmeteranalyzer.backend.graph;

import de.mvbonline.jmeteranalyzer.backend.base.Analyzer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;

/**
 * Created by mholz on 14.12.2015.
 */
public class GraphAnalyzer implements Analyzer {

    @Override
    public void analyze(Connection sqliteConnection, List<String> tables, PrintWriter resultsFileWriter, File resultDir) throws Exception {
//        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
//
//
//
//        ImageIO.write(image, "png", new File(resultDir, "image.png"));
    }
}
