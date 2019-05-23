# affidavit
Algorithm For Function-Inducing Delta Analysis Via Integration of Tables

**affidavit** is a solution for comparing two snapshots of a relational table with a fixed schema to identify record deletions, insertions and in particular updates in the form of attribute-specific value transformations. It is an implementation of the algorithms described in the corresponding research paper which is currently under review and can therefore not be published yet. If it gets accepted, a link to the paper will be added in this place.

## Configuration


## Reproducing Experimental Results from the Paper
If you are interested in running the experiments from the paper yourself, you can download all files you need from [here](http://data.dws.informatik.uni-mannheim.de/affidavit/). You need at least a Java 9 distribution to run the experiments.

In any case, you need to download and extract the **datasets_affidavit.zip** which contains ten problem instances for each dataset used in Section 5.2 and one problem instance for flight-500k used in Section 5.3.

For rerunning the explanation quality experiments, you need the **affidfavit_experiments.jar** which is a snapshot of affidavit from the time when the experiments were run.  The command to start the evaluation is:

For rerunning the row scalability experiments, you need the **affidfavit_scalability.jar** which again is a snapshot of affidavit from the time when the row scalability experiments were run.  The command to start the evaluation is:

