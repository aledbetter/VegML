# ----------------------------------------------------------
# Initial Notes defining method
# ----------------------------------------------------------
# -------- Tue, Apr 13, 2021, 8:55 AM --------
Symbolic ml

Count each element in each position that match / don’t match.
Count each sequence in each position that match don’t match.

End
1)  generate all options that match
      Generate metric based on match not match of members
2) generate set of all not match minimized

# -------- Tue, Apr 13, 2021, 11:24 AM --------
First pass make lists (matrix) of Id to count,
Next process with total matches to get probability.
Next match full text set to get correlation values for each set.

Id, count, probability, correlation

- this can all be updated incrementally
- items should be able to merge and split going back to the base words.
- reduced rule generation from reduced sets can produce symbolic rules.

Eval can take place fast directly from the matrix as well.
	

# ----------------------------------------------------------
# ISSUES:
# ----------------------------------------------------------
resolve: affix unknown carving results are not amazing with current, what is the difference?

X) Veg value as Object
X) dataSet seperate links from tags
X) dataset.removeDataS() must update ranges, should extend to other datatypes
X) after entangle vector count per numberset off after entanglement	
X) entangle has gotten slower??
X) setBaseLineBooleanModeAndClear() -> single DS with multiple values ?? accum.getValPsBoolean()


# ----------------------------------------------------------
# TODO:
# ----------------------------------------------------------
X) COMMAND Line Java or python?
X) JavaScript version	
X) Python version
	https://www.py4j.org/
    https://motmaytinh.github.io/2019/06/how-i-wrote-a-python-wrapper-for-java-implementation-of-vncorenlp/
    https://github.com/motmaytinh/vncorenlp-pywrapper
BUG-MAYBE: if focus=0 training not matching partial frames. 
 		
X) symbolic rules and back
	- VegRules: symbolic rule API
	
X) better block / stream APIs

X) tier/dependent complete for Adaptive/etc
	- dependent carving; issues with overloaded numberSet ?
	- numberSet weights and NS overloading
	
X) value inputs and results: time/scale input dimensions: deviation()/aggragation() ..
	- could use activation to convert input to useable and back? 
	- but this becomes turns it into feature work... OR the activation can be determined by the probability via tuning
	
X) continues learning example - language model or robot control?

X) learn math example: Process and Math
	
X) unsupervised	frequency transfer
	- use unsupervised model to get frequency of vectors on large dataset
	- transfer the frequencies, to local model
		- retain value ratio OR use sub-vector frequencies to determine skew in ratio based on respective ratios
	
X) logical reduction / Entanglement
	- entangle with tolerances: match same size and curve +/- tolerance
	- reduce values from accums that don't contribute, not the top 20%.. but the lesser
	- this should allow for most/many full rules to be reduced, providing space for an expanded window. perhapse significantly expaneded
	- symbolic rules then become much more like human generated rules, focusing on the unique elemen that makes them correct 
	- MUST wait until a rollup / correctness cycle...		
	X) logical tagging of weights (for reduction to best abstract rule)
		- each numberSet must have an immutable weight - currently there is a weight that can be changed, should work fine
		- each accume must have a weight that can be adjusted to correct
		- the SUM of these weights determines the equivulent numberSet weight + must account for tests of just fullestNumberSet()
		- this will allow evaluation to increase an accume to be more indicative than any others
		
X) Performance - mix frameing and vector gen into a single pass
	- build the vector set as the frames are generated



