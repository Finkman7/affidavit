# affidavit
Algorithm For Function-Inducing Delta Analysis Via Integration of Tables

**affidavit** is a solution for comparing two snapshots of a relational table with a fixed schema to identify record deletions, insertions and in particular updates in the form of attribute-specific value transformations. It is an implementation of the algorithms described in the corresponding research paper which is currently under review and can therefore not be published yet. If it gets accepted, a link to the paper will be added in this place.

The newest version of affidavit is available [here](http://data.dws.informatik.uni-mannheim.de/affidavit/affidavit.jar).
affidavit requires Java 9 or later to be run.

## Configuration
Affidavit currently takes as input two csv files that contain the source and target records, respectively. Both files need to begin with a header line which is meant to describe the attribute names. An [example](http://data.dws.informatik.uni-mannheim.de/affidavit/exampleSnapshots) is provided for the tables in Figure 1 of the paper. For the future, there are plans to extend affidavit to be able to connect via JDBC to various DBMS.

#### Parameters
affidavit has several parameters which are described in detail in the corresponding paper.

- **-s** Path to source csv file containing all source records
- **-t** Path to target csv file containing all target records
- **-sep** CSV separator char
- **-v** (Flag) Enables verbose mode if present

- **-init** Start State configuration. Valid start state options are EMPTY, BEST_ID (H^s from paper = Overlap Sampling), SINGLE_IDs (H^id from paper) and FULL_IDs. **Default:** SINGLE_IDs
- **-noise** Pessimistic estimate of the amount of noise affidavit should be able to tolerate. The higher, the slower the search will be but the more likely it is that the optimal functions for an attribute will be found despite high noise or a rare effect of the function. Note that this is the opposite semantic of Theta from the paper. In the case of two snapshots of equal size (such as during our evaluation experiments), noise is exactly 1-Theta. **Default:** 0.5
- **-conf** Confidence that describes the minimum likelihood of finding the optimal function of an attribute if the maximum amount of the configured noise is actually present. **Default:** 0.95
- **-q** Queue Width, stores up to q different states with the same amount of assignments in the queue at the same time. **Default:** 3
- **-b** Branching factor, during each state extension, the b most determined attributes are extended by the b most promising function assignments each. **Default:** 3
- **-blocksize** Maximum block size for the overlap sampling. **Default:** 100000

An example command to start affidavit with 200GB memory is:

*/path/to/jdk9+/bin/java -Xmx200g -cp affidavit.jar affidavit.main.Main -s data/vanilla/Source.csv -t  data/vanilla/Target.csv -v -sep "|" -init SINGLE_IDs -noise 0.5 -conf 0.95 -q 3 -b 2*

Note that most parameters could have been omitted in this case as they were simply set to their default values.

## Reproducing Experimental Results from the Paper
If you are interested in reproducting the experimental results from the paper yourself or try a different algorithm on the dataset, you can download all files you need from [here](http://data.dws.informatik.uni-mannheim.de/affidavit/).

In any case, you need to download and extract the **[datasets_affidavit.zip](http://data.dws.informatik.uni-mannheim.de/affidavit/datasets_affidavit.zip)** which contains ten problem instances for each dataset used in Section 5.2 and one problem instance for flight-500k used in Section 5.3.

### Affidavit Dataset
The Affidavit Dataset consists of ten problem instances per [dataset](https://hpi.de/naumann/projects/repeatability/data-profiling/fds.html#c168191) created for three settings of varying difficulty.

Every problem instance consists of several different files:
- Java Object file (\*.obj), contains information needed for the evaluation as objects, such as which functions are contained in the reference explanation
- Solution file (\*Solution.txt), can be checked by hand to take a look at the reference functions and the resulting record alignment to get a sense of what was changed in a problem instance. Note that the first attribute function is given as a full mapping and typically you need to scroll down quite a bit to the keyword "Alignment:", above which you can see the functions for the remaining attributes.
- Source core file (\*Source_Core.csv), source core records in CSV format
- Source noise file (\*Source_Noise.csv), source noise (non-core) records in CSV format
- Target core image file (\*Target_Core_Image.csv), target core image records in CSV format
- Target noise file (\*Target_Noise.csv), target noise (non-core-image) records in CSV format

The only data that may be used to solve the task, are the source records as a whole and the target records as a whole with no information how they are separated into core and non-core, of course. The rest of the data is only used in the evaluation to judge the result.

### Running Explanation Quality Experiments
For rerunning the explanation quality experiments, you need the **[affidavit_experiments.jar](http://data.dws.informatik.uni-mannheim.de/affidavit/affidavit_experiments.jar)** which is a snapshot of affidavit from the time when the experiments were run.

#### Parameters
There are several parameters that can be configured to control the evaluation. There are [example configuration files](http://data.dws.informatik.uni-mannheim.de/affidavit/evalConfigs) available which can be used to run the experiments with both start state options from the paper on all three difficulties on all datasets.

- **-t** Path to parent folder containing the subfolders for the different datasets from *datasets_affidavit.zip*
- **-a** Path to text file containing one line per start state configuration that is supposed to be evaluated. All valid start state options are contained in the [example configuration file](http://data.dws.informatik.uni-mannheim.de/affidavit/evalConfigs/startStates.txt). Lines can be commented out with #
- **-f** Path to text file containing one line per dataset that is supposed to be evaluated. All valid start state options are contained in the [example configuration file](http://data.dws.informatik.uni-mannheim.de/affidavit/evalConfigs/dataSets.txt). Lines can be commented out with #
- **-c** Path to text file containing one line per difficulty setting that is supposed to be evaluated. All valid start state options are contained in the [example configuration file](http://data.dws.informatik.uni-mannheim.de/affidavit/evalConfigs/difficulties.txt). Lines can be commented out with #
- **-n0** Begin evaluation from the i-th problem instance of each dataset and difficulty setting
- **-n** Evaluate up to the i-th problem instance (inclusive) of each dataset and difficulty setting
- **-bsize** Maximum block size for the overlap sampling
- **-q** Queue width
- **-b** Branching Factor
- **-v** (Flag) Enables verbose mode if present

As branching factor and queue width need to be specified, the experiments from the paper need to be reproduced by two separate calls for the two start state options, each with the correspoding branching factor and queue width setting.

An example command to start the evaluation with 200GB memory is:

*/path/to/jdk9+/bin/java -Xmx200g -cp affidavit_experiments.jar affidavit.eval.Evaluator -t "dataSets" -a "config/startStates.txt" -f "config/dataSets.txt" -c "config/difficulties.txt" -n0 1 -n 10 -bsize 100000 -q 5 -b 2 -v > verboseOutput.txt*

From the verbose output, one can trace the search process of the algorithm configurations for each problem instance as well as the resulting explanation's attribute functions and costs in each case. The [output of our experiments on the NURSERY dataset](http://data.dws.informatik.uni-mannheim.de/affidavit/exampleOutput) is available for both start state configurations as an example.

Furthermore, the evaluation produces a result file for each dataset for each difficulty setting in the corresponding subfolder. It has the name pattern *Result_STARTSTATESETTING_DATASET_DIFFICULTY.tsv* and contains the evaluation metrics from the paper and some more (delta_core is called aligned and delta_costs is called costs) for each problem instance as well as the macro average. The [output of our experiments on the NURSERY dataset for setting (0.3, 0.3)](http://data.dws.informatik.uni-mannheim.de/affidavit/exampleResult) is available for both start state configurations as an example.

### Running Scalability Experiments
For rerunning the row scalability experiments, you need the **[affidavit_scalability.jar](http://data.dws.informatik.uni-mannheim.de/affidavit/affidavit_scalability.jar)** which again is a snapshot of affidavit from the time when the row scalability experiments were run.

#### Parameters
The parameters are the same as [before](https://github.com/Finkman7/affidavit#parameters-1) and should be used accordingly to specify that the evaluation should be performed on one particular problem instance.
The snapshot for scalability contains one more parameter though that can be used to scale down the problem instance.

- **-scale** Needs to be a decimal in the interval (0-1), scaling factor

An example command to start the evaluation with 200GB memory is:

*/path/to/jdk9+/bin/java -Xmx200g -cp affidavit_experiments.jar affidavit.eval.Evaluator -t "dataSets" -a "config/startStates.txt" -f "config/dataSets.txt" -c "config/difficulties.txt" -n0 1 -n 1 -bsize 100000 -q 5 -b 2 -v -scale 0.5 > verboseOutput.txt*

The experiments in Section 5.3 were performed on the problem instance *flight-500k*. To reproduce the runs, make sure to set the parameters to the same configuration described in the paper.

At the end of the verbose output, there will be line that displays evaluation metrics including runtimes. For example:

*run       	runtime   	costs     	aligned   	prec      	rec       	f1        	acc       
\-         	298035ms  	0.0%      	0.0%      	1.00      	1.00      	1.00      	1.00*

## Source Code
The source code of affidavit will be made available in the near future once refactoring is finished. There will be a small guide that describes how to implement a short Java interface to support new meta functions which we did not implement yet. This way, affidavit can be quickly extended to suit individual needs.
