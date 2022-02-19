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

import VContext from './vcontext.js';
import VFrame from './vframe.js';
import VResultSet from './vresultset.js';
import VDataSets from './vdatasets.js';
import VAccum from './vaccum.js';
import VegCallOut from './vegcallout.js';
import VegFramer from './vegframer.js';
import VegML, {VegEmpty} from './vegml.js';
import * as VegVtoV from './vectortovid.js';
import ValProb from './valprob.js';


//
// dataplane 
//
var DEFAULT_PROB_WEIGHT = 0.2;
export default  class VDataPlane {
    dtag = "-";
	dptag = "-";
  	window = 0;
  	before = 0;	
	strMapId = -1;
	noEmpty = true;
	noEmptyExcept = true;
	probMethod = "Average";
	framer = null;
	scratchPad = null;
	dac = null;
	
	// numberSets
	nsh = new Array();
	numSets = new Array();
	numSetTiers = new Array();
	nsWeights = null;
	nsTypes = null;
	nsFull = -1;
	nsIdentity = -1;
	identityOnly = true;
	nsWeightBase = "Natural";
	
	// other
	mappedExceptVectNum = -1;
	noEmptyElementExcept = true;
	
	// vectors
	vectIdentity = -1;
	vectFull = -1;
	vectExcept = -1;
	vectPosition = null;
	vectLen = null;
	vectMap = null;
	probSets = null;
	valSets = null;
	groupSets = null;
	vegML = null;

  constructor(dpData) {
	// config
    this.dtag = dpData.dtag;
    this.dptag = dpData.tag;
    this.scratchPad = dpData.scratchPad;
	this.window = dpData.window;
	this.before = dpData.before;
	this.strMapId = dpData.strMapId;
	this.probMethod = dpData.probMethod;
	this.identityOnly = dpData.identityOnly;
	this.noEmpty = dpData.noEmpty;
	this.noEmptyExcept = dpData.noEmptyExcept;
	this.nsFull = dpData.nsFull;
	this.nsIdentity = dpData.nsIdentity;
	//this.mappedExceptVectNum = -1;
	//this.noEmptyElementExcept = true;	
	
	//this.dac = dpData.dac; // Array of array
	this.dac = new Map(dpData.dac); // map value/prob

	// call backs
	this.framer = VegFramer.frameToken;
	if (dpData.framer == "token") {
		this.framer = VegFramer.frameToken;
	} else if (dpData.framer == "char") {
		this.framer = VegFramer.frameChar;
	}

	// the details
	this.nsWeightBase = dpData.nsWeightBase;
	this.nsWeights = dpData.nsWeights;
	this.nsTypes = dpData.nsTypes;
	this.vectPosition = dpData.vectPosition;
	this.vectLen = dpData.vectLen;
	this.vectMap = dpData.vectMap;
	this.vectIdentity = dpData.vectIdentity;
	this.vectFull = dpData.vectFull;
	this.vectExcept = dpData.vectExcept;

	// add numberSets
	//console.log("  dp NSC: "+ dpData.nsh.length);
	for (let ns = 0; ns < dpData.nsh.length; ns++) {
		var vidsMap = new Map(dpData.nsh[ns].vids);
		this.nsh.push(vidsMap);
		this.numSets.push(dpData.nsh[ns].numSets);
		//this.numSetTiers(dpd.nsh[ns].numSetTiers);
		//console.log("    vids["+ns+"]: " + vidsMap.size);
	}
	// prob info
	this.probSets = dpData.probSets;
	this.valSets = dpData.valSets;
	this.groupSets = dpData.groupSets;
	let cnt = 0;
	for (let i=0;i<this.groupSets.length;i++) {
		cnt += this.groupSets[i][0];
	}
  }
  
  	getTag() {
		return this.dptag;
	}
	
	getDimensionTag() {
		return this.dtag;		
	}
	
	getVegML() {
		return this.vegML;
	}
	
  	isSolid() {
		return (probSets != null);
	}
  
	// TODO make all object functions
	isCfgNSContext(setNumber) {
		return this.nsTypes[setNumber] == 0;
	}
	
	getCfgFrameFocus() {
		return this.before;
	}
	
	isCfgFameFocusNone() {
		return defaultNoFocus;
	}	
	
	getCfgWindowSize() {
		return this.window;
	}
	getCfgBefore() {
		return this.before;
	}
	getCfgAfter() {		
		return this.window - this.before;
	}
	
	isCfgIdentityOnly() {
		return identityOnly;
	}
	
	getExceptVectNumber() {
		return mappedExceptVectNum;
	}	
	
	isNoEmptyElementsExcept() {
		return noEmptyElementExcept;
	}	
	
	getMapVectorNumberSet(ns) {
		return this.vectMap[ns][0]; 
	}
	getMappedVectorCount() {
		return this.vectLen.length;
	}
}

