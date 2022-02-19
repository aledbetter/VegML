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
import VegFramer from './vegframer.js';
import VegML, {VegEmpty} from './vegml.js';
import * as VegVtoV from './vectortovid.js';
import ValProb from './valprob.js';


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Predict -> tests without compare
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
export function predict(ctx, valueOut, dataSet, frameFocusPosition, dataSetNumber) {	
	if (ctx == null) return null;
	var vpList = new Array();
	let ret = getValProbs(ctx, valueOut, dataSet, frameFocusPosition, dataSetNumber, vpList);
	if (vpList.length < 1) return null;
	return ctx.vML.addStringMapping(vpList[0][0]);
}

/*
function predictFrameSet(ctx, valueOut, dataSet, frameFocusPosition) {	
	if (ctx == null) return null;
	var vpList = predictFrameSetVP(ctx, valueOut, dataSet, frameFocusPosition);
	if (vpList == null) return null;
	return ctx.vML.addStringMapping(vpList[0][0]);
}
*/
	
// VResultSet
export function predictSets(vML, dataSets) {		
	if (dataSets == null) return null;
	let dataPlane = vML.defDP;
	
	var ts = new VResultSet(dataPlane);
	//ts.start();
	ts.responseOut = new Array();
	var ctx = new VContext(vML);
	
	for (let set=0;set<dataSets.length;set++) {
		let dataSet = dataSets[set];
		let valueOut = new Array();		
		let frame = new VFrame();

		// set value size if they didn't prior
		for (let i=0;i<dataSet.length;i++) {				
			if (!VegFramer.makeFrameSetup(ctx, frame, valueOut, dataSet, i, dataSet[i], set)) {
				valueOut.push(VegEmpty);
				continue;
			}	
			let rsp = testAnswerFocus(ctx, frame, valueOut, 0);
			// add results copy
			ts.total++;
			
			console.log(" [@"+i+"] > predictType["+ rsp + "] val("+frame.vpList.length+")["+frame.vpList[0].value+"]["+vML.addStringMapping(frame.vpList[0].value)+"]");

		}
		// add simple resut 
		ts.responseOut.push(valueOut);
	}
	//ts.end();
	return ts;
}
	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Get Probability list base with mixing between dimensions and dataplanes
//
// Get Sorted list of VapProbs for this position
//
// modes 
// - double value average
// - Average 
// PredictionType
export function getValPList(ctx, frame, valueOut, nodefaults, valueId) {
	frame.vpList.length = 0;
	var pType = getValPListDirectSolid_int(ctx.vML.defDP, frame, nodefaults);			
	
	if (frame.vpList.length > 0) frame.vpList[0].type = pType;

	//if (resultCallout != null) {
	//	resultCallout.refineResults(ctx, this, resultCalloutArg, frame, valueOut, pType, frame.vpList, valueId);
	//	if (frame.vpList.size() > 0) pType = frame.vpList[0].type; // use the response type
	//}

	// resolve ties
	finailzeVPList(ctx.vML.defDP.dac, frame.vpList);
	if (frame.vpList.length > 0 && frame.vpList[0].type == null) {
		if (pType == null) pType = 4;
		frame.vpList[0].type = pType;
	}
	if (valueOut != null) {
		if (frame.vpList.length > 0) valueOut.push(frame.vpList[0].value);
		else valueOut.push(VegEmpty);	
	}	
	return pType;
}

// DOES NOT call callout AND doe not add valueOut
export function getValPListAmp(ctx, frame, valueOut, nodefaults, valueId, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity) {
	frame.vpList.length = 0;
	var pType = getValPListDirectSolidAmplify_int(ctx.vML.defDP, frame, nodefaults, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity);					
	
	if (frame.vpList.length > 0) frame.vpList[0].type = pType;
	//if (resultCallout != null) resultCallout.refineResults(Context ctx, this, resultCalloutArg, frame, valueOut, pType, frame.vpList, valueId);
	
	// resolve ties
	finailzeVPList(ctx.vML.defDP.dac, frame.vpList);
	return pType;
}

function getValPListDirectSolidAmplify_int(dp, frame, nodefaults, valSet, limitSet, noiseLimitFocus, noiseLimitContext, ampIdentity) {
	/////////////////////////////////
	// Vector generate for all
	VegVtoV.vectSetGen(dp.vectPosition, dp.vectLen, frame.frame, frame.vectSpace, dp.noEmpty, dp.vectExcept);
	
	/////////////////////////////////
	// map group Ids		
	let nsCnt = mapAccumSpace(dp, frame);
		
	var ret = 3;
	let recallValue = 0;
	
	/////////////////////////////////
	// get fullest value
	let avsGid = frame.setIds[dp.vectFull];
	if (avsGid >= 0) {
		ret = 0;
		let valList = dp.valSets[groupSets[avsGid][1]];
			
		if (valList != null && valList.length > 1) ret = 1;
		/*
		else if (getProbMethod() == ProbMethod.AverageIfNotRecall) {
			//
			// get fullest AND no collisions then this
			//
			double [] probList = this.probabilitySets[this.groupSet[avsGid][0]];
			// get sum
			double mod = 0;
			if (isValLong()) mod = VegUtil.amplifyListProbGetTotal(probList, valListL, valSet, -1);
			else mod = VegUtil.amplifyListProbGetTotal(probList, valList, valSet, -1);
			for (int i=0;i<probList.length;i++) {
				// only use values in identity
				var p = probList[i];
				var v = valList[i];
				p = VegUtil.amplifyListProbLimitSet(probList, valList, i, valSet, mod);
				
				if (p <= 0) continue;
				mergeIntoVPList(frame.vpList, v, p, 1);
			}
			//System.out.println(" RECAL[@"+frame.getDataSetPosition()+"] " + probList.length + " => " + vpList.size());
			Collections.sort(frame.vpList, VegUtil.VpSort);
			frame.vpList.get(0).type = ret;
			return ret;
		}
		*/
		recallValue = valList[0];
	}
	
	/////////////////////////////////
	// identity info if filtering by it
	let iacGid = -1;
	let iacValList = null;
	if (dp.identityOnly && dp.vectIdentity >= 0) {
		iacGid = frame.setIds[dp.vectIdentity];
		if (iacGid >= 0) {
			iacValList = dp.valSets[dp.groupSets[iacGid][1]];
		}
		//else if (!isNoFocus()) ret = 4;		
		else ret = 4;			
	}

	/////////////////////////////////
	// get the probabilties and values
	for (let ns=0;ns<frame.setIds.length;ns++) {		
		if (frame.setIds[ns] < 0) continue;	
		let numberSet = dp.getMapVectorNumberSet(ns);

		// per number set noise limits		
		//contains focus
		// per number set noise limits		
		let noiseLimit = noiseLimitContext;
		if (!dp.isNumberSetContext(numberSet)) {
			noiseLimit = noiseLimitFocus;
		}
		
		let sid = frame.setIds[ns];		
		let vaid = -1, pid = -1;
		try {
		vaid = dp.groupSets[sid][1];
		pid = dp.groupSets[sid][0];
		} catch(err) {}
		
		let probList = dp.probSets[pid];
		let valList = dp.valSets[vaid];
		if (!probList) console.log("ERROR: NO PROB LIST["+sid+"]ns["+ns+"] v["+vaid+"]["+pid+"] ["+dp.groupSets[sid]+"]");
		let mod = amplifyListProbGetTotal(probList, valList, valSet, noiseLimit);

	//AMP SET			
		let added = false;
		for (let i=0;i<probList.length;i++) {
			let p = probList[i];
			let v = valList[i];
		
			if (!limitSet) p = amplifyListProbSet(probList, valList, i, valSet, mod);
			else p = amplifyListProbLimitSet(probList, valList, i, valSet, mod);
			if (p <= 0) continue;
			
			// best is numberSet change AND limit
			if (dp.nsVectNumIdentity == ns && ampIdentity > 0) p *= ampIdentity;	// numberSet weight change?
			else if (iacValList != null && dp.identityOnly && !containsVal(iacValList, null, v)) continue; // a bit slow -> PERF issue	

		//	System.out.println("GOT IT["+probList.length+"]["+i+"] ("+ampComb+") sum["+mod+"]  ["+probList[i]+"]: " + p);
			mergeIntoVPList(frame.vpList, v, p);
			added = true;
		}
		
		if (added) {
			if (!dp.isNumberSetContext(numberSet)) {
				if (ret == 3 && ns != dp.vectIdentity) ret = 2;
			}
		}
	}
	
	/////////////////////////////////
	// if nothing -> Fall back when nothing -> use general dimension set probability: get best
	if (frame.vpList.length < 1) {
		if (nodefaults || dp.dac == null) return -1;
		// add default values
		for (let [prob, value] of dp.dac) {
			mergeIntoVPList(frame.vpList, value, prob*DEFAULT_PROB_WEIGHT);
		}
		frame.vpList[0].type = 5;
		return 5;
	}

	/////////////////////////////////
	// Average the values (based on the number here)
	for (let i=0;i<frame.vpList.length;i++) {
		let vpx = frame.vpList[i];
		vpx.probability = vpx.probability / nsCnt;
	}
	
	frame.vpList.sort(VpSort);	
	
	/*
		/////////////////////////////////
		// forced winner
		if (getProbMethod() == ProbMethod.AverageRecall && ret == PredictionType.Recall && recallValue != frame.vpList.get(0).value) {
			for (int i=0;i<frame.vpList.size();i++) {
				ValProb vp = frame.vpList.get(i);
				if (vp.value == recallValue) {
					vp.probability = frame.vpList.get(0).probability + (frame.vpList.get(0).probability * WIN_MARGIN_PERCENT);
					Collections.sort(frame.vpList, VegUtil.VpSort);	
					break;
				}
			}
		}	
		*/
	frame.vpList[0].type = ret;
	return ret;
}


function getValPListDirectSolid_int(dp, frame, nodefaults) {
	//if (dp.defAccum.total == 0) return -1;	
	
	/////////////////////////////////
	// Vector generate for all
	VegVtoV.vectSetGen(dp.vectPosition, dp.vectLen, frame.frame, frame.vectSpace, dp.noEmpty, dp.vectExcept);
	
	/////////////////////////////////
	// map group Ids		
	let nsCnt = mapAccumSpace(dp, frame);
		
	var ret = 3;
	let recallValue = 0;
	
	/////////////////////////////////
	// get fullest value
	let avsGid = frame.setIds[dp.vectFull];
	if (avsGid >= 0) {
		ret = 0;
		let valList = dp.valSets[groupSets[avsGid][1]];
		
		if (valList != null && valList.length > 1) ret = 1;
		/*else if (dp.probMethod == "AverageIfNotRecall") {
			//
			// get fullest AND no collisions then this
			//
			let probList = dp.probSets[dp.groupSets[avsGid][0]];
			for (let i=0;i<probList.length;i++) {
				// only use values in identity
				mergeIntoVPList(frame.vpList, valList[i], probList[i], 1);
			}
			//System.out.println(" RECAL[@"+frame.getDataSetPosition()+"] " + probList.length + " => " + vpList.size());
			frame.vpList.sort(VpSort);
			frame.vpList[0].type = ret;
			return ret;
		}*/
		recallValue = valList[0];
	}
	
	/////////////////////////////////
	// identity info if filtering by it
	let iacGid = -1;
	let iacValList = null;
	if (dp.identityOnly && dp.vectIdentity >= 0) {
		iacGid = frame.setIds[dp.vectIdentity];
		if (iacGid >= 0) {
			iacValList = dp.valSets[dp.groupSets[iacGid][1]];
		}
		//else if (!isNoFocus()) ret = 4;		
		else ret = 4;			
	}

	/////////////////////////////////
	// get the probabilties and values
	for (let ns=0;ns<frame.setIds.length;ns++) {		
		if (frame.setIds[ns] < 0) continue;

		let sid = frame.setIds[ns];		
		let vaid = -1, pid = -1;
		try {
		vaid = dp.groupSets[sid][1];
		pid = dp.groupSets[sid][0];
		} catch(err) {}
		
		let probList = dp.probSets[pid];
		let valList = dp.valSets[vaid];
		if (!probList) console.log("ERROR: NO PROB LIST["+sid+"]ns["+ns+"] v["+vaid+"]["+pid+"] ["+dp.groupSets[sid]+"]");

		let added = false;
		for (let i=0;i<probList.length;i++) {
			let v = valList[i];
			if (iacValList != null && dp.identityOnly && !containsVal(iacValList, null, v)) continue; // a bit slow -> PERF issue
			mergeIntoVPList(frame.vpList, v, probList[i]);
			added = true;
		}
		if (added) {
			if (!dp.isNumberSetContext(dp.getMapVectorNumberSet(ns))) {
				if (ret == 3 && ns != dp.vectIdentity) ret = 2;
			}
		}
	}

	/////////////////////////////////
	// if nothing -> Fall back when nothing -> use general dimension set probability: get best
	if (frame.vpList.length < 1) {
		if (nodefaults || dp.dac == null) return -1;
		// add default values
		for (let [prob, value] of dp.dac) {
			mergeIntoVPList(frame.vpList, value, prob*DEFAULT_PROB_WEIGHT);
		}
		frame.vpList[0].type = 5;
		return 5;
	}

	/////////////////////////////////
	// Average the values (based on the number here)
	for (let i=0;i<frame.vpList.length;i++) {
		let vpx = frame.vpList[i];
		vpx.probability = vpx.probability / nsCnt;
	}
	
	frame.vpList.sort(VpSort);	 

	//if (iacValList != null) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]  iac["+iacGid+"]["+iacValList.length+"/"+this.getString(iacValList[0])+"]("+getNSIdentityNumber()+")   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
	//else if (recallValue > 0) System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"] ix["+iacGid+"]   ["+this.getString(recallValue)+"]["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
	//else System.out.println(" Other[@"+frame.getDataSetPosition()+"]["+acCnt+"]["+sc+"]   ix["+iacGid+"] ["+this.getString(vpList.get(0).value)+"]["+vpList.get(0).probability+"] => " + vpList.size() + " ["+frame.getString()+"]  ["+s+"]");
	
	/////////////////////////////////
	// forced winner
	/*
	if (getProbMethod() == ProbMethod.AverageRecall && ret ==0 && recallValue != frame.vpList[0].value) {
		for (let i=0;i<frame.vpList.length;i++) {
			ValProb vp = frame.vpList.get(i);
			if (vp.value == recallValue) {
				vp.probability = frame.vpList[0].probability + (frame.vpList[0].probability * WIN_MARGIN_PERCENT);
				frame.vpList.sort(VpSort);	
				break;
			}
		}
	}	
	*/
	frame.vpList[0].type = ret;
	return ret;
}

// map the setids
function mapAccumSpace(dp, frame) {
	let nsCnt = 0;
	for (let i=0;i<frame.vectSpace.length;i++) {
		frame.setIds[i] = -1;
		let vid = frame.vectSpace[i];
		if (vid == -2 || vid == -1 || vid == 0) continue;		
	
		// get vector from map
		let id = dp.nsh[i].get(vid);
		if (!id || id <= 0) id = -2;
		else id = id-1;
		frame.setIds[i] = id;
		//console.log(" MAS["+i+"] => ["+vid+"] "+id);
		nsCnt++;
	}	
	return nsCnt;
}

// find value in array
function containsVal(iacValList, iacValListL, val) {
	if (iacValList != null) {
		for (let i=0;i<iacValList.length;i++) {
			if (iacValList[i] == val) return true;
		}
	} else {
		for (let i=0;i<iacValListL.length;i++) {
			if (iacValListL[i] == val) return true;
		}			
	}
	return false;
}
	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Test correct answers at position, train predict OR recall
//	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
function testAnswerFocus(ctx, frame, valueOut, valueId) {
	return testAnswerFocusFull(ctx, frame, valueOut, null, valueId, false);
}
// for segments..
function testAnswerFocusSegment(ctx, valueOut, dataSet, valueId) {
	return testAnswerFocusFull(ctx, null, valueOut, dataSet, valueId, true);	
}
function testAnswerFocusFull(ctx, frame, valueOut, dataSet, valueId, segment) {
	var ret = 3; 
	if (segment) {
	//	vpList = VegTest.predictSegmentSet_int(ctx, dp, dataSet, false, true);
	} else {
		// if this is a values direct VDataPlane 
		ret = getValPList(ctx, frame, valueOut, false, valueId);	
	}
	if (isBestValProb(frame.vpList, valueId)) { 
		return ret;
	} else if (valueId == 0) {
		return ret;
	}
	return -1;
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Utils
//	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


// finalize by resolving the ties
function finailzeVPList(dac, vpList) { 
	if (vpList.length < 2) return vpList;
		
	let pType = vpList[0].type;
	let p = vpList[0].probability;
	if (p != vpList[1].probability) {
		vpList.sort(VpSort);
		vpList[0].type = pType;
		return vpList;
	}
	/*
	// Average the values (based on the number here)
	for (let i=0;i<vpList.length;i++) {
		let vpx = vpList[i];
		let dacp = dac.getProbability(vpx.value);
		vpx.probability += dacp;
		if (vpx.probability != p) break;
	}	
	*/
	// this and finalize need to base before add.. then merge properly					
	let mw = 0;
	for (let i=0;i<vpList.length;i++) {
		let vpx = vpList[i];	
		let dacp = dac.get(vpx.value);
		let mvv = vpx.probability / dacp;
		if (mvv > mw) mw = mvv;
	}
	for (let i=0;i<vpList.length;i++) {
		let vpx = vpList[i];
		let dacp = dac.get(vpx.value);
		vpx.probability = (vpx.probability+(dacp*mw)) / 2;
		if (vpx.probability != p) break;
	}
	
	vpList.sort(VpSort);
	vpList[0].type = pType;
	return vpList;
}

// Sort
function VpSort(lvp, rvp) {
  	// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending        	
	if (lvp.probability < rvp.probability) return 1;
	if (lvp.probability > rvp.probability) return -1;
  	if (lvp.count < rvp.count) return 1;
	if (lvp.count > rvp.count) return -1;
	if (lvp.value < rvp.value) return 1;
	if (lvp.value > rvp.value) return -1;
	return 0;
}

// merge single vp into list
function mergeIntoVPListVp(vpList, vp) { 
	let idx = indexOfValProb(vpList, vp.value);
	if (idx >= 0) {
		// add to existing
		let vx = vpList[idx];
		vx.probability += vp.probability;
	} else {
		vpList.push(vp);
	}
	
	return vpList;
}

function mergeIntoVPList(vpList, value, probability) { 
	let idx = indexOfValProb(vpList, value);
	if (idx >= 0) {
		// add to existing
		let vx = vpList[idx];
		vx.probability += probability;
	} else {
		let vp = new ValProb();
		vp.value = value;
		vp.probability = probability;
		vpList.push(vp);
	}		
	return vpList;
}
	
function indexOfValProb(list, value) {
	if (list == null) return -1;
	for (let k=0;k<list.length;k++) {
		if (list[k].value == value) return k;
	}
	return -1;
}

function isBestValProb(vpList, valueId) {
	if (vpList == null || vpList.length < 1) return false;
	if (vpList[0].value == valueId) return true;
	return false;
}

// Amplify info
function amplifyListProbSet(probList, valList, pos, ampSet, mod) {
	if (mod == 0) return probList[pos];
	if (!containsv(ampSet, valList[pos])) return probList[pos]; // not this value
	return (probList[pos] * mod);
}
function amplifyListProbLimitSet(probList, valList, pos, ampSet, mod) {
	if (!containsv(ampSet, valList[pos])) return 0; // not this value
	if (mod == 0) return probList[pos];
	return probList[pos] * mod;
}
function amplifyListProbGetTotal(probList, valList, ampSet, noiseLimit) {
	if (probList == null) return 0;	
	let tot = 0;
	let sum = 0, fsum = 0;		
	let cnt = 0;
	// clear the no-noes
	for (let i = 0; i < probList.length; i++) {
		fsum += probList[i];
		if (!containsv(ampSet, valList[i])) {
			if (noiseLimit <= 0 || cnt < noiseLimit) tot++;
			else sum += probList[i];
		} else {
			sum += probList[i];
		}
		cnt++;
	}
	if (tot == 0) return 0;
	return (fsum/sum);
}
function containsv(valSet, v) {
	for (let i=0; i < valSet.length; i++) {
		if (valSet[i] == v) return true;
	}
	return false;
}
