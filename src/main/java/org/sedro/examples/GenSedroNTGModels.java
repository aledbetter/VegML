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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import vegml.VDataPlane;
import vegml.VegML;
import vegml.VFrame;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;
import vegml.ValProb;
import vegml.VegTest;
import vegml.VegTrain;
import vegml.VegTune;
import vegml.Data.VFrameUtil;
import vegml.Data.VDataSet;
import vegml.Data.VDataSet.RangeTag;
import vegml.Data.VDataSets;
import vegml.Data.VFileUtil;


/* TreeBank
 * http://surdeanu.cs.arizona.edu//mihai/teaching/ista555-fall13/readings/PennTreebankConstituents.html
 */

/***
 * Generate models from Sedro exported data for
 * - word probability
 * - word/phrase classification
 * - named entity classification
 * - content structure 
 * - word/phrase fit to roles in content structure
 * 
 * V1 Intends to address:
 * 
 * 1. input: pragma (struct) (info-vector)
 * 2. get response (via sedro): pragma info-vector(words, base-struct)
 * 3. API get response structure(s)
 * 	- input: in-struct, in-words, in-pragma, out-pragma, out-words, out-base-struct
 * 	- based on input, and specific to question/command -> then chose best between (mix in Veg, random, or logical)
 * 4. API get words for structure roles
 * 	- input: in-struct, in-words, in-pragma, out-pragma, out-words, out-struct, out-role
 * 5. done for v1
 * 		
 */

public class GenSedroNTGModels {
	
	////////////////////////////////////////////////////
	// Data input
	static boolean resetData = false;	
	
	private static String save_dir = "../../data/";
	private static String aggDsFile = "../models/sedro_ds";
	private static String exportfileName = "../models/beta-en.veg";
	private static String exportSedrofileName = "../models/sedro-resp.veg";
	

	static final String wsj_mrg_directory = "../corpus/treebank_3/treebank_3/parsed/mrg/wsj";
	static final String brown_mrg_directory = "../corpus/treebank_3/treebank_3/parsed/mrg/brown";
	static final String swbd_mrg_directory = "../corpus/treebank_3/treebank_3/parsed/mrg/swbd";
	static final String atis_mrg_directory = "../corpus/treebank_3/treebank_3/parsed/mrg/atis";	
	// merge in mixed models
	static final boolean MERGE_MIXED_ALL = true;
	static final int maxFiles = 1000;
	
	
	
	////////////////////////////////////////////////////
	// 
	// No Try, Just do
	//
	public static void main(String [] args) {
		VegML.showCopywrite();
		
		if (resetData) {
			delSedroDataSets();
		}
		
		// load known data inf complex form
		int aggCnt = getSedroDataSetsFileCount();
			
		
		// aggragate datasets if not done
		if (aggCnt <= 0) aggCnt = aggragateSedroDataFiles(maxFiles);
		else System.out.println("HAVE Aggragate files["+aggCnt+"]");	
			
		// load the first one
		int idx = 0;
		VDataSets sedroDs = getSedroDataSets(idx);
		System.out.println("Loaded dataset1["+getSedroDataSetsFilename(idx)+"]");
		sedroDs.genVSets();
		sedroDs.print();
		
		 
		// sentences
		VDataSets sedroDsS = VFileUtil.dataSetsToSentence(sedroDs);
		sedroDsS.genVSets();
		System.out.println("DATA_SENT[sedro1] LOADED train["+sedroDsS.getTrainCount()+"] tune["+sedroDsS.getTuneCount()+"] test[" + sedroDsS.getTestCount()+"] dataWidth["+ sedroDsS.getDefinition().getTagCount()+"] range["+sedroDsS.getRangeCount()+"]");	
	
				
		 

		//VDataSets dssWsj = VFileUtil.loadDataSetsDS(DataSetType.TreeBanks, wsj_mrg_directory, 25, 25);
		//System.out.println("DATASET[wsj] LOADED train["+dssWsj.getTrainCount()+"] tune["+dssWsj.getTuneCount()+"] test[" + dssWsj.getTestCount()+"] dataWidth["+ dssWsj.getDefinition().getTagCount()+"] range["+dssWsj.getRangeCount()+"]");	
		//dssWsj.print();
		
		/* Vailidate loaded dataset (test code)
		for (int i=0;i<dssWsj.size();i++) {
			VDataSet ds = dssWsj.get(i);
			for (int x=0;x<ds.size();x++) {
				List<RangeTag> rtl = ds.getRange(x);
				if (rtl == null) continue;
				for (RangeTag rt:rtl) {
					String v = null;
					if (rt.getValuesS().length == 2) v = rt.getValuesS()[0];
					System.out.println("  RT["+rt.start+" - "+rt.end+"]["+rt.getValuesS()[0]+"]["+v+"]  " + rt.depth);
				}
			}
			if (i > 4) break;
		}
		
		for (int i=0;i<dssWsj.size();i++) {
			VDataSet ds = dssWsj.get(i);
			//System.out.println("DS["+i+"]["+ds.size()+"]  ");
			//ds.printData();
			if (i > 4) break;
		}
		*/
		
		
		/*
		// load more datasets
		VDataSets dssBrown = VFileUtil.loadDataSetsDS(DataSetType.TreeBanks, brown_mrg_directory, 25, 25);
		System.out.println("DATASET[brown] LOADED train["+dssBrown.getTrainCount()+"] tune["+dssBrown.getTuneCount()+"] test[" + dssBrown.getTestCount()+"] dataWidth["+ dssBrown.getDefinition().getTagCount()+"]");	
		
		VDataSets dssSwbd = VFileUtil.loadDataSetsDS(DataSetType.TreeBanks, swbd_mrg_directory, 25, 25);
		System.out.println("DATASET[swdb] LOADED train["+dssSwbd.getTrainCount()+"] tune["+dssSwbd.getTuneCount()+"] test[" + dssSwbd.getTestCount()+"] dataWidth["+ dssSwbd.getDefinition().getTagCount()+"]");	
		
		//VDataSets dssAtis = VFileUtil.loadDataSetsDS(DataSetType.TreeBanks, atis_mrg_directory, 25, 25, atisdsTl, atisdsTuneTl, atisdsTestTl);
		//System.out.println("DATASET[atis] LOADED train["+dssAtis.getTrainCount()+"] tune["+dssAtis.getTuneCount()+"] test[" + dssAtis.getTestCount()+"] dataWidth["+ dssAtis.getDefinition().getTagCount()+"]");	
		*/
		
		// make into sentences	
		//VDataSets sdssWsj = VFileUtil.dataSetsToSentence(dssWsj);
		//sdssWsj.genVSets();
		//System.out.println("DATA_SENT[wsj] LOADED train["+sdssWsj.size()+"]  dataWidth["+ sdssWsj.getDefinition().getTagCount()+"]");	

		/*
		// unstructured english data repository -> these should go through sedro?
		int fileCount = 50;
		List<List<String>> textTokList = new ArrayList<>();
		int tok = VFileUtil.loadTextFilesToTokens(corpus_text_base_directory, textTokList, fileCount);
		System.out.println("Loadeded dataSets[" + textTokList.size()+"]tokens["+tok+"]");
		*/
		
		///////////////////////////////////
		// make veg
		String fileName = exportfileName;	
		VegML vML = new VegML("making the sauce"); 
		vML.setCfgDescription("Ways to learn and use words");

		///////////////////////////////////
		// word sequence to Next, in sentence
		// learn things, phrases, determiner/sex association, etc
		//  w w w w w <next>
		vML.addDataPlane("wnseq", "word", 5, 0, -1, NumberSetType.SequenceRight);
		vML.setCfgNoEmptyElements("wnseq", "word", true, false);
//		vML.setNoFocus("wnseq", "word", true);

		///////////////////////////////////
		// word sequence to Pre, in sentence
		//  <pre> w w w
		vML.addDataPlane("wpseq", "word", 3, 2, -1, NumberSetType.SequenceLeft);
		vML.setCfgNoEmptyElements("wpseq", "word", true, false);
//		vML.setNoFocus("wpseq", "word", true);
		
		///////////////////////////////////
		// word sequence to between, in sentence
		// find bridges
		//  w w w <between> w w
		vML.addDataPlane("wbseq", "word", 5, -1, NumberSetType.SequenceFan);
		vML.setCfgNoEmptyElements("wbseq", "word", true, false);
		vML.setCfgFrameFocus("wbseq", "word", 2);
		int ff = vML.getCfgFrameFocus("wbseq", "word");
		// x x x x
		// - x x x
		// x x x -
		// - x x -
		vML.setCfgNoEmptyElements("wbseq", "word", true, false);	
		// make the numbersets
		for (int ns = 0;ns< vML.getNSCount("wbseq", "word");ns++) {
			List<Integer> nsSet = vML.getNS("wbseq", "word", ns);
			if (nsSet.size() < 2) { // must have before and after
				vML.setCfgNSTurnedOff("wbseq", "word", ns, true);
				continue;
			}
			boolean b = false, a = false;
			for (Integer p:nsSet) {
				if (p<=ff) b = true; // focus is before
				else if (p>ff) a = true;
			}
			if (!a || !b) { // must have before and after
				vML.setCfgNSTurnedOff("wbseq", "word", ns, true);
				continue;
			}
		}
		vML.removeCfgNSTurnedOff("wbseq", "word");


		///////////////////////////////////
		// word role
		//
		// -> word sense?

		///////////////////////////////////
		// word WITH pos 
		//
		// -> this exists elsewhere (but not via sedro gen data)
		
		
		
		///////////////////////////////////
		// Word/phrase classifier
		//
		// Edge: trained aligned start AND aligned end
	//	vML.addDataPlane("word", "classifier", 16, -1, NumberSetType.SequenceEdge);
		vML.addDataPlane("word", "classifier", 17, -1, NumberSetType.SequenceEdgeId);
		vML.setCfgFrameFocus("word", "classifier", 16);
		vML.setCfgIdentityOnly("word", "classifier", true);
		
		vML.setCfgNoEmptyElements("word", "classifier", true, false);
	//	vML.setCfgNoEmptyElements("word", "classifier", true, true);
		vML.setCfgScratch("word", "classifier", "prefix", 8);
		vML.setCfgScratch("word", "classifier", "suffix", 8);
		
		///////////////////////////////////
		// Word/phrase POS/mixed classifier
		//
		// use token POS and Text to determine classification
		// same + pos first / last
		// TODO
/*
 * FIXME structure
 * - change to use actual structure of binding
 * 	- roles, ordered, puctuation, missed AND the base classifier
 * 	XXX- already have as children the list or elements and roles
 * 		- is this everything?
 * - add alg to resolve these back to structure to build phrases

 */
		
		///////////////////////////////////
		// Structure for words
		//
		// pragma, word, word, word
		//
		vML.addDataPlane("wstruct", "word", 6, -1, NumberSetType.PowerSet);
		vML.setCfgNoEmptyElements("wstruct", "word", true, false);	
		// make the numbersets
		for (int ns = 0;ns< vML.getNSCount("wstruct", "word");ns++) {
			List<Integer> nsSet = vML.getNS("wstruct", "word", ns);
			if (!nsSet.contains(0)) { // must have pragma
				vML.setCfgNSTurnedOff("wstruct", "word", ns, true);
				continue;
			}
			if (nsSet.size() == 1) { // and must have a word
				vML.setCfgNSTurnedOff("wstruct", "word", ns, true);
				continue;
			}
		}
		vML.removeCfgNSTurnedOff("wstruct", "word");

		
		///////////////////////////////////
		// Structure for words and last structure
		//
		// lpragma, lstruct, pragma, word, word, word
		//
		vML.addDataPlane("wsstruct", "word", 6, -1, NumberSetType.PowerSet);
		vML.setCfgNoEmptyElements("wsstruct", "word", true, false);	
		// make the numbersets
		for (int ns = 0;ns< vML.getNSCount("wsstruct", "word");ns++) {
			List<Integer> nsSet = vML.getNS("wsstruct", "word", ns);
			if (!nsSet.contains(0) || !nsSet.contains(1) || !nsSet.contains(2)) { // must have lpragma, lstruct, pragma
				vML.setCfgNSTurnedOff("wsstruct", "word", ns, true);
				continue;
			}
			if (nsSet.size() == 3) { // and must have a word
				vML.setCfgNSTurnedOff("wsstruct", "word", ns, true);
				continue;
			}
		}
		vML.removeCfgNSTurnedOff("wsstruct", "word");
		
		
		///////////////////////////////////
		// Words for Role in structure
		//
		// role, struct, pragma, prime word, pre-word(s), aft-word(s)
		//
		vML.addDataPlane("wrstruct", "word", 6, -1, NumberSetType.PowerSet);
		vML.setCfgNoEmptyElements("wrstruct", "word", true, false);
		
		// make the numbersets
		for (int ns = 0;ns< vML.getNSCount("wrstruct", "word");ns++) {
			List<Integer> nsSet = vML.getNS("wrstruct", "word", ns);
			if (!nsSet.contains(0) || !nsSet.contains(1) || !nsSet.contains(2)) { // must have role / struct
				vML.setCfgNSTurnedOff("wrstruct", "word", ns, true);
				continue;
			}
			if (nsSet.size() == 3) { // and must have a word
				vML.setCfgNSTurnedOff("wrstruct", "word", ns, true);
				continue;
			}
		}
		vML.removeCfgNSTurnedOff("wrstruct", "word");

		///////////////////////////////////
		// save with config
		vML.save(fileName);
		
		///////////////////////////////////
		// set DataSet
		VDataSets trainSet = sedroDsS;
//		VDataSets trainSet = sdssWsj;
		
		// Map all data strings in dataset
		HashMap<Long, String> dsStringMap = new HashMap<>();
		trainSet.mapDataValueIdS(null, dsStringMap);
		System.out.println("DataMap Created["+dsStringMap.keySet().size()+"]");

		HashMap<Long, String> dsRangeValueMap = new HashMap<>();
		// get from ranges for classify
		
		
		////////////////////////////////////////////////
		//
		// Structure of the data and words that fit
		//
		////////////////////////////////////////////////

		// make classify dataset
		VDataSets clsDs = makeClassifyDataSet(sedroDs);
		// FIXME use this dataset to make things
		
		// word struct role
		trainTestWRStruct(vML, sedroDs.getTrainDataSets(), true);
		trainTestWRStruct(vML, sedroDs.getTestDataSets(), false);
				
		// Word structure
		trainTestWordStruct(vML, sedroDs.getTrainDataSets(), true);
		trainTestWordStruct(vML, sedroDs.getTestDataSets(), false);
		vML.save(exportSedrofileName);
		
		// response structure
		trainTestRespStruct(vML, sedroDs.getTrainDataSets(), true);
		trainTestRespStruct(vML, sedroDs.getTestDataSets(), false);
		
		// classifier
		trainTestClassify(vML, sedroDs.getTrainDataSets(), dsRangeValueMap, true);
		trainTestClassify(vML, sedroDs.getTestDataSets(), dsRangeValueMap, false);		
		//   RESULT pass[246927] fail[48815/0] [83.49%]
		
		// word associations (tunes naive)
		trainTestWordSeq(vML, sedroDsS.getTrainDataSets(), dsStringMap, true);
		trainTestWordSeq(vML, sedroDsS.getTestDataSets(), dsStringMap, false);
		
		vML.print();
		
		/////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////
		// TEST
		/////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////

		
		// make frame for wset
		// X x x -> triple -> sentence type / xpos?
		/*
		testWSET(vML, dsStringMap, "to", "the", "store");
		testWSET(vML, dsStringMap, "to", "DC", "we");
		testWSET(vML, dsStringMap, " ", " ", " ");
		testWSET(vML, dsStringMap, " ", "me", " ");
		testWSET(vML, dsStringMap, " ", "Aaron", " ");
		

		testWSRELATE(vML, dsStringMap, " ", "in", "the", "water", " ", "DT", "S");
		testWSRELATE(vML, dsStringMap, " ", "was", "fast", " ", " ", "JJ", "S");

		testWClassify(vML, dsRangeValueMap, "glass", "bottom", "boat", null, null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "first", "impression", null, null, null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "Bob", "and", "Sally", "Smith", null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "big", "ass", "cookie", null, null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "not", "running", null, null, null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "walked", null, null, null, null, null, null, null);
		//default: WCLSY[10]  specific_b (100639)/1.1824012833870683E-8 lx_abstract_name (45046)/5.29242621761483E-9 lx_thing_name (36175)/4.250178005199496E-9
		testWClassify(vML, dsRangeValueMap, "talk", null, null, null, null, null, null, null);
		testWClassify(vML, dsRangeValueMap, "did", "talk", null, null, null, null, null, null);
		//FIXME: better resolve -> FULL match if have all in both prefix and suffix ?? 
		*/
		//vML.print("wrstruct", "word", true);
		//vML.print("word", "classifier", true);
		
		
		///////////////////////////////////
		// save with data
		vML.save(fileName);
	}
	
	// test WSET
	private static void testWSET(VegML vML, HashMap<Long, String> dsStringMap, String s1, String s2, String s3) {
		// make frame for wset
		List<String> frm = new ArrayList<>();
		// X x x -> triple -> sentence type / xpos?
		frm.add(s1);
		frm.add(s2);
		frm.add(s3);

		VFrame frame = VegTest.predictFrameVPFrame(vML, "wset", "word", frm);
		List<ValProb> vpList1 = frame.getVPList();
		int aCnt = frame.getAccumCount();
		//List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "wset", "word", frm);
		if (vpList1 == null) {
			System.out.println("WSET[NOTHING]");			
		} else {
			System.out.println("WSET["+vpList1.get(0).type+"]["+vpList1.size()+"]("+aCnt+") " + dsStringMap.get(vpList1.get(0).value) + " ("+vpList1.get(0).count+") / " +vpList1.get(0).probability );			
		}
	}
	// test WSET
	private static void testWSRELATE(VegML vML, HashMap<Long, String> dsStringMap, 
			String s1, String s2, String s3, String s4, String s5, 
			String pos, String sentType) {
		// FIXME no-empty even on full frame
		
		// make frame for wset
		List<String> frm = new ArrayList<>();
		// X x x -> triple -> sentence type / xpos?
		frm.add(s1);
		frm.add(s2);
		frm.add(s3);
		frm.add(s4);
		frm.add(s5);
		frm.add(pos);
		frm.add(sentType);

		//	VFrame frame = VegTest.predictFrameVPFrame(vML, "wrelate", "word", frm);
		List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "wrelate", "word", frm);
		if (vpList1 == null) {
			System.out.println("WRELATE[NOTHING]");			
		} else {
			System.out.println("WRELATE["+vpList1.get(0).type+"]["+vpList1.size()+"] " + dsStringMap.get(vpList1.get(0).value) + " ("+vpList1.get(0).count+") / " +vpList1.get(0).probability );			
		}
	}
	
	// test WSET
	private static void testWClassify(VegML vML, HashMap<Long, String> dsRangeValueMap, 
			String s1, String s2, String s3, String s4, String s5, 
			String s6, String s7, String s8) {
		List<String> block = new ArrayList<>();
		if (s1 != null) block.add(s1);
		if (s2 != null) block.add(s2);
		if (s3 != null) block.add(s3);
		if (s4 != null) block.add(s4);
		if (s5 != null) block.add(s5);
		if (s6 != null) block.add(s6);
		if (s7 != null) block.add(s7);
		if (s8 != null) block.add(s8);
		
		// make frame for wset
		int clsWs = vML.getCfgWindowSize("word", "classifier");
		int prefix = vML.getCfgFrameFocus("word", "classifier");
		int suffix = clsWs - prefix;
		List<String> frm = new ArrayList<>();

		VFrameUtil.frameBlockEdgeBothS(prefix, suffix, block, frm);

		//	VFrame frame = VegTest.predictFrameVPFrame(vML, "wrelate", "word", frm);
		List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "word", "classifier", frm);
		if (vpList1 == null) {
			System.out.println("WCLSY[NOTHING]");			
		} else {
			System.out.print("WCLSY["+vpList1.size()+"] ");
			for (int n=0;n<3 && n<vpList1.size();n++) {
				System.out.print(" " + dsRangeValueMap.get(vpList1.get(n).value) + " ("+vpList1.get(n).count+")/" +vpList1.get(n).probability );			
			}
			System.out.println("");			
		}
	}
	

	
	/**
	 * Train WR Struct model
	 * 
	 * Get Word for role in structure
	 * 
	 * @param vML
	 * @param trainDs
	 */
	public static void trainTestWRStruct(VegML vML, VDataSets dss, boolean train) {
		///////////////////////////////
		if (train) System.out.println("TRAINING Word/Role Structure wrstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		else System.out.println("TEST Word/Role Structure wrstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		
		Long valueId [] = new Long[1];
		List<String> frm = new ArrayList<>();
		List<Long> vrFrame = new ArrayList<>();
		
		VDataPlane dp = vML.getDataPlane("wrstruct", "word");
		int clsWs = vML.getCfgWindowSize("wrstruct", "word");
		int pass = 0, fail = 0, pass2 = 0;
		Set<String> valSet = new HashSet<>();
		
		for (int i=0;i<dss.size();i++) {
			VDataSet sd = dss.get(i);			
			String pragma = "pragma_p";
			for (int x=0;x<sd.size();x++) {
				List<RangeTag> rtl = sd.getRangeStart(x);
				if (rtl == null) continue;
				
				for (RangeTag rt:rtl) {
					String tag = rt.getValuesS()[0];
					String struct = rt.getValuesS()[1];
					
					if (rt.getValuesS().length > 3 && rt.getValuesS()[3] != null) {
						// FIXME more reason needed
						pragma = rt.getValuesS()[3];
					}
					
					if (isTagStruct(tag, struct)) {
						
						// get children and tags
						List<RangeTag> chList = rt.getLinkedChildren();
						if (chList == null || chList.size() < 2) continue;
						
						boolean haveStruct = false;
						String sw = null, aw = null, ow = null;
						//String structFullId = struct;
						
						//List<String> chTags = rt.getLinkedChildrenTags();
						//System.out.print(" B["+tag+"]["+struct+"] ("+chList.size()+") ");
						List<String> chTags = rt.getLinkedChildrenTags();
						for (int tn=0;tn<chList.size();tn++) {
							RangeTag crt = chList.get(tn);
							String ctag = crt.getValuesS()[0];
							String cstruct = crt.getValuesS()[1];
							if (isTagStruct(ctag, cstruct)) {
								//System.out.print(" C[@"+crt.getLength()+"]["+role+"]");
								haveStruct = true;
								break;
							} else {
								//System.out.print(" [@"+crt.getLength()+"]["+role+"]");
							}
							String role = chTags.get(tn);
							if (role.equals("subject")) sw = crt.getDataAsString();
							if (role.equals("action")) aw = crt.getDataAsString();
							if (role.equals("obj")) ow = crt.getDataAsString();
							
							// check if the position was missing
							// TODO ?? could fill with exact data for cases of punctuation
							
							// differ the structure if this is a composit?
							//structFullId += "."+role;
						}
						if (haveStruct) continue;
						// FIXME iterate children not word?
						// FIXME only lowest level?
						String structFullId = getStructureId(rt);
						
						// process this one
						String lw = " ";
						// select a base word..SAO
						String pw = sw;
						if (pw == null) pw = ow;
						if (pw == null) pw = aw;
						//System.out.println(" B[@"+rt.getStart()+"-"+rt.getEnd()+"]["+tag+"]["+struct+"] ("+chList.size()+") " + structFullId);
						
						//System.out.print(" B["+tag+"]["+struct+"] ("+chList.size()+") ");
						for (int tn=0;tn<chList.size();tn++) {
							RangeTag crt = chList.get(tn);
							String role = chTags.get(tn);
							//String ctag = crt.getValuesS()[0];
							//String cstruct = crt.getValuesS()[1];
							//System.out.print(" [@"+crt.getLength()+"]["+cstruct+"]["+role+"]");
						
							// train this one
							frm.clear();
							frm.add(role);
			//				frm.add(struct);
							// full ID
							frm.add(structFullId);							
							frm.add(pragma);
							
							String w = crt.getDataAsString();
							valueId[0] = (long)dp.getCfgVToV().toVectGen(w);
							valSet.add(w);
							
							String nw = " ";
							if ((tn+1)<chList.size()) nw = chList.get(tn+1).getDataAsString();
							// these sets should change with wsize
							frm.add(pw);						
							// before
							frm.add(lw);
							// after
							frm.add(nw);
									
							if (train) {
								// convert to input (TODO: only long no string)
								vrFrame.clear();
								for (String s:frm) {
									vrFrame.add((long)dp.getCfgVToV().toVectGen(s));
								}	
								// training
								VegTrain.trainDataFrameL(vML, "wrstruct", "word", null, vrFrame, valueId);
							} else {
								//System.out.println("TEST["+VegUtil.toStringListSeg(frm)+"]");
								// testing
								List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "wrstruct", "word", frm);
								if (vpList1 == null) {
									fail++;
								} else if (valueId[0] == vpList1.get(0).value) {							
									pass++;		
								} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
									pass2++;	
									fail++;
								} else {
									fail++;
								}	
							}
							lw = w;
						}						
						//System.out.println("");
					}
				}
			}			
		}
		if (train) {
			vML.addStringMapping("wrstruct", "word", valSet);
		//	VegTune.reduceNaive(vML, "wrstruct", "word", 2, true, true);
			VegTune.reduceWideVectors(vML, "wrstruct", "word", 20, true);
		//	   RESULT pass[138728][10858] fail[160902] [46.30%][49.92%]
		// wide 20 - full ID
		//	   RESULT pass[138090][9803] fail[161540] [46.09%][49.36%
		//	   RESULT pass[138642][9853] fail[163126] [45.94%][49.21%]

			// Naive
		//	   RESULT pass[129752][10995] fail[169878] [43.30%][46.97%]
		} else {
			double perc = (((double)(pass)/(double)(pass+fail))*100);
			double perc2 = (((double)(pass+pass2)/(double)(pass+fail))*100);
			System.out.println("   RESULT pass["+pass+"]["+pass2+"] fail["+fail+"] ["+String.format("%.2f",perc)+"%]["+String.format("%.2f",perc2)+"%]");
		}
	}

	/*
	 * make structure ID
	 */
	static String getStructureId(RangeTag rt) {
		String structFullId = rt.getValuesS()[1]; // classifier
		List<String> chTags = rt.getLinkedChildrenTags();

		//List<String> chTags = rt.getLinkedChildrenTags();
		List<RangeTag> chList = rt.getLinkedChildren();
		for (int tn=0;tn<chList.size();tn++) {
			RangeTag crt = chList.get(tn);
			String ctag = crt.getValuesS()[0];
			String cstruct = crt.getValuesS()[1];
			if (isTagStruct(ctag, cstruct)) {
				// FIXME ?
				return null;
			}
			String role = chTags.get(tn);
			structFullId += "."+role;
		}
		return structFullId;
	}
	
	/**
	 * Train struct
	 * 
	 * Get structure for words
	 * 
	 * @param vML
	 * @param trainDs
	 */
	public static void trainTestWordStruct(VegML vML, VDataSets dss, boolean train) {
		if (train) System.out.println("TRAIN Word Structure wstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		else System.out.println("TEST  Word Structure wstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		
		Long valueId [] = new Long[1];
		List<String> frm = new ArrayList<>();
		List<Long> vrFrame = new ArrayList<>();
		
		int ws = vML.getCfgWindowSize("wstruct", "word");
		VDataPlane dp = vML.getDataPlane("wstruct", "word");
		int pass = 0, fail = 0, pass2 = 0;
		int maxWords = ws-1;
		Set<String> valSet = new HashSet<>();

		for (int i=0;i<dss.size();i++) {
			VDataSet sd = dss.get(i);
			String pragma = "pragma_p";
			for (int x=0;x<sd.size();x++) {
				List<RangeTag> rtl = sd.getRangeStart(x);
				if (rtl == null) continue;
				
				for (RangeTag rt:rtl) {
					if (rt.getValuesS().length > 3 && rt.getValuesS()[3] != null) {
// FIXME more reason needed
						pragma = rt.getValuesS()[3];
					}
					
					if (rt.getValuesS().length < 5) continue;
					String tag = rt.getValuesS()[0];
					String cls = rt.getValuesS()[1];				
					//String structid = rt.getValuesS()[4];
// ISSUE: must use struct ID's ie binding names, currently classifier which is a generealization
					

					
					if (isTagStruct(tag, cls)) {
						// use the class/structure
						// generate new ID type							
						String structId = getStructureId(rt);
						if (structId == null) continue;
						valueId[0] = (long)dp.getCfgVToV().toVectGen(structId);
						valSet.add(structId);

						
						// for each word/role train
						for (int tn=rt.getStart();tn<=rt.getEnd() && tn < sd.size();tn++) {
							//String w = sd.getDataS(tn);

							
							frm.clear();
							frm.add(pragma); // pragma
							// words
							int cnt = 0;
							for (int xx=rt.getStart();xx<=rt.getEnd() && cnt < maxWords;xx++) {
								frm.add(sd.getDataS(xx));
								cnt++;
							}
							while (frm.size()<ws) frm.add(" ");

							// convert to input (TODO: only long no string)
							vrFrame.clear();
							for (String s:frm) {
								vrFrame.add((long)dp.getCfgVToV().toVectGen(s));
							}	
							
							if (train) {
								// training
								VegTrain.trainDataFrameL(vML, "wstruct", "word", null, vrFrame, valueId);
							} else {
								// testing
								List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "wstruct", "word", frm);
								if (vpList1 == null) {
									fail++;
								} else if (valueId[0] == vpList1.get(0).value) {							
									pass++;		
//	System.out.println("Wsstruct_F["+rt.getLength()+"] ["+structid+"]["+cls+"]["+VegUtil.toStringListSeg(frm)+"] => " + rt.getDataAsString());
								} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
									pass2++;	
									fail++;
								} else {
									fail++;
								}	
							}
							
						}
					}
				}
			}			
		}
		if (train) {
			vML.addStringMapping("wstruct", "word", valSet);
			VegTune.reduceNaive(vML, "wstruct", "word", 2, true, true);
		//	VegTune.reduceWideVectors(vML, "wstruct", "word", 50, true);
		//	   RESULT pass[346546][105008] fail[196040] [63.87%][83.22%]
			// wide 50
		//	   RESULT pass[346546][105008] fail[196040] [63.87%][83.22%]
			// Naive
		//	   RESULT pass[336348][102805] fail[199472] [62.77%][81.96%]
		} else {
			double perc = (((double)(pass)/(double)(pass+fail))*100);
			double perc2 = (((double)(pass+pass2)/(double)(pass+fail))*100);
			System.out.println("   RESULT pass["+pass+"]["+pass2+"] fail["+fail+"] ["+String.format("%.2f",perc)+"%]["+String.format("%.2f",perc2)+"%]");
		}
	}
	
	
	/**
	 * Train struct
	 * 
	 * Get structure for response
	 * 
	 * @param vML
	 * @param trainDs
	 */
	public static void trainTestRespStruct(VegML vML, VDataSets dss, boolean train) {
		if (train) System.out.println("TRAIN Response Structure wsstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		else System.out.println("TEST  Response Structure wsstruct["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");
		
		Long valueId [] = new Long[1];
		List<String> frm = new ArrayList<>();
		List<Long> vrFrame = new ArrayList<>();
		
		int ws = vML.getCfgWindowSize("wsstruct", "word");
		VDataPlane dp = vML.getDataPlane("wsstruct", "word");
		int pass = 0, fail = 0, pass2 = 0;
		int maxWords = ws-1;
		
		for (int i=0;i<dss.size();i++) {
			VDataSet sd = dss.get(i);
			String pragma = "pragma_p";
			String lpragma = "pragma_p";
			String lstruct = null;
			RangeTag lastpragma = null;
			RangeTag curpragma = null;
			
			for (int x=0;x<sd.size();x++) {
				List<RangeTag> rtl = sd.getRangeStart(x);
				if (rtl == null) continue;
				
				for (RangeTag rt:rtl) {
					if (rt.getValuesS().length < 5) continue;
					String tag = rt.getValuesS()[0];
					String cls = rt.getValuesS()[1];				
					String structid = rt.getValuesS()[4];
// ISSUE: must use struct ID's ie binding names, currently classifier which is a generealization
				
					if (rt.getValuesS().length > 3 && rt.getValuesS()[3] != null) {
// FIXME more reason needed
						pragma = rt.getValuesS()[3];
						// FIXME pragmas could be stacked
						curpragma = rt;
					}
					
					if (isTagStruct(tag, cls)) {
						// for each word/role train
						for (int tn=rt.getStart();tn<=rt.getEnd() && tn < sd.size();tn++) {
							//String w = sd.getDataS(tn);
							
							// use the class/structure
							valueId[0] = (long)dp.getCfgVToV().toVectGen(cls);			
							
							frm.clear();
							if (lpragma == null) frm.add("pragma_p");
							else frm.add(lpragma); // lpragma
							
							if (lstruct == null) frm.add("none");
							else frm.add(lstruct); // lstruct
							
							if (pragma == null) frm.add("pragma_p");
							else frm.add(pragma); // pragma
							
							// words
							int cnt = 0;
							for (int xx=rt.getStart();xx<=rt.getEnd() && cnt < maxWords;xx++) {
								frm.add(sd.getDataS(xx));
								cnt++;
							}
							while (frm.size()<ws) frm.add(" ");

							// convert to input (TODO: only long no string)
							vrFrame.clear();
							for (String s:frm) {
								vrFrame.add((long)dp.getCfgVToV().toVectGen(s));
							}	
							
							if (train) {
								// training
								VegTrain.trainDataFrameL(vML, "wsstruct", "word", null, vrFrame, valueId);
							} else {
								// testing
								List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "wsstruct", "word", frm);
								if (vpList1 == null) {
									fail++;
								} else if (valueId[0] == vpList1.get(0).value) {							
									pass++;	
								} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
									pass2++;	
									fail++;
								} else {
									fail++;
							//		System.out.println("Wsstruct_F["+rt.getLength()+" / "+cnt+"]["+rt.getStart()+" - "+rt.getEnd()+" / "+maxWords+"] ["+cls+"]["+VegUtil.toStringListSeg(frm)+"] => " + rt.getDataAsString());
								}	
							}
							
						}
						// save struct
						lstruct = cls;
						// identify pragma change
						if (curpragma != null) {
							if (curpragma.getEnd() <= x) {
								lpragma = pragma;
								lastpragma = curpragma;
							}
						}
					}
				}
			}			
		}
		if (train) {
		//	VegTune.reduceNaive(vML, "wsstruct", "word", 2, true, true);
		//	VegTune.reduceWideVectors(vML, "wsstruct", "word", 50, true);
		//	   RESULT pass[467241][47141] fail[75345] [86.11%][94.80%]
			// wide 50
		//	   RESULT pass[467241][47141] fail[75345] [86.11%][94.80%]
			// Naive
		//	   RESULT pass[467132][47156] fail[75454] [86.09%][94.78%]
		} else {
			double perc = (((double)(pass)/(double)(pass+fail))*100);
			double perc2 = (((double)(pass+pass2)/(double)(pass+fail))*100);
			System.out.println("   RESULT pass["+pass+"]["+pass2+"] fail["+fail+"] ["+String.format("%.2f",perc)+"%]["+String.format("%.2f",perc2)+"%]");
		}
	}
	

	/**
	 * make a dataset for classification for ranges
	 * @param dss
	 * @return
	 */
	public static VDataSets makeClassifyDataSet(VDataSets dss) {
		VDataSets clsds = new VDataSets();
		for (int i=0;i<dss.size();i++) {
			VDataSet sd = dss.get(i);			
			for (int x=0;x<sd.size();x++) {
				List<RangeTag> rtl = sd.getRangeStart(x);
				if (rtl == null) continue;			
				for (RangeTag rt:rtl) {
					String tag = rt.getValuesS()[0];
					String cls = rt.getValuesS()[1];
					if (isTagClassify(tag, cls)) {					
						VDataSet ds = new VDataSet(rt.getDataAsStringList(), cls);
						clsds.add(ds);
					}
				}
			}
		}
		clsds.setSplit(dss.getTrainCount(), dss.getTuneCount(), dss.getTestCount());
		clsds.complete();
		clsds.genVSets();
		return clsds;
	}
	
	/**
	 * train classifier model
	 * @param vML
	 * @param trainDs
	 */
	public static void trainTestClassify(VegML vML, VDataSets dss, HashMap<Long, String> dsRangeValueMap, boolean train) {

		if (train) System.out.println("TRAIN Classify["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");	
		else System.out.println("TEST  Classify["+dss.size()+"] Ranges["+dss.getRangeCount()+"]");	
		
		VDataPlane dp = vML.getDataPlane("word", "classifier");
		Long valueId [] = new Long[1];
		
		boolean ident = false;
		if (dp.getNSBaseType() == NumberSetType.SequenceEdgeId) ident = true;	
		int prefix = dp.getCfgScratchInt("prefix");
		int suffix = dp.getCfgScratchInt("suffix");
		
		List<Long> vrFrame = new ArrayList<>();
		List<String> frm = new ArrayList<>();
		
		int pass = 0, fail = 0, failm = 0, unk = 0, unkf = 0, unkf1 = 0, def = 0, deff = 0, pass2 = 0;
		
		
		for (int i=0;i<dss.size();i++) {
			VDataSet sd = dss.get(i);			
			for (int x=0;x<sd.size();x++) {
				List<RangeTag> rtl = sd.getRangeStart(x);
				if (rtl == null) continue;
				
				for (RangeTag rt:rtl) {
					String tag = rt.getValuesS()[0];
					String cls = rt.getValuesS()[1];				
					if (isTagClassify(tag, cls)) {
						// make frame					
						valueId[0] = (long)dp.getCfgVToV().toVectGen(cls);
						// make map of value->valueId
						dsRangeValueMap.put(valueId[0], cls);
						String ids = rt.getDataAsString();
/*
 * BUG: issue with duplicates, will train multiple for same, predict will fail often	
 * 															
 */
						if (train) {
							VFrameUtil.frameBlockEdgeBoth(prefix, suffix, sd, rt, vrFrame);	
							if (ident) {
								// optional identity
								Long idv = (long)dp.getCfgVToV().toVectGen(ids);
								vrFrame.add(idv);
							}
							// training
							VegTrain.trainDataFrameL(vML, "word", "classifier", null, vrFrame, valueId);
						} else {
							// Testing
							frm.clear();
							// make frame for wset
							VFrameUtil.frameBlockEdgeBothS(prefix, suffix, rt.getDataAsStringList(), frm);
							if (ident) frm.add(ids);
							//System.out.println("  TEST["+rt.getLength()+"]: "+VegUtil.toStringListSeg(rt.getDataLS()) + "  =>  "+VegUtil.toStringListSeg(frm));			
				
							//VFrame frame = VegTest.predictFrameVPFrame(vML, "word", "classifier", frm);
							List<ValProb> vpList1 = VegTest.predictFrameVP(vML, "word", "classifier", frm);
							if (vpList1 == null) {
								failm++;
								//System.out.println("WSTRUCT[NOTHING]");	
							} else {
								PredictionType pt = vpList1.get(0).type;
								if (pt == PredictionType.PredictUnknown || pt == PredictionType.Default) unk++;
								if (pt == PredictionType.Default) def++;
								if (valueId[0] == vpList1.get(0).value) {							
									pass++;																
								} else {
									if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) pass2++;						

									fail++;
									//System.out.println("Wclsy_F["+cls+"]["+VegUtil.toStringListSeg(frm)+"]");

									if (pt == PredictionType.Default) deff++;
									if (pt == PredictionType.PredictUnknown || pt == PredictionType.Default) {
										unkf++;
										if (rt.getLength() == 1) unkf1++;
									}

							//		System.out.print("WSTRUCT["+vpList1.size()+"] ");
							//		for (int n=0;n<3 && n<vpList1.size();n++) {
							//			System.out.print(" " + dsRangeValueMap.get(vpList1.get(n).value) + " ("+vpList1.get(n).count+")/" +vpList1.get(n).probability );			
							//		}
							//		System.out.println("");			
								}
							}
						}
					}
				}
			}			
		}
		if (train) {
			// reduce
		//	VegTune.reduceNaive(vML, "word", "classifier", 2, true, true);
		//	VegTune.reduceWideVectors(vML, "word", "classifier", 50, true);
			// no change
			//   RESULT pass[231504] fail[54547/0] [80.93%][85.29%]  unk[48513]f[28002]f1[12727]  def[14055]f[14055]
			// reduce Wide 50
			//   RESULT pass[230819] fail[55232/0] [80.69%][85.04%]  unk[48513]f[28003]f1[12727]  def[14055]f[14055]
			// naive reduction
			//   RESULT pass[229740] fail[56311/0] [80.31%][85.02%]  unk[48513]f[28360]f1[12791]  def[14495]f[14495]

		} else {
			double perc = (((double)(pass)/(double)(pass+fail+failm))*100);
			double perc2 = (((double)(pass+pass2)/(double)(pass+fail))*100);
			System.out.println("   RESULT pass["+pass+"] fail["+fail+"/"+failm+"] ["+String.format("%.2f",perc)+"%]["+String.format("%.2f",perc2)+"%]"
					+ "  unk["+unk+"]f["+unkf+"]f1["+unkf1+"]"
					+ "  def["+def+"]f["+deff+"]");
		}
	}

	
	/**
	 * 
	 * @param vML
	 * @param dss
	 * @param dsRangeValueMap
	 * @param train
	 */
	public static void trainTestWordSeq(VegML vML, VDataSets dss, HashMap<Long, String> dsValueMap, boolean train) {

		List<Long> bFrame = new ArrayList<>();
		List<Long> pFrame = new ArrayList<>();
		List<Long> nFrame = new ArrayList<>();
		Long [] valueId = new Long[1];
		List<Long> words = new ArrayList<>();
		List<String> wordstr = new ArrayList<>();
	
		if (train) {
			System.out.println("TRAIN w-seq Sentences["+dss.size()+"] Ranges["+dss.getRangeCount()+"] ["+dss.getRangeCount()+"]");
		} else {
			System.out.println("TEST  w-seq Sentences["+dss.size()+"] Ranges["+dss.getRangeCount()+"] ["+dss.getRangeCount()+"]");	
		}
		
		int bfocus = vML.getCfgFrameFocus("wbseq", "word");
		int bws = vML.getCfgWindowSize("wbseq", "word");
		int nws = vML.getCfgWindowSize("wnseq", "word");
		int pws = vML.getCfgWindowSize("wpseq", "word");
		int pCnt = 0, bCnt1 = 0, bCnt2 = 0, nCnt = 0;
		int bfail = 0, bpass = 0, bpass2 = 0, btot = 0;
		int nfail = 0, npass = 0, npass2 = 0, ntot = 0;
		int pfail = 0, ppass = 0, ppass2 = 0, ptot = 0;

		for (int i=0;i<dss.size();i++) {
			// sentence here
			VDataSet sent = dss.get(i);
					
		//	System.out.println("Sentence["+i+"] ["+etok+"] -> ["+stype+"] " +sent.size());
			words.clear();
			for (int x=0;x<sent.size();x++) {
				//if (sent.size() < 2) continue;
				
				String [] tag = sent.getValueSD(x);
			//	if (!isWord(tag)) continue; // ?? should ignore?
				// no train punctuation?
				
				Long [] vtag = sent.getValueVD(x);				
				Long vtok = sent.getDataV(x);
				String dat = sent.getDataS(x);
			//	words.add(vtok);
			//	wordstr.add(dat);
				
				//valueId[0] = 1L;
				valueId[0] = vtok;

				// make the sets and train
				pFrame.clear();
				nFrame.clear();
				bFrame.clear();
				pCnt = bCnt1 = bCnt2 = nCnt = 0;
				
				// Previous Frame add after				
				for (int j=0;j<pws;j++) {
					int p = (x+j)+1;
					if (p < sent.size()) {
						String [] pw = sent.getValueSD(p);
						pFrame.add(sent.getDataV(p));
						if (isWord(pw)) pCnt++;
					} else {
						pFrame.add((long)VegML.emptyVect);
					}
				}

				// Next Frame add after
				for (int j=(nws-1);j>=0;j--) {
					int p = (x-j)-1;
					if (p >= 0 && p < sent.size()) {
						String [] pw = sent.getValueSD(p);
						nFrame.add(sent.getDataV(p));
						if (isWord(pw)) nCnt++;
					} else {
						nFrame.add((long)VegML.emptyVect);
					}
				}

				// Between Frame add after
				for (int j=bfocus;j>=0;j--) {
					int p = (x-j)-1;
					if (p >= 0 && p < sent.size()) {
						String [] pw = sent.getValueSD(p);
						bFrame.add(sent.getDataV(p));
						if (isWord(pw)) bCnt1++;
					} else {
						bFrame.add((long)VegML.emptyVect);
					}
				}

				for (int j=0;j<((bws-bfocus)-1);j++) {
					int p = (x+j)+1;
					if (p < sent.size()) {
						String [] pw = sent.getValueSD(p);
						bFrame.add(sent.getDataV(p));
						if (isWord(pw)) bCnt2++;
					} else {
						bFrame.add((long)VegML.emptyVect);
					}
				}

				if (train) {
// BUG most sentences size = 1, seems bug is spliting sentences when making sentences
					
					//System.out.println("wn["+sent.size()+"/"+x+"]["+dat+"]["+nCnt+"] <= ["+VegUtil.toStringListSeg(dsValueMap, nFrame)+"]");
					//System.out.println(" p["+sent.size()+"/"+x+"]["+dat+"]["+pCnt+"] <= ["+VegUtil.toStringListSeg(dsValueMap, pFrame)+"]");
					//System.out.println(" b["+sent.size()+"/"+x+"]["+dat+"]["+bCnt1+"] <= ["+VegUtil.toStringListSeg(dsValueMap, bFrame)+"]");
					
					if (nCnt > 0) {
						VegTrain.trainDataFrameL(vML, "wnseq", "word", null, nFrame, valueId);
					}
					if (pCnt > 0) {
						VegTrain.trainDataFrameL(vML, "wpseq", "word", null, pFrame, valueId);
					}
					if (bCnt2 > 0 && bCnt1 > 0) {
						VegTrain.trainDataFrameL(vML, "wbseq", "word", null, bFrame, valueId);
					}
				} else {
					// TEST
					if (nCnt > 0) {
						List<ValProb> vpList1 = VegTest.predictFrameVPV(vML, "wnseq", "word", nFrame);
						if (vpList1 == null) {
							nfail++;
						} else if (valueId[0] == vpList1.get(0).value) {							
							npass++;	
						} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
							npass2++;	
							nfail++;
						} else {
							nfail++;
						}	
						ntot++;
					}
			//System.out.println("nw["+sent.size()+"]["+dat+"] <= ["+VegUtil.toStringListSeg(dsValueMap, nFrame)+"]");

					if (pCnt > 0) {
						List<ValProb> vpList1 = VegTest.predictFrameVPV(vML, "wpseq", "word", pFrame);
						if (vpList1 == null) {
							pfail++;
						} else if (valueId[0] == vpList1.get(0).value) {							
							ppass++;	
						} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
							ppass2++;	
							pfail++;
						} else {
							pfail++;
					//		System.out.println("Wsstruct_F["+rt.getLength()+" / "+cnt+"]["+rt.getStart()+" - "+rt.getEnd()+" / "+maxWords+"] ["+cls+"]["+VegUtil.toStringListSeg(frm)+"] => " + rt.getDataAsString());
						}	
						ptot++;
					}
					if (bCnt2 > 0 && bCnt1 > 0) {
						List<ValProb> vpList1 = VegTest.predictFrameVPV(vML, "wbseq", "word", bFrame);
						if (vpList1 == null) {
							bfail++;
						} else if (valueId[0] == vpList1.get(0).value) {							
							bpass++;	
						} else if (vpList1.size() > 1 && valueId[0] == vpList1.get(1).value) {							
							bpass2++;	
							bfail++;
						} else {
							bfail++;
					//		System.out.println("Wsstruct_F["+rt.getLength()+" / "+cnt+"]["+rt.getStart()+" - "+rt.getEnd()+" / "+maxWords+"] ["+cls+"]["+VegUtil.toStringListSeg(frm)+"] => " + rt.getDataAsString());
						}	
						btot++;
					}	
				}
			}
		}	
		
		// naive reduction
		if (train) {
			//VegTune.reduceNaive(vML, "wnseq", "word", 2, false, false);
			//VegTune.reduceNaive(vML, "wpseq", "word", 2, false, false);
			//VegTune.reduceNaive(vML, "wbseq", "word", 2, false, false);
			int mxNum = 100;
			VegTune.reduceWideVectors(vML, "wnseq", "word", mxNum, true);
			VegTune.reduceWideVectors(vML, "wpseq", "word", mxNum, true);
			VegTune.reduceWideVectors(vML, "wbseq", "word", mxNum, true);

			//vML.print("wnseq", "word", true);
			//vML.print("wpseq", "word", true);
		} else {
			double pperc = (((double)(ppass)/(double)(ppass+pfail))*100);
			double pperc2 = (((double)(ppass+ppass2)/(double)(ppass+pfail))*100);
			System.out.println("   RESULT p["+ptot+"] pass["+ppass+"]["+ppass2+"] fail["+pfail+"] ["+String.format("%.2f",pperc)+"%]["+String.format("%.2f",pperc2)+"%]");

			double nperc = (((double)(npass)/(double)(npass+nfail))*100);
			double nperc2 = (((double)(npass+npass2)/(double)(npass+nfail))*100);
			System.out.println("   RESULT n["+ntot+"] pass["+npass+"]["+npass2+"] fail["+nfail+"] ["+String.format("%.2f",nperc)+"%]["+String.format("%.2f",nperc2)+"%]");

			double bperc = (((double)(bpass)/(double)(bpass+bfail))*100);
			double bperc2 = (((double)(bpass+bpass2)/(double)(bpass+bfail))*100);
			System.out.println("   RESULT b["+btot+"] pass["+npass+"]["+bpass2+"] fail["+bfail+"] ["+String.format("%.2f",bperc)+"%]["+String.format("%.2f",bperc2)+"%]");
/*
   -- naive and with prune width: 100
   RESULT p[350319] pass[98219][19221] fail[252100] [28.04%][33.52%]
   RESULT n[349678] pass[104068][21444] fail[245610] [29.76%][35.89%]
   RESULT b[329800] pass[104068][15495] fail[233717] [29.13%][33.83%]   
   -- with prune width: 100
   RESULT p[350319] pass[110656][16933] fail[239663] [31.59%][36.42%]
   RESULT n[349678] pass[118245][17797] fail[231433] [33.82%][38.90%]
   RESULT b[329800] pass[118245][11625] fail[213885] [35.15%][38.67%]
   -- with prune width: 20
   RESULT p[350319] pass[110651][16885] fail[239668] [31.59%][36.41%]
   RESULT n[349678] pass[118244][17740] fail[231434] [33.82%][38.89%]
   RESULT b[329800] pass[118244][11624] fail[213889] [35.15%][38.67%]
 */
		}

	}
	
	//
	// check if this is a tag to train Classify
	//
	private static boolean isTagClassify(String tag, String cls) {
		if (tag == null) return false;
		// FIXME change these on export 
		//if (cls.equals("lx_bmember_seg")) return false;
		//if (cls.equals("lx_blist_seg")) return false;
		//if (cls.equals("lx_member_seg")) return false;
		if (cls.equals("couple_b")) return false;
		if (cls.equals("lx_entity_seg")) return false;
		
		// thing but not classify
		if (cls.equals("lx_ref")) return false;

		// nouns / things
		if (tag.equals("NP")) return true;
		if (tag.equals("NN")) return true;
		if (tag.equals("NNS")) return true;
		if (tag.equals("NNP")) return true;
		if (tag.equals("NNPS")) return true;
		
		// verbs / action
		if (tag.startsWith("VP")) return true;
		if (tag.equals("ACTP")) return true;
		return false;
	}
	
	//
	// check if this is a tag to train structure
	//
	private static boolean isTagStruct(String tag, String cls) {
		if (isTagClassify(tag, cls)) return false;
		if (cls.equals("couple_b")) return false;
		if (cls.startsWith("lx_")) return false;
		// TODO: need better
		if (cls.endsWith("_lo"))return false; // structure
		
		if (cls.equals("object_b")) return false;
		if (cls.equals("subject_b")) return false;
		if (cls.equals("sent_action_b")) return false;
		if (cls.equals("cmd_b")) return false;
		if (cls.equals("number_b")) return false;
		if (cls.equals("inquiry_b")) return false;
		
		if (cls.startsWith("cq_")) return false;//quest-detail
		
		//clause_sub_b
		if (cls.startsWith("clause_")) return false; // ?? is possible?
		if (cls.equals("paas_b")) return false; // ?? is possible?
		
		return true;
	}
	
	
	//
	// check if this is a tag to train Classify
	//
	private static boolean isTagStructure(String tag) {
		if (tag == null) return false;
		if (tag.startsWith("S")) return true;
		//if (tag.equals("VP")) return true;
		return false;
	}
	private static RangeTag getRTLastChild(VDataSet ds, List<RangeTag> clist, int position, int countBefore) {
		if (clist == null) return null;	
		for (int i=0;i<clist.size();i++) {
			RangeTag r = clist.get(i);
			if (r.getStart() >= position) return null; // if past -> null
			if (r.getEnd() == (position -1)) {
				// how many before?
				if (countBefore == 1) return r;
				int off = (i-countBefore)+1;
				if (off < 0) return null;
				return clist.get(off);

			}								
		}
		return null;
	}
	private static RangeTag getRTNextChild(VDataSet ds, List<RangeTag> clist, int position, int countAfter) {
		if (clist == null) return null;
		for (int i=0;i<clist.size();i++) {
			RangeTag r = clist.get(i);
			if (r.getStart() == (position + 1)) {
				// how many after?
				if (countAfter == 1) return r;
				int off = (i+countAfter)-1;
				if (off >= clist.size()) return null;
				return clist.get(off);
			}								
		}
		return null;
	}

	// see if this is a word
	static boolean isWord(String [] vtag) {
		if (vtag[0].length() == 1) return false;
		if (vtag[0].equals("CD")) return false;
		if (vtag[0].equals("''") || vtag[0].equals("``")) return false;
		if (vtag[0].startsWith("-")) return false;
		return true;
	}
	

	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// Dataset Aggragation
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * get the sedro dataset file name
	 * @param off
	 * @return
	 */
	static String getSedroDataSetsFilename(int off) {
		if (off > 0) return aggDsFile+off+".json";
		return aggDsFile+".json";
	}
	
	/**
	 * get count of sedro dataset aggragate files
	 * @return count of files
	 */
	static int getSedroDataSetsFileCount() {
		for (int i=0;;i++) {
			if (!VFileUtil.fileExists(getSedroDataSetsFilename(i))) return i;
		}
	}	
	
	/**
	 * get count of sedro dataset aggragate files
	 * @return count of files
	 */
	static int delSedroDataSets() {
		for (int i=0;;i++) {
			if (!VFileUtil.fileExists(getSedroDataSetsFilename(i))) return i;
			VFileUtil.delFile(getSedroDataSetsFilename(i));
		}
	}
	
	/**
	 * get aggragate sedro datasets
	 */
	static VDataSets getSedroDataSets(int off) {
		return VDataSets.importJSON(getSedroDataSetsFilename(off));
	}
	
	/**
	 * merge all the ds files into a single VDataSets
	 * 
	 * @param maxFiles max datasets in a dataset
	 * @return number of aggragate files created
	 */
	static int aggragateSedroDataFiles(int maxFiles) {
		System.out.print("START aggragateSedroDataFiles["+save_dir+"]");	
		
		List<String> fl = VFileUtil.fileList(save_dir);
		if (fl == null) return 0;
			
		Iterator<String> it = fl.iterator();
		while (it.hasNext()) {
			String fn = it.next();
			if (!fn.endsWith(".json")) it.remove();
		}
		
		System.out.print(" files["+fl.size()+"]");		
		if (maxFiles > 0) System.out.print("max["+maxFiles+"]");		
		System.out.println(" Processing...");
	
		// allow for multiple DSS files with max of maxFiles datasets in each
		int fni = 0, dsn = 0, fcnt = 0;
		for (;fni<fl.size();dsn++) {
			String jsonfileName = getSedroDataSetsFilename(dsn);
			VFileUtil.delFile(jsonfileName);

			Set<String> fnset = new HashSet<>();
			VDataSets dss = null;	
			for (;fni<fl.size();fni++) {
				String fn = fl.get(fni);
				if (dss != null && dss.find(fn) != null) continue; // have this one already
				
				String ffn = save_dir+fn;
				fnset.add(ffn);
				VDataSets dssm = VDataSets.importJSON(ffn);
				if (dssm == null) {
					System.out.println("ERROR: aggragateSedroDataFiles file["+ffn+"]");
					continue;
				}
				if (dssm.get(0).getName() == null) {
					dssm.get(0).setName(fn);
				}
				if (dss == null) {
					dss = dssm;
				} else {
					dss.append(dssm);
				}
				if (maxFiles > 0 && dss.size() == maxFiles) break;
			}
			// save it
			if (dss != null && fnset.size() > 0) {
				dss.setSplitPercent(50, 25, 25);
				dss.exportJSON(jsonfileName);
				System.out.println(" DONE["+dsn+"] load["+fnset.size()+"]current["+dss.size()+"] >> " + jsonfileName);	
				fcnt++;
			} else if (dss != null) {
				System.out.println(" DONE["+dsn+"] load[NONE]current["+dss.size()+"]");			
			} else {
				System.out.println(" DONE["+dsn+"] load[NONE]current[NONE]");			
			}
			dss = null;
			System.gc();
		}
		return fcnt;
	}
}

