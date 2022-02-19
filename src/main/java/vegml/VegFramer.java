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

import vegml.Data.VDataSet;
import vegml.Data.VDataSetDescriptor.DSDataType;
import vegml.Data.VDataSets;
import vegml.VegML.NumberSetType;
import vegml.VegML.PredictionType;
import vegml.ValProb;


/**
 * 
 * Framing implementation object
 * maps the data into slots in the frame
 * Override this class to create a new framer
 */
public class VegFramer implements java.io.Serializable {
	private static final long serialVersionUID = 2063014556096156681L;
	
	public VegFramer() {}
	
	/**
	 * setup AND make the frame with the framer call
	 */
	boolean makeFrameSetup(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData,  
							boolean predict, List<Long> valueOut, VDataSets dss, int dataSetNumber, int dataPosition) {
		// default just uses the data			
		frame.reset(dataplane);
		frame.setDataSet(dss, dataSetNumber, dataPosition);
		return makeFrame(ctx, dataplane, frame, frameData, predict, valueOut, dss, dataSetNumber, dataPosition);
	}
	
	/**
	 * Override this method to make the frame, it will be called with the data, frame and position in data
	 * if it returns false, the frame will not be evaluated
	 * 
	 * @param ctx context in use
	 * @param dataplane dataplane in use
	 * @param frame frame to update
	 * @param frameArgData frame argument
	 * @param predict true if this is predict, else false (for training / etc)
	 * @param valueOut list of prior result first valuesIds returned
	 * @param dataSet set of datasets
	 * @param dataSetNumber the dataset number
	 * @param dataPosition the dataset position
	 * @return true to evaluate frame, false to ignore this frame
	 */
	public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameArgData, 
							boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
		// default just uses the data			
		frame.setFrame(dataplane, dataSet, dataSetNumber, dataPosition);
		return true;
	}	
	
	/**
	 * get new numberSets for incrementing the windowSize
	 * @param dataplane
	 * @param optType
	 * @param positionNumber
	 * @param after
	 * @param numberSets
	 * @param newNsSet
	 * @return
	 */
	public int getIncWindowSizeNS(VDataPlane dataplane, PredictionType optType, int positionNumber, boolean after, List<Integer> numberSets, List<List<Integer>> newNsSet) {	
		return dataplane.addIncWindowSizeNS(optType, positionNumber, after, numberSets, newNsSet);
	}	
	
	
	/**
	 * Framer - response framing
	 * NOTE setting the args allows for training responses to be used in training OR testing for the FOLLOWING values (much faster)
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param srcDTag
	 * @param srcDPTag
	 */
	static void setCfgFramerResponse(VegML vML, final String dimensionTag, final String dataPlaneTag, final String srcDTag, final String srcDPTag) {
		// to save resolved values 
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "valrep-set", new ArrayList<ValProb>());
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "srcdtag", srcDTag);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "srcdptag", srcDPTag);
		
		VegFramer vf = new VegFramer() {
			@Override
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
									boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
				
				String sDTag = dataplane.getCfgScratchString("srcdtag");
				String sDPTag = dataplane.getCfgScratchString("srcdptag");			
				ctx.addCallout(dataplane);
				
				int framePos = 0;				
				// add n prior responses
				if (false) {
					framePos = frame.addSetResponseBefore(ctx, dataplane, framePos, (dataplane.getCfgFrameFocus()-1), 0, valueOut);
				} else {
					int voAdd = valueOut.size();					
					for (int i = (dataplane.getCfgFrameFocus()-1); i >= 0; i--) {		
						int p = voAdd-(1+i);
						if (p < voAdd && p >= 0) frame.setValue(dataplane, framePos, valueOut.get(p));
						else frame.setValueEmpty(dataplane, framePos);
						framePos++;
					}
				}
				
				// the token
				Long tv = dataSet.getDataLLV(dataSetNumber, dataPosition);
				int voAdd = valueOut.size();					
				if (tv != VegML.emptyVect) {
					List<ValProb> vpl = VegTest.predictVP(ctx, sDTag, sDPTag, valueOut, dataSet.get(frame.getDataSetNumber()), dataPosition, frame.getDataSetNumber());
					if (vpl == null || vpl.size() < 1) System.out.println("ERROR: FAIL-TEXT/POS[@"+dataPosition+"] "+dataplane.getDimensionTag()+"/"+dataplane.getTag()+" not trained yet" );
					ValProb vp = vpl.get(0);
					frame.setValue(dataplane, framePos, vp.value);
				} else {
					frame.setValueEmpty(dataplane, framePos);
				}
				while (voAdd < valueOut.size()) valueOut.remove(valueOut.size()-1); // remove anything that is added
			
				framePos++;				
				
				
				int x = 1;					
				// check if a set of values are suplied for the after POS
				VDataSet ds = frame.getDataSet(frame.getDataSetNumber());
				if (!predict && ds.haveValues()) {
					List<Long []> valSet = ds.getValueLVD();
					for (int i = (dataplane.getCfgFrameFocus()+1); i < dataplane.getCfgWindowSize();i++) {
						if (valSet.size() > (dataPosition+x)) {
							// resolve this and add to the out for better resolve of next ones
							Long [] dv = valSet.get(dataPosition+x);
							if (dv != null) frame.setValue(dataplane, framePos, dv[0]);						
							else frame.setValueEmpty(dataplane, framePos);
						} else {
							frame.setValueEmpty(dataplane, framePos);
						}
						framePos++;
						x++;
					}
				} else {
					// 1) change or set the handler before the call 				
					for (int i = (dataplane.getCfgFrameFocus()+1); i < dataplane.getCfgWindowSize();i++) {
						if (ds.size() > (dataPosition+x)) {
							// resolve this and add to the out for better resolve of next ones
							String dv = dataSet.getDataLLS(dataSetNumber, dataPosition+x);
							if (dv == null || !dv.equals(" ")) {						
								// check cache
								List<ValProb> vpl = VegTest.predictVP(ctx, sDTag, sDPTag, valueOut, dataSet.get(dataSetNumber), dataPosition+x, dataSetNumber);
								//if (vpl == null || vpl.size() < 1) {
								//	vpl = VegTest.predictVP(ctx, "affix", "pos", valuesOut, dataSet, dataPosition+x, frame.getDataSetNumber());
								//}
								if (vpl == null) return false;
								ValProb vp = vpl.get(0);
								vp.counter = dataPosition+x;
								frame.setValue(dataplane, framePos, vp.value);
							} else {
								frame.setValueEmpty(dataplane, framePos);
							}					
						} else {
							frame.setValueEmpty(dataplane, framePos);
						}
						framePos++;
						x++;
						// clean up
						while (voAdd < valueOut.size()) valueOut.remove(valueOut.size()-1); // remove anything that is added
					}
				}
				ctx.removeCallout(dataplane);

				//System.out.println("FRM1["+VegUtil.toStringList(frameOutRaw)+"]["+frameOutRaw.size()+"] ["+dataSet.get(dataPosition)+"]");
				frame.setComplete(dataplane);
				return true;
			}
		};
		vML.setCfgFramer(dimensionTag, dataPlaneTag, "response", vf);	
	}

	/**
	 * Framer - mix response framing
	 * NOTE setting the args allows for training responses to be used in training OR testing for the FOLLOWING values (much faster)
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param srcDTag
	 * @param srcDPTag
	 */
	public static void setCfgFramerResponseMix(VegML vML, final String dimensionTag, final String dataPlaneTag, final String srcDTag, final String srcDPTag) {
		// to save resolved values 
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "valrep-set", new ArrayList<ValProb>());
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "srcdtag", srcDTag);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "srcdptag", srcDPTag);
		
		VegFramer vf = new VegFramer() {
			@Override
			//	public boolean makeFrame(VContext ctx, DataPlane dataplane, VFrame frame, long trainValue, List<Long> valuesOut, 
						//List<String> dataSet, int dataPosition, String dataValue, Object frameData) {
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
								boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {

				int framePos = 0;				
				// add n prior responses
				if (false) {
					framePos = frame.addSetResponseBefore(ctx, dataplane, framePos, (dataplane.getCfgFrameFocus()-1), 0, valueOut);
				} else {
					int voAdd = valueOut.size();					
					for (int i = (dataplane.getCfgFrameFocus()-1); i >= 0; i--) {		
						int p = voAdd-(1+i);
						if (p < voAdd && p >= 0) frame.setValue(dataplane, framePos, valueOut.get(p));
						else frame.setValueEmpty(dataplane, framePos);
						framePos++;
					}
				}

				// the token			
				framePos = frame.setIdentity(dataplane, framePos);

				// use text after
				if (false) {
				//	String sDTag = dataplane.getCfgScratchString("srcdtag");
				//	String sDPTag = dataplane.getCfgScratchString("srcdptag");	
				//	framePos = frame.addSetDataAfter(dataplane, framePos, dataplane.getCfgWindowSize()-(dataplane.getCfgFrameFocus()+1));
				} else {					
					// check if a set of values are suplied for the after POS
					VDataSet ds = frame.getDataSet(frame.getDataSetNumber());
					if (ds == null) return false;
					
					int x = 1;					
					int voAdd = valueOut.size();					
					if (!predict && ds.haveValues()) {
						List<Long []> valSet = ds.getValueLVD();
						for (int i = (dataplane.getCfgFrameFocus()+1); i < dataplane.getCfgWindowSize();i++) {
							if (valSet.size() > (dataPosition+x)) {
								// resolve this and add to the out for better resolve of next ones
								Long [] dv = valSet.get(dataPosition+x);
								if (dv != null) frame.setValue(dataplane, framePos, dv[0]);						
								else frame.setValueEmpty(dataplane, framePos);
							} else {
								frame.setValueEmpty(dataplane, framePos);
							}
							framePos++;
							x++;
						}
					} else {
						// 1) change or set the handler before the call 
						String sDTag = dataplane.getCfgScratchString("srcdtag");
						String sDPTag = dataplane.getCfgScratchString("srcdptag");					
						ctx.addCallout(dataplane);
					
						for (int i = (dataplane.getCfgFrameFocus()+1); i < dataplane.getCfgWindowSize();i++) {
							if (ds.size() > (dataPosition+x)) {
								// resolve this and add to the out for better resolve of next ones
								String dv = dataSet.getDataLLS(dataSetNumber, dataPosition+x);
								if (dv == null || !dv.equals(" ")) {						
									// check cache
									List<ValProb> vpl = null;
									vpl = VegTest.predictVP(ctx, sDTag, sDPTag, valueOut, dataSet.get(dataSetNumber), dataPosition+x, dataSetNumber);
									//if (vpl == null || vpl.size() < 1) {
									//	vpl = VegTest.predictVP(ctx, "affix", "pos", valuesOut, dataSet, dataPosition+x, frame.getDataSetNumber());
									//}
									if (vpl == null) return false;
									ValProb vp = vpl.get(0);
									vp.counter = dataPosition+x;
									frame.setValue(dataplane, framePos, vp.value);
								} else {
									frame.setValueEmpty(dataplane, framePos);
								}					
							} else {
								frame.setValueEmpty(dataplane, framePos);
							}
							framePos++;
							x++;
							// clean up
							while (voAdd < valueOut.size()) valueOut.remove(valueOut.size()-1); // remove anything that is added
						}
						ctx.removeCallout(dataplane);
					}
				}
				//System.out.println("FRM1["+VegUtil.toStringList(frameOutRaw)+"]["+frameOutRaw.size()+"] ["+dataSet.get(dataPosition)+"]");
				frame.setComplete(dataplane);
				
				//if (dataplane.getCfgWindowSize() > 2) frame.printFrameDebug(dataplane);
				return true;
			}
		};
		vML.setCfgFramer(dimensionTag, dataPlaneTag, "mix", vf);	
	}

	/**
	 * Framer - breakdown to character for edge mode training
	 *	
	 * FeatureExtend: alow additional positions with a callback to get value?
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param ignorePrefix
	 * @param minLen
	 * @param includeIdentity
	 * @param includeFormat
	 * @param maxPrefix
	 */
	public static void setCfgFramerCharEdge(VegML vML, final String dimensionTag, final String dataPlaneTag, 
										final int minLen, final boolean includeIdentity, final boolean includeFormat,
										final int maxPrefix) {
		
		vML.setCfgInputDataType(dimensionTag, dataPlaneTag, DSDataType.Char);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "prefix", 0);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "suffix", 0);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "minLength", minLen);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "incIdentity", includeIdentity);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "incFormat", includeFormat);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "maxPrefix", maxPrefix);
		
		VegFramer vf = new VegFramer() {
			private static final boolean newPosRequired = false; // don't try things again
			@Override
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
								boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
			
				String dataValue = dataSet.getDataLLS(dataSetNumber, dataPosition);
				if (dataValue == null) return false;		
										
				int pre = dataplane.getCfgScratchInt("prefix");
				int suf = dataplane.getCfgScratchInt("suffix");
				int minLength = dataplane.getCfgScratchInt("minLength");
				int framePos = 0;			
				char[] ch = dataValue.toCharArray();
		//		char[] ch = dataValue.toLowerCase().toCharArray();
				
				boolean affix = true;
				if (dataValue.length() < minLength) affix = false;
				
				// or not a word..
				if (affix) {
					// prefix
					for (int p=0;p<pre;p++) {
						if (p >= ch.length) {
							frame.setValueEmpty(dataplane, framePos);
						} else {
							frame.setValue(dataplane, framePos, ch[p]);	
						}
						framePos++;
					}
					// suffix
					for (int p=0;p<suf;p++) {
						int pos = (ch.length-suf)+p;
						if (pos < 0) {
							frame.setValueEmpty(dataplane, framePos);
						} else {
							frame.setValue(dataplane, framePos, ch[pos]);	
						}
						framePos++;
					}
				} else {
					int full = pre+suf;
					for (int p=0;p<full;p++) {
						frame.setValueEmpty(dataplane, framePos);
						framePos++;
					}
				}
	
				// add full token here
				if (dataplane.getCfgScratchBool("incIdentity")) {
					Long tv = dataSet.getDataLLV(dataSetNumber, dataPosition);
					frame.setValue(dataplane, framePos, tv);
					framePos++;
				}
				
				// format info
				if (dataplane.getCfgWindowSize() > 2 && dataplane.getCfgScratchBool("incFormat")) {
			//		frame.setValue(dataplane, framePos, VegUtil.getStringFormat(dataValue));
					frame.setValue(dataplane, framePos, VegUtil.getStringFormatMin(dataValue));
					framePos++;
				}
				
				frame.setComplete(dataplane);
				//System.out.println(" [@"+frame.getDataSetPosition()+"]x["+pre+"/"+suf+"]f["+frame.getCfgFrameFocus()+"] " + VegUtil.toStringListSeg(frms));
				return true;
			}
			@Override			
			// get new numberSets for incrementing the windowSize
			// NOTE: this currently re-trys prior numbersets.. could be changed
			public int getIncWindowSizeNS(VDataPlane dataplane, PredictionType optType, int positionNumber, boolean after, List<Integer> numberSets, List<List<Integer>> newNsSet) {
				// default just uses the data	
				int wsize = dataplane.getCfgWindowSize();
				boolean pid = dataplane.getCfgScratchBool("incIdentity");
				boolean pfmt = dataplane.getCfgScratchBool("incFormat");
				int maxpre = dataplane.getCfgScratchInt("maxPrefix");
				if (wsize < 2) pfmt = false; // not on identity
				if (pid) wsize--; // identity is added after
				if (pfmt) wsize--; // format is added after
				
				// determine the split point
				int mid = wsize / 2;
				//  max prefix
				if (maxpre > 0 && mid > maxpre) {
					mid= maxpre;
				}
				
				int aft = wsize - mid;
				int pre = mid;
				int newPosition = mid;
				if (mid > 1 && after) {
					newPosition--;
				}
				List<List<Integer>> pset = MLNumberSetUtil.getSequenceEdgeSets(wsize, mid);
				if (wsize == 0) {
					mid = -1;
					pre = aft = 0;
				}

				// reduce only to new numberSets (not from prior)
				for (int i=0;i<pset.size();i++) {
					if (pset.get(i).size() < 1) {
					//if (!pset.get(i).contains(newPosition)) {
						//System.out.println(" RM: "+NumberSets.setToStringPosition(pset.get(i), wsize, wsize));
						pset.remove(i);
						i--;
					}
				}			
				//System.out.println(" ADD["+wsize+"]["+dataplane.getCfgWindowSize()+"] new["+newPosition+"]rm["+rm+"]  sets["+pset.size()+"]");

				// add links to identity
				if (pid) {
					// FIXME
				}				
				// add links to format
				if (pfmt) {
					List<List<Integer>> nsAList = new ArrayList<>();
					for (int i=0;i<pset.size();i++) {
						//System.out.println(" xx: "+NumberSets.setToStringPosition(pset.get(i), wsize, wsize));
						List<Integer> ms = new ArrayList<>(pset.get(i));
						ms.add(dataplane.getCfgWindowSize()-1); // last
						nsAList.add(ms);
					}
					List<Integer> fns = new ArrayList<>();
					fns.add(dataplane.getCfgWindowSize()-1);
					pset.add(fns);				
					pset.addAll(nsAList);
				}
				// add full numberSet
				List<Integer> fns = new ArrayList<>();
				for (int i=0;i<dataplane.getCfgWindowSize();i++) fns.add(i);
				pset.add(fns);	
				
				// remove fullest: this is expanded
				if (dataplane.getCfgWindowSize() > 2 && dataplane.getCfgNSFullNumber() >= 0) {
					if (dataplane.getNS(dataplane.getCfgNSFullNumber()).size() == dataplane.getCfgWindowSize()) {
						dataplane.removeCfgNS(dataplane.getCfgNSFullNumber());
					}
				}			
				//System.out.println("");
				//System.out.println(" AdjustNS Char["+positionNumber+"]mid["+mid+"]after["+after+"] window["+wsize+"]["+dataplane.getCfgWindowSize()+"] nsopt["+pset.size()+"] ["+dataplane.getNSBaseType()+"]");	

				// adjust current
				if (mid >= 0) {
					for (int i=0;i<dataplane.getNSCount();i++) {
						// FIXME tier update too
						List<Integer> nSet = dataplane.getNS(i);
						for (int x=0;x<nSet.size();x++) {
							int v = nSet.get(x);
							if (v >= mid) nSet.set(x, v+1);						
						}
						dataplane.getNSHash(i).setNumberSet(nSet, null);
						//System.out.println("   N1S: " + dataplane.getNSFormatString(i));		
					}
				}
				
				// adjust focus
				if (pid) {
					if (pfmt) dataplane.setCfgFrameFocus(dataplane.getCfgWindowSize()-2);
					else dataplane.setCfgFrameFocus(dataplane.getCfgWindowSize()-1);
				}
				
				int idp = dataplane.getCfgIdentityPosition();  // identity position

				// mix in the new
				for (int ns=0;ns < pset.size();ns++ ) {
					List<Integer> nl = pset.get(ns);
					//System.out.println("   ns["+ns+"]("+dataplane.getCfgFrameFocus()+") " + NumberSets.setToStringPosition(nl, dataplane.getCfgWindowSize(), dataplane.getCfgFrameFocus()));		

					// get list that have this position
					if (dataplane.findCfgNS(nl) >= 0) continue;
					
					if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {						
						if (nl.contains(idp)) continue; // no identity
					} 
					
					// must have new position
					if (newPosRequired && !nl.contains(newPosition)) {
						continue;
					}
					if (dataplane.getCfgRegion() > 0) {
						// must be <= the region size; controls complexity of dependent relationships
						if (nl.size() != dataplane.getCfgWindowSize() && nl.size() > dataplane.getCfgRegion()) {
							continue;
						}
					}
					
					int mns = ns;
					// add this set to the dataPlane: use the NS returned from the add
					mns = dataplane.addCfgNSDefined(nl, -1);
					//System.out.println("    mns["+mns+"] " + NumberSets.setToStringPosition(nl, dataplane.getCfgWindowSize(), dataplane.getCfgFrameFocus()));		
					// add it if number set
					if (mns >= 0 && !numberSets.contains(mns)) {
						numberSets.add(mns);
						newNsSet.add(nl);
					}
				}
				
				// done
				dataplane.updateCfgNS();
				//dataplane.print(true);
				//System.out.println(" END NEW["+numberSets.size()+"]");	
				//System.out.println("");
				
				dataplane.setCfgScratch("prefix", pre);
				dataplane.setCfgScratch("suffix", aft);	

				return numberSets.size();
			}				
		};
		
		vML.setCfgFramer(dimensionTag, dataPlaneTag, "character", vf);
	}

	/**
	 * Framer - breakdown sentence for edge mode training
	 * Each dataset is expected to be a sentence
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 * @param includeIdentity
	 * @param includeFormat
	 */
	public static void setCfgFramerSentence(VegML vML, final String dimensionTag, final String dataPlaneTag, 
										final boolean includeIdentity, final boolean includeFormat) {
		
		vML.setCfgInputDataType(dimensionTag, dataPlaneTag, DSDataType.String);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "prefix", 0);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "suffix", 0);
		
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "incIdentity", includeIdentity);
		vML.setCfgScratch(dimensionTag, dataPlaneTag, "incFormat", includeFormat);
		
		VegFramer vf = new VegFramer() {
			@Override
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
								boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
			
				// ignore any?
					// FIXME
				
				VDataSet ds = frame.getDataSet(dataSetNumber);
				
				int pre = dataplane.getCfgScratchInt("prefix");
				int suf = dataplane.getCfgScratchInt("suffix");
				int framePos = 0;			
						
				// prefix / start
				for (int p=0;p<pre;p++) {
					if (p >= ds.size()) {
						frame.setValueEmpty(dataplane, framePos);
					} else {
						frame.setValue(dataplane, framePos, ds.getValueVD(p));	
					}
					framePos++;
				}
				// suffix / end (exclude final punctuation FIXME)
				for (int p=0;p<suf;p++) {
					int pos = (ds.size()-suf)+p;
					if (pos < 0) {
						frame.setValueEmpty(dataplane, framePos);
					} else {
						frame.setValue(dataplane, framePos, ds.getValueVD(pos));	
					}
					framePos++;
				}
				
				// final punctuation -> normalized: ?!?!/.../."/".
				// FIXME
	
				// add full token here
				if (dataplane.getCfgScratchBool("incIdentity")) {
					Long tv = dataSet.getDataLLV(dataSetNumber, dataPosition);
					frame.setValue(dataplane, framePos, tv);
					framePos++;
				}
				
				// format info
			//	if (dataplane.getCfgWindowSize() > 2 && dataplane.getCfgScratchBool("incFormat")) {
			//		frame.setValue(dataplane, framePos, VegUtil.getStringFormat(dataValue));
			//		framePos++;
			//	}
				
				frame.setComplete(dataplane);
				//System.out.println(" [@"+frame.getDataSetPosition()+"]x["+pre+"/"+suf+"]f["+frame.getCfgFrameFocus()+"] " + VegUtil.toStringListSeg(frms));
				return true;
			}
			@Override			
			// get new numberSets for incrementing the windowSize
			// NOTE: this currently re-trys prior numbersets.. could be changed
			public int getIncWindowSizeNS(VDataPlane dataplane, PredictionType optType, int positionNumber, boolean after, List<Integer> numberSets, List<List<Integer>> newNsSet) {
				// default just uses the data	
				int wsize = dataplane.getCfgWindowSize();
				boolean pid = dataplane.getCfgScratchBool("incIdentity");
				boolean pfmt = dataplane.getCfgScratchBool("incFormat");
				if (wsize < 2) pfmt = false; // not on identity
				if (pid) wsize--; // identity is added after
				if (pfmt) wsize--; // format is added after
				
				// determine the split point
				int mid = wsize / 2;
				int aft = wsize - mid;
				int pre = mid;
				int newPosition = mid;
				if (mid > 1 && after) {
					newPosition--;
				}
				List<List<Integer>> pset = MLNumberSetUtil.getSequenceEdgeSets(wsize, mid);
				if (wsize == 0) {
					mid = -1;
					pre = aft = 0;
				}

				// reduce only to new numberSets (not from prior)
				for (int i=0;i<pset.size();i++) {
					if (pset.get(i).size() < 1) {
					//if (!pset.get(i).contains(newPosition)) {
						//System.out.println(" RM: "+NumberSets.setToStringPosition(pset.get(i), wsize, wsize));
						pset.remove(i);
						i--;
					}
				}			
				//System.out.println(" ADD["+wsize+"]["+dataplane.getCfgWindowSize()+"] new["+newPosition+"]rm["+rm+"]  sets["+pset.size()+"]");

				// add links to identity
				if (pid) {
					// FIXME
				}				
				// add links to format
				if (pfmt) {
					List<List<Integer>> nsAList = new ArrayList<>();
					for (int i=0;i<pset.size();i++) {
						//System.out.println(" xx: "+NumberSets.setToStringPosition(pset.get(i), wsize, wsize));
						List<Integer> ms = new ArrayList<>(pset.get(i));
						ms.add(dataplane.getCfgWindowSize()-1); // last
						nsAList.add(ms);
					}
					List<Integer> fns = new ArrayList<>();
					fns.add(dataplane.getCfgWindowSize()-1);
					pset.add(fns);				
					pset.addAll(nsAList);
				}
				// add full numberSet
				List<Integer> fns = new ArrayList<>();
				for (int i=0;i<dataplane.getCfgWindowSize();i++) fns.add(i);
				pset.add(fns);	
				
				// remove fullest: this is expanded
				if (dataplane.getCfgWindowSize() > 2 && dataplane.getCfgNSFullNumber() >= 0) {
					if (dataplane.getNS(dataplane.getCfgNSFullNumber()).size() == dataplane.getCfgWindowSize()) {
						dataplane.removeCfgNS(dataplane.getCfgNSFullNumber());
					}
				}			
				//System.out.println("");
				//System.out.println(" AdjustNS Char["+positionNumber+"]mid["+mid+"]after["+after+"] window["+wsize+"]["+dataplane.getCfgWindowSize()+"] nsopt["+pset.size()+"] ["+dataplane.getNSBaseType()+"]");	

				// adjust current
				if (mid >= 0) {
					for (int i=0;i<dataplane.getNSCount();i++) {
						// FIXME tier update too
						List<Integer> nSet = dataplane.getNS(i);
						for (int x=0;x<nSet.size();x++) {
							int v = nSet.get(x);
							if (v >= mid) nSet.set(x, v+1);						
						}
						dataplane.getNSHash(i).setNumberSet(nSet, null);
						//System.out.println("   N1S: " + dataplane.getNSFormatString(i));		
					}
				}
				
				// adjust focus
				if (pid) {
					if (pfmt) dataplane.setCfgFrameFocus(dataplane.getCfgWindowSize()-2);
					else dataplane.setCfgFrameFocus(dataplane.getCfgWindowSize()-1);
				}
				
				int idp = dataplane.getCfgIdentityPosition();

				// mix in the new
				for (int ns=0;ns < pset.size();ns++ ) {
					List<Integer> nl = pset.get(ns);
					//System.out.println("   ns["+ns+"]("+dataplane.getCfgFrameFocus()+") " + NumberSets.setToStringPosition(nl, dataplane.getCfgWindowSize(), dataplane.getCfgFrameFocus()));		

					// get list that have this position
					if (dataplane.findCfgNS(nl) >= 0) continue;
					
					if (optType == PredictionType.AnyUnknown || optType == PredictionType.PredictUnknown) {						
						if (nl.contains(idp)) continue; // no identity
					} 
					// must have new position
					if (!nl.contains(newPosition)) {
						continue;
					}
					int mns = ns;
					// add this set to the dataPlane: use the NS returned from the add
					mns = dataplane.addCfgNSDefined(nl, -1);
					//System.out.println("    mns["+mns+"] " + NumberSets.setToStringPosition(nl, dataplane.getCfgWindowSize(), dataplane.getCfgFrameFocus()));		
					// add it if number set
					if (mns >= 0 && !numberSets.contains(mns)) {
						numberSets.add(mns);
						newNsSet.add(nl);
					}
				}
				
				// done
				dataplane.updateCfgNS();
				//dataplane.print(true);
				//System.out.println(" END NEW["+numberSets.size()+"]");	
				//System.out.println("");
				
				dataplane.setCfgScratch("prefix", pre);
				dataplane.setCfgScratch("suffix", aft);	

				return numberSets.size();
			}				
		};
		
		vML.setCfgFramer(dimensionTag, dataPlaneTag, "sentence", vf);
	}

	
	/**
	 * Framer - for blocks, each VDataSet == single block
	 * framing is based on DataPlans numberset type, default is Edge
	 * 
	 * @param vML
	 * @param dimensionTag
	 * @param dataPlaneTag
	 */
	public static void setCfgFramerBlockEdge(VegML vML, final String dimensionTag, final String dataPlaneTag) {
		// to save resolved values 
		VDataPlane dp = vML.getDataPlane(dimensionTag, dataPlaneTag);
		final NumberSetType nst = dp.getNSBaseType();
		final int prefix = dp.getCfgScratchInt("prefix");
		final int suffix = dp.getCfgScratchInt("suffix");
		final int ws = dp.getCfgWindowSize();
		
		VegFramer vf = new VegFramer() {
			@Override
			//	public boolean makeFrame(VContext ctx, DataPlane dataplane, VFrame frame, long trainValue, List<Long> valuesOut, 
						//List<String> dataSet, int dataPosition, String dataValue, Object frameData) {
			public boolean makeFrame(VContext ctx, VDataPlane dataplane, VFrame frame, Object frameData, 
								boolean predict, List<Long> valueOut, VDataSets dataSet, int dataSetNumber, int dataPosition) {
				
				VDataSet ds = dataSet.get(dataSetNumber);
				
				boolean id = false;
				int frmPos = 0;
				frame.clear(dp);
				if (nst == NumberSetType.SequenceEdgeId) {
					frmPos = frameBlockEdgeStart(prefix, ds, frame, frmPos);
					frmPos = frameBlockEdgeEnd(suffix, ds, frame, frmPos);
					id = true;					
				} else if (nst == NumberSetType.SequenceLeft) {
					frmPos = frameBlockEdgeStart(ws, ds, frame, frmPos);
				} else if (nst == NumberSetType.SequenceLeftId) {
					frmPos = frameBlockEdgeStart(ws-1, ds, frame, frmPos);
					id = true;
				} else if (nst == NumberSetType.SequenceRight) {
					frmPos = frameBlockEdgeEnd(ws, ds, frame, frmPos);
				} else if (nst == NumberSetType.SequenceRightId) {
					frmPos = frameBlockEdgeEnd(ws-1, ds, frame, frmPos);
					id = true;
			//	} else if (nst == NumberSetType.SequenceFan) {
					// FIXME ?? what would be?
				} else {
					//nst == NumberSetType.SequenceEdge
					frmPos = frameBlockEdgeStart(prefix, ds, frame, frmPos);
					frmPos = frameBlockEdgeEnd(suffix, ds, frame, frmPos);
				}
				if (id) {
					// get String for full dataset
					String s = ds.getDataAsString();
					long v = (long)dp.getCfgVToV().toVectGen(s); // set that string as a vid
					frame.setValue(dp, ws-1, v);
				}
				
				//System.out.println("FRM1["+VegUtil.toStringList(frameOutRaw)+"]["+frameOutRaw.size()+"] ["+dataSet.get(dataPosition)+"]");
				frame.setComplete(dataplane);
				
				//if (dataplane.getCfgWindowSize() > 2) frame.printFrameDebug(dataplane);
				return true;
			}
		};
		vML.setCfgFramer(dimensionTag, dataPlaneTag, "mix", vf);	
	}
	
	public static int frameBlockEdgeStart(int prefix, VDataSet ds, VFrame frame, int fpos) {
		int frmPos = 0;
		// add prefix
		for (int i=0;i<prefix && i < ds.size();i++) {
			Long [] v = ds.getValueVD(i);
			frame.setValueV(fpos+frmPos, v);
			frmPos++;
		}
		// fill center if not long enough
		while (frmPos < prefix) {
			frame.setValueEmpty(fpos+frmPos);
			frmPos++;
		}
		return frmPos+fpos;
	}
	
	public static int frameBlockEdgeEnd(int suffix, VDataSet ds, VFrame frame, int fpos) {
		int c = suffix - ds.size();
		int frmPos = 0;
		
		// fill center if not long enough
		while (c > 0) {
			frame.setValueEmpty(fpos+frmPos);
			c--;
			frmPos++;
		}
		c = suffix - frmPos;
		
		// add suffix
		for (int i=0;i<ds.size() && i<c;i++) {
			int pos = ds.size()-(c-i);
			Long [] v = ds.getValueVD(pos);
			frame.setValueV(fpos+frmPos, v);
			frmPos++;
		}
		return frmPos+fpos;
	}

}
