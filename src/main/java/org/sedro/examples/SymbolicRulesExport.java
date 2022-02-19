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

import vegml.VegRules;
import vegml.VegRules.VRuleFormatter;
import vegml.VegML;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VFileUtil.DataSetType;



/**
 * Export rules from a trained model. 
 * The rules are symbolic and yield probabilities, for any information any number of rules may match.
 * To get the best result the all the matching rules will need to be summed.
 * 
 * The model in this case is trained with Brown Corps for Part of speech tagging
 * 
 */
public class SymbolicRulesExport {
	//static private DataSetType dataSetToUse = DataSetType.BrownPennTreebankTags; 	// moved 'almost' to penn treebank (49)
	static private DataSetType dataSetToUse = DataSetType.BrownCleanTags; 			// just primary tags per token (91)
	//static private DataSetType dataSetToUse = DataSetType.BrownOldTags; 			// base form (4xx)
	//static private DataSetType dataSetToUse = DataSetType.WSJTreebank;			// load WSJ if you got it (I don't $$$)

	// expects a neg and pos directory under it
	// THERE MUST BE NO OTHER FILES IN THIS DIRECTORY 
	// - when you download the content there are a few indexes/etc.. delete them or results will be significantly flawed
	static final String file_base_directory = "../corpus/brownPos";
	static final String wsj_file_base_directory = "../corpus/treebank/tagged";
	static final String wsj_full_file_base_directory = "../corpus/treebank_3/treebank_3/tagged/pos/wsj";

	
	////////////////////////////////////////////////////
	// brown text -> POS 
	// Train a dataplan to provide POS for input text
	public static void main(String [] args) {
		VegML.showCopywrite();

		testPOS(25, true);
	}

	static void testPOS(int percentTest, boolean lowerCase) {
		int window = 7;			// window size if not loop
		int valueFocus = 4;		// frame focus if not loop	
		int percentTune = 0;
		
		// load dataSet
		String filename = file_base_directory;
		if (dataSetToUse == DataSetType.WSJTreebank) filename = wsj_file_base_directory;
		else if (dataSetToUse == DataSetType.WSJTreebank3) filename = wsj_full_file_base_directory;
		VDataSets ds = VFileUtil.loadDataSetsDS(dataSetToUse, filename, percentTune, percentTest);
		
		System.out.println("DATASET["+dataSetToUse+"] LOADED train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	
		
		VegML vML = new VegML("brown-pos-"+window); // b,b,b,X,a,a,a
		
		// Setup dataPlane
		vML.addDataPlane("text", "pos", window, valueFocus, ds.getDefinition().getTagCount()); // set the posibilities for values
		
		//////////////////////////
		// TRAIN DATA
		// train test train/correct cycles for lesser results like in a NNs
		VegTrain.trainDataSets(vML, "text", "pos", ds);

		// reduce noise before calculating correctness
		VegTune.reduceNaive(vML, "text", "pos");
		
		// gota smash it for good rules
		vML.smash(true);
		vML.print();
		System.gc();
		
		////////////////////////////////////////////////////////////
		// rule formatter: just the default
		VRuleFormatter rfmt = new VRuleFormatter();
		
		// generate rules
		boolean returnRules = true;
		List<String> rules = VegRules.generateAll(vML, "text", "pos", rfmt, null, returnRules);
		if (rules != null) {
			// show first 50 rules to demo
			//for (int i=0;i<100 && i < rules.size();i++) {
			for (int i=0;i < rules.size();i++) {
				String rule = rules.get(i);
		//		System.out.println(rule);
			}
			System.out.println("Rules: " + rules.size());
		} else {
			System.out.println("Rules: NONE");
		}

	}
	
}
