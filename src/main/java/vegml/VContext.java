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

package vegml;

import java.util.HashSet;
import java.util.Set;

import vegml.VegTest.TestModSet;


/*
 * context for calls
 * 
 * - stops recursion, optimizes memory for recursive calls, etc
 */
public class VContext {
	private static long CONTEXT_ID = 10;
	
	private long id;
	private VegML vML;
	private Set<String> callThrough = null;
	private TestModSet tests = null;
	private int test = -1;
	
	/**
	 * Create Evaluation context for a VegML instance
	 * 
	 * @param vML instance of VegML
	 */
	public VContext(VegML vML) {
		id = CONTEXT_ID++;
		this.vML = vML;
		test = -1;
	}
	
	/**
	 * Create Evaluation context for a DataPlane instance
	 * 
	 * @param dataplane instance of dataplane
	 */
	public VContext(VDataPlane dataplane) {
		id = CONTEXT_ID++;
		test = -1;
		this.vML = dataplane.getVegML();
	}
	
	/**
	 * Get the ID for this context
	 * @return context id
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Get the ID for this context
	 * @return context id as string
	 */
	public String getIdString() {
		return id+"";
	}
	
	/**
	 * Get the VegML instance for this context
	 * @return VegML instance
	 */
	public VegML getVegML() {
		return vML;
	}
	
	//
	// mod tests for this context
	//
	
	/**
	 * Get modification tests associated with this context
	 * 
	 * @return Modification tests
	 */
	public TestModSet getModTests() {
		return tests;
	}
	
	/**
	 * Set modification tests for this context
	 * 
	 * @param tests modification set
	 */
	public void setModTests(TestModSet tests) {
		this.tests = tests;
	}
	
	/**
	 * Get current modification test
	 * 
	 * @return index of modification test
	 */
	public int getModTest() {
		return test;
	}
	
	/**
	 * Set current modification test index
	 * @param test index of current modification test
	 */
	public void setModTest(int test) {
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
	public boolean addCallout(VDataPlane dp) {
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
	public boolean addCallout(String dtag, String dptag) {
		String t = dtag+"/"+dptag;
		if (callThrough == null) {
			callThrough = new HashSet<>();
		} else if (callThrough.contains(t)) {
			return false;			
		}
		callThrough.add(t);
		return true;
	}
	
	/**
	 * remove callout for dataplane
	 * @param dp dataplane to remove
	 */
	public void removeCallout(VDataPlane dp) {
		removeCallout(dp.getDimensionTag(), dp.getTag());
	}
	
	/**
	 * remove callout for tags
	 * @param dtag dimension tag
	 * @param dptag dataplane tag
	 */
	public void removeCallout(String dtag, String dptag) {
		if (callThrough != null) callThrough.remove(dtag+"/"+dptag);
	}

}
