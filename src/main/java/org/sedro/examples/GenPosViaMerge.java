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
import vegml.VegML.PredictionType;
import vegml.VegTune;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VFileUtil.DataSetType;
import vegml.VegCallOut;
import vegml.VegCallOut.VegCOMCondition;
import vegml.VegCallOut.VegCallOutMods;


/**
 * Part of speech tagging with merged models
 * 
 * Here we generate a few merged models from the models generated in Affix/Mix/Text
 * 
 * These models use the callout to merge and amplify results as well as in-line logic in the models decisioning
 * 
 * user VegTestModels to view results, or load the models directly and use them
 */
public class GenPosViaMerge {
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
		// mixed models		
		buildMixedModels(prefix, suffix);
	}
	
	
	/**
	 * Build and test mixed models
	 */
	static void buildMixedModels(int prefix, int suffix) {
		// set the models to use based on their results
		int afxNum = 4;
		int textNum = 6;
		
		// merged model
		// the afx and text models are one less than optimal to get better merge results; more little information makes a difference
		int textMergeNum = 5;
		int afxMergeNum = 3;
		int textUnNum = 6;
		int mixNum = 2;
		int mixUnNum = 3;

		////////////////////////////////
		// mix with text 
		VegML vML = VegML.load("../models/vafx-"+(prefix+suffix)+"-w4-s"+afxNum+".veg");  // affix
		VegML vMLt = VegML.load("../models/crv-text-5-w1-sp"+textNum+".veg"); // text
		vML.merge(vMLt); // merge into one
		vML.setCfgDefaultDataPlane("text", "pos");
		// make integration callout for text
		VegCallOutMods vcom = VegCallOut.makeCallOut();		
		vcom.setReplace(PredictionType.AnyUnknown, "affix", "pos"); 		
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgCalloutToDefault("text", "pos");
		// save it
		vML.save("../models/vmerged-text-unaffix-5.veg");
				

		////////////////////////////////
		// mix with text 
		// with logic(lowercase) in model's resolve
		vML = VegML.load("../models/vafx-"+(prefix+suffix)+"-w4-s"+afxNum+".veg");  // affix
		vMLt = VegML.load("../models/crv-text-5-w1-sp"+textNum+".veg"); // text
		vML.merge(vMLt); // merge into one
		vML.setCfgDefaultDataPlane("text", "pos");
		// make integration callout for text
		vcom = VegCallOut.makeCallOut();	
		VegCOMCondition cond1 = VegCallOut.getConditionCheckLowerCase();
		cond1.init(vML.getDataPlane("text", "pos"));
		vcom.setLower(PredictionType.AnyUnknown, cond1);	
		vcom.setReplace(PredictionType.AnyUnknown, "affix", "pos"); 
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgCalloutToDefault("text", "pos");
		// save it
		vML.save("../models/vmerged-text-unaffix-logic-5.veg");
		
		
		////////////////////////////////
		// mix with text, mix, text_un, mix_un
		vML = VegML.load("../models/vafx-14-w4-s"+afxMergeNum+".veg"); // affix merging
		vMLt = VegML.load("../models/crv-text-5-w1-sp"+textMergeNum+".veg");	// text
		vML.merge(vMLt); // merge into one
		
		VegML vMLtun = VegML.load("../models/crv_un-text-5-w1-sp"+textUnNum+".veg");	// text_un
		vMLtun.moveDataPlane("text", "pos", "text_un", "pos");
		vML.merge(vMLtun); // merge into one
		
		VegML vMLmun = VegML.load("../models/vmixun-5-w1-sp"+mixNum+".veg");	// mix_un
		vMLmun.moveDataPlane("mix", "pos", "mix_un", "pos");
		vML.merge(vMLmun); // merge into one
		
		VegML vMLm = VegML.load("../models/vmix-5-w1-sp"+mixUnNum+".veg");		// mix
		vML.merge(vMLm); // merge into one
		vML.setCfgDefaultDataPlane("text", "pos");

		// make integration callout from text/pos
		vcom = VegCallOut.makeCallOut();
		VegCOMCondition cond = VegCallOut.getConditionCheckLowerCase();
		cond.init(vML.getDataPlane("text", "pos"));
		vcom.setLower(PredictionType.AnyUnknown, cond);	
		vcom.setReplace(PredictionType.AnyUnknown, "text_un", "pos"); 
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgCalloutToDefault("text", "pos");
		
		// add affix/pos for NotUnknown -> optimize merge
		vcom.setMerge(PredictionType.AnyUnknown, "affix", "pos", 0.5); 
		vML.setCfgCalloutDefault("text", "pos", vcom, null);			
		VegTune.optimizeMergeModels(vML, "text", "pos", "affix", "pos", false, false, PredictionType.AnyUnknown, false, true, false, ds.getTuneDataSets());
		
		// add mix_un/pos for AnyUnknown -> optimize merge
		vcom.setMerge(PredictionType.AnyUnknown, "mix_un", "pos", 0.5); 
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		VegTune.optimizeMergeModels(vML, "text", "pos", "mix_un", "pos", false, false, PredictionType.AnyUnknown, false, true, false, ds.getTuneDataSets());
		
		// add mix/pos for NotUnknown -> optimize merge
		vcom.setMergeAmp(PredictionType.NotUnknown, "mix", "pos", 0.5, -1);
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		VegTune.optimizeMergeModels(vML, "text", "pos", "mix", "pos", false, false, null, false, true, false, ds.getTuneDataSets());

		// save it
		vML.save("../models/vmerged-text-mixed-5.veg");		
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


}
