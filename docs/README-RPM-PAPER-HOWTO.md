# RPM Architecture Paper How-To Test
These instructions will allow you to build, train and test the models used in the paper. 
The models can also be used for general purpose part-of-speech tagging when coupled with a matching parser.

## System Requirements
Make sure your working on a system that can support the task or it may take some time:

 - 75gb free disk space
 - 16gb+ memory
 - a modern multi-core processor
 - software installed; java development kit; mvn


## DataSets
The datasets used for these examples are available at the following locations:
 
 - WSJ     - [WSJ Treebank3 Corpus; cost $$$](https://catalog.ldc.upenn.edu/LDC2000T43)
 - brown   - [Brown Corpus](https://www.kaggle.com/nltkdata/brown-corpus)
 - ConLL17 - [ConLL 2017 Shared Task - multi-language corpus](https://lindat.mff.cuni.cz/repository/xmlui/handle/11234/1-1989)
 

## Build VegML
Build VegML and the examples and install needed dependencies

	mvn clean;mvn install dependency:copy-dependencies 

## Generating Models
To generate the models the last argument for each is the corpus and location as well as the output directory for
the generated models;there will be a lot of them so make ensure you have 70gb free on your drive.

Dataset is the dataset to use and location

	dataset=<dataset-name>:<corpus-directory>
	
	 dataset=WSJ                 -- Treebank3 Corpus 
	 dataset=brown               -- brown corpus as is 
	 dataset=brown-penntreebank  -- brown corpus tags mapped to penn treebank tags
	 dataset=ConLL17/UD_Chinese  -- ConLL corpus with ConLL test sub-dataset name:UD_English-LinES/UD_Vietnamese/etc
	 dataset=ConLL17-gold/UD_Chinese  -- ConLL corpus with ConLL gold sub-dataset name:UD_English-LinES/UD_Vietnamese/etc

	Example:			
	 dataset=WSJ:../../corpus
	
Directory is the output directory to save models to:

	directory=../../models
	
	

Run the commands, they make take some time and a bit of memory	
	
	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.GenPosViaAffix dataset=WSJ:../corpus directory=../models
	
	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.GenPosViaText dataset=WSJ:../corpus directory=../models
	
	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.GenPosViaMix dataset=WSJ:../corpus directory=../models

	
To generate the mixed models once base models are created
	
	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.GenPosViaMerge dataset=WSJ:../corpus directory=../models


To generate learning curve models and export data as .csv 

	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.MapLearningCurve dataset=WSJ:../corpus directory=../models


## Evaluating Models
	
To test each model in ../models and show the results with tuning and test data sets

	java -Xms2g -Xmx128g -Dfile.encoding=UTF-8 -classpath ./target/classes:./target/dependency/trove-3.0.3.jar:./target/dependency/kryo-4.0.2.jar:./target/dependency/reflectasm-1.11.3.jar:./target/dependency/asm-5.0.4.jar:./target/dependency/minlog-1.3.0.jar:./target/dependency/objenesis-2.5.1.jar:./target/dependency/gson-2.8.5.jar:./target/vegml-1.0.000.jar org.sedro.examples.VegTestModels -corpus WSJ:../dataset directory=../models

Each model will be evaluated with tuning and test portions of the dataset and results printed

Example output for a model

	// CFG win[5]ns[31] [w0]t[11]m[miser]fullData[false] step[6] created[03/16/2022 09:37.18] vcnt[9735465]vs[0]file[../models/dep-text-id-w-5w.veg]
	  STD[96.17%] [142477 /  5681 of 148158]  =>  r[  19970 /  276][98.64%] rp[   736][70.84%] p[ 118785] kn[ 139491 /  3811] unk[  2986 / 1870]
	  AMP[95.96%] [142173 /  5985 of 148158]  =>  r[  19972 /  274][98.65%] rp[   726][69.87%] p[ 118542] kn[ 139240 /  4062] unk[  2933 / 1923]
	T-STD[96.01%] [164302 /  6836 of 171138]  =>  r[  24095 /  367][98.50%] rp[   849][69.88%] p[ 136747] kn[ 161691 /  4852] unk[  2611 / 1984]
	T-AMP[95.99%] [164274 /  6864 of 171138]  =>  r[  24095 /  367][98.50%] rp[   858][70.62%] p[ 136711] kn[ 161664 /  4879] unk[  2610 / 1985]
	T-USR[96.71%] [165506 /  5632 of 171138]  =>  r[  24095 /  378][98.46%] rp[   841][69.22%] p[ 136787] kn[ 161723 /  4820] unk[  3783 / 812]

Each line the result of a evaluation	 of a dataset and callout

	  STD - tuning set
	  AMO - tuning set amplified
	T-STD - test set
	T-AMP - test set amplified
	T-USR - results are finalized by user added callout: multiple models, amplification and/or logic
	
The results are read as 

	AMP[percent_correct%] - [correct / incorrect of total] =>  
	r[recall_correct / recall_incorrect][recall_correct_percent%] 
	rp[recallPredict_correct][recallPredict_correct_percent%] 
	p[predict_correct] kn[known_correct / known_incorrect] unk[uknown_correct / unknown_incorrect]


	