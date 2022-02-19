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


import java.util.ArrayList;
import java.util.List;

import vegml.ValProb;


/**
 * Accumulator for boolean values
 *
 */
class AccumBool extends Accum {
	private static final long serialVersionUID = 7684734010520136918L;

	protected boolean ignore = false;
	protected int val1 = 0;
	protected int cnt1 = 0;
	protected int val2 = 0;
	protected int cnt2 = 0;

	public AccumBool() {
	}
	

	// how balanced is this
	@Override
	public double getBalance(int dataWidth) {
		if (total == 0) return 0;
		if (cnt1 == cnt2) return 0;
		// compare most to least
		if (cnt2 > cnt1) return (double)cnt1/(double)cnt2;
		return (double)cnt2/(double)cnt1;
	}

	// get most probable, this takes nothing of the current situation into account
	@Override
	public boolean isMostProbableValue(long val) {
		if (total == 0) return false;
		if (val != val1 && cnt1 > cnt2) return true;
		if (val != val2 && cnt2 > cnt1) return true;
		return false;
	}
	// get most probable, this takes nothing of the current situation into account
	@Override
	public boolean isMostProbableValues(long val) {
		if (total == 0) return false;
		if (val != val1 && cnt1 >= cnt2) return true;
		if (val != val2 && cnt2 >= cnt1) return true;
		return false;
	}
	
	// get most probable
	@Override
	public ValProb getFirstMostProbable() {
		if (total == 0) return null;
		ValProb valProb = new ValProb(); 
		if (cnt1 >= cnt2) {
			valProb.value = val1;
			valProb.probability = getProbability(val1);
			valProb.count = cnt1;			
		} else {
			valProb.value = val2;
			valProb.probability = getProbability(val2);
			valProb.count = cnt2;			
		}
		return valProb;
	}
	@Override
	public double getFirstMostProbablity() {
		if (total == 0) return 0;
		if (cnt1 >= cnt2) return this.getProbability(val1);
		if (cnt2 >= cnt1) return this.getProbability(val2);
		return 0;
	}
	@Override
	public long getFirstMostProbablityValue() {
		if (total == 0) return 0;
		if (cnt1 >= cnt2) return val1;
		return val2;
	}
	@Override
	public int getMaxCount() {
		if (total == 0) return 0;
		if (cnt1 >= cnt2) return cnt1;
		return cnt2;
	}
	
	// get the list of values and probabilities
	@Override
	public List<ValProb> getValPs(List<ValProb> vpList) {
		if (total == 0) return null;
		if (vpList == null) vpList = new ArrayList<>();
		if (cnt1 > 0) {
			ValProb vp = new ValProb(); 
			vp.value = val1;
			vp.probability = this.getProbability(val1);
			vp.count = cnt1;
			vpList.add(vp);					
		}
		if (cnt2 > 0) {
			ValProb vp = new ValProb(); 
			vp.value = val2;
			vp.probability = this.getProbability(val2);
			vp.count = cnt2;
			if (vpList.size() > 0 && cnt2 > cnt2) vpList.add(0, vp);
			else vpList.add(vp);					
		}
		return vpList;
	}
	
	@Override
	public int getValueCount() {
		if (total == 0) return 0;
		if (cnt1 > 0 && cnt2 > 0) return 2;
		return 1;
	}
	
	@Override
	int setCount(long val, int count, int crtCount) {	
		if (val == val1) {
			cnt1 = count;
			return cnt1;
		} else if (val == val2) {
			cnt2 = count;
			return cnt2;
		} else if (cnt1 < 1) {
			val1 = (int)val;
			cnt1 = count;
		} else if (cnt2 < 1) {
			val2 = (int)val;
			cnt2 = count;		
		}		
		return count;
	}
	
	@Override
	int addCount(long val, int count) {	
		if (total != Integer.MAX_VALUE) total += count; // HACK to stop things here
		if (val == val1) {
			cnt1 += count;
			return cnt1;
		} else if (val == val2) {
			cnt2 += count;
			return cnt2;
		} else if (cnt1 < 1) {
			val1 = (int)val;
			cnt1 = count;
		} else if (cnt2 < 1) {
			val2 = (int)val;
			cnt2 = count;		
		}
		return count;
	}
	@Override
	public int getCount(long val) {
		if (total == 0) return 0;
		if (val == val1) {
			return cnt1;
		} else if (val == val2) {
			return cnt2;
		}
		return 0;
	}
	@Override
	public boolean hasValue(long val) {
		if (total == 0) return false;
		if (val == val1) {
			return true;
		} else if (val == val2) {
			return true;
		}
		return false;
	}

	@Override
	int reduceCount(long val, int count) {
		if (total == 0) return 0;
		if (val == val1) {
			cnt1 -= count;
			return cnt1;
		} else if (val == val2) {
			cnt2 -= count;
			return cnt2;
		}
		return 0;
	}
	// remove a value
	@Override
	int remove(long val) {
		if (total == 0) return 0;
		if (val == val1) {
			val1 = cnt1 = 0;
		} else if (val == val2) {
			val2 = cnt2 = 0;
		}
		return 0;
	}
	
	@Override
	void clear() {
		if (total == 0) return;
		total = 0;
		val2 = cnt2 = 0;
		val1 = cnt1 = 0;
		ignore = false;
	}
	
	// merge this into intoAc
	@Override
	void mergeInto(Accum intoAc) {
		if (total == 0 || intoAc == null) return;
		if (cnt1 > 0) intoAc.addCount((long)val1, cnt1);
		if (cnt2 > 0) intoAc.addCount((long)val2, cnt2);
	}
	@Override
	public Accum copy() {
		AccumBool nc = new AccumBool();
		nc.vsid = this.vsid;
		nc.vectorCode = this.vectorCode;
		nc.total = this.total;
		nc.val1 = this.val1;
		nc.val2 = this.val2;
		nc.cnt1 = this.cnt1;
		nc.cnt2 = this.cnt2;
		nc.ignore = this.ignore;
		return nc;
	}
	@Override
	public void copyFrom(Accum from) {
		this.vsid = from.vsid;
		this.vectorCode = from.vectorCode;
		from.mergeInto(this);
	}
	
	// compare probabilities
	@Override
	public boolean compare(Accum ac) {
		if (ac == null) return false;
		if (total != ac.getTotal()) return false;
		if (ac.getValueCount() != this.getValueCount()) return false;
		if (ac.getCount(val1) != cnt1) return false;
		if (ac.getCount(val2) != cnt2) return false;
		return true;
	}


	@Override
	public int diff(Accum ac) {
		int cnt = super.diff(ac);
		if (ac.getCount(val1) != cnt1) cnt++;
		if (ac.getCount(val2) != cnt2) cnt++;
		return cnt;
	}
	

	@Override
	public boolean isIgnore() {
		return ignore;
	}
	@Override
	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}
	
	
	@Override
	void optimize() {
	}
	
	@Override
	void lock(long value) {
	}
	@Override
	int addCrtCount(long val, int count) {
		return 0;
	}

	@Override
	public int getCrtCount(long val) {
		return 0;
	}




}
