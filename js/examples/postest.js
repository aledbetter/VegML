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
//
// Select RPM model: number is window, bigger number = larger and slightly more accurate
//
//var RPMmodelName = 'models/test-solid-2.json';
var RPMmodelName = 'models/test-solid-3.json';
//var RPMmodelName = 'models/test-solid-4.json';
//var RPMmodelName = 'models/test-solid-5.json';
var vML = null;

console.log("RPM Model: " + RPMmodelName);

fetch(RPMmodelName, {mode: 'cors'})
	.then(response => response.json())
  	.then(data => {loadRPM(data);});

//
// Load Model from JSON function
//
function loadRPM(data) {
	console.log("loading RPM: " + data.tag);
	vML = new veg.VegML(data);
}

// with lib
//vML = VegML.load(RPMmodelName);

// on load complete
var button = null;
window.addEventListener('load', function () {
	// button click
	button = document.getElementById("process");
	button.addEventListener("click", doClick);
});


// clicked it
function doClick(event){
	let v = document.getElementById("data").value;
	var stokens = v.trim().split(/\s+/);
	var tokens = new Array();

	// comma/period/etc are all still with the token if no whitespace -< 10 min hack job parser
	for (let i = 0; i< stokens.length;i++) {
		var tok = stokens[i];
		if (tok.endsWith('.') || tok.endsWith(',') || tok.endsWith('!')  || tok.endsWith('?')
			|| tok.endsWith(':') || tok.endsWith(';') || tok.endsWith('/"') || tok.endsWith("/'")
			|| tok.endsWith(')') || tok.endsWith('}') || tok.endsWith(']')) {
			tokens.push(tok.substring(0, tok.length-1));
			tokens.push(tok.substring(tok.length-1, tok.length));
		} else if (tok.startsWith('$') || tok.endsWith('#') || tok.endsWith(',') || tok.endsWith('/"') || tok.endsWith("/'")
			|| tok.endsWith('(')|| tok.endsWith('{')|| tok.endsWith('[')) {
			tokens.push(tok.substring(0, 1));
			tokens.push(tok.substring(1, tok.length));
		} else if (tok.endsWith("'all") || tok.endsWith("'til")) {
			tokens.push(tok.substring(0, tok.length-4));
			tokens.push(tok.substring(tok.length-4, tok.length));
		} else if (tok.endsWith("n't") || tok.endsWith("'re") || tok.endsWith("'ve") || tok.endsWith("'ll") || tok.endsWith("'em")) {
			tokens.push(tok.substring(0, tok.length-3));
			tokens.push(tok.substring(tok.length-3, tok.length));
		} else if (tok.endsWith("'m") || tok.endsWith("'s") || tok.endsWith("'d") || tok.endsWith("'m")) {
			tokens.push(tok.substring(0, tok.length-2));
			tokens.push(tok.substring(tok.length-2, tok.length));
		} else {
			tokens.push(tok);
		}
	}
	document.getElementById("results").innerHTML = "Processing "+tokens.length+" tokens...";
	
	var dataSets = new Array();
	dataSets.push(tokens);
	var rs = veg.VegTest.predictSets(vML, dataSets);
	
	
	console.log(" DONE["+rs.responseOut.length+"] of ["+rs.total+"]");
	var res = document.getElementById("results");
	document.getElementById("results").innerHTML = "";
	for (let i=0;i<rs.responseOut[0].length;i++) {
		let pos = vML.addStringMapping(rs.responseOut[0][i]);
		//console.log("  V["+tokens[i]+"] >> ["+pos+"]");
		document.getElementById("results").innerHTML += tokens[i]+ "/"+pos+"&nbsp;&nbsp;";
		if (i > 0 && (i%10) == 0) document.getElementById("results").innerHTML += "<br>";
	}


}
