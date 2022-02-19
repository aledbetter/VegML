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


import vegml.VResultSet;
import vegml.VegML;
import vegml.VegML.NSWeightBase;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.VegML.PredictionType;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VFileUtil.DataSetType;

/**
 * Part of speech tagging
 * 
 * train and Test based on mixed token input
 * <r-pos> <r-pos> <text> <p-pos> <p-pos>
 * 
 * Were r-pos are results from predictions on prior frames and p-pos are predictions on future frames based
 * on another model; either affix/pos or text/pos
 */
public class GenPosViaMix {
	//static private DataSetType dataSetToUse = DataSetType.BrownPennTreebankTags; 	// moved 'almost' to penn treebank (49)
	//static private DataSetType dataSetToUse = DataSetType.BrownUniversalTags; 	// universal tagset
	//static private DataSetType dataSetToUse = DataSetType.BrownOldTags; 			// base form (4xx)
	static private DataSetType dataSetToUse = DataSetType.WSJTreebank3;			// load WSJ Full
	//static private DataSetType dataSetToUse = DataSetType.WSJTreebank;			// load WSJ if you got it (I don't $$$)

	// expects a neg and pos directory under it
	// THERE MUST BE NO OTHER FILES IN THIS DIRECTORY  
	// - when you download the content there are a few indexes/etc.. delete them or results will be significantly flawed
	static final String file_base_directory = "../corpus/brownPos";
	static final String wsj_file_base_directory = "../corpus/treebank/tagged";
	static final String wsj_full_file_base_directory = "../corpus/treebank_3/treebank_3/tagged/pos/wsj"; 

	static PredictionType pType = null;
	static String pre = "vmix";
	
	////////////////////////////////////////////////////
	// brown text -> POS 
	// Train a dataplan to provide POS for input text
	public static void main(String [] args) {
		boolean reduceDefinition = false;
		VegML.showCopywrite();

		testMixPOS(15, reduceDefinition, false);
		
		testMixPOS(15, reduceDefinition, true);
		
		pType = PredictionType.AnyUnknown;
		pre = "vmixun";
		testMixPOS(15, reduceDefinition, true);		
	}
	
	
	static void testMixPOS(double percentTest, boolean reduceDefinition, boolean optimize) {
		boolean showProgress = false;
		double percentTune = 0;
		int window = 5;
		int focus = 2;

		// load dataSet
		String filename = file_base_directory;
		if (dataSetToUse == DataSetType.WSJTreebank) filename = wsj_file_base_directory;
		else if (dataSetToUse == DataSetType.WSJTreebank3) filename = wsj_full_file_base_directory;
		VDataSets ds = VFileUtil.loadDataSetsDS(dataSetToUse, filename, percentTune, percentTest);
				
		System.out.println("DATASET["+dataSetToUse+"] LOADED train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	
		
		NSWeightBase nsBase = NSWeightBase.Distance;	
		
		if (optimize) {
			String iName = "../models/vmix-"+window+".veg";
			int dropSet = 1;
			int phases = 1;
			double dropPercent = 18; // 3
			int setFullData = 0;
			int minProgress = 8;
			boolean identityFilter = true;
			boolean noBackingUp = false;
			boolean useReduction = true;
			double downWeight = 0.8;	
		
			int steps = VegTune.carveSteps("mix", "pos", iName, "../models/"+pre, window, nsBase, pType, 
														dropPercent, dropSet, phases, minProgress, identityFilter, 
														noBackingUp, useReduction, downWeight, setFullData, ds);			
			System.out.println("COMPLETE in "+steps+" steps");
			return;
		} 
		
		VegML vML = new VegML("vmix-pos-"+window);
		ds.genVSets();
		trainModel(vML, window, focus, nsBase, reduceDefinition, ds.getTrainDataSets());

		// traintest
		trainTestModel(vML, "mix", showProgress, false, ds);
		vML.print();
		
		vML.save("../models/vmix-"+window+".veg");
	}
	

	static VegML trainModel(VegML vML, int window, int focus, NSWeightBase nswBase, boolean reduceDefinition, VDataSets dss) {
		
		if (vML == null) {
			System.out.print("ERROR: train POS-<TXT>-POS["+dss.size()+"]  NO Veg");
			return null;
		}

		// model merge ratio					
		if (vML.isTrained("mix", "pos")) {
			window = vML.getCfgWindowSize("mix", "pos");
			focus = vML.getCfgFrameFocus("mix", "pos");
			System.out.println(" HAVE POS-<TXT>-POS["+dss.size()+"] window["+window+"]["+focus+"]");
			return vML;
		} 
		System.out.print(" train POS-<TXT>-POS["+dss.size()+"] dataWidth["+dss.getDefinition().getTagCount()+"] window["+window+"]["+focus+"]ns["+nswBase+"] .");
		// Setup dataPlane
		vML.addDataPlane("mix", "pos", window, focus, dss.getDefinition().getTagCount(), nswBase);
		vML.setCfgDescription("mix", "pos", "Maps POS-Text-POS for Part of Speech prediction");;		
		vML.addStringMapping("mix", "pos", dss.getDefinition().getTagsStrings());
		// SET the FRAMER
		vML.setCfgFramerResponseMix("mix", "pos", "text", "pos");		

		/////////////////////////
		// TRAIN
		vML.setCfgFramerArg("mix", "pos", dss.getValLLV());
		VResultSet ts = VegTrain.trainDataSets(vML, "mix", "pos", dss);	
		vML.setCfgFramerArg("mix", "pos", null);
		System.out.println(".. COMPLETE total["+ts.total+"]  Time["+ts.getDurration()+"]");		
		
		// clear the breaks/whitespace added from values and results
		vML.removeAllValueId("mix", "pos", new Object [] {"CBR", "SBR", "BR"});	
		// clear the breaks/whitespace added from values and results
		vML.setCfgIgnoreInputs("mix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		
		if (reduceDefinition) {
			VegTune.logicalDataDefinitionReduction(vML, "mix", "pos", null);
		}
		vML.setStateTrained("mix", "pos");
		return vML;
	}

	
	static VegML trainTestModel(VegML vML, String dtag, boolean showProgress, boolean genTune, VDataSets dss) {
		int window = vML.getCfgWindowSize(dtag, "pos");
		VDataSets trainDs = dss.getTrainDataSets();
		trainDs.genVSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		tuneDs.genVSets();		
		
		int percent = 0;
		
		vML.setCfgFramerArg(dtag, "pos", trainDs.getValLLV());
		
		if (showProgress) System.out.println("TRAINING Recall ... "+ trainDs.size()+ " / "+tuneDs.size());	
		VResultSet tsk = VegTest.testSets(vML, dtag, "pos", trainDs, VegTest.SaveStats.SaveRecall);
		System.out.println(" RESULT_R["+dtag+"]["+String.format("%.2f", tsk.passPercent)+"%] miss["+tsk.failTotal + " / "+tsk.passTotal+ " of " + tsk.total+"] train["+window+"]["+percent+"% of data]  =>  T["+tsk.getDurration()+"]");			
		vML.setCfgFramerArg(dtag, "pos", null);

		vML.setCfgFramerArg(dtag, "pos", tuneDs.getValLLV());
		if (showProgress) System.out.println("TRAINING Predict ... "+ tuneDs.size());
		VResultSet rtsk = VegTest.testSets(vML, dtag, "pos", tuneDs, VegTest.SaveStats.SavePredict);
		System.out.println(" RESULT_P["+dtag+"]["+String.format("%.2f", rtsk.passPercent)+"%] miss["+rtsk.failTotal + " / "+rtsk.passTotal+ " of " + rtsk.total+"]  =>  T["+rtsk.getDurration()+"]");		

		vML.setCfgFramerArg(dtag, "pos", null);
		vML.setStateReady(dtag, "pos");		
		return vML;
	}
	static VegML trainModel(VegML vML, boolean showProgress, VDataSets dss) {
		if (showProgress) System.out.println("TRAIN-EXT ... "+ dss.size());	
		VResultSet tsk = VegTrain.trainDataSets(vML, "mix", "pos", dss);			
		System.out.println(" TRAIN COMPLETE[mix] total[" + tsk.total+"]  =>  T["+tsk.getDurration()+"]");	
		return vML;
	}

}
