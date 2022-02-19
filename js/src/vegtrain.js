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
import VDataSets from './vdatasets.js';
import VDataPlane from './vdataplane.js';
import VFrame from './vframe.js';
import VResultSet from './vresultset.js';
import VAccum from './vaccum.js';
import * as VegVtoV from './vectortovid.js';
import ValProb from './valprob.js';

/**
 * data item value training
 * This could train 1 set or a few sets at a time, allowing for memory swaping or partial saving
 * @param dp DataPlane in training
 * @param frame input data frame
 * @param valueIds values to train to if multiple
 * @return true if value trained
 */
export function trainFocus(dp, frame, valueIds) {
	dp.getVegML().incCount();

	if (dp.getCfgNonValue() == valueIds[0] && valueIds.length == 1) return false; // nope
	
	// single pass vector gen and check
	VegVtoV.vectSetGen(dp.getNSMapVectorPositions(), dp.getNSMapVectorLength(), frame.getFrameFull(), frame.getVectSpace(), dp.isNoEmptyElements(), dp.getExceptVectNumber());				
	
	let filtered = false;
	/*
	// check training filter for identity only
	// need to create / add to total on context numberSets; not add value
	if (dp.getTrainingFilter(valueIds[0]) == 1) filtered = true;	
	
	if (dp.isCfgSaveChildVids() && !filtered) {
		// generate child Vids here
		VectorToVid.vectSetGen(dp.getNSChildMapVectorPositions(), dp.getNSChildMapVectorLength(), frame.getFrameFull(), frame.getVectSpaceCh(), dp.isNoEmptyElements(), dp.getExceptVectNumber());		
	}
	*/
	
	/*
	// get or add full set, get vsid to use
	let fvid = -1;
	if (dp.getCfgNSVectNumFullNumber() >= 0) fvid = frame.getVectSpace()[dp.getCfgNSVectNumFullNumber()];	
	MLNumberSetHash fnsh = dp.getNSHash(dp.getCfgNSFullNumber());
	
	// if predicting at position: no sets that include that position 
	// use valueFocus or default
	let nofull = false;
	if (fnsh == null || fnsh.isTurnedOff()) nofull = true;
	
	int [] fvectSet = frame.getFrame();	
	//frame.print();
	
	// for each value training to
	for (int vc = 0;vc<valueIds.length;vc++) {
		let valueId = valueIds[vc];
		
		let accumFValue = valueId;
		let accumFVid = fvid;

		// get full set
		let vsid = 0;
		if (nofull || fvid == -2 || fvid == -1 || fvid == 0 || filtered) {
			if (dp.getVegML().isCfgSaveVectSets()) {
				Accum vs = fnsh.get(accumFVid);
				if (vs == null) vsid = dp.getVegML().vectSetMap.add(Arrays.copyOf(fvectSet, fvectSet.length));
				else vsid = vs.getVectSetId();
			}
		} else if (fnsh != null) {
			Accum vs = fnsh.addCount(accumFVid, accumFValue);	
			if (vs == null) {
				// make this variable to improve mem/performance for the set
				vs = dp.getAccumulator();			
				if (dp.getVegML().isCfgSaveVectSets()) {					
					vsid = dp.getVegML().vectSetMap.add(Arrays.copyOf(fvectSet, fvectSet.length));
					vs.setVectSetId(vsid);
				} else if (dp.isCfgSaveChildVids()) {
					vs.setVectChildVid(frame.getVectSpaceCh()[dp.getCfgNSVectNumFullNumber()]);	
				}
				vs.setVectorCode(accumFVid);
				vs.addCount(accumFValue);
				//if (isLocked) vs.lock(value);
				fnsh.put(vs);
			} else {
				vsid = vs.getVectSetId();
			}
		}
					
		// defAccumulator accounting
		if (!filtered) dp.getAccumDefault().addCount(valueId);
		//else dp.getAccumDefault().adjustTotalInc(1);
		
		// account for set value
		if (fnsh != null) {
			if (!filtered) fnsh.getAccumSetDefault().addCount(accumFValue);	
			else fnsh.getAccumSetDefault().adjustTotalInc(1);
		}
		
		//
		// all subsets add a vector accum value
		for (let vectNum=0;vectNum<frame.getVectSpace().length;vectNum++) {
			if (dp.getCfgNSVectNumFullNumber() == vectNum) continue;
			let vid = frame.getVectSpace()[vectNum];		
			if (vid == -2 || vid == -1 || vid == 0) continue;
			
			let setNumber = dp.getMapVectorNumberSet(vectNum);
			// if filtered then only on context
			if (filtered && !dp.isCfgNSContext(setNumber)) {
				continue;
			}
										
			// add direct for position (in window)
			MLNumberSetHash nsh = dp.getNSHash(setNumber);
				
			// set probability
			if (!filtered) nsh.getAccumSetDefault().addCount(valueId);
			else nsh.getAccumSetDefault().adjustTotalInc(1);
		
			// get the Vid to set in the accumulator vectCode
			let accumVid = vid;
			let accumValue = valueId;		
			
			if (filtered) {
				// just add to total
				Accum vs = nsh.addTotal(accumVid, accumValue);	
				if (vs == null) {
					// make this variable to improve mem/performance for the set
					vs = dp.getAccumulator();
					if (!dp.isCfgSaveChildVids()) {
						vs.setVectSetId(vsid);	
					}
					vs.setVectorCode(accumVid);
					vs.adjustTotal(1);	
					nsh.put(vs);
				} 
			} else {
				// add or update the value
				Accum vs = nsh.addCount(accumVid, accumValue);	
				if (vs == null) {
					// make this variable to improve mem/performance for the set
					vs = dp.getAccumulator();
					if (dp.isCfgSaveChildVids()) {
						vs.setVectChildVid(frame.getVectSpaceCh()[vectNum]);	
					} else {
						vs.setVectSetId(vsid);	
					}
					vs.setVectorCode(accumVid);
					vs.addCount(accumValue);
					nsh.put(vs);
				} 
			}
		}
	}
	*/
	return true;
}

