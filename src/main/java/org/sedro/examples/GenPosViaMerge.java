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
	static VDataSets ds = null;		
	static String directory = "../models";

	////////////////////////////////////////////////////
	// brown text -> POS 
	// Train a dataplan to provide POS for input text
	public static void main(String [] args) {
		int prefix = 4, suffix = 10;
		double percentTune = 25, percentTest = 25;
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
		if (ds == null) {
			System.out.println("ERROR DATASET["+dataset+"] not found at: " + corpusDir);
			return;
		}
		System.out.println("DATASET["+dataset+"] train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	

		
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
		VegML vML = VegML.load(directory+"/vafx-"+(prefix+suffix)+"-w4-s"+afxNum+".veg");  // affix
		VegML vMLt = VegML.load(directory+"/crv-text-5-w1-sp"+textNum+".veg"); // text
		vML.merge(vMLt); // merge into one
		vML.setCfgDefaultDataPlane("text", "pos");
		// make integration callout for text
		VegCallOutMods vcom = VegCallOut.makeCallOut();		
		vcom.setReplace(PredictionType.AnyUnknown, "affix", "pos"); 		
		vcom.setAmp(PredictionType.NotUnknown, -1);	
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgCalloutToDefault("text", "pos");
		// save it
		vML.save(directory+"/vmerged-text-unaffix-5.veg");
				

		////////////////////////////////
		// mix with text 
		// with logic(lowercase) in model's resolve
		vML = VegML.load(directory+"/vafx-"+(prefix+suffix)+"-w4-s"+afxNum+".veg");  // affix
		vMLt = VegML.load(directory+"/crv-text-5-w1-sp"+textNum+".veg"); // text
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
		vML.save(directory+"/vmerged-text-unaffix-logic-5.veg");
		
		
		////////////////////////////////
		// mix with text, mix, text_un, mix_un
		vML = VegML.load(directory+"/vafx-14-w4-s"+afxMergeNum+".veg"); // affix merging
		vMLt = VegML.load(directory+"/crv-text-5-w1-sp"+textMergeNum+".veg");	// text
		vML.merge(vMLt); // merge into one
		
		VegML vMLtun = VegML.load(directory+"/crv_un-text-5-w1-sp"+textUnNum+".veg");	// text_un
		vMLtun.moveDataPlane("text", "pos", "text_un", "pos");
		vML.merge(vMLtun); // merge into one
		
		VegML vMLmun = VegML.load(directory+"/vmixun-5-w1-sp"+mixNum+".veg");	// mix_un
		vMLmun.moveDataPlane("mix", "pos", "mix_un", "pos");
		vML.merge(vMLmun); // merge into one
		
		VegML vMLm = VegML.load(directory+"/vmix-id-5-w1-sp"+mixUnNum+".veg");		// mix
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
		vML.save(directory+"/vmerged-text-mixed-5.veg");		
	}
}
