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
import java.util.concurrent.ConcurrentHashMap;

import vegml.VDRetainSet.DsRetainVal;
import vegml.VegTest.TestMod;
import vegml.VegTest.TestModSet;
import vegml.Data.VDataSets;


class MLThreadUtil {
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// Thread Pool
	private static final int TOTAL_THREADS = 20;
	private static int threadcount = TOTAL_THREADS;
	static private List<Thread> theadList = null;
	static private boolean thruning = false;
	static class workItem {
		int cmd;
		Object item;
		int pos;
		ConcurrentHashMap<Integer, Object> outList;
	}
	static private List<workItem> workItemList = null;

	// work item
	static class test extends workItem {
		public VDataPlane dp;
		public TestModSet modtest;
		public int currentTotPass;
		List<List<DsRetainVal>> rsdSet = null;
		boolean noRsv;
		boolean recordInfo = false;
		VDataSets dss = null;
		public double mergeValue;
		
		test(int position, VDataPlane dp, TestModSet modtest, List<List<DsRetainVal>> rsdSet, VDataSets dss, boolean noRsv, boolean recordInfo) {
			this.dp = dp;
			this.modtest = modtest;
			this.rsdSet = rsdSet;
			this.noRsv = noRsv;
			this.recordInfo = recordInfo;
			this.pos = position;
			this.dss = dss;
		}
	}
	static List<VResultSet> runTestPredictModify(VDataPlane dp, List<Object> set, TestModSet modtest, List<List<List<DsRetainVal>>> rsdSets, List<VDataSets> dssl, boolean noRsv) {
		set.clear();
		for (int xx=0;xx<dssl.size();xx++) {
			set.add(new test(xx, dp,  modtest, rsdSets.get(xx), dssl.get(xx), noRsv, false));
		}
		return processSetRS(set, 1);
	}
	public static List<VResultSet> runTestPredictModify(VDataPlane dp, List<Object> set, TestModSet modtest, List<VDataSets> dssl, boolean noRsv, boolean recordInfo) {
		set.clear();
		for (int xx=0;xx<dssl.size();xx++) {
			set.add(new test(xx, dp,  modtest, null, dssl.get(xx), noRsv, recordInfo));
		}
		return processSetRS(set, 3);
	}

	static VResultSet runtestSetsDsRetain(VDataPlane dp, List<VDataSets> dssl) {
		List<Object> set = new ArrayList<>();
		for (int xx=0;xx<dssl.size();xx++) {
			set.add(new test(xx, dp, null, null, dssl.get(xx), false, false));
		}
		List<VResultSet> rs = processSetRS(set, 4);
		return rs.get(0);
	}

	public static VResultSet runTrainDataSets(VDataPlane dp, List<VDataSets> dssl) {
		List<Object> set = new ArrayList<>();
		for (int xx=0;xx<dssl.size();xx++) {
			set.add(new test(xx, dp, null, null, dssl.get(xx), false, false));
		}
		List<VResultSet> rs = processSetRS(set, 11);
		dp.removeAllEmptyAccum();
		return rs.get(0);
	}
	
	
	static VResultSet runTestPredictFull(VDataPlane dp, List<Object> set, double mergeValue, List<VDataSets> dssl) {
		set.clear();
		for (int xx=0;xx<dssl.size();xx++) {
			test tp = new test(xx, dp, null, null, dssl.get(xx), false, false);
			tp.mergeValue = mergeValue;
			set.add(tp);
		}
		List<VResultSet> rs = processSetRS(set, 90);
		return rs.get(0);
	}
	
	public static VResultSet runTestSetsFull(VDataPlane dp, List<VDataSets> dssl) {
		List<Object> set = new ArrayList<>();
		for (int xx=0;xx<dssl.size();xx++) {
			test tp = new test(xx, dp, null, null, dssl.get(xx), false, false);
			tp.mergeValue = 0;
			set.add(tp);
		}
		List<VResultSet> rs = processSetRS(set, 90);
		return rs.get(0);
	}
	
	
	// 
	// do something
	// move complete items from inList to outList
	//
	static private void processWorkItem(workItem wi, int position) {
		if (wi.item == null) return;
		Object out = null;
		
		if (wi.cmd == 1) {
			test tsp = (test)wi.item;
			// process predict
			out = VDRetainSet.testSetsModify(tsp.dp, tsp.modtest, tsp.rsdSet, tsp.noRsv);			
		} else if (wi.cmd == 3) {
			test tsp = (test)wi.item;
			// process predict
			out = VegTest.testSetsModify(tsp.dp.getVegML(), tsp.dp.getDimensionTag(), tsp.dp.getTag(), tsp.modtest, tsp.dss, true, tsp.recordInfo, false);	
		} else if (wi.cmd == 4) {
			test tsp = (test)wi.item;			
			TestModSet dsrTest = new TestModSet();
			dsrTest.add(tsp.dp, new TestMod(tsp.dp.getCfgNSWeightRaw()));
			out = VegTest.testSetsModify(tsp.dp.getVegML(), tsp.dp.getDimensionTag(), tsp.dp.getTag(), dsrTest, tsp.dss, false, false, true);
		} else if (wi.cmd == 11) {
			test tsp = (test)wi.item;
			VResultSet rs = VegTrain.trainDataSets(tsp.dp.getVegML(), tsp.dp.getDimensionTag(), tsp.dp.getTag(), tsp.dss, true);	
			List<VResultSet> rl = new ArrayList<>();
			rl.add(rs);
			out = rl;
		} else if (wi.cmd == 90) {
			// process predict
			test tp = (test)wi;
			VResultSet rs = VegTest.testSets(tp.dp.getVegML(), tp.dp.getDimensionTag(), tp.dp.getTag(), tp.dss);
			List<VResultSet> rl = new ArrayList<>();
			rl.add(rs);
			out = rl;
		} else {
			out = wi.item;
			// FIXME process this predictionSet
		}
		wi.outList.put(position, out);
	}	
	static public List<VResultSet> processSetRS(List<Object> set, int cmd) {	
		ConcurrentHashMap<Integer, Object> rl = processSet(set, cmd);
		// merge results
		List<VResultSet> rModList = null;
		int i=0;
		while (true) {
			Object r = rl.get(i);
			if (r == null) break;	
			@SuppressWarnings("unchecked")
			List<VResultSet> ml = (List<VResultSet>)r;
			if (rModList == null) rModList = ml;
			else {
				for (int xx=0;xx<ml.size();xx++) rModList.get(xx).add(ml.get(xx));
			}
			i++;
		}
		//System.out.println(" MOD_DONE["+rModList.size()+"]");
		return rModList;		
	}

	// process this set with the pool
	static public ConcurrentHashMap<Integer, Object> processSet(List<Object> set, int cmd) {	
		if (theadList == null) {
			loadThreads(threadcount);
		}
		
		//System.out.println("processList["+total+"] " + sess.getContext());
		int total = set.size();
		ConcurrentHashMap<Integer, Object> listComplete = new ConcurrentHashMap<>();

		// add each to work items
		for (int i=0;i<set.size();i++) {
			Object x = set.get(i);
			addWorkItem(cmd, (workItem)x, listComplete, i == (set.size()-1));
		}

		// wait for complete
		while (listComplete.keySet().size() < total) {
		//	System.out.println("  work-out["+listComplete.keySet().size()+"]["+total+"]");
			// check if more time
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) { }
		}
		return listComplete;
	}
	
	// add some work
	static private void addWorkItem(int cmd, workItem wi, ConcurrentHashMap<Integer, Object> outList, boolean signal) {
		wi.cmd = cmd;
		wi.outList = outList;
		wi.item = wi;
		synchronized (workItemList) {
			workItemList.add(wi);
			if (signal) workItemList.notifyAll();
			//if (workItemList.size() == 1) workItemList.notifyAll();
		}
	}
	static private void loadThreads(int count) {
		theadList = new ArrayList<>();
		workItemList = new ArrayList<>();
		thruning = true;
		for (int i=0;i<count;i++) addThread();
		//System.out.println("STARTED Threads["+count+"] ");
	}
	static void endThreads() {
		if (!thruning) return;
		synchronized (workItemList) {
			if (workItemList == null) return;
			thruning = false;
			workItemList.notifyAll();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) { }
		theadList = null;
		workItemList = new ArrayList<>();
		//System.out.println("STARTED Threads["+count+"] ");
	}
	
	// add async processing...
	static private Thread addThread() {
		// add thread / timer to process this
		Thread thread = new Thread("Thread:rss") {
			public void run() {
				// would need to have work list... with all params in the global context
				while (thruning) {
					workItem wi = getNextWorkItem();
					if (wi != null) {
						processWorkItem(wi, wi.pos);
					} else {
						synchronized (workItemList) {
						try {
							workItemList.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						}
					}
				}
			}
		};
		thread.start();
		synchronized (theadList) {
			theadList.add(thread);
		}		
		return thread;
	}	
	static private workItem getNextWorkItem() {
		synchronized (workItemList) {
			if (workItemList.size() > 0) return workItemList.remove(0);
			return null;
		}	
	}
	
}
