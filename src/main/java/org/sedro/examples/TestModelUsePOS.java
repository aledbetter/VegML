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

import vegml.VFrame;
import vegml.VegML;
import vegml.ValProb;
import vegml.VegTest;
import vegml.VegUtil;

/**
 * this example is to show loading and using a model to make predictions
 * as well as converting to a solid model and exporting a JSON
 * - load model
 * - predict POS for words in strings
 * - make solid model and save it
 * - save as JSON
 */
public class TestModelUsePOS {
	
	/**
	 * use a model generated from GenPosViaText
	 * any model will work
	 */
	static String fileName = "../models/text-id-w-rn-rdef-5w.veg";
	
	////////////////////////////////////////////////////
	// MAIN
	public static void main(String [] args) {
		VegML.showCopywrite();

		VegML vML = VegML.load(fileName);
		if (vML == null) {
			System.err.println("MODEL["+fileName+"] does not exist; generate via GenPosViaText first");
			return;
		}
		
		// show the model info
		vML.print();	// vML.print(true); // to display all numberSets
		
		// get the default dataplane / sub-model
		String dTag = vML.getCfgDefaultDTag();
		String dpTag = vML.getCfgDefaultDPTag();
		
		
		/////////////////////
		// use dataplane's framer for this		
		List<String> wl = makeWordList("find out what this part of speach is");
		List<Long> frm = vML.frameDataS(dTag, dpTag, wl, 3); // 4th word 'this'

		
		/////////////////////
		// predict the part-of-speech for some word		
		List<ValProb> vpList = VegTest.predictFrameVPV(vML, dTag, dpTag, frm);
		if (vpList != null && vpList.size() > 0) {
			String pos = vML.getStringMapping(dTag, dpTag, vpList.get(0).value);
			System.out.println("RESULTS["+vpList.size()+"] best: " + pos);
		} else {
			System.out.println("NO RESULT for: " + VegUtil.toStringListSeg(vML.getDataPlane(dTag, dpTag), frm));
		}

		// predict the part-of-speech and get some tracing information 
		VFrame vFrame = VegTest.predictFrameVPVFrame(vML, dTag, dpTag, frm);
		vFrame.print(true);
		
		
		/////////////////////				
		// convert to a solid model
		vML.makeSolid();
		// save to a file; you can see the disk space difference now
		// to see the memory size difference change fileName to the solid version and re-run this app
		vML.save(fileName+"-solid");
		
		
		/////////////////////
		// export to filename-base.json
		String jfile = fileName.substring(0, fileName.length()-4)+".json";
		vML.exportJSON(jfile);	
		System.out.println("exported as JSON: " + jfile);
	}
	
	/**
	 * parse string to list of words for framing
	 * @param words words to be parsed
	 * @return list of words
	 */
	static List<String> makeWordList(String words) {
		String [] xf = words.split("\\s+");
		List<String> wordList = new ArrayList<>();
		for (int i=0;i<xf.length;i++) wordList.add(xf[i]);
		return wordList;
	}
	
}
