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

import vegml.VDataPlane;
import vegml.VResultSet;
import vegml.VContext;
import vegml.VFrame;
import vegml.VegFramer;
import vegml.VegML;
import vegml.VegML.PredictionType;
import vegml.VegML.ProbMethod;
import vegml.VegML.NSWeightBase;
import vegml.VegML.NumberSetType;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.VegUtil;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VFileUtil.DataSetType;



/**
 * Part of speech tagging
 * 
 * train and Test based on characters in the text and a string format
 * <s1> <s2> <s3> <e3> <e2> <e1> <format>
 * 
 * Where the characters from the start are set to start of frame for prefix size
 * and characters from the end are set to the end of frame for suffix size
 * 
 * This is not very affective for languages that do not have prefixes/suffixes or formats that imply semantics
 */
public class GenPosViaAffix {
	//static private DataSetType dataSetToUse = DataSetType.BrownPennTreebankTags; 	// moved 'almost' to penn treebank (49)
	//static private DataSetType dataSetToUse = DataSetType.BrownUniversalTags; 	// universal tagset
	//static private DataSetType dataSetToUse = DataSetType.BrownCleanTags; 		// just primary tags per token (91)
	//static private DataSetType dataSetToUse = DataSetType.BrownOldTags; 			// base form (4xx)
	static private DataSetType dataSetToUse = DataSetType.WSJTreebank3;				// load WSJ Full
	//static private DataSetType dataSetToUse = DataSetType.WSJTreebank;			// load WSJ if you got it (I don't $$$)
	//static private DataSetType dataSetToUse = DataSetType.UDPipeConLL;			// load CONLL17 UDPipe.conllu

	// expects a neg and pos directory under it
	// THERE MUST BE NO OTHER FILES IN THIS DIRECTORY 
	// - when you download the content there are a few indexes/etc.. delete them or results will be significantly flawed
	static final String file_base_directory = "../corpus/brownPos";
	static final String wsj_file_base_directory = "../corpus/treebank/tagged";
	static final String wsj_full_file_base_directory = "../corpus/treebank_3/treebank_3/tagged/pos/wsj";
	static final String corpus_base_directory = "../corpus";	
 
	static int OPTIMIZE = 1;		
	static VDataSets ds = null;		

	////////////////////////////////////////////////////
	// brown text -> POS 
	// Train a dataplan to provide POS for input text
	public static void main(String [] args) {
		int prefix = 4;
		int suffix = 10;
		VegML.showCopywrite();
		loadData(25);
	
		////////////////////////////////
		// baseline
		OPTIMIZE = 0;
		testAffixPOS(15, prefix, suffix);
		
		////////////////////////////////
		// carve unknown
		OPTIMIZE = 1;
		testAffixPOS(15, prefix, suffix);
	}

	
	/**
	 * load the data set
	 */
	static void loadData(double percentTest) {
		double percentTune = 0;

		// load dataSet
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
	}

	static int testAffixPOS(double percentTest, int prefix, int suffix) {
		
		if (OPTIMIZE > 0) {
			PredictionType pType = PredictionType.AnyUnknown;
			String iName = "../models/vafx-14.veg";
			// affix -> larger range
			int dropSet = 0;
			int phases = 2;
			int setFullData = 100;
			boolean identityFilter = false;
			boolean noBackingUp = false;
			boolean useReduction = true;
			double downWeight = 0.8;
			
			if (OPTIMIZE == 1) {
				return VegTune.carveSteps("affix", "pos", iName, "../models/vafx", suffix+prefix, NSWeightBase.None, 
													pType, 0, dropSet, phases, 6, identityFilter, noBackingUp, 
													useReduction, downWeight, setFullData, ds);
			} else {
				String initName = "../models/vafx-14-w4-s4.veg";	
				boolean fullData = true;
				useReduction = false;
				return VegTune.logicalViaDependencyMapping(initName, "affix", "pos", NSWeightBase.None, 
														pType, useReduction, fullData, ds, false);
			}
		}
		
		
		//////////////////////////
		// configure model
		VDataSets trainDs = ds.getTrainDataSets();
		VDataSets testDs = ds.getTestDataSets();
		
		VegML vML = new VegML("affix-pos-"+(prefix+suffix));
		vML.setCfgDescription("Affix to POS Models");			
		
		int totsize = prefix+suffix;
		int focus = totsize-2;

		System.out.print(" train <AFX+>["+trainDs.size()+"] dataWidth["+ trainDs.getDefinition().getTagCount()+"] window["+totsize+"]["+prefix+"/"+suffix+"] sets["+ trainDs.size()+"] .");
		
		// Setup dataPlane
		vML.addDataPlane("affix", "pos", totsize, focus, trainDs.getDefinition().getTagCount(), NumberSetType.None); 
		vML.setCfgDescription("affix", "pos", "Maps prefix, suffix and '-' for Part of Speech prediction");
		vML.setCfgNoEmptyElements("affix", "pos", true, true);
		vML.addStringMapping("affix", "pos", trainDs.getDefinition().getTagsStrings());
		
		//vML.setCfgProbMethod("affix", "pos", ProbMethod.AverageIfNotRecall); // always faster, but not as good info for merge
		vML.setCfgProbMethod("affix", "pos", ProbMethod.Average);
		vML.setCfgPCalc("affix", "pos", vML.getPCalcProbabilityOnly());			
		vML.setCfgInputDataType("affix", "pos", DSDataType.Char);

		// SET the FRAMER	
		vML.setCfgScratch("affix", "pos", "prefix", prefix);
		vML.setCfgScratch("affix", "pos", "suffix", (suffix-2));
		vML.setCfgFramer("affix", "pos", "character", new VegFramer() {
			@Override
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
									boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
				
				String dataValue = dataSet.getDataLLS(dataSetNumber, dataPosition);
				
				int pre = dataplane.getCfgScratchInt("prefix");
				int suf = dataplane.getCfgScratchInt("suffix");
								
				int framePos = 0;
				char[] ch = dataValue.toCharArray();
				
				boolean affix = true;
				// have number?
				int nidx = -1, cidx = -1;
				for (int i=0;i<ch.length;i++) {
					if (Character.isDigit(ch[i])) {
						nidx = i;
						if (cidx < 0) cidx = i;
						break;
					}
					if (cidx < 0 && !Character.isLetter(ch[i])) cidx = i;
				}
				if (dataValue.length() < 2) affix = false;
				else if (nidx >= 0) affix = true;				
				else if (dataValue.length() < 3) affix = false;

				// or not a word..
				if (affix) {
					// prefix
					for (int p=0;p<pre;p++) {
						if (p >= ch.length) frame.setValueEmpty(dataplane, framePos);
						else frame.setValue(dataplane, framePos, ch[p]);		
						framePos++;
					}
					// suffix
					for (int p=0;p<suf;p++) {
						int pos = (ch.length-suf)+p;
						if (pos < 0) frame.setValueEmpty(dataplane, framePos);
						else frame.setValue(dataplane, framePos, ch[pos]);	
						framePos++;
					}
				} else {
					int full = pre+suf;
					for (int p=0;p<full;p++) {
						frame.setValueEmpty(dataplane, framePos);
						framePos++;
					}
				}

				// add full word here
				frame.setValue(dataplane, framePos, dataValue);
				framePos++;
				// fmt
				frame.setValue(dataplane, framePos, VegUtil.getStringFormat(dataValue));
								
				frame.setComplete(dataplane);
				return true;
			}
		});		

						
		// setup special numberSets
		vML.clearCfgNS("affix", "pos");
		// make back sequences on by one
		int end = suffix-2;  // '-'
		String prefmt = "";
		for (int i=0;i<prefix;i++) prefmt += "- ";
		for (int i=0;i<end;i++) {
			String suffmt = "";
			for (int ix=0;ix<i;ix++) suffmt += "- ";	
			for (int ix=i;ix<end;ix++) suffmt += "x ";
			vML.addCfgNSFormat("affix", "pos", prefmt + suffmt, -1); 
		}
		prefmt = "";
		for (int x=0;x<prefix;x++) {
			prefmt += "x ";
			vML.addCfgNSFormatToAll("affix", "pos", prefmt, -1); 
		}
		
		String prtfmt = "";
		for (int i=0;i<prefix;i++) prtfmt += "- ";
		
		for (int x=0;x<(suffix-2);x++) {
			String pfmt = prtfmt;
			for (int i=x;i<(suffix-2);i++) pfmt += "x ";
			vML.addCfgNSFormat("affix", "pos", pfmt+"- x", -1); 
			prtfmt += "- ";
		}
		prtfmt = "";
		for (int i=0;i<(totsize-2);i++) prtfmt += "- ";
		vML.addCfgNSFormat("affix", "pos", "- "+prtfmt+"x", -1);
		vML.addCfgNSFormat("affix", "pos", "x "+prtfmt+"x", -1);
		
		prtfmt = "";
		for (int i=0;i<(focus-1);i++) prtfmt += "- ";
		vML.addCfgNSFormat("affix", "pos", "- "+prtfmt+"x", -1);
		
		prtfmt = "";
		for (int i=0;i<totsize;i++) prtfmt += "x ";
		vML.addCfgNSFormat("affix", "pos", prtfmt, -1); 
		vML.updateCfgNS("affix", "pos");
		// clear the breaks/whitespace added from values and results
		vML.setCfgIgnoreInputs("affix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});

		
		//////////////////////////
		// TRAIN		
		VResultSet ts = VegTrain.trainDataSets(vML, "affix", "pos",  trainDs);
		System.out.println(".. COMPLETE total["+ts.total+"]  Time["+ts.getDurration()+"]");		
		
		// clear the breaks/whitespace added from values and results
		vML.removeAllValueId("affix", "pos", new Object [] {"CBR", "SBR", "BR"});	
		vML.setStateTrained("affix", "pos");

		//////////////////////////
		// Test/Train Recall
		System.out.println("TEST Recall ... " +  trainDs.size());	
		VResultSet tsk = VegTest.testSets(vML, "affix", "pos", trainDs, VegTest.SaveStats.SaveRecall);		
		System.out.println(" RESULT_R[affix]["+String.format("%.2f", tsk.passPercent)+"%] miss["+tsk.failTotal + " / "+tsk.passTotal+ " of " + tsk.total+"] train["+prefix+"/"+suffix+"]  =>  T["+tsk.getDurration()+"]");		
	
		
		//////////////////////////
		// Test/Train Prediction
		System.out.println("TEST Predict ... "+ testDs.size());	
		VResultSet ptsk = VegTest.testSets(vML, "affix", "pos", testDs, VegTest.SaveStats.SavePredict);	
		System.out.println(" RESULT_P[affix]["+String.format("%.2f", ptsk.passPercent)+"%] miss["+ptsk.failTotal + " / "+ptsk.passTotal+ " of " + ptsk.total+"]  =>  T["+ptsk.getDurration()+"]");			

		vML.setStateReady("affix", "pos");
		
		vML.print();
		vML.save("../models/vafx-"+(prefix+suffix)+".veg");
		return -1;
	}
	
}
