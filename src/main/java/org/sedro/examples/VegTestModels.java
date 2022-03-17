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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vegml.VResultSet;
import vegml.VegML.PredictionType;
import vegml.VegCallOut;
import vegml.VegTest;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;


/**
 * Utility to test and view models results as a single or set
 * command line options allow some flexibility
 * 
 * **Dataset usage not so amazing**
 * - must have corpus in the path specified in the static's
 */
public class VegTestModels {
	
	/////////////////////////////////////////////////////////////////////
	//
	// Main to load Veg Models and show eval results
	//
	/////////////////////////////////////////////////////////////////////
	public static void main(String [] args) {		
		VegML.showCopywrite();
		
		/////////////////////////////////////////////////////////////////////
		// Configuration
		String directory = null, pre = "vtext", dtag = "text", dptag = "pos";
		int windowMax = 7;
		int window = windowMax = 1;
		boolean amp = true;
		int optMax = 50;
		double percentTest = 15, percentTune = 0;		
		boolean showNumberSets = false;
		boolean fullData = false;
		boolean endOnMiss = true;
		String corpusDir = "../corpus";
		String dataset = "WSJ"; // brown/brown-penntreebank
				
		directory = "../models";
		//directory = ".";
		
		/////////////////////////////////////////////////////////////////////
		// parse the arguments if from command line
		if (args != null && args.length > 0) {    		
	    	// test/testadd/rss/
			for (String a:args) {
				String [] ap = a.split("=");	
				if (a.startsWith("window=")) {
					int m = Integer.parseInt(ap[1]);
					if (m > 0) window = m;				
				} else if (a.startsWith("wmax=")) {
					int m = Integer.parseInt(ap[1]);
					if (m > 0) windowMax = m;
				} else if (a.startsWith("amp=")) {
					if (ap[1].equalsIgnoreCase("false")) amp = false;
				} else if (a.startsWith("directory=")) {
					directory = ap[1];
				} else if (a.startsWith("prefix=")) {
					pre = ap[1];
				} else if (a.startsWith("dtag=")) {
					dtag = ap[1];
				} else if (a.startsWith("dptag=")) {
					dptag = ap[1];		
				} else if (a.startsWith("fulldata=")) {
					if (ap[1].equalsIgnoreCase("true")) fullData = true;
				} else if (a.startsWith("dataset=")) {
					// this is messy: WSJ:../corpus
					String sq [] = ap[1].split(":");
					if (sq.length == 2) corpusDir = sq[1];
					dataset = sq[0];
				}
			}
		} 

		
		/////////////////////////////////////////////////////////////////////
		// load dataSet
		VDataSets dss = VFileUtil.loadDataSet(dataset, corpusDir, percentTune, percentTest);
		System.out.println("DATASET["+dataset+"] LOADED train["+dss.getTrainCount()+"] tune["+dss.getTuneCount()+"] test[" + dss.getTestCount()+"] dataWidth["+ dss.getDefinition().getTagCount()+"]");	

		VDataSets tuneDs = dss.getTuneDataSets();
		VDataSets testDs = dss.getTestDataSets();
		VDataSets fullDs = tuneDs;
		if (fullData) fullDs = dss.getTrainAndTuneDataSets();

		
		/////////////////////////////////////////////////////////////////////
		// Show the models
		/////////////////////////////////////////////////////////////////////		
		// if testing a merge or other with special call out (not used in amp)
		VegCallOut specialCa = null, specialAmpCa = null;
		boolean showConfig = true, first = true;
		
		// loading directory of models?
		List<String> fnmodels = null;
		if (directory != null) {
			fnmodels = VFileUtil.fileDirList(directory);
			if (fnmodels != null) {
				List<String> nl = new ArrayList<>();
				for (int x=0;x<fnmodels.size();x++) {
					if (fnmodels.get(x).endsWith(".veg")) {
						nl.add(directory+"/"+fnmodels.get(x));
					}
				}
				fnmodels = nl;
				Collections.sort(fnmodels);
				optMax = fnmodels.size();
			}
		}

		/////////////////////////////////////////////////////////////////////
		// Iterate through all the models found infiles
		/////////////////////////////////////////////////////////////////////
		for (;window<=windowMax;window++) {
			int i = 1;
			if (fnmodels != null) i = 0;
			for (;i<optMax;i++) {
				// load modle if present
				String fileName = pre+"-"+i+".veg";
				if (fnmodels != null) fileName = fnmodels.get(i);
						
				VegML vML = VegML.load(fileName);	
				if (vML == null) {
					if (i > 9 && endOnMiss) return; // to many not found
					continue;
				}
				
				// get DataPlan from vml (may change as sequence changes)
				String ndtag = vML.getCfgDefaultDTag();
				dptag = vML.getCfgDefaultDPTag();
				if (dtag == null || !dtag.equals(ndtag)) {
					dtag = ndtag;
					first = true;
				}
				if (!vML.haveDataPlane(dtag, dptag)) {
					System.out.println("ERROR no dataplane["+dtag+"/"+dptag+"] " + fileName);
					continue;
				}
				if (vML.getVectorCount(dtag, dptag) == 0) {
					System.out.println("ERROR empty dataplane["+dtag+"/"+dptag+"] " + fileName);
					continue;
				}
				if (first) {
					System.out.println("---- DATA["+dtag+"/"+dptag+"]w["+window+" - "+windowMax+"] pre["+pre+"] ----");
					first = false;
				}
				
				int optType = vML.getCfgScratchInt(dtag, dptag, "optType");
				boolean optFullData = vML.getCfgScratchBool(dtag, dptag, "optFullData");
				if (showConfig) {
					// show this data... this could be different for each ..
					int vectCnt = vML.getVectorCount(dtag, dptag);
					int vscnt = vML.getVectSetsCount();
					int optNSWeightBase = vML.getCfgScratchInt(dtag, dptag, "optNSWeightBase");
					int optStep = vML.getCfgScratchInt(dtag, dptag, "optStep");
					String optTime = (String)vML.getCfgScratch(dtag, dptag, "optTime");
					String method = (String)vML.getCfgScratch(dtag, dptag, "optMethod");						
					System.out.println("// CFG win["+vML.getCfgWindowSize(dtag, dptag)+"]ns["+vML.getNSCount(dtag, dptag)+"] [w"+optNSWeightBase+"]t["+optType+"]m["+method+"]fullData["+optFullData+"] step["+optStep+"] created["+optTime+"] vcnt["+vectCnt+"]vs["+vscnt+"]file["+fileName+"]");
					showConfig = true;
					if (showNumberSets) vML.print(dtag, dptag, true);
				}
				
				// SHOW DEV/TEST
				int r = testFileDev(vML, dtag, dptag, window, i, fileName, specialCa, specialAmpCa, amp, fullDs);
				if (r < 0) continue;
				testFileTest(vML, dtag, dptag, window, i, specialCa, specialAmpCa, amp, testDs);
				vML = null;
				System.gc();
				System.out.println("");
			}
			System.out.println("");
		}
	}
	
	/////////////////////////////////////////////////////////////////////
	//
	// The printed output for each
	//
	private static void printLine(VResultSet ptsk) {
		double rp = (double)100 * ((double)ptsk.getPassTotal(PredictionType.Recall)/(double)(ptsk.getPassTotal(PredictionType.Recall)+ptsk.getFailTotal(PredictionType.Recall)));
		double rpp = (double)100 * ((double)ptsk.getPassTotal(PredictionType.RecallPredict)/(double)(ptsk.getPassTotal(PredictionType.RecallPredict)+ptsk.getFailTotal(PredictionType.RecallPredict)));
			
		String t = "["+String.format("%.2f", ptsk.passPercent)+"%] ["+String.format("%6d", ptsk.passTotal)+" / "+String.format("%5d", ptsk.failTotal) + " of " + ptsk.total+"]  =>"
				// recall
				+ "  r["+String.format("%7d", ptsk.getPassTotal(PredictionType.Recall))+" / "+String.format("%4d", ptsk.getFailTotal(PredictionType.Recall))+"]["+String.format("%.2f", rp)+"%]"
				+ " rp["+String.format("%6d", ptsk.getPassTotal(PredictionType.RecallPredict))+"]["+String.format("%.2f", rpp)+"%]"
				
				// any predict
				+ " p["+String.format("%7d", ptsk.getPassTotal(PredictionType.AnyPredict))+"]"
				// any known
				+ " kn["+String.format("%7d", ptsk.getPassTotal(PredictionType.NotUnknown))+" / "+String.format("%5d", ptsk.getFailTotal(PredictionType.NotUnknown))+"]"
				// any unknown
				+ " unk["+String.format("%6d", ptsk.getPassTotal(PredictionType.AnyUnknown))+" / "+ptsk.getFailTotal(PredictionType.AnyUnknown)+"]";
		System.out.println(t);
	}
	
	/////////////////////////////////////////////////////////////////////
	//
	// tune data eval
	//
	private static int testFileDev(VegML vML, String dtag, String dptag, int window, int i, String fileName,
									VegCallOut specialCa, VegCallOut specialAmpCa, boolean amp, VDataSets dss) {
		VegCallOut bc = vML.getCfgCallout(dtag, dptag);
		
		// Dev Base
		vML.setCfgCallout(dtag, dptag, null, null);
		if (specialCa != null) vML.setCfgCallout(dtag, dptag, specialCa, null);
		
		vML.setCfgFramerArg(dss.getValLLV());
		System.out.print("  STD");
		VResultSet ptsk = VegTest.testSets(vML, dtag, dptag, dss);
		if (ptsk == null) {
			System.out.print("ERROR["+dtag+"/"+dptag+"]");
			vML.print();
			return -1;
		}
		printLine(ptsk);

		// Dev USR or AMP
		if (bc != null) {
			vML.setCfgCallout(dtag, dptag, bc, null);
			System.out.print("  USR");
			ptsk = VegTest.testSets(vML, dtag, dptag, dss);
			printLine(ptsk);
		} else if (amp) {			
			vML.setCfgCallout(dtag, dptag, VegCallOut.getCallOutAmp(vML, dtag, dptag), null);
			if (specialAmpCa != null) vML.setCfgCallout(dtag, dptag, specialAmpCa, null);
			System.out.print("  AMP");
			ptsk = VegTest.testSets(vML, dtag, dptag, dss);
			printLine(ptsk);
		}
		vML.setCfgCallout(dtag, dptag, bc, null);
		vML.setCfgFramerArg(null);	
		return 0;
	}
	
	/////////////////////////////////////////////////////////////////////
	//
	// Test data eval
	//
	private static void testFileTest(VegML vML, String dtag, String dptag, int window, int i,
									VegCallOut specialCa, VegCallOut specialAmpCa, boolean amp, VDataSets dss) {

		VegCallOut bc = vML.getCfgCallout(dtag, dptag);
		
		// Test Base
		vML.setCfgCallout(dtag, dptag, null, null);
		if (specialCa != null) vML.setCfgCallout(dtag, dptag, specialCa, null);
		vML.setCfgFramerArg(dss.getValLLV());
		System.out.print("T-STD");
		VResultSet ptsk = VegTest.testSets(vML, dtag, dptag, dss);
		printLine(ptsk);		
		
		// Test USR or AMP
		if (bc != null) {
			vML.setCfgCallout(dtag, dptag, bc, null);
			System.out.print("T-USR");
			ptsk = VegTest.testSets(vML, dtag, dptag, dss);
			printLine(ptsk);		
		} else if (amp) {
			vML.setCfgCallout(dtag, dptag, VegCallOut.getCallOutAmp(vML, dtag, dptag), null);
			if (specialAmpCa != null) vML.setCfgCallout(dtag, dptag, specialAmpCa, null);
			System.out.print("T-AMP");
			ptsk = VegTest.testSets(vML, dtag, dptag, dss);
			printLine(ptsk);		
		}
		vML.setCfgCallout(dtag, dptag, bc, null);
		vML.setCfgFramerArg(null);	
	}

}
