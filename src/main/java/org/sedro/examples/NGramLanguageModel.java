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
import vegml.VegML.NumberSetType;
import vegml.ValProb;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.Data.VDataSet;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VFileUtil.DataSetType;

import java.util.ArrayList;
import java.util.List;

import vegml.VResultSet;
import vegml.VFrame;

/**
 * Language Model: Simple N-Grams 
 * 
 * With VegML an N-Gram is represented by a single NumberSet 3-Gram: x x x -> v
 * So a more practical simple N-Gram for this example would be the 1 - 8 Grams
 * - where each can provide its own result of next or the standard results can be used to smooth the value
 * 
 * NumberSets for the Dataplane are set as left edge
 * x x x x x x x x
 * - x x x x x x x
 * - - x x x x x x
 * - - - x x x x x 
 * - - - - x x x x
 * - - - - - x x x
 * - - - - - - x x
 * - - - - - - - x
 * 
 * - a power set could be used to allow predictions for last, next and missing word but this example is 
 *   sticking to a standard N-Gram language model
 */
public class NGramLanguageModel {
	//static private DataSetType dataSetToUse = DataSetType.BrownPennTreebankTags; 	// moved 'almost' to penn treebank (49)
	//static private DataSetType dataSetToUse = DataSetType.BrownUniversalTags; 	// universal tagset
	//static private DataSetType dataSetToUse = DataSetType.BrownCleanTags; 		// just primary tags per token (91)
	//static private DataSetType dataSetToUse = DataSetType.BrownOldTags; 			// base form (4xx)
	static private DataSetType dataSetToUse = DataSetType.WSJTreebank3;			// load WSJ Full
	//static private DataSetType dataSetToUse = DataSetType.WSJTreebank;			// load WSJ if you got it (I don't $$$)
	//static private DataSetType dataSetToUse = DataSetType.UDPipeConLL;					// load CONLL17 UDPipe.conllu

	// expects a neg and pos directory under it
	// THERE MUST BE NO OTHER FILES IN THIS DIRECTORY 
	// - when you download the content there are a few indexes/etc.. delete them or results will be significantly flawed
	static final String file_base_directory = "../corpus/brownPos";
	static final String wsj_file_base_directory = "../corpus/treebank/tagged";
	static final String wsj_full_file_base_directory = "../corpus/treebank_3/treebank_3/tagged/pos/wsj";
	static final String corpus_base_directory = "../corpus";	

	


	////////////////////////////////////////////////////
	// Do it
	public static void main(String [] args) {
		VegML.showCopywrite();
		int window = 9; // max n-gram to retain
		String fileName = "ngram-"+window+".veg";

		////////////////////////////////////////////////////
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
			ds = VFileUtil.loadDataSetsDS(dataSetToUse, filename, 0, 0);
		}		
		
		// this data set is currently text to POS, must change it to text and make it lowercase, tag ends
		ds.toLowercase();
		// remove all breaks 
		ds.removeDataS("<===sss>");
		ds.removeDataS("<===ccc>");
		ds.removeDataS("<====>");
		for (VDataSet d:ds.getAll()) {
			List<String> nvl = new ArrayList<>();
			for (int i=0;i<d.size();i++) {
				if (i != (d.size()-1)) nvl.add(d.getDataS(i+1));
				else nvl.add("<EOF>");
			}
			// set new values
			d.setValueLS(nvl);
		}
		ds.genVSets();

		
		//////////////////////////
		// CONFIGURE
		VegML vML = null;
		vML = new VegML("N-Gram 1-"+window);
		vML.setCfgDescription("1-Gram, 2-Gram, 3-Gram... "+window+"-Gram");

		// Setup dataPlane
		vML.addDataPlane("text", "text", window, 0, -1, NumberSetType.SequenceRight, NSWeightBase.None);
		//vML.setCfgNSWeight("text", "text", NSWeightBase.Flat); 		// reduces all
		//vML.setCfgNSWeight("text", "text", NSWeightBase.Natural); 	// longer over shorter
		//vML.setCfgNSWeight("text", "text", NSWeightBase.Distance); 	// Closer to end over farther		
		//vML.setCfgProbMethod("text", "text", ProbMethod.AverageIfNotRecall); 	
		vML.setCfgIdentityOnly("text", "text", false);
		vML.setCfgNoEmptyElements("text", "text", true, false);
		vML.setCfgFrameFocus("text", "text", window-1);
		vML.addStringMappingValues("text", "text", ds);	// add all the data strings in the ds
		vML.setCfgDefaultDataPlane("text", "text");
		vML.setCfgPCalc("text", "text", vML.getPCalcProbabilityOnly());
		
		//////////////////////////
		// TRAIN DATA
		VDataSets trainDs = ds.getTrainDataSets();
		System.out.print("TRAINING[N-Gram] w["+window+"] sets["+ trainDs.size()+"] .");
		VResultSet ts = VegTrain.trainDataSets(vML, "text", "text", trainDs);
		System.out.println(".. COMPLETE total["+ts.total+"]  Time["+ts.getDurration()+"]");	
		vML.print(true);

		
		//////////////////////////
		// TUNE MODEL
		// TODO: go for it
		
		// save the model
		vML.save(fileName);		

		
		//////////////////////////
		// TEST
		
		// Mixed Grams
		String tst = "there was a";
		List<ValProb> vp = VegTest.predictFrameVP(vML, "text", "text", makeFrame(window, tst));
		if (vp != null) {
			// vp is a list of most probable next words
			String rs = vML.getStringMapping("text", "text", vp.get(0).value);
			System.out.println("Mixed-Gram["+tst+"] -> ["+rs+"] ("+vp.get(0).type+")");	
		} else {
			System.out.println("Mixed-Gram["+tst+"] -> NOPE");					
		}
		
		// another
		tst = "and what do you think";
		vp = VegTest.predictFrameVP(vML, "text", "text", makeFrame(window, tst));
		if (vp != null) {
			String rs = vML.getStringMapping("text", "text", vp.get(0).value);
			System.out.println("Mixed-Gram["+tst+"] -> ["+rs+"] ("+vp.get(0).type+")");		
		} else {
			System.out.println("Mixed-Gram["+tst+"] -> NOPE");					
		}
		
		// 3-Gram: just use the NumberSet for 3-Gram
		int SetNum3 = 2;
		tst = "and there was a";
		// just get the frame back with all the results (and the combined as well)
		VFrame vf = VegTest.predictFrameVPFrame(vML, "text", "text", makeFrame(window, tst));
		if (vf != null && vf.getVPListNumberSet(SetNum3) != null) {
			vp = vf.getVPListNumberSet(SetNum3);
			String rs = vML.getStringMapping("text", "text", vp.get(0).value);
			System.out.println("3-Gram["+tst+"] -> ["+rs+"]");			
		} else {
			System.out.println("3-Gram["+tst+"] -> NOPE");	
		}
		// for more details of the results
		//vf.print(true);
		
		
		//////////////////////////
		// get the probability of specific next word
		// toward calculating perplexity
		// probabilities from Veg are affected by the count of numberSets, relative probabilities
		// just using the last phrase here
		String word = "book";
		long valueId = vML.getVaulueId("text", "text", word);
		if (vf != null) {
			ValProb v = ValProb.find(vf.getVPList(), valueId);
			if (v == null) System.out.println("PROB["+tst+"]["+word+"] -> 0");
			else {
				// this might not be the best probability to use... but
				System.out.println("PROB["+tst+"]["+word+"] -> "+v.probability);
			}
		} else {
			System.out.println("PROB["+tst+"]["+word+"] -> -0");				
		}		
	}


	/**
	 * make array from string
	 * @param window size of window
	 * @param words words to be parsed
	 * @return frame of correct size
	 */
	static List<String> makeFrame(int window, String words) {
		String [] xf = words.split("\\s+");
		List<String> frame = new ArrayList<>();
		for (int i=0;i<xf.length;i++) frame.add(xf[i]);
		while (frame.size() > window) frame.remove(0);
		while (frame.size() < window) frame.add(0, "");
		return frame;
	}

}
