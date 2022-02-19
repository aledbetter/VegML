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


package vegml.Data;

import java.util.List;

import vegml.VegML;
import vegml.Data.VDataSet.RangeTag;

public class VFrameUtil {

	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeBoth(int prefix, int suffix, VDataSet ds, RangeTag rt, List<Long> outFrame) {
		outFrame.clear();
		if (rt == null || rt.getLength() < 1) return false;		
		// add prefix
		frameBlockEdgeStart(prefix, ds, rt, outFrame, false);
		// add suffix
		frameBlockEdgeEnd(prefix, ds, rt, outFrame, false);	
		return true;
	}
	
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeStart(int suffix, VDataSet ds, RangeTag rt, List<Long> outFrame) {
		return frameBlockEdgeStart(suffix, ds, rt, outFrame, true);
	}
	public static boolean frameBlockEdgeStart(int prefix, VDataSet ds, RangeTag rt, List<Long> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (rt == null || rt.getLength() < 1) return false;
		}
		
		int c = 0;
		// add prefix
		for (int i=0;i<prefix && i < rt.getLength();i++) {
			Long v = ds.getDataV(rt.getStart()+i);
			outFrame.add(v);
			c++;
		}
		// fill center if not long enough
		while (c < prefix) {
			outFrame.add((long)VegML.emptyVect);
			c++;
		}
		return true;
	}
	
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeEnd(int suffix, VDataSet ds, RangeTag rt, List<Long> outFrame) {
		return frameBlockEdgeEnd(suffix, ds, rt, outFrame, true);
	}
	public static boolean frameBlockEdgeEnd(int suffix, VDataSet ds, RangeTag rt, List<Long> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (rt == null || rt.getLength() < 1) return false;
		}
		int c = suffix - rt.getLength();
		int a = 0;
		
		// fill center if not long enough
		while (c > 0) {
			outFrame.add((long)VegML.emptyVect);
			c--;
			a++;
		}
		c = suffix - a;
		
		// add suffix
		for (int i=0;i<rt.getLength() && i<c;i++) {
			int pos = rt.getLength()-(c-i);
			Long v = ds.getDataV(rt.getStart()+pos);
			outFrame.add(v);
		}
		
		return true;
	}
	
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeBoth(int prefix, int suffix, List<Long> block, List<Long> outFrame) {
		outFrame.clear();
		if (block == null || block.size() < 1) return false;
		
		// add prefix
		frameBlockEdgeStart(prefix, block, outFrame, false);
		// add suffix
		frameBlockEdgeEnd(prefix, block, outFrame, false);	
		
		return true;
	}
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeStart(int suffix, List<Long> block, List<Long> outFrame) {
		return frameBlockEdgeStart(suffix, block, outFrame, true);
	}
	public static boolean frameBlockEdgeStart(int prefix, List<Long> block, List<Long> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (block == null || block.size() < 1) return false;
		}
		
		int c = 0;
		// add prefix
		for (int i=0;i<prefix && i < block.size();i++) {
			outFrame.add(block.get(i));
			c++;
		}
		// fill center if not long enough
		while (c < prefix) {
			outFrame.add((long)VegML.emptyVect);
			c++;
		}
		return true;
	}
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeEnd(int suffix, List<Long> block, List<Long> outFrame) {
		return frameBlockEdgeEnd(suffix, block, outFrame, true);
	}
	public static boolean frameBlockEdgeEnd(int suffix, List<Long> block, List<Long> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (block == null || block.size() < 1) return false;
		}
		int c = suffix - block.size();
		int a = 0;
		
		// fill center if not long enough
		while (c > 0) {
			outFrame.add((long)VegML.emptyVect);
			c--;
			a++;
		}
		c = suffix - a;
		
		// add suffix
		for (int i=0;i<block.size() && i<c;i++) {
			int pos = block.size()-(c-i);
			outFrame.add(block.get(pos));
		}
		
		return true;
	}
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeBothS(int prefix, int suffix, List<String> block, List<String> outFrame) {
		outFrame.clear();
		if (block == null || block.size() < 1) return false;
		
		// add prefix
		frameBlockEdgeStartS(prefix, block, outFrame, false);
		// add suffix
		frameBlockEdgeEndS(prefix, block, outFrame, false);	
		
		return true;
	}
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param prefix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeStartS(int suffix, List<String> block, List<String> outFrame) {
		return frameBlockEdgeStartS(suffix, block, outFrame, true);
	}
	public static boolean frameBlockEdgeStartS(int prefix, List<String> block, List<String> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (block == null || block.size() < 1) return false;
		}
		
		int c = 0;
		// add prefix
		for (int i=0;i<prefix && i < block.size();i++) {
			outFrame.add(block.get(i));
			c++;
		}
		// fill center if not long enough
		while (c < prefix) {
			outFrame.add(" ");
			c++;
		}
		return true;
	}
	
	/**
	 * Frame a block for Edges
	 * 
	 * @param suffix
	 * @param block
	 * @param outFrame
	 * @return
	 */
	public static boolean frameBlockEdgeEndS(int suffix, List<String> block, List<String> outFrame) {
		return frameBlockEdgeEndS(suffix, block, outFrame, true);
	}
	public static boolean frameBlockEdgeEndS(int suffix, List<String> block, List<String> outFrame, boolean clear) {
		if (clear) {
			outFrame.clear();
			if (block == null || block.size() < 1) return false;
		}
		int c = suffix - block.size();
		int a = 0;
		
		// fill center if not long enough
		while (c > 0) {
			outFrame.add(" ");
			c--;
			a++;
		}
		c = suffix - a;
		
		// add suffix
		for (int i=0;i<block.size() && i<c;i++) {
			int pos = block.size()-(c-i);
			outFrame.add(block.get(pos));
		}
		
		return true;
	}
	
}
