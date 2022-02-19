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
import {VegEmpty} from './vegml.js';
import * as VegVtoV from './vectortovid.js';


//
// Frame for processing
//
export default class VFrame {
	//private long [][] lframe;
	frame = null;
	frameFocus = 0;
	tiers = 1;
	windowSize = 1;
	
	dataSet = null;
	dataSetObj = null;
	//private List<Object []> dataSetObjDep = null;
	
	dataSetNumber = 0; // if multiple 
	dataSetPosition = 0;
	
	vectSpace = null;
	setIds = null;
	vpList = null;
	
	
	clear(dp) {
		this.frameFocus = 0;
		this.dataSetPosition = -1;
		this.dataSet = null;
		this.dataSetObj = null;
		this.dataSetObjDep = null;
		this.init(dp);
	}
	
	init(dp) {
		if (this.frame == null) {
		//	this.tiers = dp.getInputDataTiers();
			this.windowSize = dp.window;	
				
			// processing space for DP
			//1st, need to define some 1D array
			this.frame = [this.tiers];
			
			for (let x=0;x < this.tiers;x++) {
				var ary = new Array();
				for (let i=0;i<dp.window;i++) ary.push(0);
				this.frame[x] = ary;
			}
				
			this.setIds = new Array();
			this.vectSpace = new Array();
			for (let x=0;x < dp.getMappedVectorCount();x++) {
				this.setIds.push(-1);
				this.vectSpace.push(BigInt(-1));
			}
			this.vpList = new Array();
		//	console.log("    frame["+dp.window+"] " + this.frame);
		}
	}
	
	setDataSet(dataSet, dataSetPosition, dataSetNumber) { 
		this.dataSetPosition = dataSetPosition;
		this.dataSet = dataSet;
		this.dataSetNumber = dataSetNumber;
	}
	
	setDataSetObj(dataSet, dataSetPosition, dataSetNumber) { 
		this.dataSetPosition = dataSetPosition;
		this.dataSetObj = dataSet;
		this.dataSetNumber = dataSetNumber;
	}
	
	setValue(framePosition, data) {
		//this.frame[0][framePosition] = VegVtoV.toVector(data);
		this.frame[framePosition] = VegVtoV.toVector(data);
	}
	
	setValueEmpty(framePosition) {
		//this.frame[0][framePosition] = VegEmpty;
		this.frame[framePosition] = VegEmpty;
	}
	
	setFrame(dp, dataSet, dataSetPosition) {
		this.init(dp);
		
		this.dataSet = dataSet;
		this.frameFocus = dp.getFrameFocus();
		this.dataSetPosition = dataSetPosition;
		
		let s = dataSetPosition-dp.getFrameFocus();
		if (s < 0 || (dataSetPosition+dp.getAfter()) >= dataSet.length) {
			// fill with empty
			for (let i=0;i<dp.window;i++) {
				let v = null;
				if (!((s+i) >= dataSet.length || (s+i) < 0)) v = dataSet[s+i];
				if (v == null) {
					this.frame[0][i] = VegEmpty;
				} else {
					this.frame[0][i] = VegVtoV.toVector(v);					
				}
			}
		} else {
			// if next in a list then just shift and add (optimizaiton)
			if (dataSetPosition > 0 && dataSetPosition == (dataSetPosition-1)) {
				for (let i=1;i<dp.window;i++) {
					this.frame[0][i-1] = this.frame[0][i];
				}
				let end = (dataSetPosition-dp.getFrameFocus())+(dp.window-1);
				this.frame[0][dp.window-1] = VegVtoV.toVector(dataSet[end]);
			} else {
				// make full frame
				for (let i=0;i<dp.window;i++) {
					let v = dataSet[(dataSetPosition-dp.getFrameFocus())+i];
					this.frame[0][i] = VegVtoV.toVector(v);
				}	
			//console.log(" FRM2[@"+dataSetPosition+"]"+this.frame[0].length+"] ["+this.frame[0][0]+", "+this.frame[0][1]+", "+this.frame[0][2]+"]");
			}
		}
	}
}
