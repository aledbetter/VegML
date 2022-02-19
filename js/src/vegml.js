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
//http://sedro.us-east-2.elasticbeanstalk.com/w/test-solid-2.json
// implement predict and information only; no training / tuning

// Prediction type
/*
PredictionType {
	0 Recall,			// recall, seen it before
	1 RecallPredict,	// recall, multiple options
	2 PredictRelate,	// predicted from elements with a relation
	3 Predict,		// predicted from elements
	4 PredictUnknown,	// predicted for unknown token
	5 Default,		// No info: default for VDataPlane		
	6 Fail,			// Error: no trained data?
	7 NotUnknown,		// not unknown
	8 AnyRecall,		// RecallPredict or Recall
	9 AnyPredict,		// PredictRelate or Predict
	10 AnyUnknown,		// PredictRelate or Predict
	11 All,			// All or baseLine
	-1 None,
}
*/
import VDataPlane from './vdataplane.js';
import ValProb from './valprob.js';

export var VegEmpty = 0;

//
// Veg ML instance
//
export default class VegML {
  dataPlans = new Map();
  defDP = null;
  defSM = null;
  scratchPad = null;
  stringMapReg = null;
  stringMaps = null;
  empty = 0;
  tag = "defult";
  desc = "--";
  nsh = null;
  numSets = null;
  numSetTiers = null;
  inCount = 0;

  constructor(vdata) {	
    this.tag = vdata.tag;
    this.desc = vdata.desc;
    this.empty = vdata.empty;
	VegEmpty = vdata.empty;
    this.scratchPad = vdata.scratchPad;
    this.stringMapReg = vdata.stringMap.reg.stringMapReg;
    this.stringMaps = new Map();
	
	// strings
	// Add the string maps
	for (let i = 0; i < vdata.stringMap.maps.length; i++) {
		//stringMap.maps[id].map[] {"220401487":"DT"},
		var sm = new Map(vdata.stringMap.maps[i].map);
		this.stringMaps.set(vdata.stringMap.maps[i].id, sm);
	}
	
	// DataPlans
	let ddptag = this.scratchPad["inf_default_dptag"];
	let ddtag = this.scratchPad["inf_default_dtag"];
	for (let i = 0; i < vdata.dataPlans.length; i++) {
		var dpd = vdata.dataPlans[i];
		// add dataPlane
		var dp = new VDataPlane(dpd);
		this.dataPlans.set(dpd.dtag+"/"+dpd.tag, dp);
		// if default	
		if (dp.dtag == ddtag && dp.dptag == ddptag) {
			this.defDP = dp;	// default DP
			this.defSM = this.stringMaps.get(dp.strMapId); // default stringmap
		console.log(" defaultDP["+ddtag+"/"+ddptag+"] w["+dp.window+"]ns["+dp.nsh.length+"] strMapId[" + dp.strMapId+"]");
		}
	}
  }
  
  	//
	// Load Model from JSON function
	// add callback to return handle: FIXME
	static load(modelPath) {
		fetch(modelPath, {mode: 'cors'})
		.then(response => response.json())
	  	.then(data => {
			console.log("loading RPM: " + data.tag);
			// issue this is async
			vML = new VegML(data);
		});
	}
	
	
	addStringMapping(valueId) {
		return this.defSM.get(valueId);
	}
	
	getInCount() {
		return inCount;
	}
	
	setInCount(inCount) {
		this.inCount = inCount;
	}
	
	incCount() {
		inCount++;
	}
}



