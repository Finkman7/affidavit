# affidavit
Algorithm For Function-Inducing Delta Analysis Via Integration of Tables

**affidavit** is a solution for comparing two snapshots of a relational table with a fixed schema to identify record deletions, insertions and in particular updates in the form of attribute-specific value transformations. It is an implementation of the algorithms described in the corresponding research paper which is currently under review and can therefore not be published yet. If it gets accepted, a link to the paper will be added in this place.

## Configuration


## Reproducing Experimental Results from the Paper
If you are interested in reproducting the experimental results from the paper yourself or try a different algorithm on the dataset, you can download all files you need from [here](http://data.dws.informatik.uni-mannheim.de/affidavit/). You need at least a Java 9 distribution to run the experiments.

### Affidavit datasets
In any case, you need to download and extract the **datasets_affidavit.zip** which contains ten problem instances for each dataset used in Section 5.2 and one problem instance for flight-500k used in Section 5.3.
Every problem instance consists of several different files:
- Java Object file (\*.obj), contains information needed for the evaluation as objects, such as which functions are contained in the reference explanation
- Solution file (\*Solution.txt), can be checked by hand to take a look at the reference functions and the resulting record alignment to get a sense of what was changed in a problem instance. Note that the first attribute functions is given as a full mapping and typically you need to scroll down quite a bit to the keyword "Alignment:", above which you can see the functions for the remaining attributes.
- Source core file (\*Source_Core.csv), source core records in CSV format
- Source noise file (\*Source_Noise.csv), source noise (non-core) records in CSV format
- Target core image file (\*Target_Core_Image.csv), target core image records in CSV format
- Target noise file (\*Target_Noise.csv), target noise (non-core-image) records in CSV format
The only data that may be used to solve the task, are the source records as a whole and the target records as a whole with no information how they are separated into core and non-core, of course. The rest of the data is only used in the evaluation to judge the result.

### Running Explanation Quality Experiments
For rerunning the explanation quality experiments, you need the **affidfavit_experiments.jar** which is a snapshot of affidavit from the time when the experiments were run.

#### Parameters
There are several parameters that can be configured to control the evaluation. There are [example configuration files](http://data.dws.informatik.uni-mannheim.de/affidavit/evalConfigs) available which can be used to run the experiments with both start state options from the paper on all three difficulties on all datasets.

- **-t** Path to parent folder containing the subfolders for the different datasets from *datasets_affidavit.zip*
- **-a** Path to text file containing one line per start state configuration that is supposed to be evaluated. Valid start state options are BEST_IDs (H^s from paper = Overlap Sampling) and SINGLE_IDs (H^id from paper). Lines can be commented out with #
- **-c** Path to text file containing one line per dataset that is supposed to be evaluated. All valid start state options are contained in the example configuration file. Lines can be commented out with #
- **-c** Path to text file containing one line per difficulty setting that is supposed to be evaluated. Valid start state options are "0.3 0.3", "0.5 0.5" and "0.7 0.7" (without quotes). Lines can be commented out with #
- **-n0** Begin evaluation from the i-th problem instance of each dataset and difficulty setting
- **-n** Evaluate up to the i-th problem instance of each dataset and difficulty setting
- **-bsize** Maximum block size for the overlap sampling
- **-q** Queue width (\varrho)
- **-b** Branching Factor (\beta)
- **-v** (No Parameter) Enables verbose mode if present

As branching factor and queue width need to be specified, the experiments from the paper need to be reproduced by two separate calls for the two start state options, each with the correspoding branching factor and queue width setting.

An example command to start the evaluation with 200GB memory is:

/path/to/jdk9+/bin/java -Xmx200g -cp affidfavit_experiments.jar affidavit.eval.Evaluator -t "dataSets" -a "config/startStates.txt" -f "config/dataSets.txt" -c "config/difficulties.txt" -n0 1 -n 10 -bsize 100000 -q 5 -b 2 -v > verboseOutput.txt

From the verbose output, one can trace the search process of the algorithm configurations for each problem instance as well as the resulting explanation's attribute functions and costs in each case. The [output of our experiments on the NURSERY dataset](http://data.dws.informatik.uni-mannheim.de/affidavit/exampleOutput/) is available for both start state configurations as an example.

Furthermore, the evaluation produces a result file for each dataset for each difficulty setting in the corresponding subfolder. It has the name pattern *Result_STARTSTATESETTING_DATASET_DIFFICULTY.tsv* and contains the evaluation metrics from the paper and some more (delta_core is called aligned and delta_costs is called costs) for each problem instance as well as the macro average. The [output of our experiments on the NURSERY dataset for setting (0.3, 0.3)](http://data.dws.informatik.uni-mannheim.de/affidavit/exampleOutput/) is available for both start state configurations as an example.

### Running Scalability Experiments
For rerunning the row scalability experiments, you need the **affidfavit_scalability.jar** which again is a snapshot of affidavit from the time when the row scalability experiments were run.

#### Parameters

The command to start the evaluation is:

