package ie.deri.esa.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;

import util.gen.SpearmanCorrelation;
import edu.wiki.search.MongoESASearcher;


public class SemRelTest {
	private static ArrayList<Float> humanSimScores;
	private static ArrayList<Float> esaSimScores;
	private static MongoESASearcher esa;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		humanSimScores = new ArrayList<Float>();
		esaSimScores = new ArrayList<Float>();
		esa = new MongoESASearcher();
		
		getSimData("/config/wordsim353-combined.tab");
		
		SpearmanCorrelation spCorr = new SpearmanCorrelation();
		float[] humanSimScoresArr = ArrayUtils.toPrimitive(humanSimScores.toArray(new Float[]{}));
		float[] esaSimScoresArr = ArrayUtils.toPrimitive(esaSimScores.toArray(new Float[]{}));
		Double corr = spCorr.spearmanCorrelationCoefficient(humanSimScoresArr, esaSimScoresArr);
		
		System.out.println("Correlation: " + corr.toString());
	}
	
	private static void getSimData(String dataFilePath) {
		InputStream is = SemRelTest.class.getResourceAsStream(dataFilePath);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		try {
			//Skip the first line (column description).
			br.readLine();
			
			while (true) {
				String strScorePair = br.readLine();
				if (strScorePair == null)
					break;
				
				String[] strSepScorePair = strScorePair.split("\t");
				String term1 = strSepScorePair[0];
				String term2 = strSepScorePair[1];
				double humanSim = Double.valueOf(strSepScorePair[2]);
				
				double esaSim = esa.getRelatedness(term1, term2);
				esa.clean();
				
				humanSimScores.add((float)humanSim);
				esaSimScores.add((float)esaSim);
			}
			
			br.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
