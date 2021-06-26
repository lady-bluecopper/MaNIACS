JVM='java -Xmx200g -XX:-UseGCOverheadLimit -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewSize=6g -XX:+CMSParallelRemarkEnabled -XX:+ParallelRefProcEnabled -XX:+CMSClassUnloadingEnabled -cp'
PACKAGE_PATH=./MaNIAC/target/
MANIAC_JAR=MaNIAC-1.0.jar
DEPENDENCY_JAR=MaNIAC-1.0-jar-with-dependencies.jar
MANIAC_MAIN=anonymous.maniac.Main
LATTICE_MAIN=anonymous.maniac.lattice.LatticeGeneration

MANIAC_CMD=$PACKAGE_PATH/$MANIAC_JAR:$PACKAGE_PATH/$DEPENDENCY_JAR $MANIAC_MAIN
LATTICE_CMD=

input_data='./datasets/'
output_data='./output/'
dataset=citeseer

patternSize=3
seed=11
failure=0.1
c=0.5
percent=false

frequency=0.19
sampleSize=1700
numLabels=6
preComputed=false
isExact=false


#sampleSize, seed, failure, c, percent为随机算法需要的参数，非随机不需要这些参数并将isExact设为true
#
OUTPUT="$input_data/lattices/"
mkdir -p $OUTPUT
$JVM $lattice_generation_jar \
	dataFolder=${input_data} \
	patternSize=${patternSize} \
	numLabels=${numLabels} \


$JVM $MANIAC_jar \
	dataFolder=${input_data} \
	outputFolder=${output_data} \
	inputFile=${dataset}.lg \
	patternSize=$patternSize \
	seed=$seed \
	failure=$failure \
	c=$c \
	percent=$percent \
	frequency=${frequency} \
	sampleSize=${sampleSize} \
	numLabels=${numLabels} \
	preComputed=${preComputed} \
	isExact=${isExact} 