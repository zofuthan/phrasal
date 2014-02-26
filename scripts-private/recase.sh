#!/usr/bin/env bash
#
# Re-case French and German text. The procedures for converting MT
# output to "readable" text differs for the two languages.
#
# German: 1) re-case 2) detokenize
#
# French: 1) re-case, 2) detokenize
#
# Author: Spence Green
#

if [ $# -ne 2 ]; then
	echo Usage: $(basename $0) "[French|German]" file
	exit -1
fi

lang=$1
infile=$2

model_path=/scr/nlp/data/WMT/recasers

if [ $RECASE_MODEL ]; then
  model=srilm:$RECASE_MODEL
elif [ $lang == "French" ]; then
	model=srilm:${model_path}/french.hmm.recaser.arpa
else
	model=kenlm:${model_path}/german.hmm.recaser.probing
fi

HOST=`hostname -s`
JVM_OPTS="-server -Xmx20g -Xms2g -XX:+UseParallelGC"
JNI_OPTS="-Djava.library.path=/scr/nlp/data/gale3/KENLM-JNI/${HOST}:/scr/nlp/data/gale3/SRILM-JNI/${HOST}"

java $JVM_OPTS $JNI_OPTS edu.stanford.nlp.mt.tools.LanguageModelTrueCaser $model < $infile 
# 2>/dev/null

