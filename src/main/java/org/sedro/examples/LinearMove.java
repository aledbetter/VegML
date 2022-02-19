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


package org.sedro.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import vegml.VDataPlane;
import vegml.VContext;
import vegml.VegML;
import vegml.ValProb;
import vegml.VegTest;
import vegml.VegTrain;

/*
 * test move from integer start to integer goal then stop. both points are selected randomly.
 * This is a very simple test of training model for controlling linear motion. A few modifications
 * are added to allow positive and negative training.
 * 
 * train with:
 *  - getBetter() producing a number indicating current status of move
 *  - getScore() producing the number of moves once complete
 *  - train true/false for each move
 *  - moves allowed: ++/--/==
 *  - second pass for optimize testing adds: +2/-2/nop
 *  - variable window size for history
 *  - options on selection method for next move when not exact match
 *  	- all positives, all positives and unknowns, all without negative, all
 *  - 
 */
public class LinearMove {
	static long vp = 0, vm = 0, vp2 = 0, vm2 = 0, veq = 0;
	static boolean allOptionsAlways = false;
	static boolean noNegative = true;	
	static int MAX_SCORE = 1000;
	
	public static void main(String [] args) {
		int min = 1, max = 100;
		int window = 2;
		VegML.showCopywrite();
			
		///////////////////////
		// setup positive and negative DataPlans
		VegML vML = new VegML("move1-"+window); 
		vML.setCfgDescription("train move from a to b positive");		
		vML.addDataPlane("op", "val", window, 0, 3); // option
		vML.setCfgIdentityOnly("op", "val", false);
		vML.setCfgNoEmptyElements("op", "val", false);
		vML.setCfgDefaultDataPlane("op", "val");
		
		vML.setCfgDescription("train move from a to b negative");		
		vML.addDataPlane("opn", "val", window, 0, 3); // option
		vML.setCfgIdentityOnly("op", "val", false);
		vML.setCfgNoEmptyElements("opn", "val", false);

		
		// TODO: add dataset definition with all options
		// make list of all options (FIXME: this should be in the DP datasetdefinition)
		List<Long> allOptions = new ArrayList<>();
		VDataPlane dp = vML.getDataPlane("op", "val");
		vp = (long)dp.getCfgVToV().toVectGen("--");
		vm = (long)dp.getCfgVToV().toVectGen("++");
		vp2 = (long)dp.getCfgVToV().toVectGen("+2");
		vm2 = (long)dp.getCfgVToV().toVectGen("-2");
		veq = (long)dp.getCfgVToV().toVectGen("==");
		allOptions.add(veq);
		allOptions.add(vp);
		allOptions.add(vm);
		// step 2: faster option
		allOptions.add(vm2);
		allOptions.add(vp2);
			
		// create OR train model for > < == and use it as a filter		
		// FIXME: this model should use computers ability to work with numbers, but provide probalistic interface
		
		//////////////////////////
		// TRAIN
		// once before, once after
		trainLinear(vML, allOptions, 20, 7);
		trainLinear(vML, allOptions, 10, 21);
		// if train all posible then better... but its a big list
	//	trainLinear(vML, allOptions, 21, 7);
	//	trainLinear(vML, allOptions, 66, 47);
	//	trainLinear(vML, allOptions, 99, 120);
	//	trainLinear(vML, allOptions, 40, 10);
	//	trainLinear(vML, allOptions, 900, 980);
	//	trainLinear(vML, allOptions, 11, 21);
		vML.print(true);
		
		//////////////////////////
		// TEST
		for (int i=0;i<100;i++) {
			int start = ThreadLocalRandom.current().nextInt(min, max + 1);
			int goal = ThreadLocalRandom.current().nextInt(min, max + 1);
			testLinear(i, vML, start, goal);
		}
	}
	
	//////////////////////////
	// test it
	static void testLinear(int test, VegML vML, int start, int goal) {
		VDataPlane dp = vML.getDataPlane("op", "val");
		VDataPlane dpn = vML.getDataPlane("op", "val");
		System.out.print("TEST["+dp.getDimensionTag()+"/"+dp.getTag()+"] goal["+String.format("%5d", start)+"  >>  "+String.format("%5d", goal)+"] window["+dp.getCfgWindowSize()+"] => ");
		// train via test
		List<Long> frmVals = new ArrayList<>(dp.getCfgWindowSize());
		long [] frmVect = new long[dp.getCfgWindowSize()];
		
		List<Long> valOutp = new ArrayList<>();
		List<Long> valOut = new ArrayList<>();
		VContext ctx = new VContext(dp);
				
		int position = start;		
		boolean done = false;
		int score = 0;
		while (!done) {	
			/////////////////////////
			// make frame window 2
			frmVals.clear();
			int dist = goal - position;
			frmVect[0] = getDistVal(dp, dist);
			if (valOut.size() > 0) frmVect[1] = valOut.get(valOut.size()-1);
			else frmVect[1] = (long)VegML.emptyVect;			
			frmVals.add(frmVect[0]);
			frmVals.add(frmVect[1]);			
			
			ValProb bvp = null;
			/////////////////////////
			// get best option AND generate positive and negative training values
			List<ValProb> vpFList = dp.getFullVpList(frmVect);
			if (vpFList != null && vpFList.size() > 0) {	
				bvp = vpFList.get(0); // have Full answer
	//			if (vpFList.size() > 1) System.out.println("  @0["+score+"] position["+position+"]>["+goal+"] val["+bvp.value+"]["+vpFList.get(1).value+"] ["+vpFList.size()+"]  " + veq);

			} else {			
				// get positive
				List<ValProb> vpList = VegTest.predictFrameVPV(ctx, "op", "val", valOutp, frmVals);
				// get negative (should be exact negative only?
				List<ValProb> vpListn = dpn.getFullVpList(frmVect);
				//List<ValProb> vpListn = VegTest.predictFrameVPV(ctx, "opn", "val", valOut, frmVals);					
				if (noNegative) {
					// First that is not negative
					for (ValProb vp:vpList) {
						if (ValProb.find(vpListn, vp.value) == null) {
							bvp = vp;
							break;
						}
					}
				} 
				//System.out.println("  @1["+score+"] position["+position+"]>["+goal+"] val["+bvp.value+"] " + veq);
			}
			if (bvp == null) {
				// FAIL!! did not find a way out
				System.out.println("FAILED at["+position+"] steps["+score+"]");
				break;
			}

			// move: update position
			position = applyOption(bvp.value, position, goal);
			valOut.add(bvp.value);			
			score++;
			
			// think done?
			if (bvp.value == veq) {
				if (position == goal) {
					System.out.println("COMPLETE in steps["+score+"]");
					break;
				} else {
					// fail
					System.out.println("FAILED == at["+position+"] steps["+score+"]");
					break;
				}
			}			

			if (score > MAX_SCORE) {
				System.out.println("FAIL in steps["+score+"]");
				break;
			}
		}
	}
	
	//////////////////////////
	// train it
	static void trainLinear(VegML vML, List<Long> allOptions, int start, int goal) {
		VDataPlane dp = vML.getDataPlane("op", "val");
		VDataPlane dpn = vML.getDataPlane("op", "val");
		System.out.print("TRAIN["+dp.getDimensionTag()+"/"+dp.getTag()+"] goal["+String.format("%5d", start)+"  >>  "+String.format("%5d", goal)+"] window["+dp.getCfgWindowSize()+"] => ");
		
		// train via test
		List<Long> frmVals = new ArrayList<>(dp.getCfgWindowSize());
		long [] frmVect = new long[dp.getCfgWindowSize()];
		
		List<Long> valOutp = new ArrayList<>();
		List<Long> valOut = new ArrayList<>();
		List<Long> tryList = new ArrayList<>();
		List<Long[]> ptrainVals = new ArrayList<>();
		List<Long[]> ntrainVals = new ArrayList<>();
		VContext ctx = new VContext(dp);
				
		int position = start;		
		boolean done = false;
		int score = 0;
		while (!done) {	
			/////////////////////////
			// make frame window 2
			frmVals.clear();
			int dist = goal - position;
			frmVect[0] = getDistVal(dp, dist);
			if (valOut.size() > 0) frmVect[1] = valOut.get(valOut.size()-1);
			else frmVect[1] = (long)VegML.emptyVect;			
			frmVals.add(frmVect[0]);
			frmVals.add(frmVect[1]);			
			
			
			/////////////////////////
			// get best option AND generate positive and negative training values
			List<ValProb> vpFList = dp.getFullVpList(frmVect);
			if (vpFList != null && vpFList.size() > 0) {
				// have Full answer
				for (ValProb vp:vpFList) tryList.add(vp.value);
				//System.out.println("FULL["+score+"]["+valOut.size()+"] " + vpFList.size());
			} else {			
				// get positive
				List<ValProb> vpList = VegTest.predictFrameVPV(ctx, "op", "val", valOutp, frmVals);
				// get negative 
				List<ValProb> vpListn = dpn.getFullVpList(frmVect);
				//List<ValProb> vpListn = VegTest.predictFrameVPV(ctx, "opn", "val", valOut, frmVals);
				tryList.clear();
						
				if (vpList == null || vpList.size() == 0) {
					// all options always
					tryList.addAll(allOptions);
					//System.out.println("NONE["+score+"] ");
				} else if (allOptionsAlways) {
					// all options always
					tryList.addAll(allOptions);
				} else if (noNegative) {
					// all options except negative
					//System.out.println("SOME["+score+"] " + vpList.size());
					for (ValProb vp:vpList) {
						if (ValProb.find(vpListn, vp.value) != null) continue;
						tryList.add(vp.value);
					}
					// add unknowns
					for (Long val:allOptions) {
						if (ValProb.find(vpListn, val) != null) continue;
						if (ValProb.find(vpList, val) != null) continue;
						tryList.add(val);
					}
				}
			}
			
			// try options these; make training list
			int bestPosition = position;
			int startScore = getBetter(position, goal);
			int bestScore = startScore;
			long bestVal = -1;
			for (Long val:tryList) {
				int npos = applyOption(val, position, goal);
				int bet = getBetter(npos, goal);
				
				boolean m = false;
				if (bet < bestScore) m = true; 
				else if (startScore == 0 && bet == startScore) m = true; // matching end
				
				if (m) {
					bestPosition = npos;
					bestScore = bet;
					bestVal = val;	
					// positive, but maybe not the best
					// FIXME
				} else if (bet <= startScore) {
					// is negative
					Long [] tval = new Long[1];
					tval[0] = val;	
					ntrainVals.add(tval);
				}
			}
			if (bestVal == -1) {
				// FAIL!! did not find a way out
				System.out.println("FAILED at["+position+"] steps["+score+"]");
				break;
			}
			//System.out.println("  OP["+score+"]v["+bestVal+"]s["+bestScore+"]    ["+position+" vs "+bestPosition+"]");
			
			// train best val
			Long [] tval = new Long[1];
			tval[0] = bestVal;
			ptrainVals.add(tval);			
			// move: update position
			position = bestPosition;
			valOut.add(bestVal);		
			
			/////////////////////////
			// train models
			for (Long [] ptval:ptrainVals) {
				// train in the best option (block or frame)
				VegTrain.trainDataFrameL(vML, "op", "val", null, frmVals, ptval);
			}
			for (Long [] ntval:ntrainVals) {
				// train in the best option (block or frame)
				VegTrain.trainDataFrameL(vML, "opn", "val", null, frmVals, ntval);				
			}
			// clear
			ptrainVals.clear();
			ntrainVals.clear();
			score++;
			
			// is complete: must predict end
			if (position == goal && bestVal == veq) { 	
				done = true;
			}
		}
	
		// score is just the count
		System.out.println("COMPLETE in steps["+score+"]");
	}
	
	// get value (should come from trained model)
	static long getDistVal(final VDataPlane dp, final int dist) {
		if (dist == 0) return 1;
		// need these?
		if (dist > 1) return 2;
		if (dist < -1) return 3;
		if (dist > 0) return 4;
		return 5;
	}

	// apply option and get new position
	static int applyOption(final long option, final int position, final int goal) {
		if (option == veq) return position;
		if (option == vp) return position -1;
		if (option == vp2) return position -2;
		if (option == vm2) return position +2;
		return position +1;
	}
	
	// producing a number indicating current status of move
	static int getBetter(final int position, final int goal) {
		// ABS of diff
		if (position == goal) return 0;
		if (position >= goal) return (position-goal)+1;
		return (goal-position) +1;
	}

}
