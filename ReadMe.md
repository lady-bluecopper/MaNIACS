# MaNIACS

## Overview
MaNIACS is a sampling-based randomized algorithm for computing approximations of the collection of the subgraph patterns that are frequent in a single vertex-labeled graph, according to the Minimum Node Image-based (MNI) frequency measure. 
The output of MaNIACS comes with strong probabilistic guarantees. The quality of the approximation is obtained using the empirical Vapnik-Chervonenkis (VC) dimension, a key concept from statistical learning theory.
In particular, given a failure probability <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;\delta" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;\delta"/></a>, a frequency threshold <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;&space;\tau" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;&space;\tau"/></a>, and a sample size *s*, with probability at least <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;(1&space;-&space;\delta)" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;(1&space;-&space;\delta)"/></a> over the choice of the sample *S* of size *s*, the output of MaNIACS contains a triplet <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;(P,&space;\tilde{f}(P),&space;\epsilon_k)" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;(P,&space;\tilde{f}(P),&space;\epsilon_k)"/></a> for every pattern *P* of size *k* with relative MNI frequency <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;f(P)&space;\geq&space;\tau" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;f(P)&space;\geq&space;\tau"/></a>, and the triplet is such that <a href="https://www.codecogs.com/eqnedit.php?latex=\dpi{150}&space;\small|\tilde{f}(P)&space;-&space;f(P)|&space;\leq&space;\epsilon_k" target="_blank"><img src="https://latex.codecogs.com/svg.latex?\dpi{150}&space;|\tilde{f}(P)&space;-&space;f(P)|&space;\leq&space;\epsilon_k"/></a>.
MaNIACS leverages properties of the frequency function to aggressively prune the pattern search space, and thus to reduce the time spent in exploring subspaces containing no frequent patterns. 
The framework includes both an exact and an approximate mining algorithm.

## Content
    run.sh
    config.cfg
    MaNIAC/

## Requirements
    Java JRE v1.8.0

## Input Format
The file containing the information about the nodes and the edges in the graph must comply with the following format:

	v <node_id> <node_label>
	v <node_id> <node_label>
	...
	e <node_id1> <node_id2>
	e <node_id1> <node_id2>
	...

The file must first list all the graph nodes in ascending order of id (lines starting with the letter *v*), and then all the edges (lines starting with the letter *e*).  Ids and labels must be integers.
The filename must have extension *.lg*.

## Usage
You can use MaNIACS either by using the script *run.sh* included in this package or by running the following commands.
As a preprocessing step, build the project with dependencies and rename the *jar* as *MANIAC.jar*. 

### Using the Script
The value of each parameter used by MaNIACS must be set in the configuration file *config.cfg*:
* General settings:
    * *input_data*: path to the folder containing the graph file.
    * *output_data*: path to the folder to store the results.
    * *patternSize*: maximum number of vertices of the patterns to mine. 
    * *seeds*: space-separated list of seeds to use to extract a random sample of graph vertices. MaNIACS will run once for each seed value.
    * *failure*: failure probability used to compute the epsilon values. This value is usually set to 0.1.
    * *c*: constant used to compute the epsilon values. This value is usually set to 0.5.
    * *percent*: boolean indicating whether the sample size specified is a percentage of the total number of vertices in the graph or the number of vertices to sample.

* Dataset-related settings:
    * Dataset names: names of the graph files.
    * Default values: comma-separated list of default values and information about the graph, i.e., default frequency threshold, default sample size, number of distinct vertex labels, boolean indicating if the pattern search space is pre-generated. 
    * Frequencies: comma-separated list of frequency thresholds.
    * Samples: comma-separated list of sample sizes.
    * Experimental flags: test to perform among (1) test multiple sample sizes with default frequency threshold, (2) test multiple frequency thresholds with default sample size, (3) run the exact algorithm for multiple frequency thresholds, and (4) generate the pattern search space for the number of vertex labels of this graph.

Then, the arrays that store the names, the frequencies, the sample sizes, and the experimental flags of each dataset to test must be declared at the beginning of the script *run.sh*. For example, if in the configuration file *config.cfg* you wrote:

    mygraph_db='mygraph'
    mygraph_defaults=0.5,1000,5,false
    mygraph_freqs=0.5,0.4,0.3
    mygraph_samples=1000,2000,3000
    mygraph_flags='0,0,1,0'
    
Then, in the script *run.sh* you must write:

    declare -A datasets
    datasets[$mygraph_db]=$mygraph_defaults
    declare -A test_freqs
    test_freqs[$mygraph_db]=$mygraph_freqs
    declare -A test_samples
    test_samples[$mygraph_db]=$mygraph_samples
    declare -A flags
    flags[$mygraph_db]=$mygraph_flags

### Running the Commands

1. **Run the exact algorithm**:
    
>    java -cp MANIAC.jar:lib/* anonymous.maniac.Main dataFolder=<input_data> outputFolder=<output_data> inputFile=<file_name> frequency=<frequency_threshold> numLabels=<num_vertex_labels> preComputed=<is_search_space_precomputed> patternSize=<pattern_size> isExact=true

2. **Run the approximate algorithm**:

>    java -cp MANIAC.jar:lib/* anonymous.maniac.Main dataFolder=<input_data> outputFolder=<output_data> inputFile=<file_name> frequency=<frequency_threshold> sampleSize=<sample_size> numLabels=<num_vertex_labels> preComputed=<is_search_space_precomputed> patternSize=<pattern_size> seed=<seed_number> failure=<failure_probability> c=<constant_c> isExact=false percent=<sample_size_is_a_percentage> 

3. **Generate the pattern search space**:
 
> java -cp MANIAC.jar:lib/* anonymous.maniac.lattice.LatticeGeneration dataFolder=<input_data> patternSize=<pattern_size> numLabels=<num_vertex_labels> 

## Output
The output files of the exact algorithm will be saved into the folder *output_data/exact*, the output files of the approximate algorithm when varying sample size into the folder *output_data/samples*, and the output files of the approximate algorithm when varying frequency threshold into the folder *output_data/frequency*.
The pattern search spaces are saved into the folder *input_data/lattices*. When *preComputed=true*, the algorithm assumes that the pattern search space for that dataset is in this folder.

### Output Format
MaNIACS creates two output files: *MANIAC_\** includes all the frequent patterns found, and *statistics.csv* contains some statistics.
The statistics file is a tab-separated file where each line indicates:

- input file
- time stamp
- runtime (in seconds)
- number of frequent patterns
- frequency threshold
- max pattern size
- is the search space pre-generated
- sample size (or "EX" if output of exact algorithm)
- seed (or 0 if output of exact algorithm)

The frequent pattern file has name:

MANIAC_<*input_file*>_F<*frequency_threshold*>P<*patternSize*><*T* if the search space is pre-generated; *F* otherwise><*AX* if approximate; *EX* otherwise><*sample_size* if approximate>S<*seed* if approximate; *0* otherwise>

and contains, for each pattern size:
- a line with the epsilon for that pattern size, and
- a line for each frequent pattern of that size. This line includes the relative MNI frequency of the pattern in the sample together with the pattern.
