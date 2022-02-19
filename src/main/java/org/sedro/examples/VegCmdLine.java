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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vegml.VegRules;
import vegml.VegRules.VRuleFormatter;
import vegml.VContext;
import vegml.VegML;
import vegml.VegTune;
import vegml.Data.VFileUtil;
import vegml.VegML.ProbMethod;

/*
 * FEATUES TODO
 * 
 * 0) finish FIXME's
 * 1) segmented training
 * 2) next / prior etc results and language generation from seed/nothing for value/nothing
 * 3) multi dimension / dataplan usage
 */
// make direct exacutable
//
//https://medium.com/mpercept-academy/how-to-make-a-executable-file-from-your-java-code-3f521938ae5c
//
/*
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd new=tst1 window=5 print
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd new=tst1 window=5,3 print save=test.vml
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print 
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=others save=test.vml
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=values,4 save=test.vml

java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print save=test.vml full-frames-always=false save-vect-sets=false save-dimension-strings=false show-progress=false
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print save-symbolic=eval-sym.txt save-vect-sets=true save-dimension-strings=true

## train directory text
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=others train=../corpus/reviews/neg train-value=negative train-data-mode=directory-text
## train directory text dirtag
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=others train=../corpus/reviews/pos train-data-mode=directory-text-dirtags

## correctness
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=others train-correctness=../corpus/reviews/neg train-value=negative train-data-mode=directory-text
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd load=test.vml print dimension=text add-dataplane=others train-correctness=../corpus/reviews/pos train-data-mode=directory-text-dirtags

## use the script
java -cp sedroNLP-1.0.001-jar-with-dependencies.jar org.sedro.sinml.vegCmd script=../vegtest.veg


TRAIN/TEST PARTIAL
	- trains all the data... set limit count or %
	- test set range?


 */
public class VegCmdLine {

	///////////////////////////////////////////////
	//
	// entry point
	//
	public static void main(String [] args) {
		todoSet wit = new todoSet();
    	VegML vML = null;
		
    	// parse args
    	String scriptFile = parseArgs(wit, args);
    	
    	// if script do that
    	if (scriptFile != null) {
			System.out.println("##VegML-SCRIPT START: " + scriptFile);
			// load file
			String line = null;
			int lineNumber = 1;
	        try (BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {      	
	            while ((line = br.readLine()) != null) {
	            	if (line.length() < 4) continue;
	            	char ch = line.charAt(0);
	            	if (ch == '#') continue; // comment
	            	
	            	// parse the args
	            	String largs [] = line.split("\\s+");
	            	wit = new todoSet();
	            	if (parseArgs(wit, largs) != null) continue;
		    		System.err.println("##line-"+lineNumber+": " + line);
		    		lineNumber++;
		    		
	            	// do the work
					vML = doTheWork(vML, wit);
	            }

	        } catch (IOException e) {
	    		System.err.println("ERROR >> script file["+scriptFile+"] " + e.getMessage());
	    		System.exit(1);
	        }
			System.out.println("##VegML-SCRIPT END: " + scriptFile);	
    	} else {
    		// just do it once
    		vML= doTheWork(vML, wit);
    	}
	}
	
	
	///////////////////////////////////////////////
	// command information set
	static class todoSet {
		List<String> loadFiles = null;
		String saveFile = null;
		
		String saveSymbolicFile = null;
		int maxDepth = -1;
		
		// config
		String vegTag = null, dimensionTag = null, dataPlaneTag = null, vegDesc = null;
		List<String> dimensionTags = null;

		// training: text, TSV, CSV
		List<String> trainingFiles = null;
		int trainSegments = -1;
		int trainLimit = -1;
		boolean trainLimitPercent = false;

		String trainValue = null;
		String trainTestdataMode = null;
		String trainSplitString = "_";
		int trainDimensionIndex = -1;	// column to get dimension data
		int trainValueIndex = -1;		// column to get value data
		// (transformations future maybe)
		
		// function List: view / get / list
		// ? load strings into dimension/dataPlane
		// ? export strings/justdata/vectSets
		List<String> functList = null;
		
		// modifications: smash, smash-reducer, reducer, optimize, reduce-blanced
		List<String> modList = null;		
		
		// testing -> output OR test print
		List<String> testFiles = null;
		// test text: 1) next/prior word/vlaue 2) test if match eval-value
		String testText = null;
		String testValue = null;
		String traceText = null;
		int testDimensionIndex = -1;	// column to get dimension data
		int testValueIndex = -1;		// column to get value data
		ProbMethod testProbMethod = ProbMethod.Default;
		String testSplitString = "_";
		
		String testEvalType = null;
		boolean saveResults = false;

		boolean close = false;

		boolean print = false;
		boolean printLong = false;
		boolean printStrings = false;
		boolean printVectSets = false;
		
		int window = 3, valueFocus = -999, dataWidth = -1;
		int saveVectSets = -1, saveDimensionStrings = -1, showProgress = -1;
		boolean addDp = false;
				
	}
	
	///////////////////////////////////////////////
	// parse args into wit -> if script return it
	static String parseArgs(todoSet wit, String [] args) {

		// parse the arguments
    	if (args != null && args.length > 0) {    		
        	// test/testadd/rss/
    		for (String a:args) {
    			String [] ap = a.split("=");
    			
        		/////////////////////////
    			// Saveing and Loading
    			if (a.startsWith("script=")) {
    				return ap[1];  // that is all..
    			} else if (a.startsWith("load=")) {
    				wit.loadFiles = addToList(wit.loadFiles, ap[1]);
    			} else if (a.startsWith("save=")) {
    				wit.saveFile = ap[1]; 
    			} else if (a.startsWith("save-symbolic=")) {
    				wit.saveSymbolicFile = ap[1]; 
    				
    				
        		/////////////////////////
        		// config
    			} else if (a.startsWith("new=")) {
    				wit.vegTag = ap[1];   				
    			} else if (a.startsWith("window=")) {
    				// window,before OR just window
 // FIXME move window to dataPlane   				
    				String sp [] = ap[1].split(",");
    				wit.window = Integer.parseInt(sp[0]);
    				if (wit.window < 1) wit.window = 1;
    				if (sp.length == 2) {
    					wit.valueFocus = Integer.parseInt(sp[1]);
    				}
    			} else if (a.startsWith("description=")) {		
    				wit.vegDesc = ap[1];   // use quotes
    			} else if (a.startsWith("add-dataplane=")) {
    				// <dataplane>,<Datawidth>
    				String sp [] = ap[1].split(",");
    				wit.dataPlaneTag = sp[0];
    				wit.addDp = true;
    				if (sp.length == 2) wit.dataWidth = Integer.parseInt(sp[1]);
    			} else if (a.startsWith("save-vect-sets=")) {
    				wit.saveVectSets = getBoolean(ap[1]);
    			} else if (a.startsWith("save-dimension-strings=")) {
    				wit.saveDimensionStrings = getBoolean(ap[1]);
    			} else if (a.startsWith("show-progress=")) {
    				wit.showProgress = getBoolean(ap[1]);
    			} else if (a.startsWith("dataplane=")) {
    				wit.dataPlaneTag = ap[1];
    			} else if (a.startsWith("dimension=")) {
    				wit.dimensionTags = addToList(wit.dimensionTags, ap[1]);
    				wit.dimensionTag = wit.dimensionTags.get(0);
    				
    			/////////////////////////
    			// training
    			} else if (a.startsWith("train=")) {
    				wit.trainingFiles = addToList(wit.trainingFiles, ap[1]);				
    			} else if (a.startsWith("train-value=")) {
    				wit.trainValue = ap[1]; 
    			} else if (a.startsWith("train-dimension-index=")) {
    				wit.trainDimensionIndex = Integer.parseInt(ap[1]);   
    			} else if (a.startsWith("train-value-index=")) {
    				wit.trainValueIndex = Integer.parseInt(ap[1]);    
    			} else if (a.startsWith("train-split-string=")) {
    				wit.trainSplitString = ap[1];   
    			} else if (a.startsWith("train-limit=")) {
    				if (ap[1].endsWith("%")) {
    					wit.trainLimit = Integer.parseInt(ap[1].substring(0, ap[1].length()-1));
    					wit.trainLimitPercent = true;
    				} else {
    					wit.trainLimit = Integer.parseInt(ap[1]);
    					wit.trainLimitPercent = false;
    				}
    			} else if (a.startsWith("train-segments=")) {
    				wit.trainSegments = Integer.parseInt(ap[1]);
    				if (wit.trainSegments < 1) wit.trainSegments = 1;
    				if (wit.trainSegments > 10000) wit.trainSegments = 10000;
    			} else if (a.startsWith("eval-data-mode=") || a.startsWith("train-data-mode=")) {
    				wit.trainTestdataMode = ap[1];         			    	
    				
    				
        		/////////////////////////
    			// testing
    			} else if (a.startsWith("eval=")) {
    				wit.testFiles = addToList(wit.trainingFiles, ap[1]);
    			} else if (a.startsWith("eval-text=")) {
    				wit.testText = ap[1]; 
    			} else if (a.startsWith("eval-value=")) {
    				wit.testValue = ap[1]; 
    			} else if (a.startsWith("eval-dimension-index=")) {
    				wit.testDimensionIndex = Integer.parseInt(ap[1]);    
    			} else if (a.startsWith("eval-value-index=")) {
    				wit.testValueIndex = Integer.parseInt(ap[1]);  
    			} else if (a.startsWith("eval-split-string=")) {
    				wit.testSplitString = ap[1];   
       			} else if (a.startsWith("eval-eval-type=")) {
    				wit.testEvalType = ap[1];   
    			} else if (a.startsWith("trace-text=")) {
    				wit.traceText = ap[1];   
    			} else if (a.startsWith("save-results=")) {
    				wit.saveResults = (getBoolean(ap[1]) == 1)?true:false;   				
   
    				
            	/////////////////////////
        		// model mods and such
    			} else if (a.startsWith("mods=")) {
    				wit.modList = addToList(wit.modList, ap[1].toLowerCase());
    			} else if (a.startsWith("function=")) {
    				wit.functList = addToList(wit.functList, ap[1].toLowerCase());
    				
    				
            	/////////////////////////
    			// utility / functional
    			} else if (a.startsWith("getstring=")) {
    				// string for dimension or dataplane and vector
    // FIXME
    			} else if (a.equals("print")) {
    				wit.print = true;
    			} else if (a.equals("print-long")) {
    				wit.printLong = true;
    			} else if (a.equals("print-strings")) {
    				wit.printStrings = true;
    			} else if (a.equals("print-vectsets")) {
    				wit.printVectSets = true;
    				
    			} else if (a.startsWith("getnext=")) {
    				// get next vapProbs
    // FIXME
    			} else if (a.startsWith("removeInfo=")) {
    // FIXME
    				
    			} else if (a.startsWith("removeInfoFile=")) {
    // FIXME

    			} else if (a.equals("close")) {
    				wit.close = true;
    			} else if (a.equals("help") || a.equals("--help") || a.equals("-?")) {
    				help();
    			}
    		}
    	} else {
    		help();
    	}
		return null;
	}
	

	///////////////////////////////////////////////
	// do the work for a command set
	static VegML doTheWork(VegML vML, todoSet wit) {
		
		// first thing, close if listed
		if (wit.close) {
			vML = null;
			return null;
		}
		
    	////////////////////////////////////////
    	// create or load
    	if (wit.loadFiles != null) {
    		// first is base, additional are merged in
    		for (int i=0;i<wit.loadFiles.size();i++) {
    			VegML vMLin = VegML.load(wit.loadFiles.get(i));
    			if (vMLin == null) {
    				System.err.println("ERROR >> File not found or loaded: " + wit.loadFiles.get(i));
    				System.exit(1);
    			}
    			if (i == 0) vML = vMLin;
    			else {
    				if (!vML.merge(vMLin)) {
        				System.err.println("ERROR >> merge faild["+vML.getTag()+"] <= [" + wit.loadFiles.get(i)+"]");
        				System.exit(1);    					
    				}
    			}
    		}
        	//vML.setCfgProbMethodDefault(wit.testProbMethod);
    	} else if (wit.vegTag != null) {
    		// create
    		vML = new VegML(wit.vegTag);
        	if (wit.saveSymbolicFile != null) {
        		wit.saveDimensionStrings = wit.saveVectSets = 1; // these must be turned on
        	}
        	if (wit.valueFocus > -999) vML.setCfgFrameFocus(wit.dimensionTag, wit.dataPlaneTag, wit.valueFocus);
        	//vML.setCfgProbMethodDefault(wit.testProbMethod);	
    	}
    	
    	//vML.setNoFocus(false);
    	
		/////////////////////////////////////
    	// configuration
    	if (wit.saveVectSets >= 0) vML.setCfgSaveVectSets((wit.saveVectSets==0)?false:true);
    	if (wit.showProgress >= 0) vML.setCfgShowProgress((wit.showProgress==0)?false:true);
		if (wit.addDp) {
			if (wit.dimensionTag == null) {
				System.err.println("ERROR >> missing dimension for dataplane["+wit.dataPlaneTag+"]");
				System.exit(1);    					
			}
	    	vML.addDataPlane(wit.dimensionTag, wit.dataPlaneTag, wit.window, wit.dataWidth);	
		}

	
		/////////////////////////////////////
		// TRAINING
    	if (wit.trainingFiles != null) {
    		// first is base, additional are merged in
    		for (int i=0;i<wit.trainingFiles.size();i++) {
    			trainFileOrDir(vML, wit, wit.trainingFiles.get(i), wit.trainTestdataMode, wit.trainValue);
    		}
    	}

		/////////////////////////////////////
		// MODS
    	if (wit.modList != null) {
    		for (int i=0;i<wit.modList.size();i++) {
    			if (wit.modList.get(i).equals("smash")) {
    				vML.smash();
    			} else if (wit.modList.get(i).equals("optimize")) {
    				vML.optimize();
    			} else if (wit.modList.get(i).equals("entangle")) {
    				vML.entangle(wit.dimensionTag, wit.dataPlaneTag);
    			} else if (wit.modList.get(i).equals("deentangle")) {
    				vML.deentanglement(wit.dimensionTag, wit.dataPlaneTag);
    			} else if (wit.modList.get(i).equals("reduce-balanced")) {
    				VegTune.reduceBalanced(vML, wit.dimensionTag, wit.dataPlaneTag, false);
    			} else if (wit.modList.get(i).equals("reduce-redundant")) {
    				VegTune.reduceRedundant(vML, wit.dimensionTag, wit.dataPlaneTag, true);
    			} else if (wit.modList.get(i).equals("reduce-nonunique")) {
    				VegTune.reduceNonUnique(vML, wit.dimensionTag, wit.dataPlaneTag);    			
    			} else if (wit.modList.get(i).equals("reduce-logical")) {
    				VegTune.reduceNaive(vML, wit.dimensionTag, wit.dataPlaneTag);
    			}
    		}
    	}
    	
    	
		/////////////////////////////////////
		// TEST / PREDICT / work
    	if (wit.testFiles != null) {
    		for (int i=0;i<wit.testFiles.size();i++) {
    			testFileOrDir(vML, wit, wit.testFiles.get(i), wit.trainTestdataMode, wit.testValue, wit.testEvalType);
    		}
    	} else if (wit.testText != null) {
    		//wit.testValue
    		// TODO
    	} else if (wit.traceText != null) {
    		// TODO
    	}
    	
 
		/////////////////////////////////////
		// FUNCTIONS and alterations
    	if (wit.functList != null) {
    		for (int i=0;i<wit.functList.size();i++) {
    			if (wit.modList.get(i).equals("clear-vect-sets")) {
    				vML.clearVectSets();
    			} else if (wit.modList.get(i).equals("clear-dimension-strings")) {
    				vML.clearDimensionStrings();
    			}
    		}
    	}
    	
		/////////////////////////////////////
    	// save model
    	if (wit.saveFile != null) {
			vML.save(wit.saveFile);
    	}   
    	
		/////////////////////////////////////
    	// save symbolic rules
    	if (wit.saveSymbolicFile != null) {
    	
    		if (!vML.isCfgSaveVectSets()) {
				System.err.println("ERROR >> unable to generate Symbolic rules MUST have: save-dimension-strings AND save-vect-sets ON");
   			
    		} else {
	    		// rule formatter
	    		VRuleFormatter rfmt = new VRuleFormatter();
	    		List<String> rules = null;
	    		if (wit.dimensionTag != null && wit.dataPlaneTag != null) {
	    			// generate rules
	    			rules = VegRules.generateAll(vML, wit.dimensionTag, wit.dataPlaneTag, 0, wit.maxDepth, null, rfmt, null, true);
	    		} else if (wit.dimensionTag != null) {
	    			// For all dataplanes
	    			List<String> dpl = vML.getDataPlaneTagList(wit.dimensionTag);
	    			if (dpl == null) {
	    				System.err.println("ERROR >> dimension or dataplane not found");	    				
	    			} else {
	    				
	    				// FIXME
	    			}
	    		} else if (wit.dataPlaneTag != null) {
    				System.err.println("ERROR >> dimension not found");	    				
	    		} else {
	    			// for all dimensnsions and dataPlanes
	    			List<String> dl = vML.getDataPlaneDTagList();
	    			if (dl == null) {
	    				System.err.println("ERROR >> dimension not found");	    					    				
	    			} else {
	    				
	    			}
	    			// FIXME
	    		}
	    			
	    		if (rules != null) {
	    			System.out.println("##Veg generated Rules[" + rules.size()+"] => ["+wit.saveSymbolicFile+"]");
		    	    VFileUtil.writeListToFile(rules, wit.saveSymbolicFile, false);
	    		} else {
	    			System.out.println("##Veg generated Rules[NONE]");
	    		}
    		}
    	}
    		
    	if (wit.printLong) {
    		vML.print(true);
    	}
        if (wit.print) {
    		vML.print();
    	}
        if (wit.printStrings) {
        	vML.printVectorStrings();      
    	}
        if (wit.printVectSets) {
        	vML.printVectorSets();      
    	}  	
		return vML;
	}
	

	///////////////////////////////////////////////
	// training 
	static int trainFileOrDir(VegML vML, todoSet wit, String filename, String mode, String trainValue) {
		int cnt = 0;
		if (wit.dimensionTag == null || wit.dataPlaneTag == null) {
			System.err.println("ERROR >> must set dimension= and dataplan= to train");
			return 0;
		}
			
		if (wit.trainSegments > 1) {
// FIXME for all of these need to setup segments... 
		}
		
		if (mode != null && mode.startsWith("directory-text")) {
			System.out.println("##VegML-TRAINING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] directory-text");
			
			// last directory name is the tag, files are read as text
			List<String> fileSet =  VFileUtil.loadTextFiles(filename);
			
			// limit data size if one is set
			if (wit.trainLimit > 0) {
				int limit = wit.trainLimit;
				if (wit.trainLimitPercent) limit = (int)((double)fileSet.size() * (double)((double)wit.trainLimit/100));
				while (limit > 0 && fileSet.size() > limit) fileSet.remove(fileSet.size()-1);
			}
			
			// tokenize
			List<List<String>> trainDataSet = new ArrayList<>();
			for (String s:fileSet) {
				List<String> tks = VFileUtil.tokenizeString(s, false, true);
				trainDataSet.add(tks);
			}
			fileSet = null;
			
			if (mode.equals("directory-text-dirtags") || trainValue == null) {
				// directory is the tag
				trainValue = filename.substring(filename.lastIndexOf("/")+1, filename.length());
			}
			/*
			if (trainValue != null) {
				// train to value
//				VegTrain.trainSegments(vML, wit.dimensionTag, wit.dataPlaneTag, trainDataSet, trainValue);	
			} else if (mode != null && mode.equals("directory-text-self")) {
				// ) self (token to self) for patterns like POS, etc
				VegTrain.trainDataSetsS(vML, wit.dimensionTag, wit.dataPlaneTag, trainDataSet, trainDataSet);	
				
			} else if (mode != null && mode.equals("directory-text-split")) {
				// 1) token to tag(s): in one file need split
				for (int x=0;x<trainDataSet.size();x++) {
					List<List<String>> spl = VFileUtil.tokenizeString(trainDataSet.get(x), wit.trainSplitString);
					List<List<String>> ds = new ArrayList<>();
					ds.add(spl.get(0));
					List<List<String>> dsv = new ArrayList<>();
					dsv.add(spl.get(1));
					VegTrain.trainDataSetsS(vML, wit.dimensionTag, wit.dataPlaneTag, ds, dsv);	
				}
			}
			*/
			return cnt;
		} else if (mode != null && mode.equals("directory-tables-tags")) {
			System.out.println("##VegML-TRAINING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] directory-tables");
			// last directory name is the tag, files are tables with rows
			trainValue = filename.substring(filename.lastIndexOf("/")+1, filename.length());
			//List<String> fileSet =  FileUtil.loadTextFiles(filename);	
			// FIXME
			return cnt;
		}
		
		//////////////
		// single File: type decided by extension: .csv, .tsv, *
		String fex = VFileUtil.getFileExtension(filename);
		if (fex.equals("txt")) {
			System.out.println("##VegML-TRAINING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] text");
			String fileText = VFileUtil.loadTextFile(filename);
			// tokenize
			List<String> trainData = VFileUtil.tokenizeString(fileText, false, true);
			if (trainValue != null) {
			//	VegTrain.trainDataSet(vML, wit.dimensionTag, wit.dataPlaneTag, trainData, trainValue);	
			/*
			} else if (mode != null && mode.equals("self")) {
				// ) self (token to self) for patterns like POS, etc
				List<List<String>> ds = new ArrayList<>();
				ds.add(trainData);
				List<List<String>> dsv = new ArrayList<>();
				dsv.add(trainData);
				VegTrain.trainDataSetsS(vML, wit.dimensionTag, wit.dataPlaneTag, ds, dsv);
			} else if (mode != null && mode.equals("split")) {
				// 1) token to tag(s): in one file need split
				List<List<String>> spl = VFileUtil.tokenizeString(trainData, wit.trainSplitString);
				List<List<String>> ds = new ArrayList<>();
				ds.add(spl.get(0));
				List<List<String>> dsv = new ArrayList<>();
				dsv.add(spl.get(1));
				VegTrain.trainDataSetsS(vML, wit.dimensionTag, wit.dataPlaneTag, ds, dsv);
				*/
			}
			/*
		} else if (fex.equals("tsv") || fex.equals("csv")) {
			if (wit.trainDimensionIndex < 0 || (trainValue == null && wit.trainValueIndex < 0)) {
				System.err.println("ERROR >> must have eval-dimension-index AND train-value or train-value-index to train file[" + filename+"]");
				return 0;				
			}
			System.out.println("##VegML-TRAINING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] table["+ fex + "] colum["+wit.trainDimensionIndex+"] to colum["+wit.trainValueIndex+"]");
			
			List<String[]> inTable = null;
			if (fex.equals("tsv")) inTable = VFileUtil.loadTsvFile(filename);
			else inTable = VFileUtil.loadCsvFile(filename);
			if (inTable == null) {
				System.err.println("ERROR >> FILE not loaded file[" + filename+"]");
				return 0;
			}
			
			List<List<String>> dataTable = new ArrayList<>();
			List<String> valueList = new ArrayList<>();
			for (String [] strs:inTable) {	
				if (strs == null || strs[wit.trainDimensionIndex] == null) continue;
				List<String> tks = VFileUtil.tokenizeString(strs[wit.trainDimensionIndex], false, false);
				dataTable.add(tks);	
				if (wit.trainValueIndex > 0) valueList.add(strs[wit.trainValueIndex]);			
			}
			System.out.println("##VegML-TRAINING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] table["+ fex + "] colum["+wit.trainDimensionIndex+"] to colum["+wit.trainValueIndex+"] ["+dataTable.size()+"]");
			inTable = null;
	
			if (valueList.size() > 0) {
				// one value per data item
//				VegTrain.trainDataTable(vML, wit.dimensionTag, wit.dataPlaneTag, dataTable, valueList);
			} else {
				// one value for all data items
//				VegTrain.trainDataTable(vML, wit.dimensionTag, wit.dataPlaneTag, dataTable, trainValue);
			}
				*/
		} else {
			System.err.println("ERROR >> training file["+filename+"] unknown extension["+fex+"]");
			return 0;
		}
		
		return cnt;
	}
	
	
	///////////////////////////////////////////////
	// testing - loading is mostly the same as for training
	static double testFileOrDir(VegML vML, todoSet wit, String filename, String mode, String testValue, String evalType) {
		if (wit.dimensionTag == null || wit.dataPlaneTag == null) {
			System.err.println("ERROR >> must set dimension= and dataplan= to test or resolve");
			return 0;
		}

		double total = 0, miss = 0;
		if (mode != null && mode.startsWith("directory-text")) {
			// last directory name is the tag, files are read as text
			List<String> fileSet =  VFileUtil.loadTextFiles(filename);
			
			// tokenize
			List<List<String>> testDataSet = new ArrayList<>();
			for (String s:fileSet) {
				List<String> tks = VFileUtil.tokenizeString(s, false, true);
				testDataSet.add(tks);
			}
//EVAL type			
			if (mode.equals("directory-text-dirtags") || testValue == null) {
				// directory is the tag
				testValue = filename.substring(filename.lastIndexOf("/")+1, filename.length());
			}
			VContext ctx = new VContext(vML);

			if (testValue != null) {
				System.out.println("##VegML-TESTING"+wit.dimensionTag+"/"+wit.dataPlaneTag+"]("+testDataSet.size()+") value: " + testValue);

				// test to value
				for (int i=0;i<testDataSet.size();i++) {
					/*
					String xx = VegTest.predictSegment(ctx, wit.dimensionTag, wit.dataPlaneTag, testDataSet.get(i));
					if (xx == null || !xx.equals(testValue)) {
						miss++;
						// RECORD this??
					}
					*/
					total++;
				}
			} else if (mode != null && mode.equals("directory-text-self")) {
				System.out.println("##VegML-TESTING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"]("+testDataSet.size()+") self");
			// ) self (token to self) for patterns like POS, etc
				for (int x=0;x<testDataSet.size();x++) {
					List<Long> valueOut = new ArrayList<>();
					for (int i=0;i<testDataSet.get(x).size();i++) {
						/*
						String xx = VegTest.predict(ctx, wit.dimensionTag, wit.dataPlaneTag, valueOut, testDataSet.get(x), i, x);
						if (xx == null || !xx.equals(testDataSet.get(x).get(i))) {
							miss++;
							// RECORD this??
						}
				//		valueOut.add(xx);
						total++;
						*/
					}
					
				}

			} else if (mode != null && mode.equals("directory-text-split")) { 
				// 1) token to tag(s): in one file need split
				System.out.println("##VegML-TESTING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"]("+testDataSet.size()+") split");
				for (int x=0;x<testDataSet.size();x++) {
					List<List<String>> spl = VFileUtil.tokenizeString(testDataSet.get(x), wit.testSplitString);
					List<Long> valueOut = new ArrayList<>();
					for (int i=0;i<testDataSet.get(x).size();i++) {
						/*
					
						String xx = VegTest.predict(ctx, wit.dimensionTag, wit.dataPlaneTag, valueOut, spl.get(0), i, x);
						if (xx == null || !xx.equals(spl.get(1).get(i))) {
							miss++;
							// RECORD this??
						}
					//	valueOut.add(xx);
						total++;
						*/
					}
				}
			}
			double perPass = (double)100 - (miss/ total)*(double)100;
			String val = "";
			if (testValue != null) val = " value["+testValue+"]";
	        System.out.println("TEST pass[" + String.format("%.2f", perPass) +"%] [miss "+((int)miss)+" of "+((int)total)+"] for [" + filename+"]x "+val);
			return total;
			
		} else if (mode != null && mode.equals("directory-tables-tags")) {
			// last directory name is the tag, files are tables with rows
			testValue = filename.substring(filename.lastIndexOf("/")+1, filename.length());
			//List<String> fileSet =  FileUtil.loadTextFiles(filename);	
			// FIXME
			System.out.println("##VegML-TESTING["+wit.dimensionTag+"/"+wit.dataPlaneTag+"] tables TODO");
			double perPass = (double)100 - (miss/ total)*(double)100;
	        System.out.println("TEST pass[" + String.format("%.2f", perPass) +"%] [miss "+((int)miss)+" of "+((int)total)+"] for [" + filename+"]");
			return total;
		}
		
		String fex = VFileUtil.getFileExtension(filename);
		if (fex.equals("txt")) {
			String fileText = VFileUtil.loadTextFile(filename);
			// tokenize
			List<String> testData = VFileUtil.tokenizeString(fileText, false, true);
			
			List<String> results = null;
			if (wit.saveResults) results = new ArrayList<>();
			VContext ctx = new VContext(vML);

			if (testValue != null || (evalType != null && evalType.equals("segment"))) {
				/*
				String xx = VegTest.predictSegment(ctx, wit.dimensionTag, wit.dataPlaneTag, testData);
				if (evalType == null && (xx == null || !xx.equals(testValue))) miss++;				
				if (results != null) results.add(xx);
				*/
				total++;
			} else if (evalType != null || (mode != null && mode.equals("self"))) {
				// ) self (token to self) for patterns like POS, etc
				List<Long> valueOut = new ArrayList<>();
				for (int i=0;i<testData.size();i++) {
					/*
					String xx = VegTest.predict(ctx, wit.dimensionTag, wit.dataPlaneTag, valueOut, testData, i, 0);
					if (evalType == null && (xx == null || !xx.equals(testData.get(i)))) miss++;
					if (results != null) results.add(xx);
			//		valueOut.add(xx);
					total++;
					*/
				}
			} else if (mode != null && mode.equals("split")) {
				// 1) token to tag(s): in one file need split
				List<List<String>> spl = VFileUtil.tokenizeString(testData, wit.testSplitString);
				List<Long> valueOut = new ArrayList<>();
				for (int i=0;i<testData.size();i++) {
					/*
					String xx = VegTest.predict(ctx, wit.dimensionTag, wit.dataPlaneTag, valueOut, spl.get(0), i, 0);
					if (xx == null || !xx.equals(spl.get(1).get(i))) miss++;					
					if (results != null) results.add(xx);
			//		valueOut.add(xx);
					total++;
					*/
				}
			}
			
			// save results
			if (wit.saveResults) VFileUtil.writeListToFile(results, filename+"-out.txt", true);
			/*
		} else if (fex.equals("tsv") || fex.equals("csv")) {
			if (wit.testDimensionIndex < 0 || (testValue == null && wit.testValueIndex < 0)) {
				System.err.println("ERROR >> must have eval-dimension-index AND compare-value or eval-value-index to train file[" + filename+"]");
				return 0;				
			}
			List<String[]> inTable = null;
			if (fex.equals("tsv")) inTable = VFileUtil.loadTsvFile(filename);
			else inTable = VFileUtil.loadCsvFile(filename);
			if (inTable == null) {
				System.err.println("ERROR >> FILE not loaded file[" + filename+"]");
				return 0;
			}
			
			List<List<String>> dataTable = new ArrayList<>();
			List<String> valueList = new ArrayList<>();
			for (String [] strs:inTable) {	
				if (strs == null || strs[wit.testDimensionIndex] == null) continue;
				List<String> tks = VFileUtil.tokenizeString(strs[wit.testDimensionIndex], false, false);
				dataTable.add(tks);	
				if (wit.testValueIndex > 0) valueList.add(strs[wit.testValueIndex]);			
			}
			System.out.println("##VegML-DATA-TEST["+dataTable.size()+"]");
			inTable = null;
			
			List<String[]> resultV = null;
			if (wit.saveResults) resultV = new ArrayList<>();
			VContext ctx = new VContext(vML);

			// one value per data item
			for (int i=0;i<dataTable.size();i++) {
			
				String xx = VegTest.predictSegment(ctx, wit.dimensionTag, wit.dataPlaneTag, dataTable.get(i));
				boolean pass = true;
				if (evalType == null) {
					if (valueList.size() > 0) {
						if (xx == null || !xx.equals(valueList.get(i))) pass = false;
					} else if (xx == null || !xx.equals(testValue)) pass = false;
					if (!pass) miss++;
					if (resultV != null) {
						String rs [] = new String[3];
						rs[0] = ""+i;
						rs[1] = xx;
						rs[2] = ""+pass;
						resultV.add(rs);
					}
				} else {
					if (resultV != null) {
						String rs [] = new String[2];
						rs[0] = ""+i;
						rs[1] = xx;
						resultV.add(rs);
					}					
				}
				
				total++;

			}
			// save results
			if (wit.saveResults) {
				if (fex.equals("csv")) VFileUtil.writeListToTable(resultV, filename+"-out.csv", false);
				else VFileUtil.writeListToTable(resultV, filename+"-out.tsv", true);
			}
			*/
		} else {
			System.err.println("ERROR >> testing file["+filename+"] unknown extension["+fex+"]");
			return 0;
		}

		if (evalType == null) {
			String val = "";
			if (testValue != null) val = " value["+testValue+"]";
			double perPass = (double)100 - (miss/ total)*(double)100;
			System.out.println("TEST pass[" + String.format("%.2f", perPass) +"%] [miss "+((int)miss)+" of "+((int)total)+"] for [" + filename+"]"+val);
		}
		return total;
	}
	

	/*
	 * FUTURE
	 *  1) udpate and link in AutoVegML
	 *  2) multi-dimensional aligment
	 *  3) element export partial model OR symbolic model
	 *  
	 */
	
	// one at a time or comma list
	private static List<String> addToList(List<String> sl, String str) {
		String [] list = str.split(",");
		if (list.length < 1) return sl;
		if (sl == null) sl = new ArrayList<>();
		for (String s:list) sl.add(s);
		return sl;
	}
	private static int getBoolean(String str) {
		if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("yes") || str.equalsIgnoreCase("on")) return 1;
		return 0;
	}
	
	static void help() {
		System.out.println("VegML Command line help\n"
				+ " \n"
				+ "  script=<filename>		\n"
				+ "  		script file to run commands from\n"
				+ "  	\n"
				+ "  new=<name>		\n"
				+ "  		Create a new model retain the name inside the model\n"
				+ "  \n"
				+ "  window=<size>(,<value-focus-offset>)	\n"
				+ " 		size 1-12 larger window retains more data and is much larger\n"
				+ "  		<value-focus-offset> optionally the position of the data relative to the window-frame-offset to train to\n"
				+ "  			default is the window-frame-offset	\n"
				+ "  \n"
				+ "  description=<description>\n"
				+ "  		description of the model in words, length not limited but less is always more\n"
				+ "  										 \n"
				+ "  load=<filename(s)>\n"
				+ "  		model file to load, may be multiple with ','s or multiple params\n"
				+ "  		the first is the base model, all others will be merged into it, creating a larger model\n"
				+ "  			load=center.vml\n"
				+ "  			load=center.vml,left.vml,right.vml\n"
				+ "  			load=center.vml load=left.vml load=right.vml\n"
				+ "  \n"
				+ "  dimension=<tag>\n"
				+ "  		specify the dimension to use in the model\n"
				+ "   		multiple dimensions may be set, order matters, for use in testing. Only one dimension can be trained at a time\n"
				+ "  \n"
				+ "  dataplane=<tag>\n"
				+ "  		specify the dataplane to use in the model, make sure it has been added to the model\n"
				+ "  \n"
				+ "  add-dataplane=<name>(,<data-width>)\n"
				+ "		Add a dataplane is added if it does not exist in the dimension, dimension= must be set\n"
				+ "		dimension will be created if it does not exist.\n"
				+ "		<data-width> optionally sets the 'width' of the data value options if not a strict limit do not set\n"
				+ "			boolean data values -> 2\n"
				+ "			set of data value: red, blue, green -> 3\n"
				+ "			set is all possible integers -> do not set\n"
				+ "    		\n"
				+ "  save-vect-sets=\n"
				+ "  		Save all VectSets trained. default is true \n"
				+ "  		This is required for some extended features to work, but takes extra memory\n"
				+ "  		\n"
				+ "  save-dimension-strings=\n"
				+ "   		Save all strings trained in the dimension. default is true \n"
				+ "   		This is required for some extended features to work, but takes extra memory\n"
				+ "   		\n"
				+ "  show-progress=\n"
				+ "  		show training progress. default is true \n"
				+ "\n"
				+ "  full-frames-always=\n"
				+ "   		Train full frames of data at edges. default is true \n"
				+ " \n"
				+ "  save=<save-filename>		\n"
				+ "  		filename to save the model to, not saved if not listed\n"
				+ "  		\n"
				+ "  save-symbolic=<filename>	\n"
				+ "  		filename to save symbolic rules to, not saved if not listed\n"
				+ "  		the model must have been trained with vectSets and Dimension strings\n"
				+ "  		  save-vect-sets=true AND save-dimension-strings=true\n"
				+ "  		If you don't know use print to see if these are in the model\n"
				+ "  \n"
				+ "  train=<filename(s)>	\n"
				+ "  		training file to load, may be multiple with ','s or multiple params\n"
				+ "  \n"
				+ "  train-correctness=\n"
				+ "    	set exactly the same as training files, use all the same params as well\n"
				+ "    	this should be done for all trained data, after training is complete\n"
				+ "  \n"
				+ "  train-value=\n"
				+ "  		single value to train data to, common for classification of documents\n"
				+ "  		\n"
				+ "  train-dimension-index=\n"
				+ "    	index/column of feature data in the csv/tsv file, start with 0\n"
				+ "  \n"
				+ "  train-value-index=\n"
				+ "      	index/column of value data in the csv/tsv file, start with 0\n"
				+ "  \n"
				+ "  train-split-string=\n"
				+ "  		if train-data-mode=split set the string to split tokens with. default is '_'\n"
				+ "  		\n"
				+ "  train-segments=<number>\n"
				+ "  		Segmented training - TODO: incomplete in cmdline\n"
				+ "  \n"
				+ "  train-limit=<number/%>\n"
				+ "  		Used to limit the number of files trained to hard number or percentage\n"
				+ "  		This makes setting 70% of data for training very easy: train-limit=70%\n"
				+ "  		\n"
				+ "  train-data-mode= \n"
				+ "  eval-data-mode=\n"
				+ "  		The type of training/eval file and value arrangement to use\n"
				+ "  		*default* 				- file: uses file extension .txt/.tsv/.csv\n"
				+ "  		self 					- file: train against dimension 1-to-1 -> use for patterns\n"
				+ "  		split 					- file: split tokens with value in train-split-string= \n"
				+ "  								  token_value -> token for dimension, value to train to\n"
				+ "  		directory-text 			- each file to value train-value= -> use for classification\n"
				+ "   		directory-text-dirtags	- each file to directory name as value -> use for classification\n"
				+ "  		directory-text-split 	- same as split, for all files in directory\n"
				+ "  		directory-text-self 	- same as self, for all files in directory\n"
				+ "  \n"
				+ "  eval=<filename(s)>	\n"
				+ "  		Eval file to load, may be multiple with ','s or multiple params\n"
				+ " 	\n"
				+ "  eval-text=\n"
				+ "  		Direct text to eval against\n"
				+ "  		\n"
				+ "  eval-value=\n"
				+ "  		Value to eval predicted value against\n"
				+ "  		 \n"
				+ "  eval-split-string=\n"
				+ "  		if eval-data-mode=split set the string to split tokens with. default is '_'\n"
				+ "\n"
				+ "  eval-dimension-index=\n"
				+ "  		index/column of feature data in the csv/tsv file, start with 0\n"
				+ "  		\n"
				+ "  eval-value-index=\n"
				+ "    	index/column of value data in the csv/tsv file, start with 0\n"
				+ "  \n"
				+ "  eval-eval-type=\n"
				+ "  		For non-testing results, when there is no test value\n"
				+ "  		segment - to eval data as a segment: block of text, classification, etc\n"
				+ "  		series  - to eval data as a series to iterate, token by token prediction/validation\n"
				+ "  \n"
				+ "  save-results=\n"
				+ "  		True to save the results for testing. results files will be one-to-one with test files\n"
				+ " \n"
				+ "  trace-text=\n"
				+ "  		TODO\n"
				+ "  		\n"
				+ "  mods=<mod>\n"
				+ "	  	smash		- entangle and optimize -> should always do this after training complete; decreases memory use\n"
				+ "		optimize	- optimize for performance and memory\n"
				+ "		entangle	- entangle accumulators based on probabilities\n"
				+ "		deentangle	- reverse entanglement\n"
				+ "		reduce-balanced		- remove balanced accumulators (with a probability of .5 for all values in the set)\n"
				+ "		reduce-redundant	- remove redundant accumulators, where a smaller subset produces the same result.\n"
				+ "							  this will change results slightly, is done before symbolic rules are generated\n"
				+ "		reduce-nonunique	- remove all accumulators that have more than 1 value, adds significan't bias to results\n"
				+ "	\n"
				+ "  function=<function>\n"
				+ "		clear-vect-sets 		- clear all the saved vectSets\n"
				+ "    	clear-dimension-strings - clear all the saved Dimension strings\n"
				+ "    \n"
				+ "  getstring=\n"
				+ "  		TODO\n"
				+ "  		\n"
				+ "  getnext=\n"
				+ "  		TODO Text /dimensional generation\n"
				+ "  		\n"
				+ "  getlast=\n"
				+ "  		TODO Text /dimensional generation  \n"
				+ "  \n"
				+ "  removeInfo=\n"
				+ "  		TODO\n"
				+ "  \n"
				+ "  close\n"
				+ "  		Script only: closes the current model, run line alone\n"
				+ "  \n"
				+ "  print\n"
				+ "  		Show the VegML model information\n"
				+ "  		\n"
				+ "  print-long\n"
				+ "  		Show more detailed VegML model information\n"
				+ "  		\n"
				+ "  print-strings\n"
				+ "  		Show the Dimension and DataPlane String sets\n"
				+ "  		\n"
				+ "  print-vectsets\n"
				+ "  		Show the vectSet break down\n"
				+ "  \n"
				+ " General use:\n"
				+ " 	1) CREATE a model and configure it\n"
				+ " 		add dimension and dataplane to use. set dataplane dataWidth if known\n"
				+ " 		\n"
				+ " 	2) TRAIN data using one of the modes into the dimension and dataplane\n"
				+ " 	\n"
				+ " 	3) TRAIN-CORRECTNESS using the same data and methods\n"
				+ " 		if this is skipped then only probability is used in decisions\n"
				+ " 		correctness results are best if trained for correctness with full set, after complete. \n"
				+ " 		incremental works, but will produce biased results\n"
				+ " 		\n"
				+ " 	4) SAVE: smash it first to make it optimal\n"
				+ " 	\n"
				+ " 	5) EVAL: use model to get results, run tests, or generate content\n"
				+ " 	\n"
				+ " 	6) MODIFY: the model with reductions, additional dimensions / dataplans, or training to get desired results\n"
				+ " 	\n"
				+ " \n"
				+ " 			\n"
				+ " Examples:\n"
				+ " \n"
				+ "	Create model train it and save it\n"
				+ "		veg new=tagged-random train=tagged.txt save=data.vml\n"
				+ "   	\n"
				+ "	Create larger model train it for dimension and dataplane, smash it to save space then save it\n"
				+ "		veg new=tagged-random window=5 train=tagged.txt dimension=text add-dataplane=others mod=smash save=data-5.vml\n"
				+ "   \n"
				+ "	Load model and test a data set, and save the results\n"
				+ "		veg load=data.vml dimension=text dataplane=others eval=random.txt save-results=true\n"
				+ "   \n"
				+ "	Print info\n"
				+ "		veg load=test.vml print-long\n"
				+ "    \n"
				+ "	Use a script (see vegtest.veg for example)\n"
				+ "		veg script=../vegtest.veg \n"
				+ "    \n"
				+ "  \n"
				+ "	Add new dataplans and dimensions\n"
				+ "		veg load=test.vml print dimension=text add-dataplane=others save=test.vml\n"
				+ "		veg load=test.vml print dimension=moretext add-dataplane=values,4 save=test.vml\n"
				+ "	\n"
				+ "	Save symbolic rules (in our not-so-real-rule-format)\n"
				+ "		veg load=test.vml print save-symbolic=eval-sym.txt save-vect-sets=true save-dimension-strings=true\n"
				+ "	\n"
				+ "	Train  directory text\n"
				+ "		veg load=test.vml print dimension=text add-dataplane=others train=../corpus/reviews/neg train-value=negative train-data-mode=directory-text\n"
				+ "	Train directory text dirtag\n"
				+ "		veg load=test.vml print dimension=text add-dataplane=others train=../corpus/reviews/pos train-data-mode=directory-text-dirtags\n"
				+ "	\n"
				+ "    Train correctness for the data\n"
				+ "		veg load=test.vml print dimension=text add-dataplane=others train-correctness=../corpus/reviews/neg train-value=negative train-data-mode=directory-text\n"
				+ "		veg load=test.vml print dimension=text add-dataplane=others train-correctness=../corpus/reviews/pos train-data-mode=directory-text-dirtags\n"
				+ ""
				);

		

		
	}
	
}
