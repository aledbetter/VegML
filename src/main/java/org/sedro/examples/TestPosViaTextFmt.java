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

import java.util.ArrayList;
import java.util.List;

import vegml.VegML;
import vegml.VegML.NSWeightBase;
import vegml.VegTest.SaveStats;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.VegUtil;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VDataSetDescriptor.DSInputSetType;
import vegml.Data.VDataSets.DSFmt;
import vegml.VResultSet;


/**
 * Part of speech tagging
 * 
 * train and Test based on token text input and format
 * <text1> <text2> <text-f> <text4> <text5>
 * <fmt>   <fmt>   <fmt>    <fmt>   <fmt>
 * 
 * Text is simply tokenized and fed in with the tags, in some datasets multiple tags may be
 * trained to the same frame (WSJTreebank3)
 * 
 * This utilizes the dependent associated data variables, thus the input frame is a matrix
 * 
 */
public class TestPosViaTextFmt {
	static String directory = "../models";

	////////////////////////////////////////////////////
	// Train or optimize a model for events
	//
	public static void main(String [] args) {
		double percentTune = 25, percentTest = 25;
		int window = 5, valueFocus = 2;	
		// 2/1 TEST    => RESULT[94.44%] miss[9520 of 171138]  Time[0:00.659]
		// 3/1 TEST    => RESULT[95.58%] miss[7563 of 171138]  Time[0:03.474]
		// 4/2 TEST    => RESULT[95.82%] miss[7152 of 171138]  Time[0:09.705]
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
		VDataSets dss = VFileUtil.loadDataSet(dataset, corpusDir, percentTune, percentTest);
		System.out.println("DATASET["+dataset+"] LOADED train["+dss.getTrainCount()+"] tune["+dss.getTuneCount()+"] test[" + dss.getTestCount()+"] dataWidth["+ dss.getDefinition().getTagCount()+"]");	
		VDataSets trainDs = dss.getTrainDataSets();
		VDataSets tuneDs = dss.getTuneDataSets();
		VDataSets testDs = dss.getTestDataSets();
		
		// add format to all inputs
		List<List<Object []>> dsList = makeData(trainDs.getLLS());
		List<List<Object []>> dsTuneList = makeData(tuneDs.getLLS());
		List<List<Object []>> dsTestList = makeData(testDs.getLLS());

		// update DataSets
		for (int i=0;i<trainDs.size();i++) {
			trainDs.get(i).setDataLOD(dsList.get(i));
		}
		for (int i=0;i<tuneDs.size();i++) {
			tuneDs.get(i).setDataLOD(dsTuneList.get(i));
		}
		for (int i=0;i<testDs.size();i++) {
			testDs.get(i).setDataLOD(dsTestList.get(i));
		}
		trainDs.setFmtData(DSFmt.LLOD);
		trainDs.genVSets();
		tuneDs.setFmtData(DSFmt.LLOD);
		tuneDs.genVSets();
		testDs.setFmtData(DSFmt.LLOD);
		testDs.genVSets();
		
		trainModel(window, valueFocus, (int)percentTune, dss, trainDs, tuneDs, testDs);
	}
	
	// add an extra dependent param that is the FORMAT of the token
	static List<List<Object []>> makeData(List<List<String>> dsListi) {
		List<List<Object []>> dsList = new ArrayList<>();
		// convert to objects HACK for some testing
		for (int i=0;i<dsListi.size();i++) {
			List<String> lsi = dsListi.get(i);
			List<Object []> ls = new ArrayList<>();
			dsList.add(ls);
			for (int x=0;x<lsi.size();x++) {
				String v = lsi.get(x);
				Object [] o = new Object[2];
				o[0] = v;
				o[1] = VegUtil.getStringFormat(v); // Format
				ls.add(o);
			}
		}
		return dsList;
	}
	
	static VegML trainModel(int window, int valueFocus, int percent, VDataSets dss,
			VDataSets trainDs, VDataSets tuneDs, VDataSets testDs) {
		
		VegML vML = new VegML("pos-text-fmt-"+window);
		vML.setCfgDescription("pos tagging with fmt window " +window);
	
		// Setup dataPlane
		vML.addDataPlane("textfmt", "pos", window, valueFocus, trainDs.getDefinition().getTagCount(), NSWeightBase.Distance); // set the posibilities for values	
		vML.setCfgDescription("textfmt", "pos", "decide pos from token & dependent format");
		vML.addStringMapping("textfmt", "pos", dss.getDefinition().getTagsStrings());	
		vML.setCfgIdentityOnly("textfmt", "pos", true);
		// add definition for second tier
		vML.setCfgDataDefinitionInput("textfmt", "pos", "format", DSInputSetType.Unique, DSDataType.String, false);
		vML.updateCfgNS("textfmt", "pos");

		
		//////////////////////////
		// TRAIN DATA
		System.out.print("TRAINING[text] w["+window+"/"+valueFocus+"]ns["+NSWeightBase.Distance+"] sets["+ trainDs.size()+"] .");
		VResultSet ts = VegTrain.trainDataSets(vML, "textfmt", "pos", trainDs);
		System.out.println(".. COMPLETE total["+ts.total+"]  Time["+ts.getDurration()+"]");	
		
		// clear the breaks/whitespace added from values and results
		vML.removeAllValueId("textfmt", "pos", new Object [] {"CBR", "SBR", "BR"});	
		// clear the breaks/whitespace added from values and results
//		testT = 171138;devT = 148158;	
		vML.setCfgIgnoreInputs("textfmt", "pos", new String [] {"<===SSS>", "<===CCC>", "<====>"}, new String [] {"SBR", "CBR", "BR"});		
		//vML.print(true);
		
		
		//////////////////////////
		// TUNE a bit
//		vML.makeSolid("textfmt", "pos");
		VegTune.logicalDataDefinitionReduction(vML, "textfmt", "pos", null);
		VegTune.logicalDataIdentityReduction(vML, "textfmt", "pos", false, trainDs);
		//VegTune.reduceNaive(vML, "textfmt", "pos");


		//////////////////////////
		// Test/Train Recall
		System.out.print("RECALL  =>");	
		VResultSet tsk = VegTest.testSets(vML, "textfmt", "pos", trainDs, SaveStats.SaveRecall);		
		System.out.println(" RESULT["+String.format("%.2f", tsk.passPercent)+"%] miss["+tsk.failTotal + " of " + tsk.total+"] w["+vML.getCfgWindowSize("textfmt", "pos")+"] Time["+tsk.getDurration()+"]");			

		//////////////////////////
		// Dev/Tune Prediction		
		System.out.print("DEV     =>");	
		VResultSet ptsk = VegTest.testSets(vML, "textfmt", "pos", tuneDs, SaveStats.SavePredict);
		System.out.println(" RESULT["+String.format("%.2f", ptsk.passPercent)+"%] miss["+ptsk.failTotal + " of " + ptsk.total+"]  Time["+ptsk.getDurration()+"]");		

		//////////////////////////
		// Test Data
		System.out.print("TEST    =>");		
		ptsk = VegTest.testSets(vML, "textfmt", "pos", testDs);
		System.out.println(" RESULT["+String.format("%.2f", ptsk.passPercent)+"%] miss["+ptsk.failTotal + " of " + ptsk.total+"]  Time["+ptsk.getDurration()+"]");		

		
		//////////////////////////
		// Save it		
		vML.print();
		vML.save("vtextfmt-"+window+".veg");
		return vML;
	}
	
}
