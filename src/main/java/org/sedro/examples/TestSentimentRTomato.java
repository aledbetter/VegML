 /*************************************************************************
 * VegML version 1.0.0
 * __________________
 * 
 * Copyright (C) [2022] Aaron Ledbetter
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of Aaron Ledbetter. The intellectual and technical 
 * concepts contained herein are proprietary to Aaron Ledbetter and 
 * may be covered by U.S. and Foreign Patents, patents in process, 
 * and are protected by trade secret or copyright law. 
 *
 * @author Aaron Ledbetter
 */
/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package org.sedro.examples;


import java.util.List;

import vegml.Accum;
import vegml.VDataPlane;
import vegml.VegML;
import vegml.VegML.NumberSetType;
import vegml.VegML.ProbMethod;
import vegml.VResultSet;
import vegml.ValProb;
import vegml.VegML.VegPCalc;
import vegml.VegTest.SaveStats;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.Data.VDataSet;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;



/**
 * Boolean Sentiment for movie reviews - Rotten Tomatoes
 * 
 * Train model with 70% of the data. Training is as segments that is each review is a segment has has a single value 
 * trained to it. Prediction follows the same, thus the Segment APIs are used in VegML.
 * 
 * Data pre-processing: tokenized, lowercase, start and end of file marked
 * 
 * Dataset located
 * https://www.cs.cornell.edu/people/pabo/movie-review-data/
 */
public class TestSentimentRTomato {
	// Where the Rotten-Tomatoes data is
	// !!! make sure ONLY review files are present !!!
	static final String file_base_directory = "../corpus/review-polarity";	
	static final double trainSize = 0.7; // 70% train 30% test
	
	public static void main(String [] args) {
		VegML.showCopywrite();

		//////////////////////////////////////////
		// load dataset
		List<String> negl =  VFileUtil.loadTextFiles(file_base_directory+"/neg");
		List<String> posl =  VFileUtil.loadTextFiles(file_base_directory+"/pos");		
		VDataSets dssN = new VDataSets();
		VDataSets dssP = new VDataSets();
		// tokenize and build datasets
		for (String s:negl) dssN.add(new VDataSet(VFileUtil.tokenizeString(s.toLowerCase(), false, true), "negative"));
		for (String s:posl) dssP.add(new VDataSet(VFileUtil.tokenizeString(s.toLowerCase(), false, true), "positive"));
		
		// set the train size: no tuning
		dssP.setSplitPercent(trainSize, 0, 1-trainSize);
		dssN.setSplitPercent(trainSize, 0, 1-trainSize);
		dssP.genVSets();
		dssN.genVSets();
		
		
		//////////////////////////////////////////		
		// process for a set of window sizes
		// window size: try 2 - 7
		for (int w=2;w < 8;w++) {
			trainAndTestSentiment(w, dssN, dssP);
		}
	}
	
	// run tests on movie reviews
	static void trainAndTestSentiment(int window, VDataSets dssN, VDataSets dssP) {    		
	
		//////////////////////////////////////////
		// Configure model
		//////////////////////////////////////////
		VegML vML = new VegML("review-sentiment");		
		// add dataplane note the dataWidth is boolean = 2
		// Alternates to the default PowerSet
		vML.addDataPlane("text", "sentiment", window, 2, NumberSetType.SequenceFan);
		//vML.addDataPlane("text", "sentiment", window, 2, NumberSetType.Linear);
		//vML.addDataPlane("text", "sentiment", window, 2, NumberSetType.SequenceEdge);
		//vML.addDataPlane("text", "sentiment", window, 2, NumberSetType.SequenceLeft);
		//vML.addDataPlane("text", "sentiment", window, 2, NumberSetType.SequenceRight);
		vML.setCfgNoEmptyElements("text", "sentiment", false);
		//vML.setCfgProbMethod("text", "sentiment", ProbMethod.AverageIfNotRecall);	
		
		// set Accumulator Instance Weight Calculation (AIWC) 
		// -> this one is probability offset by set correctness and training bias
		
		//vML.setCfgPCalc("text", "sentiment", vML.getPCalcProbabilityOnly());
		vML.setCfgPCalc("text", "sentiment", new VegPCalc() {
			@Override
			public double calculateProb(ProbMethod method, VDataPlane dataplane, Accum dac, double [] nsWeights, 
					int setNumber, boolean haveIdentity, Accum sac, Accum sval, ValProb vp) {
				// in a boolean set this will be the ratio A & B and can offset training bias (some)				
				return vp.probability / sac.getProbability(vp.value);
			}
		});
		
		//////////////////////////////////////////
		// train as segment
		//////////////////////////////////////////
		VegTrain.trainSegments(vML, "text", "sentiment", dssP.getTrainDataSets());
		VegTrain.trainSegments(vML, "text", "sentiment", dssN.getTrainDataSets());
		
		
		//////////////////////////////////////////
		// Tuning or just data-reductions
		VegTune.reduceNaive(vML, "text", "sentiment");		
		//vML.smash();	// if you like smaller and faster SMASH it		
		// NOTE: tuning here could make a significant difference in results OR size/speed of model
		
		
		//////////////////////////////////////////
		// Test Recall on the trained data
		//////////////////////////////////////////
		System.out.println("testing[Recal]["+window+"] " + dssP.getTrainCount());
		VResultSet rPos = VegTest.testSegments(vML, "text", "sentiment", dssP.getTrainDataSets(), SaveStats.SaveRecall);
		VResultSet rNeg = VegTest.testSegments(vML, "text", "sentiment", dssN.getTrainDataSets(), SaveStats.SaveRecallNoReset);
		

		//////////////////////////////////////////
		// Test Prediction
		//////////////////////////////////////////
		System.out.println("testing[Predict]w["+window+"] " + dssP.getTestCount());
		VResultSet tPos = VegTest.testSegments(vML, "text", "sentiment", dssP.getTestDataSets(), SaveStats.SavePredict);
		VResultSet tNeg = VegTest.testSegments(vML, "text", "sentiment", dssN.getTestDataSets(), SaveStats.SavePredictNoReset);

		// print to see detailed breakdown for: recal/recall-predict/predict/default
		vML.print(true);
			
		System.out.println("   RECALL  NEG["+String.format("%.2f", rNeg.passPercent)+"%]["+rNeg.failTotal + " of " + rNeg.total+"]  POS["+String.format("%.2f", rPos.passPercent)+"%]["+rPos.failTotal + " of " + rPos.total+"] ");			
		System.out.println("   PREDICT NEG["+String.format("%.2f", tNeg.passPercent)+"%]["+tNeg.failTotal + " of " + tNeg.total+"]  POS["+String.format("%.2f", tPos.passPercent)+"%]["+tPos.failTotal + " of " + tPos.total+"] ");			
		
		rNeg.passPercent = ((rNeg.passPercent+rPos.passPercent) / 2);
		rNeg.failTotal += rPos.failTotal;
		rNeg.total += rPos.total;
		
		tNeg.passPercent = ((tNeg.passPercent+tPos.passPercent) / 2);
		tNeg.failTotal += tPos.failTotal;
		tNeg.total += tPos.total;
		
		rPos.failTotal = tNeg.failTotal + rNeg.failTotal;
		rPos.total = tNeg.total + rNeg.total;
		double pass = (double)100 - (((double)rPos.failTotal / (double)rPos.total)*(double)100);
		System.out.println("RESULT["+String.format("%.2f", pass)+"%]["+rPos.failTotal + " of " + rPos.total+"] =>"
				+ "  PREDICT["+String.format("%.2f", tNeg.passPercent)+"%]["+tNeg.failTotal + " of " + tNeg.total+"]"
				+ "  RECALL["+String.format("%.2f", rNeg.passPercent)+"%]["+rNeg.failTotal + " of " + rNeg.total+"] "
				+ " w["+vML.getCfgWindowSize("text", "sentiment")+"]");
				
	}
}
