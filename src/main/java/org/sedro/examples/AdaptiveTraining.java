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

import java.util.HashMap;

import vegml.VDataPlane;
import vegml.VegCallOut;
import vegml.VegML;
import vegml.VegML.NSWeightBase;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;

import vegml.VegTune;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.VegCallOut.VegCOMCondition;
import vegml.VegCallOut.VegCallOutMods;


/**
 * Example use of Adaptive training to create a complex mixed model
 * 
 * Adaptive training allows the correct relationships and numberSets to be automatically assessed as information
 * is added to the model. This reduces human errors and judgment from the process and produces smaller, faster, 
 * and better models.
 * 
 * It is not documented anywhere at this time: TODO
 * 
 * Again this is part of speech tagging used to demonstrate the methodology and software feature that implements it
 *
 */
public class AdaptiveTraining {
	static String directory = "../models";
	static VDataSets ds = null;
	
	
	////////////////////////////////////////////////////
	// Adaptive Training
	public static void main(String [] args) {
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
		if (ds == null) {
			System.out.println("ERROR DATASET["+dataset+"] not found at: " + corpusDir);
			return;
		}
		System.out.println("DATASET["+dataset+"] train["+ds.getTrainCount()+"] tune["+ds.getTuneCount()+"] test[" + ds.getTestCount()+"] dataWidth["+ ds.getDefinition().getTagCount()+"]");	
		
		
		//////////////////////////
		// Configure tuned elements
		boolean useReduction = true, fullData = false;
		int maxWindow = -1;						
		
		
		//////////////////////////
		// CREATE MODEL(s)
		VegML vML = new VegML("text-pos");
		vML.setCfgDescription("English POS tagging Model");

		HashMap<String, Object> cfg = new HashMap<>();
		VegCallOutMods vcom = null;
		VegCOMCondition cond = null;
		
		//////////////////////////			
		// TEXT/POS
		vML.addDataPlane("text", "pos", ds.getDefinition().getTagCount()); 
		vML.addStringMapping("text", "pos", ds.getDefinition().getTagsStrings());
		vML.setCfgRegion("text", "pos", 5);	
		vML.setCfgDefaultDataPlane("text", "pos");
		vML.setCfgIgnoreInputs("text", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		String initName = "v"+vML.getCfgDefaultDTag()+".veg";
		System.out.println("");
		System.out.println("------------------------------------------------------------------------------------------------");	
		System.out.println("  TEXT: " + initName);	
		System.out.println("------------------------------------------------------------------------------------------------");	
		vML.save(initName);			
		cfg.clear();
		cfg.put("applyDependency", true);
		cfg.put("applyDependencyMin", true); // better
		cfg.put("betterThanDrop", 5);
		cfg.put("processCycleCount", 3);  // 3 
		cfg.put("CARVE_MAX_NS", 8);	// 8
		cfg.put("MAX_NS_PER_PASS", 4);  // 3-4
		cfg.put("MIN_NS_PER_PASS", 3);		
		cfg.put("exportJSON", true);  // save json for each
		maxWindow = 6;		
		initName = VegTune.adaptiveTraining(vML, initName, maxWindow, null, useReduction, cfg, fullData, ds);
		
		//////////////////////////			
		// TEXT_UN/POS un
		System.out.println("");
		System.out.println("------------------------------------------------------------------------------------------------");	
		System.out.println("  TEXT_UN: " + initName);	
		System.out.println("------------------------------------------------------------------------------------------------");	
		vML = VegML.load(initName);
		vML.addDataPlane("text_un", "pos", ds.getDefinition().getTagCount()); 
		vML.addStringMapping("text_un", "pos", ds.getDefinition().getTagsStrings());
		vML.setCfgRegion("text_un", "pos", 6);	
		vML.setCfgIgnoreInputs("text_un", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		vcom = VegCallOut.makeCallOut();
		cond = VegCallOut.getConditionCheckLowerCase();  // use lower case check condition
		cond.init(vML.getDataPlane("text", "pos"));
		vcom.setLower(PredictionType.AnyUnknown, cond);	
		vcom.setReplace(PredictionType.AnyUnknown, "text_un", "pos"); 
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgDefaultDataPlane("text_un", "pos");
		vML.save(initName);		
		cfg.clear();		
		cfg.put("applyPrepCarve", true);
		cfg.put("applyCarve", true);
		cfg.put("applyDependency", false);	
		cfg.put("betterThanDrop", 2);
		cfg.put("betterThanPossibleRaw", 0);
		cfg.put("betterThanPossiblePrep", 0);
		cfg.put("maxNoAddCnt", 3);
		cfg.put("betterTryLimit", 8);
		cfg.put("CARVE_MAX_NS", 4);	// 4
		cfg.put("processCycleCount", 2);  // 2
		cfg.put("MAX_NS_PER_PASS", 8);  // 8-6 -3070
		cfg.put("MIN_NS_PER_PASS", 6);
		cfg.put("mergeSetInfoDefaultD", "text");
		cfg.put("mergeSetInfoDefaultDP", "pos");
		cfg.put("extendUnknowns", true);
		maxWindow = 6;		
		initName = VegTune.adaptiveTraining(vML, initName, maxWindow, PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
		
		//////////////////////////			
		// AFFIX/POS un
		System.out.println("");
		System.out.println("------------------------------------------------------------------------------------------------");	
		System.out.println("  AFFIX: " + initName);	
		System.out.println("------------------------------------------------------------------------------------------------");	

		vML = VegML.load(initName);
		vML.addDataPlane("affix", "pos", ds.getDefinition().getTagCount(), NumberSetType.SequenceEdgeId, NSWeightBase.None); 
		vML.setCfgNoEmptyElements("affix", "pos", true, true);
		//vML.setCfgProbMethod("affix", "pos", ProbMethod.Average);
		vML.setCfgFramerCharEdge("affix", "pos", 3, true, true, 4);
		vML.setCfgIgnoreInputs("affix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		vML.addStringMapping("affix", "pos", ds.getDefinition().getTagsStrings());
		vML.setCfgRegion("affix", "pos", 12);
		// make integration callout for text
		vcom = VegCallOut.makeCallOut();
		cond = VegCallOut.getConditionCheckLowerCase();  // use lower case check condition
		cond.init(vML.getDataPlane("text", "pos"));
		vcom.setLower(PredictionType.AnyUnknown, cond);	
		vcom.setReplace(PredictionType.AnyUnknown, "text_un", "pos"); 
		//vcom.setReplace(PredictionType.AnyUnknown, "affix", "pos"); 
		vcom.setMerge(PredictionType.AnyUnknown, "affix", "pos", 0.5); 
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		vML.setCfgDefaultDataPlane("affix", "pos");
		vML.save(initName);
		cfg.clear();
		//maxWindow = 12;
		maxWindow = 16;
		//CHAR: all - carve/prepcarve/no-dep OR prepcarve only
		cfg.put("applyPrepCarve", true);
		cfg.put("applyCarve", true);
		cfg.put("maxNoAddCnt", 10);
		cfg.put("betterTryLimit", 32);
		cfg.put("betterThanPossiblePrep", 0);
		cfg.put("betterThanPossibleRaw", 0);
		cfg.put("MAX_NS_PER_PASS", 12);
		cfg.put("MIN_NS_PER_PASS", 6);
		cfg.put("processCycleCount", 2);
		cfg.put("CARVE_MAX_NS", 2);
		cfg.put("betterThanDrop", 1);
		cfg.put("mergeValueOnly", true);
		cfg.put("mergeSetInfoDefaultD", "text");
		cfg.put("mergeSetInfoDefaultDP", "pos");
		cfg.put("extendUnknowns", true);
		fullData = false;
		//merged: 3138 -> 3721
		// 3164 -> 3722
	//	initName = VegTune.adaptiveTrainingMerge(vML, initName, "affix", "pos", maxWindow, PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
	//	initName = VegTune.adaptiveTraining(vML, initName, "affix", "pos", maxWindow, PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
		initName = VegTune.adaptiveTraining(vML, initName, maxWindow, PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
		
		
		//////////////////////////			
		// mix_un/POS
		System.out.println("");
		System.out.println("------------------------------------------------------------------------------------------------");	
		System.out.println("  MIX_UN: " + initName);	
		System.out.println("------------------------------------------------------------------------------------------------");	
		vML = VegML.load(initName);
		vML.addDataPlane("mix_un", "pos", ds.getDefinition().getTagCount());
		vML.setCfgFramerResponseMix("mix_un", "pos", "text", "pos");		
		vML.addStringMapping("mix_un", "pos", ds.getDefinition().getTagsStrings());
		vML.setCfgRegion("mix_un", "pos", 6);
		vML.setCfgIgnoreInputs("mix_un", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		// make integration callout for text
		vcom = VegCallOut.makeCallOut();
		cond = VegCallOut.getConditionCheckLowerCase();
		cond.init(vML.getDataPlane("text", "pos"));
		vcom.setLower(PredictionType.AnyUnknown, cond);			
		vcom.setReplace(PredictionType.AnyUnknown, "text_un", "pos"); 
		vcom.setMerge(PredictionType.AnyUnknown, "affix", "pos", 0.5); 
		vcom.setMerge(PredictionType.AnyUnknown, "mix_un", "pos", 0.5); 
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		
		vML.setCfgDefaultDataPlane("mix_un", "pos");
		vML.save(initName);		
		cfg.clear();		
		cfg.put("applyPrepCarve", false);
		cfg.put("applyCarve", true);
		cfg.put("applyDependency", false);	
		cfg.put("betterThanPossibleRaw", 0);
		cfg.put("betterThanPossiblePrep", 0);		

		cfg.put("CARVE_MAX_NS", 2);
		cfg.put("processCycleCount", 2);
		cfg.put("betterThanDrop", 2);
		cfg.put("MAX_NS_PER_PASS", 16);
		cfg.put("MIN_NS_PER_PASS", 8);
		cfg.put("maxNoAddCnt", 3);
		cfg.put("betterTryLimit", 8);
		cfg.put("mergeValueOnly", true);
		cfg.put("mergeSetInfoDefaultD", "text");
		cfg.put("mergeSetInfoDefaultDP", "pos");
		cfg.put("extendUnknowns", true);
		maxWindow = 6;		
		//initName = VegTune.adaptiveTrainingMerge(vML, initName, "mix_un", "pos", maxWindow, vML.getCfgDefaultNSWeight(), PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
		initName = VegTune.adaptiveTraining(vML, initName, maxWindow, PredictionType.AnyUnknown, useReduction, cfg, fullData, ds);
		
		
		//////////////////////////			
		// MIXED/POS
		System.out.println("");
		System.out.println("------------------------------------------------------------------------------------------------");	
		System.out.println("  MIX: " + initName);	
		System.out.println("------------------------------------------------------------------------------------------------");	
		vML = VegML.load(initName);
		vML.addDataPlane("mix", "pos", ds.getDefinition().getTagCount());
		vML.setCfgFramerResponseMix("mix", "pos", "text", "pos");		
		vML.addStringMapping("mix", "pos", ds.getDefinition().getTagsStrings());
		vML.setCfgRegion("mix", "pos", 6);
		vML.setCfgIgnoreInputs("mix", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});
		
		// make integration callout for text
		vcom = getCallOutNew(vML, "text", "pos");
		vML.setCfgCalloutDefault("text", "pos", vcom, null);	
		
		vML.setCfgDefaultDataPlane("mix", "pos");
		vML.save(initName);	
		cfg.clear();
		cfg.put("applyPrepCarve", false);
		cfg.put("applyCarve", true);
		cfg.put("applyDependency", false);		
		cfg.put("betterThanPossibleRaw", 0);
		cfg.put("betterThanPossiblePrep", 0);
		cfg.put("betterTryLimit", 16);
		cfg.put("CARVE_MAX_NS", 4); // 2
		cfg.put("processCycleCount", 2); // 1
		cfg.put("betterThanDrop", 2);
		cfg.put("MAX_NS_PER_PASS", 24);
		cfg.put("MIN_NS_PER_PASS", 16);
		// 6182
		cfg.put("mergeValueOnly", true);
		cfg.put("mergeSetInfoDefaultD", "text");
		cfg.put("mergeSetInfoDefaultDP", "pos");
		/// result and drop better number are not related ?? where are they commming from?
		maxWindow = 5;
		//initName = VegTune.adaptiveTrainingMerge(vML, initName, "mix", "pos", maxWindow, vML.getCfgDefaultNSWeight(), null, useReduction, cfg, fullData, ds);
		initName = VegTune.adaptiveTraining(vML, initName, maxWindow, null, useReduction, cfg, false, ds);
		/*
		 TUNE => [98.13%] pass[108908 /   2080 of   110988]  -> BEST[108668]
		 CNTL => [97.42%] pass[114515 /   3027 of   117542]  -> BEST[114386]
		 CULT => [97.79%] pass[114941 /   2601 of   117542]  -> BEST[114922]
		 TEST => [97.60%] pass[256556 /   6319 of   262875]  -> BEST[256431]
		 TSLT => [97.92%] pass[257409 /   5466 of   262875]  -> LESS[257414]	
		 */
		vML = VegML.load(initName);
		vML.setCfgDefaultDataPlane("text", "pos");
		vML.saveSilent(initName);
		
	}
	
	
	/////////////////////////////////////////////////////////////////////
	// Merge Callout
	/////////////////////////////////////////////////////////////////////
	public static VegCallOutMods getCallOutNew(VegML vML, String dTag, String dpTag) {	
		VDataPlane dp = vML.getDataPlane(dTag, dpTag);

		// use lower case check condition
		VegCOMCondition cond = VegCallOut.getConditionCheckLowerCase();
		cond.init(dp);
		
		VegCallOutMods vcom = VegCallOut.makeCallOut();
		// unknown path
		// conditional lowerCase
		vcom.setLower(PredictionType.AnyUnknown, cond);	
		vcom.setReplace(PredictionType.AnyUnknown, "text_un", "pos"); 
		vcom.setMerge(PredictionType.AnyUnknown, "affix", "pos", 0.5); 
	//	vcom.setMerge(PredictionType.AnyUnknown, "mix_un", "pos", 0.5); 

		// known path
		vcom.setAmp(PredictionType.NotUnknown, -1);
		vcom.setMerge(PredictionType.NotUnknown, "mix", "pos", 0.5); 

		return vcom;
	}
	
}
