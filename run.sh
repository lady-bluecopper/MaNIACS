#!/bin/bash
echo ''
echo ''
echo '  __  __    _____    __ __    __    _____    _____  '
echo ' |  \/  |  |___  |  |  \  |  |  |  |___  |  |  ___| '
echo ' | |\/| |  |  __ |  |  |  |  |  |  |  __ |  | |___  '
echo ' |_|  |_|  |_____|  |__|__|  |__|  |_____|  |_____| '
echo '|""""""""||"""""""||"""""""||""""||"""""""||"""""""|'
echo -e '\n\n'

# Loading configurations for experiments
echo '>> Loading config file config.cfg'
source config.cfg

unset datasets
declare -A datasets
datasets[$citeseer_db]=$citeseer_defaults
datasets[$mico_db]=$mico_defaults
datasets[$patents_db]=$patents_defaults
datasets[$phy_citations_db]=$phy_citations_defaults
datasets[$youtube_db]=$youtube_defaults

unset test_freqs
declare -A test_freqs
test_freqs[$citeseer_db]=$citeseer_freqs
test_freqs[$mico_db]=$mico_freqs
test_freqs[$patents_db]=$patents_freqs
test_freqs[$phy_citations_db]=$phy_citations_freqs
test_freqs[$youtube_db]=$youtube_freqs

unset test_samples
declare -A test_samples
test_samples[$citeseer_db]=$citeseer_samples
test_samples[$mico_db]=$mico_samples
test_samples[$patents_db]=$patents_samples
test_samples[$phy_citations_db]=$phy_citations_samples
test_samples[$youtube_db]=$youtube_samples

unset flags
declare -A flags
flags[$citeseer_db]=$citeseer_flags
flags[$mico_db]=$mico_flags
flags[$patents_db]=$patents_flags
flags[$phy_citations_db]=$phy_citations_flags
flags[$youtube_db]=$youtube_flags

echo -e '\n\n>> Creating directories ...'
mkdir -p $output_data

for dataset in ${!datasets[@]}
do
	dataset_path="$input_data"
	default=${datasets[${dataset}]}
	flag=${flags[${dataset}]}
	defaults=(`echo $default|tr "," "\n"`)
	experiments=(`echo $flag|tr "," "\n"`)

	echo ">> Processing dataset ${dataset} with default values (${defaults[@]})"
	echo ">> Experiment flags ${experiments[@]}"

	if [[ ${experiments[0]} -eq "1" ]]; then
		echo '-----------------------------'
		echo '     Varying Sample Size 	   '
		echo '-----------------------------'

		OUTPUT="$output_data/samples/"
		mkdir -p $OUTPUT

		samples=(`echo ${test_samples[${dataset}]}|tr "," "\n"`)
		for seed in ${seeds[*]}
		do
			for s in ${samples[*]}
			do
				echo "Running command ..."
				echo "$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=${defaults[0]} sampleSize=$s numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize seed=$seed failure=$failure c=$c isExact=false percent=$percent"
				echo "---- `date`"
				$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=${defaults[0]} sampleSize=$s numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize seed=$seed failure=$failure c=$c isExact=false percent=$percent
			done
		done
	fi

	if [[ ${experiments[1]} -eq "1" ]]; then
		echo '-----------------------------'
		echo '      Varying Frequency 	   '
		echo '-----------------------------'

		OUTPUT="$output_data/frequency/"
		mkdir -p $OUTPUT

		freqs=(`echo ${test_freqs[${dataset}]}|tr "," "\n"`)

		for seed in ${seeds[*]}
		do
			for freq in ${freqs[*]}
			do
				echo "Running command ..."
				echo "$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=$freq sampleSize=${defaults[1]} numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize seed=$seed failure=$failure c=$c isExact=false percent=$percent"
				echo "---- `date`"
				$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=$freq sampleSize=${defaults[1]} numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize seed=$seed failure=$failure c=$c isExact=false percent=$percent
			done
		done
	fi

	if [[ ${experiments[2]} -eq "1" ]]; then
		echo '-----------------------------'
		echo '    Exact Pattern Mining 	   '
		echo '-----------------------------'

		OUTPUT="$output_data/exact/"
		mkdir -p $OUTPUT

		freqs=(`echo ${test_freqs[${dataset}]}|tr "," "\n"`)

		for freq in ${freqs[*]}
		do
			echo "Running command ..."
			echo "$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=$freq numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize isExact=true"
			echo "---- `date`"
			$JVM $MANIAC_jar dataFolder=${input_data} outputFolder=$OUTPUT inputFile=${dataset}.lg frequency=$freq numLabels=${defaults[2]} preComputed=${defaults[3]} patternSize=$patternSize isExact=true
		done
	fi

	if [[ ${experiments[3]} -eq "1" ]]; then
		echo '-----------------------------------------'
		echo '    	     Generate Lattice              '
		echo '-----------------------------------------'

		OUTPUT="$input_data/lattices/"
		mkdir -p $OUTPUT

		echo "Running command ..."
		echo "$JVM $lattice_generation_jar dataFolder=${input_data} patternSize=$patternSize numLabels=${defaults[2]}"
		echo "---- `date`"
		$JVM $lattice_generation_jar dataFolder=${input_data} patternSize=$patternSize numLabels=${defaults[2]}
	fi
done
echo 'Terminated.'
