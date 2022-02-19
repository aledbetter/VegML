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


import vegml.VegML;
import vegml.VegML.NSWeightBase;
import vegml.VegML.PredictionType;
import vegml.VegTest.SaveStats;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VFileUtil.DataSetType;
import vegml.VResultSet;

/**
 * Part of speech tagging
 * 
 * train and Test based on token text input
 * <text1> <text2> <text-f> <text4> <text5>
 * 
 * Text is simply tokenized and fed in with the tags, in some datasets multiple tags may be
 * trained to the same frame (WSJTreebank3)
 * 
 */
public class GenPosViaText {
	//static private DataSetType dataSetToUse = DataSetType.BrownPennTreebankTags; 	// moved 'almost' to penn treebank (49)
	//static private DataSetType dataSetToUse = DataSetType.BrownUniversalTags; 	// universal tagset
	//static private DataSetType dataSetToUse = DataSetType.BrownCleanTags; 		// just primary tags per token (91)
	//static private DataSetType dataSetToUse = DataSetType.BrownOldTags; 			// base form (4xx)
	static private DataSetType dataSetToUse = DataSetType.WSJTreebank3;				// load WSJ Full ($$$)
	//static private DataSetType dataSetToUse = DataSetType.WSJTreebank;			// load WSJ if you got it (I don't $$$)
	//static private DataSetType dataSetToUse = DataSetType.UDPipeConLL;			// load CONLL17 UDPipe.conllu

	// expects a neg and pos directory under it
	// THERE MUST BE NO OTHER FILES IN THIS DIRECTORY 
	// - when you download the content there are a few indexes/etc.. delete them or results will be significantly flawed
	static final String file_base_directory = "../corpus/brownPos";
	static final String wsj_file_base_directory = "../corpus/treebank/tagged";
	static final String wsj_full_file_base_directory = "../corpus/treebank_3/treebank_3/tagged/pos/wsj";
	static final String corpus_base_directory = "../corpus";	

		
	// save / load
	static final boolean load_file = false;
	static final boolean save_file = false;
	

	////////////////////////////////////////////////////
	// OPTIMIZE
	//static NSWeightBase nswBase = NSWeightBase.Natural;
	//static NSWeightBase nswBase = NSWeightBase.Flat;
	static NSWeightBase nswBase = NSWeightBase.Distance;
	static String optPre = "vtext";
	static PredictionType optType = null;
	//static String optPre = "vtextun";
	//static PredictionType optType = PredictionType.AnyUnknown;
	//static String optPre = "vtextkn";
	//static PredictionType optType = PredictionType.NotUnknown;
	//static String optPre = "vtextr";
	//static PredictionType optType = PredictionType.Recall;
	//static PredictionType optType = PredictionType.AnyRecall;	
	
	static int OPTIMIZE = 1; // 1=train, 2=vote, 3=carve, 11=dependency, 90=merge&carve-mix/text
	
	
	////////////////////////////////////////////////////
	// text -> POS 
	// Train a dataplan to provide POS for input text
	public static void main(String [] args) {
		VegML.showCopywrite();

		// base loop: no-id-filter
		testPOS("text-no-id", 0, 15, true, false, NSWeightBase.None, false, false);
		// base loop: id-filter
		testPOS("text-id", 0, 15, true, true, NSWeightBase.None, false, false);	
		// base loop: id-filter / distance
		testPOS("text-id-w", 0, 15, true, true, NSWeightBase.Distance, false, false);	
		// base loop: id-filter / distance / naive
		testPOS("text-id-w-rn", 0, 15, true, true, NSWeightBase.Distance, false, true);	
		// base loop: id-filter / distance / definition
		testPOS("text-id-w-rdef", 0, 15, true, true, NSWeightBase.Distance, true, false);	
		// base loop: id-filter / distance / definition / naive
		testPOS("text-id-w-rn-rdef", 0, 15, true, true, NSWeightBase.Distance, true, true);	
		
		// base 5: carving reduction:  id-filter / distance
		OPTIMIZE = 3;
		VFileUtil.copyFile("../models/text-id-w-5w.veg", "../models/crv-text-id-w-5w.veg");	 
		testPOS("text-id-w", 0, 15, false, true, NSWeightBase.Distance, false, false);	
		
		// base 5: carving unknown reduction:  id-filter / distance
		OPTIMIZE = 3;
		optType = PredictionType.AnyUnknown;
		VFileUtil.copyFile("../models/text-id-w-5w.veg", "../models/crv_un-text-id-w-5w.veg");
		testPOS("crv-text-id-w", 0, 15, false, true, NSWeightBase.Distance, false, false);	
		
		// base 5: dependency reduction:  id-filter / distance
		OPTIMIZE = 11;
		VFileUtil.copyFile("../models/text-id-w-5w.veg", "../models/dep-text-id-w-5w.veg");
		testPOS("dep-text-id-w", 0, 15, false, true, NSWeightBase.Distance, false, false);	
	}
	
	
	////////////////////////////////////////////////////
	// best WINDOWS
	// 8/4 -> b,b,b,b,?,a,a,a
	// 7/3 -> b,b,b,?,a,a,a
	// 6/3 -> b,b,b,?,a,a
	// 5/2 -> b,b,?,a,a
	// 4/2 -> b,b,?,a
	// 3/1 -> b,?,a
	// 2/0 -> ?,a
	// 1/0 -> ?
	static void testPOS(String base, double percentTune, double percentTest, boolean loop, boolean identityOnly, 
			NSWeightBase nswBasec, boolean reduceDefinition, boolean reduceNaive) {
		int window = 5;			// window size if not loop
		int valueFocus = 2;		// frame focus if not loop	
		
		// load dataSet
		VDataSets ds = null;	
		if (dataSetToUse == DataSetType.UDPipeConLL) {
			String type = "gold"; // gold/test
			String languageTag = "zh";
			String setDir = "UD_Chinese";
			//String setDir = "UD_English";
			//String setDir = "UD_English-LinES";
			//String setDir = "UD_English-ParTUT";
			//String setDir = "UD_Vietnamese";
			//String setDir = null; // "UD_English-LinES"
			//String languageTag = null;
			//String ps = "UPOS"; // "XPOS";
			String ps = "XPOS"; // "XPOS";
			ds = VFileUtil.loadDataSetsDSConLL(type, corpus_base_directory, languageTag, setDir, ps);
		} else {
			String filename = file_base_directory;
			if (dataSetToUse == DataSetType.WSJTreebank) filename = wsj_file_base_directory;
			else if (dataSetToUse == DataSetType.WSJTreebank3) filename = wsj_full_file_base_directory;
			ds = VFileUtil.loadDataSetsDS(dataSetToUse, filename, percentTune, percentTest);
		}
		
		ds.genVSets();
		System.out.println("DATASET["+dataSetToUse+"] LOADED train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	
		
		String initName = "../models/"+base+"-"+window+"w.veg";
		if (OPTIMIZE > 1) {
			optimize(initName, OPTIMIZE, ds);
		} else if (!loop) {
			runtest(initName, identityOnly, reduceDefinition, reduceNaive, nswBasec, window, valueFocus, ds);
		} else {
			// loop through set as per the window/framefocus in the prior diagram
			for (int i=1;i<7;i++) {
				int w = 0;
				if (i == 3) w = 1;
				else if (i == 4) w = 2;
				else if (i == 5) w = 2;
				else if (i == 6) w = 3;
				else if (i == 7) w = 3;
				else if (i == 8) w = 4;
				else if (i == 9) w = 4;
				initName = "../models/"+base+"-"+i+"w.veg";
				runtest(initName, identityOnly, reduceDefinition, reduceNaive, nswBasec, i, w, ds);
				System.gc();
			}
		}
	}
	
	static void runtest(String initName, boolean identityOnly, boolean reduceDefinition, boolean reduceNaive, 
					NSWeightBase nswBasec, int window, int valueFocus, VDataSets ds) {		
		// load existing?
		VegML vML = null;
		if (load_file) vML = VegML.load(initName);	
		
		VDataSets trainDs = ds.getTrainDataSets();
		
		if (vML == null) {
			vML = new VegML("text-pos-"+window); // b,b,b,X,a,a,a
			vML.setCfgDescription("text to POS Model with window " +window);

			// Setup dataPlane
			vML.addDataPlane("text", "pos", window, valueFocus, ds.getDefinition().getTagCount(), nswBasec); 
			// set the posibilities for values	

			vML.addStringMapping("text", "pos", ds.getDefinition().getTagsStrings());
			vML.setCfgDataDefinition("text", "pos", ds.getDefinition());
			vML.setCfgDescription("text", "pos", "Maps Text-Text-Text for Part of Speech prediction");	
			// for brown and others AverageIfNotRecall is better; WSJ has many long chains with different tags
			//vML.setCfgProbMethod("text", "pos", ProbMethod.Average); /// ProbMethod.AverageIfNotRecall		
			vML.setCfgIdentityOnly("text", "pos", identityOnly);

			//////////////////////////
			// TRAIN DATA
			System.out.print("TRAINING[text] w["+window+"/"+valueFocus+"]ns["+nswBasec+"] sets["+ trainDs.size()+"] .");
			VResultSet ts = VegTrain.trainDataSets(vML, "text", "pos", trainDs);
			System.out.println(".. COMPLETE total["+ts.total+"]  Time["+ts.getDurration()+"]");		
	
			// clear the breaks/whitespace added from values and results
			vML.removeAllValueId("text", "pos", new Object [] {"CBR", "SBR", "BR"});		
			// clear the breaks/whitespace added from values and results
			vML.setCfgIgnoreInputs("text", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});

			
			//////////////////////////
			// Tune Set
			if (reduceDefinition) {
				VegTune.logicalDataDefinitionReduction(vML, "text", "pos", null);
				//VegTune.logicalDataIdentityReduction(vML, "text", "pos", false, trainDs);
			}
			if (reduceNaive) {
				// reduce noise before calculating correctness		
				System.out.print(" REDUCE data .");
				ts.start();
				VegTune.reduceNaive(vML, "text", "pos");
				System.gc();
				ts.end();
				System.out.println("... COMPLETE Time["+ts.getDurration()+"]");		
			}

			//////////////////////////
			// Make solid
			//vML.makeSolid("text", "pos");
		}

		//////////////////////////
		// Test/Train Recall
		System.out.println("TEST Recall ... "+ trainDs.size());	
		VResultSet tsk = VegTest.testSets(vML, "text", "pos", trainDs, SaveStats.SaveRecall);		
		System.out.println(" RESULT_R["+String.format("%.2f", tsk.passPercent)+"%] miss["+tsk.failTotal + " of " + tsk.total+"] train["+vML.getCfgWindowSize("text", "pos")+"]naiveReduce["+reduceNaive+"]w["+nswBasec+"]  Time["+tsk.getDurration()+"]");			
					
		//////////////////////////
		// Test/Train Prediction
		VDataSets tuneDs = ds.getTuneDataSets();
		System.out.println("TEST Predict ... "+ tuneDs.size());
		VResultSet ptsk = VegTest.testSets(vML, "text", "pos", tuneDs, SaveStats.SavePredict);
		System.out.println(" RESULT_P["+String.format("%.2f", ptsk.passPercent)+"%] miss["+ptsk.failTotal + " of " + ptsk.total+"]  Time["+ptsk.getDurration()+"]");		
		
		//////////////////////////
		// Save it		
		if (OPTIMIZE == 1 || save_file) {
			vML.save(initName);		
		}
		vML.print();
		// check		
		System.out.println("\n");
	}
	
	
	static void optimize(String initName, int opt, VDataSets ds) {
		int window = 5;	
		int phases = 3;
		double dropPercent = 0.00;
		//dropPercent = 10.00; // 5un
		//dropPercent = 14.00; // 5 or 6un
		//dropPercent = 20.00; // 6		
		int dropSet = 0;			
		int setFullData = 0;
		boolean identityFilter = true;
		int minProgress = 4;
		boolean noBackingUp = false;
		boolean useReduction = true;
		double downWeight = 0.8;

			
		if (opt == 2) {
			// vote reduction
			optType = PredictionType.AnyUnknown;
			optPre = "vtextun";
			optPre = optPre+"z";
			VegTune.logicalVoteReduction(2, "text", "pos", initName, optPre, window, nswBase, optType, false, ds);
			return;
			
		} else if (opt == 11) {	
			// dependency mapping
			optType = null;
			boolean fullData = false;
			useReduction = false;
			VegTune.logicalViaDependencyMapping(initName, "text", "pos", nswBase, optType, useReduction, fullData, ds, false);
			return;

		} else if (opt == 90) {
			// text->mix
			// MERGE and CARVE mix
			optPre += "x2m";
			System.out.println("OPT merge-text/mix["+window+"] => " + optPre);
			String mergeName = "vmix-s8.veg";
			String initMergeName = optPre+"-"+window+"-w"+nswBase.ordinal()+".veg";
			VegML vmML = VegML.load(initName);
			VegML vML = VegML.load(mergeName);
			vmML.merge(vML);
			vmML.save(initMergeName);
			
			vML = vmML = null;
			boolean mergeIntoOther = false;
			minProgress = 3; // for un

			int steps = VegTune.carveMergeSteps("text", "pos", initMergeName, optPre, window, nswBase, 
														optType, dropPercent, dropSet, phases, minProgress, identityFilter, 
														noBackingUp, useReduction, downWeight, 
														"mix", "pos", mergeIntoOther, setFullData, ds);
			System.out.println("COMPLETE in "+steps+" steps");			
			
			return;
		} 
		
		optPre = "../models/crv-text";
		if (optType == PredictionType.AnyUnknown) optPre = "../models/crv_un-text";
		
		int steps = VegTune.carveSteps("text", "pos", initName, optPre, window, nswBase, optType, 
														dropPercent, dropSet, phases, minProgress, identityFilter, 
														noBackingUp, useReduction, downWeight, setFullData, ds);
		System.out.println("COMPLETE in "+steps+" steps");
	}

}
