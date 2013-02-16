#!/usr/bin/env bash
#
# Moses training and tuning on the Stanford NLP cluster.sh
#
# Author: Spence Green

# IMPORTANT! TODO: Ensure that the following is set in your environment:
#
# LD_LIBRARY_PATH=/u/nlp/packages/boost_1_42_0/lib

if [ $# -ne 1 ]; then
	echo Usage: `basename $0` tune-dir-name
	exit 0
fi

TUNEDIR=`basename $1`

# Paths to stuff
HOME=`pwd`
MOSES=/u/nlp/packages/mosesdecoder
GIZA=/u/nlp/packages/GIZA++

# TODO: Change these for your language
CORPUSDIR=/u/spenceg/BOLT-Arabic-all/corpus2/sampled/dedup
LM=/u/spenceg/BOLT-Arabic-all/lm/5gm-dedup.unk.model.gz
TUNE_SET="$HOME"/mt06.prep
REF_PREFIX="$HOME"/refs/mt06/ref

# Phrase and feature extraction using Galley and Manning (2008)
# hierarchical re-ordering models.
mkdir -p $HOME/train/model
$MOSES/scripts/training/train-model.perl --max-phrase-length 7 \
--external-bin-dir $GIZA --first-step 3 --last-step 9 \
-root-dir $HOME/train -corpus $CORPUSDIR/corpus -f ar -e en \
-giza-f2e $CORPUSDIR/giza.ar-en \
-giza-e2f $CORPUSDIR/giza.en-ar \
-alignment grow-diag \
-lm 0:3:"$LM":0 \
-reordering hier-mslr-bidirectional-fe




# Batch mira Model tuning with MIRA
# For MERT: remove the "--batch-mira" parameter
# For PRO: replace mira with "--pairwise-ranked"
rm -rf $HOME/$TUNEDIR
mkdir -p $HOME/$TUNEDIR
$MOSES/bin/mira -f $HOME/train/moses.ini -i $TUNE_SET -r ${REF_PREFIX}* \
    --accumulate-weights 1 \
    --shuffle 1 
#$MOSES/scripts/training/mert-moses.pl \
#--working-dir $HOME/$TUNEDIR \
#--decoder-flags="-distortion-limit 5 -threads all" --mertdir $MOSES/bin/ \
#--nbest=500 \
#--batch-mira --return-best-dev \
#$TUNE_SET $REF_PREFIX \
#$MOSES/bin/moses $HOME/train/model/moses.ini
