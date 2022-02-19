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
import VDataPlane from './vdataplane.js';

//
// Context
//
var CTX_CONTEXT_ID = 10;
export default class VContext {
	vML = null;
	id = 0;
	callThrough = null;
	tests = null;
	test = -1;
	
	constructor(vML) {	
		this.vML = vML;
		this.id = CTX_CONTEXT_ID++;
	}
	
	/**
	 * Get the ID for this context
	 * @return context id
	 */
	getId() {
		return id;
	}
	
	/**
	 * Get the ID for this context
	 * @return context id as string
	 */
	getIdString() {
		return id+"";
	}
	
	/**
	 * Get the VegML instance for this context
	 * @return VegML instance
	 */
	getVegML() {
		return vML;
	}
	
	/**
	 * Get modification tests associated with this context
	 * 
	 * @return Modification tests
	 */
	getModTests() {
		return tests;
	}
	
	/**
	 * Set modification tests for this context
	 * 
	 * @param tests modification set
	 */
	setModTests(tests) {
		this.tests = tests;
	}
	
	/**
	 * Get current modification test
	 * 
	 * @return index of modification test
	 */
	getModTest() {
		return test;
	}
	
	/**
	 * Set current modification test index
	 * @param test index of current modification test
	 */
	setModTest(test) {
		this.test = test;
	}

	/**
	 * Recursion prevention
	 * 
	 * Add dataplane to the callout in use list
	 * this will prevent this dataplane from being called again until the dataplane is removed
	 * 
	 * @param dp dataplane call through
	 * @return
	 */
	addCallout(dp) {
		return addCallout(dp.getDimensionTag(), dp.getTag());
	}
	
	/**
	 * Recursion prevention
	 * 
	 * Add dataplane to the callout in use list
	 * this will prevent this dataplane from being called again until the dataplane is removed
	 * 
	 * @param dtag dimension tag to map
	 * @param dptag dataplane tag to map
	 * @return
	 */
	addCallout(dtag, dptag) {
		let t = dtag+"/"+dptag;
		if (callThrough == null) {
			callThrough = new Set()
		} else if (callThrough.has(t)) {
			return false;			
		}
		callThrough.add(t);
		return true;
	}
	
	/**
	 * remove callout for dataplane
	 * @param dp dataplane to remove
	 */
	removeCallout(dp) {
		removeCallout(dp.getDimensionTag(), dp.getTag());
	}
	
	/**
	 * remove callout for tags
	 * @param dtag dimension tag
	 * @param dptag dataplane tag
	 */
	removeCallout(dtag, dptag) {
		if (callThrough != null) callThrough.delete(dtag+"/"+dptag);
	}
	
	
}
