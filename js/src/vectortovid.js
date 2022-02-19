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
import {VegEmpty} from './vegml.js';

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Vector Ids
//	
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
var PRIME = 59;
var PRIME_BI = 59n;
var PRIME2 = 37;
var PRIME2_BI = 37n;
var PRIME_START = 17;
var PRIME_START_BI = 17n;
var PRIME_BASE = 1125899906842597;
var PRIME_BASE_BI = 1125899906842597n;
var VIDplaceHolder = 1975;

// too many collisions
//
// make number(vector) from word
//
export function toVector(string) {
	if (string == null || string.length == 0) return VegEmpty;
	if (string.length == 2) {
		// ugly hack to prevent colisions...(same alg hashCode32()) 
		let res = (PRIME*PRIME_START) + string.charCodeAt(0);
		res = (PRIME*res) + '~'.charCodeAt(0);
		res = (PRIME*res) + ' '.charCodeAt(0);
		return (PRIME*res) + string.charCodeAt(1);
	}
	return hashCode32(string);
}

export function toVectorObj(obj) {
	if (obj == null) return VegEmpty;
	if (obj instanceof String) return toVector(obj);
	return hashCode32(obj.toString());
}

// map tiers
export function vectSetGen(nsMap, nsMapLen, frameVect, vectSpace, noEmpty, vectNumAllowEmpty) {
	// base zero
	for (let i=0;i<vectSpace.length;i++) vectSpace[i] = BigInt(-1);

	// for space in window
	for (let i=0;i<nsMap[0].length;i++) {  // NS
		// for numberSet
		for (let x=0;x<nsMapLen.length; x++) {	// Window
			// for each get the info
			if (nsMapLen[x] < 1 || !nsMap[x][i][0]) continue; // have this position? (this works dependent or not)
			if (noEmpty && x != vectNumAllowEmpty && frameVect[0][i] == VegEmpty) vectSpace[x] = -2;
			if (vectSpace[x] == -2) continue; // had empty spot
			
			// FOR dependent probability values
			if (nsMap[x][i].length > 1) {
				// may have dependent values 0 - n; map in those that exist (the above check for 0 ensures at least 1 element event if base)
				// position 0 -> used above to designage vector here
				// position 1 -> is the primary
				// position 2-n -> are the dependents
				for (let d=1;d<nsMap[x][i].length;d++) {
					let framedpos = d-1;
					if (nsMap[x][i][d] && frameVect[framedpos][i] == VegEmpty && noEmpty && x != vectNumAllowEmpty) {
						// need this one and not here
						vectSpace[x] = -2;
						break;
					} 
					// add all empty postions to prevent collisoins when depdent values are the same
					// increment the vector value					
					if (!nsMap[x][i][d]) vectSpace[x] = toVectorV64Inc(nsMapLen[x], VIDplaceHolder, vectSpace[x]);							
					else vectSpace[x] = toVectorV64Inc(nsMapLen[x], frameVect[framedpos][i], vectSpace[x]);						
				}
			} else {
				// increment the vector value
				vectSpace[x] = toVectorV64Inc(nsMapLen[x], frameVect[0][i], vectSpace[x]);		
			}
		}
	}
}

// incremental vector building.. 
function toVectorV64Inc(length, vect, curVect) {		
	if (length == 1) {
		return vect;
	} else if (length == 2) {
		if (curVect == -1) return BigInt.asUintN(32, (BigInt(vect)*PRIME2_BI));
		return BigInt.asIntN(64, ((BigInt(vect) << 32n) | curVect));
	}
	if (curVect == -1) curVect = 0; 
	curVect = hashCode64(curVect, vect);
	return curVect;
}


function toVectorV64Single(vect) {
	if (vect == null) return 0;
	if (vect.length == 1) {
		return vect[0];
	} else if (vect.length == 2) {
		let low = BigInt.asUintN(32, BigInt(vect[0])*PRIME2_BI);
		let hi = BigInt.asIntN(64, (BigInt(vect) << 32n));
		return BigInt.asIntN(64, (hi | low));
		//return ((vect[1]) << 32) | ((vect[0]*PRIME2_BI) & 0xffffffff);	
	}
	let vid = 0;
	for (let i=0;i<vect.length;i++) vid = hashCode64(vid, vect[i]);
	return vid;
}


// make vector from vect for set
function toVectorV64(vectSet, fset, set) {
	if (vectSet == null || fset == null || set == null) return 0;
	if (vectSet.length == set.length) {
		if (vectSet.length != fset.length) return 0;
		return toVectorV64Single(vectSet);
	}
	if (set.length == 1) {
		if (set[0] >= vectSet.length) return VegEmpty;
		return vectSet[set[0]];
		
	} else if (set.length == 2) {	
		let i0 = 0, i1 = 0;
		if (set[0] >= vectSet.length) i0 = VegEmpty;
		else i0 = vectSet[set[0]];
		if (set.get(1) >= vectSet.length) i1 = VegEmpty;
		else i1 = vectSet[set[1]];	
		return ((i1) << 32) | ((i0*PRIME2) & 0xffffffff);
	}	

	// do it from full set: offsets align from 0
	let vid = 0;
	for (let i=0;i<set.length;i++) {
		let p = set[i];	
		if (p >= vectSet.length) vid = hashCode64(vid, VegEmpty);
		else vid = hashCode64(vid, vectSet[p]);
	}			
	return vid;
}


// 32 bit hashcode
function hashCode32(string) {
	let result = PRIME_START;
	for (let i = 0; i < string.length; i++) {
		result = (PRIME*result) + string.charCodeAt(i);
		result = result & result; // Convert to 32bit integer
	}
	return result;
}

/*
function hashCode32(lastResult, string) {
	let len = string.length;
	for (let i = 0; i < len; i++) {
		lastResult = (PRIME*lastResult) + string.charCodeAt(i);
	}
	return lastResult;
}
//function hashCode32(c) {
//	return (PRIME*PRIME_START) + c;
//}
//function hashCode32(lastResult, c) {
//	return (PRIME*lastResult) + c;
//}
*/
// 64 bit hashcode
function hashCode64String(string) {
	let result = PRIME_BASE; // prime
	let len = string.length;
	for (let i = 0; i < len; i++) {
		result = (PRIME*result) + string.charCodeAt(i);
	}
	return result;
}
function hashCode64(cur, num) {
	if (cur == 0) cur = PRIME_BASE_BI; // prime
	if (num < 0) {
		cur = (PRIME_BI*cur) + 45n; // '-'
		num = (-num);
	} else {
	//	cur = (PRIME*cur) + 43; // '+'
	}
	while (num > 0) {
	    cur = BigInt.asIntN(64, (PRIME_BI*cur) + BigInt((num % 10)+48)); // char values (no need)
	    num = Math.floor(num / 10);
	}
	return cur;
}

	