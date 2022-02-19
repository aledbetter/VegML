### VegML JavaScript 
Contained is the JavaScript implementation of VegML this can be packaged and distributed via nmp
or linked directly with vegml.min.js


##
This is in progress

Need to get full minified bundle that does not include useless stuff like lodash
https://stackoverflow.com/questions/25956937/how-to-build-minified-and-uncompressed-bundle-with-webpack

Then the code can be fully ported and tested


# packaging the code
VegML is writen in ES6, thus it must be packaged to a single file for use as a library

	Node.js is required to package vegml


# package webpack



First initialize the package and install webpack

	npm init -y
	npm i --save-dev webpack webpack-cli @babel/core @babel/preset-env babel-loader


Now make a package VegML

build package for production: is minified

	npm run build
	
	/js/dist/vegml.min.js


build package for development: bundled / not minified
	
	npm run dev
	
	/js/dist/vegml.min.js
	
	
# Using the library

CommonJS module require:

	const veg = require('vegml');
	// ...
	veg.wordToNum('Two');


AMD module require:

	require(['veg'], function (veg) {
	  // ...
	  veg.wordToNum('Two');
	});


Script tag:

	<!DOCTYPE html>
	<html>
	  ...
	  <script src="https://example.org/vegml.js"></script>
	  <script>
	    // ...
	    // Global variable
	    veg.wordToNum('Five');
	    // Property in the window object
	    window.veg.wordToNum('Five');
	    // ...
	  </script>
	</html>
	

