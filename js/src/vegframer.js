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

/**
 * 
 * Framing implementation object
 * maps the data into slots in the frame
 * Override this class to create a new framer
 */
export default class VegFramer {

	//
	// Data Framers
	//
	static frameToken(ctx, frame, valueOut, dataSet, dataPosition, dataValue, dataSetNumber) {
		frame.setFrame(ctx.vML.defDP, dataSet, dataPosition);
		return true;
	}
	static frameChar(ctx, frame, valueOut, dataSet, dataPosition, dataValue, dataSetNumber) {
		// FIXME char left AND char right
		return true;
	}
	// call the frame function for the default dataplane
	static makeFrameSetup(ctx, frame, valueOut, dataSet, dataPosition, dataValue, dataSetNumber) {
		frame.clear(ctx.vML.defDP);
		frame.setDataSet(dataSet, dataPosition, dataSetNumber);
		return ctx.vML.defDP.framer(ctx, frame, valueOut, dataSet, dataPosition, dataValue, dataSetNumber);
	}	
}




