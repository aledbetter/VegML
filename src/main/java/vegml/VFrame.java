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

import vegml.VegML.PredictionType;
import vegml.ValProb;
import vegml.Data.VDataSet;
import vegml.Data.VDataSets;

public class VFrame {
	static final int VPLIST_BASE = 1024;
	static boolean FRAME_PADDED = true;
		
	private long [][] lframe;
	private int [][] frame;

	private int frameFocus = 0;
	private int tiers = 1;
	private int windowSize = 1;
	private int numberSetCount = 1;
	
	// dataset
	private VDataSets dss = null;
	private int dataSetNumber = 0; // if multiple 
	private int dataSetPosition = 0;
	
	private long [] vectSpace = null;
	private long [] vectSpaceCh = null;
	
	// set ids for solid models
	private int [] setIds = null;
	private Accum [] accumSpace = null;
	private VDataPlane dataplane = null;
	
	List<ValProb> vpList = null;
	List<ValProb> vpTempList = null;
		
	public VFrame() {
		
	}
	
	public VFrame(VDataPlane dp) {
		this.init(dp);
	}
	
	/**
	 * Copy the configuration from another frame
	 * 
	 * @param frame frame to copy fomr
	 */
	void copyFrom(VFrame frame) {
		this.dataplane = frame.dataplane;
		this.dataSetPosition = frame.dataSetPosition;
		this.dataSetNumber = frame.dataSetNumber;
		this.dss = frame.dss;
		
		this.frameFocus = frame.frameFocus;
		this.tiers = frame.tiers;
		this.windowSize = frame.windowSize;
		this.numberSetCount = frame.numberSetCount;
		
		for (int i=0;i<lframe.length;i++) {
			for (int x=0;x<this.lframe[0].length;x++) {
				this.lframe[i][x] = frame.lframe[i][x];
				this.frame[i][x] = frame.frame[i][x];
			}			
		}
		for (int i=0;i<setIds.length;i++) {
			this.setIds[i] = frame.setIds[i];
		}
		if (this.vpList == null) this.vpList = new ArrayList<>();
		if (this.vpTempList == null) this.vpTempList = new ArrayList<>();
		if (this.vectSpace == null) this.vectSpace = new long[frame.vectSpace.length];
		if (this.vectSpaceCh == null) this.vectSpaceCh = new long[frame.vectSpace.length];			
		if (this.accumSpace == null) this.accumSpace = new Accum[frame.vectSpace.length];
	}
	
	/**
	 * reset the frame
	 * @param dp dataplane frame is associated with
	 */
	public void reset(VDataPlane dp) {
		clear(dp);
	}
	
	/**
	 * clear the frame
	 * @param dp dataplane frame is associated with
	 */
	public void clear(VDataPlane dp) {
		frameFocus = 0;
		dataSetPosition = -1;
		dss = null;
		init(dp);
	}

	/**
	 * initialize a frame for a dataplane
	 * @param dp dataplane frame is associated with
	 */
	public void init(VDataPlane dp) {
		if (this.lframe == null || this.windowSize != dp.getCfgWindowSize()) {
			this.tiers = dp.getCfgInputDataTiers();
			this.windowSize = dp.getCfgWindowSize();
			this.numberSetCount = dp.getNSCount();
			
			this.frame = new int[tiers][windowSize];
			this.lframe = new long[tiers][windowSize];
			
			// processing space for DP
			this.setIds = new int[dp.getMappedVectorCount()];
			this.vectSpace = new long[dp.getMappedVectorCount()];
			this.vectSpaceCh = new long[dp.getMappedVectorCount()];			
			this.accumSpace = new Accum[dp.getMappedVectorCount()];

			int vsz = dp.getCfgDataWidth();
			if (vsz < 2 || vsz > VPLIST_BASE) vsz = VPLIST_BASE;
			this.vpList = new ArrayList<>(vsz);
			this.vpTempList = new ArrayList<>(vsz);
		}
		this.dataplane = dp;
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// strings if string
	
	/**
	 * get the tokens in the frame as strings, if string types
	 * @return list of strings from the frame
	 */
	public List<String> getFrameStrings() {
		List<String> frmSet = new ArrayList<>();
		int spos = (dataSetPosition - frameFocus);
		int epos = (spos + windowSize);
		
		if (dss != null && dss.isFmtDataS()) {
			VDataSet ds = dss.get(this.dataSetNumber);
			if (dss.isFmtDataD()) {
				for (int i=spos;i<epos;i++) {
					if (i < 0 || i >= ds.size()) frmSet.add(" ");
					else frmSet.add(ds.getDataSD(i)[0]);
				}				
			} else {
				for (int i=spos;i<epos;i++) {
					if (i < 0 || i >= ds.size()) frmSet.add(" ");
					else frmSet.add(ds.getDataS(i));
				}
			}
		}
		return frmSet;
	}
	
	/**
	 * get the tokens in the frame as strings, if string types
	 * add additional tokens before and after the frame from the dataset
	 * @param addition count of tokens to add before and after
	 * @return list of strings from the frame
	 */
	public List<String> getFrameStringsAdd(int addition) {
		List<String> frmSet = getFrameStrings();
		int len = (windowSize - frameFocus-1);
		if (dss != null && dss.isFmtDataS()) {
			VDataSet ds = dss.get(this.dataSetNumber);
			if (dss.isFmtDataD()) {
				// before
				for (int i=1;i<=addition;i++) {
					int pos = (this.dataSetPosition-frameFocus)-i;
					if (pos < 0) frmSet.add(0, " ");
					else frmSet.add(0, ds.getDataSD(pos)[0]);
				}			
				// after
				for (int i=1;i<=addition;i++) {
					int pos = this.dataSetPosition+i+len;		
					if (ds.size() > pos) frmSet.add(ds.getDataSD(pos)[0]);
					else frmSet.add(" ");				
				}
			} else {
				// before
				for (int i=1;i<=addition;i++) {
					int pos = (this.dataSetPosition-frameFocus)-i;
					if (pos < 0) frmSet.add(0, " ");
					else frmSet.add(0, ds.getDataS(pos));
				}			
				// after
				for (int i=1;i<=addition;i++) {
					int pos = this.dataSetPosition+i+len;		
					if (ds.size() > pos) frmSet.add(ds.getDataS(pos));
					else frmSet.add(" ");				
				}
			}
		}
		return frmSet;
	}	
	
	/**
	 * get the tokens in the frame as lowercase fucos string, if string types
	 * @return list of strings from the frame
	 */
	public List<String> getFrameStringsLower() {
		return getFrameStringsLowerAdd(0);
	}
	
	/**
	 * get the tokens in the frame as lowercase strings, if string types
	 * @return list of strings from the frame
	 */
	public List<String> getFrameStringsLowerAll() {	
		List<String> ls = getFrameStringsLowerAdd(0);
		for (int i=0;i<ls.size();i++) ls.set(i, ls.get(i).toLowerCase());
		return ls;
	}
	
	/**
	 * get the tokens in the frame as strings set focus to lowercase, if string types
	 * add additional tokens before and after the frame from the dataset
	 * @param addition count of tokens to add before and after
	 * @return list of strings from the frame
	 */
	public List<String> getFrameStringsLowerAdd(int addition) {
		if (dss == null) return null;
		List<String> frmSet = getFrameStringsAdd(addition);
		frmSet.set(this.frameFocus+addition, frmSet.get(this.frameFocus+addition).toLowerCase());
		return frmSet;
	}

	/**
	 * get the string for the value at frame focus, if string type
	 * @return string for focus input value
	 */
	public String getString() {
		if (dss != null) return dss.get(this.dataSetNumber).getDataS(this.dataSetPosition);
		return "<unk>";
	}

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// frame data
	
	/**
	 * get the frame focus
	 * @return
	 */
	public int getFrameFocus() {
		return this.frameFocus;
	}
	
	/**
	 * get the raw data for the frame
	 * @return direct access to the direct frame data
	 */
	public int [] getFrameData() {
		return this.frame[0];
	}
	
	/**
	 * get the raw data for the frame as longs
	 * @return direct access to the direct frame data
	 */
	public long [] getFrameDataLong() {
		return this.lframe[0];
	}
	
	/**
	 * get the frame data for a given tier
	 * @param tier tier to get the data from
	 * @return direct access  data frame for tier
	 */
	public int [] getFrameData(int tier) {
		return frame[tier];
	}

	/**
	 * get the frame data for a given tier long
	 * @param tier tier to get the data from
	 * @return direct access  data frame for tier
	 */
	public long [] getFrameDataLong(int tier) {		
		return lframe[tier];
	}

	
	/**
	 * Get the space in the frame for vectors long values
	 * @return direct access  vector space
	 */
	long [] getVectSpace() {
		return vectSpace;
	}
	
	/**
	 * Get the space in the frame for vectors long values
	 * @return direct access vector space
	 */
	long [] getVectSpaceCh() {
		return vectSpaceCh;
	}
	
	/**
	 * get space for set ids used in solid models
	 * @return direct access to set ids
	 */
	int [] getSetIds() {
		return this.setIds;
	}
	
	/**
	 * Accumulator space for processing and access
	 * @return direct access to the accumulator space
	 */
	Accum [] getAccumSpace() {
		return this.accumSpace;
	}
	
	/**
	 * direct full access to long frame space
	 * @return
	 */
	long [][] getLFrameFull() {
		return lframe;
	}
	
	/**
	 * direct access to long frame tier 0 space
	 * @return
	 */
	long [] getLFrame() {
		return lframe[0];
	}
	
	/**
	 * direct full access to frame space
	 * @return
	 */
	int [][] getFrameFull() {
		return frame;
	}
	
	/**
	 * direct access to frame tier 0 space
	 * @return
	 */
	int [] getFrame() {
		return frame[0];
	}
	
	/**
	 * get the count of tiers in frames
	 * @return
	 */
	int getTiers() {
		return tiers;
	}
	
	
	/**
	 * get the frame valueIds as a List
	 * @return
	 */
	public List<Long> getFrameV() {
		List<Long> ll = new ArrayList<>();
		for (long l:lframe[0]) ll.add(l);
		return ll;
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Results
	

	/**
	 * Get predictionType of best result
	 * @return prediction type of best result or Fail
	 */
	public PredictionType getPredictionType() {
		if (this.vpList == null || this.vpList.size() < 1) return PredictionType.Fail;
		return this.vpList.get(0).type;
	}

	/**
	 * Get the prediction value of the best result
	 * @return best result or 0
	 */
	public long getPredictionValue() {
		if (this.vpList == null || this.vpList.size() < 1) return 0;
		return this.vpList.get(0).value;
	}	
	
	/**
	 * Get the sorted result set for this frame
	 * @return list of ValProbs sorted
	 */
	public List<ValProb> getVPList() {
		return vpList;
	}
	
	
	/**
	 * Get the vpList for a specific NumberSet / vector number
	 * 
	 * @param setNumber number set to get for
	 * @return
	 */
	public List<ValProb> getVPListNumberSet(int setNumber) {
		if (this.dataplane.isSolid()) {
			if (setNumber >= setIds.length || setNumber < 0) return null;
			return dataplane.getVPListForSolidID(this.setIds[setNumber]);
		}
		if (setNumber >= accumSpace.length || setNumber < 0) return null;
		if (this.accumSpace[setNumber] != null) return this.accumSpace[setNumber].getValPsSorted();
		return null;
	}
	
	
	/**
	 * set the dataset, index and position
	 * @param dss current datasets
	 * @param dataSetNumber current dataset index
	 * @param dataSetPosition current dataset position
	 */
	public void setDataSet(VDataSets dss, int dataSetNumber, int dataSetPosition) { 
		this.dss = dss;
		this.dataSetNumber = dataSetNumber;
		this.dataSetPosition = dataSetPosition;
	}
	
	/**
	 * get current position in the dataset
	 * @return
	 */
	public int getDataSetPosition() {
		return this.dataSetPosition;
	}
	
	/**
	 * get current dataset index or number
	 * @return
	 */
	public int getDataSetNumber() {
		return dataSetNumber;
	}
	
	/**
	 * set the dataset index
	 * @param dataSetNumber index of current dataset
	 */
	void setDataSetNumber(int dataSetNumber) {
		this.dataSetNumber = dataSetNumber;
	}
	
	/**
	 * get the datasets in use
	 * @return
	 */
	public VDataSets getDataSet() {
		return dss;
	}
	
	/**
	 * get the dataset at setnumber
	 * @param setNumber index of dataset to get
	 * @return
	 */
	public VDataSet getDataSet(int setNumber) {
		if (dss == null) return null;
		return dss.get(setNumber);
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	// Set frame data
	//
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Set a value at a position in the frame
	 * data will be converted to valueId via the dataplane's converter
	 * 
	 * @param dp dataplane associated with frame
	 * @param framePosition position to set
	 * @param data data to place in position
	 */
	public <T> void setValue(final VDataPlane dp, final int framePosition, final T data) {
		this.lframe[0][framePosition] = dp.getCfgVToV().toVectGen(data);
		this.frame[0][framePosition] = (int)this.lframe[0][framePosition];	
	}

	/**
	 * Set a value at a position in the frame that has multiple tiers
	 * data will be converted to valueId via the dataplane's converter
	 * 
	 * @param dp dataplane associated with frame
	 * @param framePosition position to set
	 * @param data data to place in position
	 */
	public <T> void setValue(final VDataPlane dp, final int framePosition, final T [] data) {	
		for (int i=0;i<lframe.length;i++) {
			if (data.length <= i) {
				this.lframe[i][framePosition] = VegML.emptyVect;
				this.frame[i][framePosition] = VegML.emptyVect;			
				continue;
			}
			this.lframe[i][framePosition] = dp.getCfgVToV().toVectGen(data[i]);
			this.frame[i][framePosition] = (int)this.lframe[i][framePosition];			
		}
	}
	
	/**
	 * Set a value at a position in the frame, data must be valueId
	 * 
	 * @param framePosition position to set
	 * @param data valueId to place in position
	 */
	public void setValueV(final VDataPlane dp, final int framePosition, final Long data) {
		this.lframe[0][framePosition] = data;
		this.frame[0][framePosition] = (int)this.lframe[0][framePosition];	
	}

	/**
	 * Set a value at a position in the frame that has multiple tiers, data must be valueId
	 * 
	 * @param framePosition position to set
	 * @param data valueId to place in position
	 */
	public void setValueV(final int framePosition, final Long [] data) {	
		for (int i=0;i<lframe.length;i++) {
			if (data.length <= i) {
				this.lframe[i][framePosition] = VegML.emptyVect;
				this.frame[i][framePosition] = VegML.emptyVect;			
				continue;
			}
			this.lframe[i][framePosition] = data[i];
			this.frame[i][framePosition] = (int)this.lframe[i][framePosition];			
		}
	}	
	/**
	 * set the frame position to empty
	 * @param dp dataplane associated with this frame
	 * @param framePosition position in frame to set to empty
	 */
	public void setValueEmpty(VDataPlane dp, int framePosition) {
		this.frame[0][framePosition] = VegML.emptyVect;
		this.lframe[0][framePosition] = VegML.emptyVect;
	}
	public void setValueEmpty(int framePosition) {
		this.frame[0][framePosition] = VegML.emptyVect;
		this.lframe[0][framePosition] = VegML.emptyVect;
	}
	
	public void setValues(VDataPlane dp, VDataSets dss, VDataSet data, int dataSetNumber, int dataSetPosition) { 
		init(dp);
		for (int i=0;i<dp.getCfgWindowSize();i++) {
			this.lframe[0][i] = data.getDataV(dataSetPosition);
			this.frame[0][i] = (int)this.lframe[0][i];
		}
		this.frameFocus = dp.getCfgFrameFocus();
		setDataSet(dss, dataSetNumber, dataSetPosition);
	}
		
	// set the frame directly from data
	public void setValuesLS(VDataPlane dp, VDataSets dss, List<String> data, int dataSetNumber, int dataSetPosition) { 
		init(dp);
		for (int i=0;i<dp.getCfgWindowSize();i++) {
			this.lframe[0][i] = dp.getCfgVToV().toVectGen(data.get(i));
			this.frame[0][i] = (int)this.lframe[0][i];
		}	
		this.frameFocus = dp.getCfgFrameFocus();
		setDataSet(dss, dataSetNumber, dataSetPosition);
	}
	public void setValuesLV(VDataPlane dp, VDataSets dss, List<Long> dataV, int dataSetNumber, int dataSetPosition) { 
		init(dp);
		for (int i=0;i<dp.getCfgWindowSize();i++) {
			this.lframe[0][i] = dataV.get(i);
			this.frame[0][i] = (int)this.lframe[0][i];
		}
		this.frameFocus = dp.getCfgFrameFocus();
		setDataSet(dss, dataSetNumber, dataSetPosition);
	}
	public void setValuesLVD(VDataPlane dp, VDataSets dss, List<Long []> dataDepV, int dataSetNumber, int dataSetPosition) { 
		init(dp);
		for (int p=0;p<dp.getCfgWindowSize();p++) {
	 		for (int i=0;i<lframe.length;i++) {
				if (dataDepV.get(p).length <= i) {
					this.lframe[i][p] = VegML.emptyVect;
					this.frame[i][p] = VegML.emptyVect;			
					continue;
				}
	 			this.lframe[i][p] = dataDepV.get(p)[i];
	 			this.frame[i][p] = (int)this.lframe[i][p];
	 		}
		}
		this.frameFocus = dp.getCfgFrameFocus();
		setDataSet(dss, dataSetNumber, dataSetPosition);
	}
	
	/**
	 * Set the frame data from the dataset provides and position
	 * @param dp dataplane assocated with this frame
	 * @param dataSet datasets to make frame from
	 * @param dataSetNumber dataset to make frame from
	 * @param dataSetPosition position in dataset
	 */
	public void setFrame(VDataPlane dp, VDataSets dataSet, int dataSetNumber, int dataSetPosition) {
		init(dp);
		
		this.dss = dataSet;
		this.frameFocus = dp.getCfgFrameFocus();
		this.dataSetPosition = dataSetPosition;
		this.dataSetNumber = dataSetNumber;
		
		int s = dataSetPosition-dp.getCfgBefore();
		VDataSet ds = dss.get(dataSetNumber);
		if (dss.isFmtDataD()) {
			// data has dependencies
			if (s < 0 || (dataSetPosition+dp.getCfgAfter()) >= ds.size()) {
				// fill with empty
				for (int i=0;i<dp.getCfgWindowSize();i++) {
					Long [] v = null;
					if (!((s+i) >= ds.size() || (s+i) < 0)) v = dataSet.getDataLLVD(dataSetNumber, s+i);	
			 		for (int t=0;t<lframe.length;t++) {
						if (v == null || v.length <= t) this.lframe[t][i] = VegML.emptyVect;
						else this.lframe[t][i] = v[t];
						this.frame[t][i] = (int)this.lframe[t][i];
			 		}
				}
			} else {
				// make full frame
				for (int i=0;i<dp.getCfgWindowSize();i++) {
					Long [] v = dataSet.getDataLLVD(dataSetNumber, (dataSetPosition-dp.getCfgFrameFocus())+i);
			 		for (int t=0;t<lframe.length;t++) {
						if (v == null || v.length <= t) {
							this.lframe[t][i] = VegML.emptyVect;
							this.frame[t][i] = VegML.emptyVect;			
							continue;
						}
			 			this.lframe[t][i] = v[t];
			 			this.frame[t][i] = (int)this.lframe[t][i];
			 		}
				}	
			}
		} else {
			// data is list
			if (s < 0 || (dataSetPosition+dp.getCfgAfter()) >= ds.size()) {
				// fill with empty
				for (int i=0;i<dp.getCfgWindowSize();i++) {
					Long v = null;
					if (!((s+i) >= ds.size() || (s+i) < 0)) v = dataSet.getDataLLV(dataSetNumber, s+i); // always get the vector
					if (v == null) this.lframe[0][i] = VegML.emptyVect;
					else this.lframe[0][i] = v;					
					this.frame[0][i] = (int)this.lframe[0][i];
				}
			} else {
				// make full frame
				for (int i=0;i<dp.getCfgWindowSize();i++) {					
					Long v = dataSet.getDataLLV(dataSetNumber, (dataSetPosition-dp.getCfgFrameFocus())+i);
					if (v == null) v = (long)VegML.emptyVect;
					this.lframe[0][i] = v;
					this.frame[0][i] = (int)this.lframe[0][i];
				}
			}
		}
		//this.printFrameDebug();
	}
	
	/**
	 * Set the identity value to the value in dataSet
	 * @param dp dataplane associated with this frame
	 * @param framePos frame position to set
	 * @return
	 */
	public int setIdentity(VDataPlane dp, int framePos) {
		setValue(dp, framePos, dss.getDataLLV(dataSetNumber, dataSetPosition));				
		framePos++;
		return framePos;
	}

	/**
	 * Add response values generated from dataset if train/tuning or specified model in predict (or if not provided)
	 * 
	 * @param ctx context for the call
	 * @param dp dataplane framing for
	 * @param framePos position in frame start
	 * @param count count to add
	 * @param offset offset from current position to start (+/1)
	 * @param valueOut valueOut list for prior values
	 * @param predict true if this is a prediction
	 * @param sDTag dimension tag for response generator
	 * @param sDPTag dataplane tag for response generator
	 * @return position in frame end
	 */
	public int addSetResponseAfter(VContext ctx, VDataPlane dp, int framePos, int count, int offset, List<Long> valueOut, 
									boolean predict, String sDTag, String sDPTag) {

		if (count < 1) return framePos;
		// check if a set of values are suplied for the after POS
		VDataSet ds = this.getDataSet(this.getDataSetNumber());
		if (ds == null) return framePos;
		
		int x = 1;					
		int voAdd = valueOut.size();					
		if (!predict && ds.haveValues()) {
			// if dependents
			List<Long []> valSet = ds.getValueLVD();
			for (int i = 0; i < count;i++) {
				if (valSet.size() > (this.getDataSetPosition()+x+offset)) {
					// resolve this and add to the out for better resolve of next ones
					Long [] dv = valSet.get(this.getDataSetPosition()+x+offset);
					if (dv != null) this.setValue(dataplane, framePos, dv[0]);						
					else this.setValueEmpty(dataplane, framePos);
				} else {
					this.setValueEmpty(dataplane, framePos);
				}
				framePos++;
				x++;
			}
		} else {
			// 1) change or set the handler before the call 			
			ctx.addCallout(dataplane);
		
			for (int i = 0; i < count;i++) {
				if (ds.size() > (this.getDataSetPosition()+x+offset)) {
					// resolve this and add to the out for better resolve of next ones
					String dv = this.getDataSet().getDataLLS(dataSetNumber, this.getDataSetPosition()+x+offset);
					if (dv == null || !dv.equals(" ")) {						
						// check cache
						List<ValProb> vpl = null;
						vpl = VegTest.predictVP(ctx, sDTag, sDPTag, valueOut, this.getDataSet().get(dataSetNumber), this.getDataSetPosition()+x, dataSetNumber);
						if (vpl == null) {
							this.setValueEmpty(dataplane, framePos);
						} else {
							ValProb vp = vpl.get(0);
							vp.counter = this.getDataSetPosition()+x+offset;
							this.setValue(dataplane, framePos, vp.value);
						}
					} else {
						this.setValueEmpty(dataplane, framePos);
					}					
				} else {
					this.setValueEmpty(dataplane, framePos);
				}
				framePos++;
				x++;
				// clean up
				while (voAdd < valueOut.size()) valueOut.remove(valueOut.size()-1); // remove anything that is added
			}
			ctx.removeCallout(dataplane);
		}
		return framePos;
	}

	/**
	 * Add response values before the focus from the valueOut
	 * 
	 * @param ctx context in
	 * @param dp dataplane framing for
	 * @param framePos position in frame
	 * @param count count to add
	 * @param offset offset from current position to start (+/1)
	 * @param valueOut prior value out list
	 * @return position in frame at end
	 */
	public int addSetResponseBefore(VContext ctx, VDataPlane dp, int framePos, int count, int offset, List<Long> valueOut) {
		if (count < 1) return framePos;
		int voAdd = valueOut.size();					
		for (int i = (count-1); i >= 0; i--) {		
			int p = voAdd-(1+i)+offset;
			if (p < voAdd && p >= 0) {
				this.setValue(dataplane, framePos, valueOut.get(p));
				//System.out.print("x["+dp.getString(valueOut.get(p))+"]("+p+")");
			}
			//else this.setValueEmpty(dataplane, framePos);
			framePos++;

		}
		//System.out.println(" ---- " + voAdd);
		return framePos;
	}
	
	/**
	 * Add count values from data set to the frame starting at framePos
	 * this will take count values BEFORE the datasetposition
	 * @param framePos position in frame to start
	 * @param count count to add
	 * @return new frame position
	 */
	public int addSetDataBefore(int framePos, int count) {
		for (int j=count;j>0;j--) {
			int p = dataSetPosition-j;
			Long v = (long)VegML.emptyVect;
			if (p >= 0) v = dss.getDataLLV(dataSetNumber, dataSetPosition-j);
			if (v == null) v = (long)VegML.emptyVect;
			this.lframe[0][framePos] = v;
			this.frame[0][framePos] = (int)this.lframe[0][framePos];
			framePos++;
		}

		return framePos;
	}

	/**
	 * Add count values from data set to the frame starting at framePos
	 * this will take count values AFTER the datasetposition
	 * @param framePos position in frame to start
	 * @param count count to add
	 * @return new frame position
	 */
	public int addSetDataAfter(int framePos, int count) {
		if (count < 1) return framePos;
		for (int i=1;i <= count && i <= windowSize;i++) {					
			Long v = dss.getDataLLV(dataSetNumber, dataSetPosition+i);
			if (v == null) v = (long)VegML.emptyVect;
			this.lframe[0][framePos] = v;
			this.frame[0][framePos] = (int)this.lframe[0][framePos];
			framePos++;
		}
		return framePos;
	}

	/**
	 * Set the frame as complete, should be done at the end of framing
	 * @param dp dataplane associated with this frame
	 */
	public void setComplete(VDataPlane dp) { 
		this.dataplane = dp;
		this.frameFocus = dp.getCfgFrameFocus();
	}

	/**
	 * Get count of vectors that were generated
	 * NOTE: this is SLOW
	 * @return count of vecors
	 */
	public int getVectorCount() {
		int cnt = 0;
		for (int i=0;i<this.vectSpace.length;i++) {
			if (vectSpace[i] != -1 && vectSpace[i] != -2 && vectSpace[i] != 0) cnt++;
		}
		return cnt;
	}
	
	/**
	 * Get the vector ID (vid) for a numberset
	 * @param setNumber numberset to check
	 * @return vid or 0
	 */
	public long getVectorId(int setNumber) {
		if (setNumber >= vectSpace.length || setNumber < 0) return 0;
		if (vectSpace[setNumber] == -1) return 0;
		if (vectSpace[setNumber] == -2) return 0;
		if (vectSpace[setNumber] == 0) return 0;
		return vectSpace[setNumber];
	}
	
	/**
	 * get the used accumulator account
	 * NOTE: this is SLOW
	 * @return count of filled accumulators
	 */
	public int getAccumCount() {
		int cnt = 0;
		for (int i=0;i<this.accumSpace.length;i++) {
			if (accumSpace[i] != null) cnt++;
		}
		return cnt;
	}

	/**
	 * get the frame strings as a single string to print
	 * @return
	 */
	public String getStringDebug() {
		return VegUtil.toStringListSeg(this.getFrameStrings());
	}
	
	/**
	 * Show the frame information
	 */
	public void print() {
		print(false);
	}
	
	/**
	 * Show the frame information and optionally the result
	 * @param showResults true to show results info per numberSet and combind
	 */
	public void print(boolean showResults) {		
		synchronized (dataplane) {
			
		System.out.println("FrameData["+this.dataSetPosition+"]["+this.dataSetNumber+"]");				
		if (this.dss != null) {	
			if (!this.dss.isFmtDataV()) {
				// show frame in dataset inputs: will be odd if not focus
				for (int fp=0;fp<frame[0].length;fp++) {
					if (this.getFrameFocus() == fp) System.out.print("   xDS["+fp+"]");
					else System.out.print("    DS["+fp+"]");
					int p = this.dataSetPosition;
					if (fp < this.getFrameFocus()) p -= this.getFrameFocus() - fp;
					else if (fp > this.getFrameFocus()) p += fp - this.getFrameFocus();
					System.out.println("  "+this.dss.getString(dataSetNumber, p));
				}
			}
			
			// show per position V: will be odd if not focus
			System.out.println("     -------["+this.dss.getFmtData()+"]["+this.dss.getFmtValue()+"]");
			for (int fp=0;fp<frame[0].length;fp++) {
				if (this.getFrameFocus() == fp) System.out.print("   xV["+fp+"]");
				else System.out.print("    V["+fp+"]");
				int p = this.dataSetPosition;
				if (fp < this.getFrameFocus()) p -= this.getFrameFocus() - fp;
				else if (fp > this.getFrameFocus()) p += fp - this.getFrameFocus();
				System.out.println("  "+this.dss.getStringV(dataSetNumber, p));
			}
			System.out.println("     -------");
		}
		
		// tier/position
		for (int fp=0;fp<frame[0].length;fp++) {
			if (this.getFrameFocus() == fp) System.out.print("    x["+fp+"]");
			else System.out.print("     ["+fp+"]");
			
			if (frame.length == 1) {
				String s = null;
				if (frame[0][fp] == VegML.emptyVect) s = frame[0][fp]+"e";	
				else if (dataplane != null) s = dataplane.getString(frame[0][fp]);
				if (s == null) s = String.format("%10d", frame[0][fp]);	
				System.out.print("  "+s);	
			} else {
				for (int x=0;x<frame.length;x++) {
					String s = null;
					if (frame[x][fp] == VegML.emptyVect) s= String.format("%9d", frame[x][fp])+"e";	
					else if (dataplane != null) s = dataplane.getString(frame[x][fp]);
					if (s == null) s = String.format("%10d", frame[x][fp]);					
					System.out.print("  "+x+"["+s+"]");	
				}
			}
			System.out.println("");	
		}	
		
		if (showResults && this.dataplane != null) {
			System.out.println(" ------- NS Results -------");
			// show per numberSet
			for (int i=0;i<this.accumSpace.length;i++) {
				List<ValProb> vpl = this.getVPListNumberSet(i);
				if (vpl == null || vpl.size() == 0) System.out.println("     ns["+i+"] (0)  vid("+getVectorId(i)+")");	
				else {
					System.out.print("     ns["+i+"] ("+vpl.size()+") =>");	
					// show a few results
					for (int n=0;n<vpl.size() && n<5;n++) {
						String s = dataplane.getString(vpl.get(n).value);
						if (s == null) s = "<"+vpl.get(n).value+">";
						System.out.print(" ["+n+"] " + s);	
					}
					System.out.println("");		
				}
			}			
			System.out.println(" ------- Results -------");
			// combind
			System.out.print("     vplist["+this.vpList.size()+"] ");	
			if (this.vpList.size() > 0) {
				System.out.print("["+vpList.get(0).type+"] => ");	
				// show a few results
				for (int i=0;i<this.vpList.size() && i<5;i++) {
					String s = dataplane.getString(vpList.get(i).value);
					if (s == null) s = "<"+vpList.get(i).value+">";
					System.out.print(" ["+i+"] " + s);	
				}
			} 
			System.out.println("");		
		}
		}
	}
	
	
}
