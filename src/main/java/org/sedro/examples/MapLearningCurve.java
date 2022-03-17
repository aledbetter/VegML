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

import vegml.VegML;
import vegml.VegTrain;
import vegml.VegML.NSWeightBase;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;
import vegml.VegML.ProbMethod;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VDataSetDescriptor.DSDataType;


/**
 * Map the learning curve of RPM
 * 
 * For this test WSJ treebank3 is used (can change) and model is trained and tested for Part of Speech tagging
 * 
 * The process:
 *  until all tokens trained
 *  	- train N number of tokens from training set
 *  	- test full test data set and get correct count, save data point for graph
 *  
 *  exports as a csv file to load and graph
 *  
 *  This could be extended to tune the data for each cycle as well, this would improve accuracy and MAY learn faster
 *
 */
public class MapLearningCurve {
	static String directory = "../models";	
	static VDataSets ds = null;	

	////////////////////////////////////////////////////
	// Adaptive Training
	public static void main(String [] args) {
		int realTotal = 171138;	// WSJ test
		double percentTune = 15, percentTest = 15;
		String corpusDir = "../corpus";
		String dataset = "WSJ"; // brown/brown-penntreebank
		
		VegML.showCopywrite();

		/////////////////////////////////////////////////////////////////////
		// parse the arguments if from command line
		if (args != null && args.length > 0) {    		
			for (String a:args) {
				String [] ap = a.split("=");	
				if (a.startsWith("directory=")) {
					directory = ap[1];
				} else if (a.startsWith("dataset=")) {
					// this is messy: WSJ:../corpus
					String sq [] = ap[1].split(":");
					if (sq.length == 2) corpusDir = sq[1];
					dataset = sq[0];
				}
			}
		} 		
		ds = VFileUtil.loadDataSet(dataset, corpusDir, percentTune, percentTest);
		System.out.println("DATASET["+dataset+"] LOADED train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	
		
		//////////////////////////
		// Configure tuned elements
		
		// Base NumberSet Weight
		//static NSWeightBase nswBase = NSWeightBase.Natural;	
		//static NSWeightBase nswBase = NSWeightBase.Flat;	
		NSWeightBase nswBase = NSWeightBase.Distance;	
		
		// Tune to PredictionType
		PredictionType optType = null;
		optType = PredictionType.AnyUnknown;
		//optType = PredictionType.NotUnknown;
		//optType = PredictionType.Recall;
		//optType = PredictionType.AnyRecall;	

		
		//////////////////////////
		// CONFIG MODEL
		VegML vML = new VegML("text-pos"); // b,b,b,X,a,a,a	
		vML.setCfgDescription("English POS tagging Model");

		// TEXT/POS
		vML.addDataPlane("text", "pos", 1, 0, ds.getDefinition().getTagCount()); // set the posibilities for values	
		vML.setCfgDescription("text", "pos", "Maps Text-Text-Text for Part of Speech prediction");		
		vML.setCfgNSWeight("text", "pos", nswBase);
		vML.setCfgIgnoreInputs("text", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		
		// MIXED/POS
		vML.addDataPlane("mix", "pos", 1, 0, ds.getDefinition().getTagCount());
		vML.setCfgDescription("mix", "pos", "Maps POS-Text-POS for Part of Speech prediction");
		vML.setCfgNSWeight("mix", "pos", nswBase);
		vML.setCfgFramerResponseMix("mix", "pos", "text", "pos");		
		vML.setCfgIgnoreInputs("mix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		
		// AFFIX/POS
		boolean includeIdentity = true;
		boolean includeFormat = true;
		if (includeIdentity) vML.addDataPlane("affix", "pos", 1, 0, ds.getDefinition().getTagCount(), NumberSetType.SequenceEdgeId, NSWeightBase.None); 
		else vML.addDataPlane("affix", "pos", 1, 0, ds.getDefinition().getTagCount(), NumberSetType.SequenceEdge, NSWeightBase.None); 
		vML.setCfgDescription("affix", "pos", "Maps prefix/suffix for Part of Speech prediction");
		vML.setCfgNoEmptyElements("affix", "pos", true, true);
		vML.setCfgProbMethod("affix", "pos", ProbMethod.AverageIfNotRecall); // always faster, but not as good info for merge
		vML.setCfgPCalc("affix", "pos", vML.getPCalcProbabilityOnly());
		vML.setCfgInputDataType("affix", "pos", DSDataType.Char);
		vML.setCfgFramerCharEdge("affix", "pos", 2, includeIdentity, includeFormat, 4);
		vML.setCfgIgnoreInputs("affix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		
		
		vML.setCfgDefaultDataPlane("text", "pos");
		//vML.setCfgDefaultDataPlane("mix", "pos");
		//vML.setCfgDefaultDataPlane("affix", "pos");
		
		// initial Name
		String initName = "v"+vML.getCfgDefaultDTag()+"-w"+nswBase.ordinal()+".veg";

		// Save empty
		vML.save(initName);	
		int dataSkip = 1000;
		int minWindow = 1;
		int maxWindow = 5;
		
		//ws = 1;           // 100/49.8043%  250/55.2084%
		//ws = maxWindow = 2; // 100/54.8032%  250/60.1065%
		//ws = maxWindow = 3; // 100/62.1387%  250/65.4669%
		//ws = maxWindow = 4; // 100/61.2310%  250/65.7229%
		//ws = maxWindow = 5; // 100/61.0077%  250/65.5874%
		//ws = maxWindow = 6; // 100/60.7498%  250/65.5429%
		//ws = maxWindow = 7; // 100/60.7859%  250/65.7719%
		
		List<List<Integer>> lcSets = VegTrain.trainDataSetsLearningCurve(vML, minWindow, maxWindow, dataSkip, ds, realTotal);
		VFileUtil.delFile(initName);
		VFileUtil.saveCsvFileValues(vML.getCfgDefaultDTag()+"-sk"+dataSkip+"-("+minWindow+"-"+maxWindow+")-lcurve.csv", lcSets);
	}
	
}
