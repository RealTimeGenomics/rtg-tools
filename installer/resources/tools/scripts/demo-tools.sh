#!/bin/bash

# This script runs a complete demonstration of RTG simulation and
# variant processing tools, using data simulated on the fly.  When
# running it, give the full path to newly installed RTG, e.g.:
#
# demo-tools.sh /path/to/rtg-tools-NNN/rtg
#
# or (if rtg is installed on your $PATH)
#
# demo-tools.sh rtg
#


if [ ! "$1" ]; then
    echo "Usage: $0 /path/to/rtg-tools-NNN/rtg" >&2
    exit 1
fi

RTG=$1

# Define NOWAIT to run through without any pausing
# Define MD to format output as markdown

DEMODIR=demo-tools
echo "Making directory for demo data: ${DEMODIR}" >&2
if [ -e ${DEMODIR} ]; then
    echo "Directory for working data '${DEMODIR}' already exists, please delete it first" >&2
    exit 1
fi
if ! mkdir ${DEMODIR}; then
    echo "Could not create a working directory for demo data.  Do you have write permission here?" >&2
    exit 1
fi
cd ${DEMODIR} || exit 1

echo "Checking RTG is executable" >&2
if ! "$RTG" version >/dev/null; then
    cat<<EOF >&2

Could not execute "$RTG" version. 

For this demo the path to RTG must be given as an absolute path.

EOF
    exit 1
fi

echo "Checking if Graphviz is installed" >&2

if dot -V >/dev/null 2>&1; then
    HAVEDOT=1
else
    cat<<EOF >&2

Could not execute "dot" from Graphviz.  The demo will still work, but
will skip displaying pedigree diagrams. (You might want to run this demo
again after installing Graphviz).

EOF
    HAVEDOT=
fi


function pause() {
    if [ ! "$NOWAIT" ]; then
        echo
        read -erp "Press enter to continue..."
        echo
    fi
}

if [ "$MD" ]; then
    filter=("sed" "s/^/    /")
else
    filter=("cat")
fi

function exiterror() {
    cat<<EOF >&2

Something unexpected happened during the demo.
Check you are using the most recent version of RTG and this script.
Exiting.
EOF
    exit 1
}
function echocommand() {
    echo "\$ $*" | "${filter[@]}"
}
function docommand() {
    echocommand "$@"
    "$@" 2>&1 | "${filter[@]}"
    if [ "${PIPESTATUS[0]}" -ne 0 ]; then
        exiterror
    fi
}

function doimage() {
    if [ "$MD" ]; then
        cat<<EOF
![ROC Image](file:///$PWD/$1)
EOF
    else
        cat<<EOF

You can look at the image 
$PWD/$1
with your favorite image viewer now or after the demo.

EOF
    fi
}

cat<<EOF

RTG Tools Simulation and Variant Processing Demonstration
=========================================================

In this demo we will give you a taste of the capabilities of RTG with
a demonstration of simulated dataset generation and variant processing.

To start with we will use RTG simulation utilities to generate a
synthetic dataset from scratch:

* \`genomesim\` - simulate a reference genome
* \`popsim\` - simulate population variants
* \`samplesim\` - generate two founder individuals
* \`childsim\` - simulate offspring of the two founders
* \`denovosim\` - simulate de novo mutations in some of the offspring
* \`readsim\` - simulate next-gen sequencing of the individuals

We will also demonstrate RTG variant processing and other analysis with
the following commands:

* \`mendelian\` - check variants for Mendelian consistency
* \`vcffilter\` - VCF record filtering
* \`vcfsubset\` - Columnwise VCF alterations
* \`vcfeval\` - compare two VCF call sets for agreement
* \`rocplot\` - produce static or interactive ROC graphs
* \`sdfstats\` - output information about data stored in SDF
* \`pedfilter\` - convert pedigree information between PED and VCF
* \`pedstats\` - display summary pedigree information

EOF
pause

cat<<EOF

Genome Simulation
-----------------

First we simulate a reference genome by generating random DNA, in this
case 10 chromosomes with lengths between 40kb and 50kb.  We will be
using fixed random number seeds during this demo in order to ensure we
have deterministic results.  (We take reproducability seriously - so you
can be sure that you get repeatable results with RTG).

EOF
pause
docommand "$RTG" genomesim --output reference.sdf --num-contigs 10 --min-length 40000 --max-length 50000 --seed 42 --prefix Chr
cat<<EOF >reference.sdf/reference.txt
# Simulated reference. This file format is described in the user manual
version 1

# Default for any non-specified sequences, e.g. decoys
either  def     diploid linear

# Autosomes
either  seq     Chr1 diploid linear
either  seq     Chr2 diploid linear
either  seq     Chr3 diploid linear
either  seq     Chr4 diploid linear
either  seq     Chr5 diploid linear
either  seq     Chr6 diploid linear
either  seq     Chr7 diploid linear
either  seq     Chr8 diploid linear

# Sex chromosomes, here Chr9 is like human X, Chr10 is like human Y
male    seq     Chr9    haploid linear  Chr10
male    seq     Chr10   haploid linear  Chr9
female  seq     Chr9    diploid linear
female  seq     Chr10   none    linear

# If we were to define a pseudoautosomal region it might look like 
# this. While mapping and variant calling fully supports PAR regions,
# the simulation tools do not, so we won't use any in this demo.
#male    dup     Chr9:1501-4500  Chr10:5001-8000
EOF

cat<<EOF

This command has created the reference as an SDF, which is a directory
structure that allows direct access to individual sequences or parts
of sequences, as well as containing metadata about the sequences
contained within.  RTG commands use SDFs for both reference and read
storage.

The reference SDF can optionally contain configuration that specifies
additional genomic information regarding ploidy and sex chromosomes.
For typical reference genomes, RTG will automatically recognize the
reference and use an appropriate metadata file.  For this simulated
dataset, we have manually set that up using the following reference
configuration:

EOF
docommand cat reference.sdf/reference.txt
pause

cat <<EOF

You can find out more information about any SDF by using the
\`sdfstats\` command.  Here we will also request specific information
about how the reference sequences are interpreted for a male by adding
the flags \`--sex male\`.  Every RTG command accepts the \`--help\`
option in order to display the list of available options.

EOF
docommand "$RTG" sdfstats reference.sdf --sex male

cat<<EOF

Recall that we have defined Chr9 as analogous to the human X
chromosome and Chr 10 as analogous to the human Y chromosome, and this
is reflected in the output.

EOF
pause



cat<<EOF

Variant Simulation
------------------

Now we will simulate some variants on this reference, using the \`popsim\`
command.  The output will be a VCF containing these population
variants.

EOF
pause
docommand "$RTG" popsim --reference reference.sdf --seed 42 --output pop.vcf.gz

cat<<EOF

The generated VCF contains variants with allele frequency
annotations that can be used to simulate a member of the population.
The types of variants and frequencies are based off variants from the
1000 Genomes Project.  We can examine some example variants using \`rtg
extract\`.  The \`extract\` command can be used with any SAM/BAM/BED/VCF
file that has been coordinate-sorted, block-compressed and indexed
according to standard NGS practise.  RTG commands automatically index
output files by default.

EOF
pause
docommand "$RTG" extract --header pop.vcf.gz Chr9:2000+1000
pause

cat<<EOF

Sample Simulation (including pedigree)
--------------------------------------

Now let's simulate a couple of members of the population that we can
use as parents for a family.  For each sample we specify the desired
sample name and sex.  The \`samplesim\` command outputs a VCF including a
new sample column for the generated individual (and optionally an SDF
containing the whole genome for the sample, which we will use
later).  We will run this twice, to generate each parent.

EOF
pause
docommand "$RTG" samplesim --reference reference.sdf --seed 572 --sex male --sample father --input pop.vcf.gz --output pop-1.vcf.gz --output-sdf genome-father.sdf
docommand "$RTG" samplesim --reference reference.sdf --seed 126 --sex female --sample mother --input pop-1.vcf.gz --output pop-2.vcf.gz --output-sdf genome-mother.sdf
pause

cat<<EOF

The genotypes selected for the two samples were determined on the basis
of the allele frequency annotations in the input VCF so a large fraction
of the low frequency population variants were not used.  Let's prune
them from the VCF to keep things simple.  First we use the \`vcffilter\`
(which performs "row-wise" VCF processing) to filter the whole file, and
then \`extract\` to pull out a subset of records for display.

EOF
pause
docommand "$RTG" vcffilter --input pop-2.vcf.gz --output parents.vcf.gz --remove-all-same-as-ref
docommand "$RTG" extract --header parents.vcf.gz Chr9:2000+1000
pause

cat<<EOF

Now let's simulate some offspring of those two parents.  The RTG
\`childsim\` command obeys the reference chromosome information,
selecting variant genotypes following Mendelian inheritance and
recombination.  For each child we specify the sample name of the father
and mother, and the desired sample name and sex of the child.

For the final child, we will use \`denovosim\` to add novel variants
that are not present in either of the parents (the \`denovosim\` command
can also be used to simulate tumor/normal genomes).

As with the \`samplesim\` command, both \`childsim\` and \`denovosim\`
generate the individual as a new sample column in the output VCF, as
well as generating the whole genome SDF which we will use next for read
simulation.

EOF
pause
docommand "$RTG" childsim --reference reference.sdf --seed 837 --input parents.vcf.gz --output family-1.vcf.gz --output-sdf genome-son1.sdf --father father --mother mother --sex male --sample son1
docommand "$RTG" childsim --reference reference.sdf --seed 923 --input family-1.vcf.gz --output family-2.vcf.gz --output-sdf genome-son2.sdf --father father --mother mother --sex male --sample son2
docommand "$RTG" childsim --reference reference.sdf --seed 269 --input family-2.vcf.gz --output family-3.vcf.gz --output-sdf genome-daughter1.sdf --father father --mother mother --sex female --sample daughter1
docommand "$RTG" childsim --reference reference.sdf --seed 284 --input family-3.vcf.gz --output family-4.vcf.gz --father father --mother mother --sex female --sample daughter2-initial
docommand "$RTG" denovosim --reference reference.sdf --seed 841 --num-mutations 50 --input family-4.vcf.gz --output family.vcf.gz --output-sdf genome-daughter2.sdf --original daughter2-initial --sample daughter2


cat<<EOF

The sample called "daughter-initial" is an intermediate sample
representing the daughter before the addition of de novo variants.  We
will use \`vcfstats\` to get summary statistics for every sample
contained in the VCF.

EOF

pause
#docommand "$RTG" vcfsubset --remove-sample daughter2-initial --input family-5.vcf.gz --output family.vcf.gz
rm pop-[0-9]*.vcf* family-[0-9]*.vcf*
docommand "$RTG" vcfstats family.vcf.gz

cat<<EOF

Now we can use the \`pedstats\` command to look at the pedigree structure
that has just been simulated.  \`rtg pedstats\` understands pedigree
information in standard PED format files as well as when the pedigree
information is encoded in a VCF header using standard VCF SAMPLE and
PEDIGREE header fields.

EOF

pause
docommand "$RTG" pedstats family.vcf.gz

cat<<EOF

We can use the \`pedfilter\` command to convert from VCF pedigree
information to PED format (and vise versa).

EOF
    docommand "$RTG" pedfilter family.vcf.gz
pause

if [ "$HAVEDOT" ]; then
    cat<<EOF

We can use the \`pedstats\` command in conjunction with \`dot\` to
generate a visual representation of the pedigree.

EOF
    echocommand "$RTG" pedstats --dot "Simulated-Family" family.vcf.gz ">family-simulated.dot"
    "$RTG" pedstats --dot "Simulated-Family" family.vcf.gz >family-simulated.dot || exiterror
    docommand dot -Tpng -o family-simulated.png family-simulated.dot
    doimage family-simulated.png
fi
pause

cat <<EOF

Note that we really want to treat "daughter1" (containing inherited plus
the de novo variants) as a direct offspring of the parents, so let's
create the pedigree file that reflects that structure.  This is a
standard PED format:

EOF
cat<<EOF >family.ped
1	father	0	0	1	0
1	mother	0	0	2	0
1	son1	father	mother	1	0
1	son2	father	mother	1	0
1	daughter1	father	mother	2	0
1	daughter2	father	mother	2	0
EOF

docommand cat family.ped

if [ "$HAVEDOT" ]; then
    cat<<EOF

Here is the Graphviz representation of the new pedigree.

EOF

    echocommand "$RTG" pedstats --dot "Family" family.ped ">family.dot"
    "$RTG" pedstats --dot "Family" family.ped >family.dot || exiterror
    docommand dot -Tpng -o family.png family.dot
    doimage family.png
fi
pause

cat<<EOF

We can look at some of the variants produced for the samples on Chr9
(one of the sex chromosomes in our synthetic genome).  For the males
they are generated as haploid variants and for the female they are
diploid.  In addition, the second daughter has the occasional de novo
variant (annotated with a DN value of 'Y') in the final sample column
corresponding to daughter2.

EOF
pause
docommand "$RTG" extract family.vcf.gz Chr9:10000+10000
pause

cat<<EOF

If we use \`rtg sdfstats --lengths\` to look at the genome SDF for one of
the male samples, we see that the chromosomes have been generated
according to the appropriate ploidy for the sample sex.  Each chromosome
incorporates the appropriate variant alleles specified for that sample
in the VCF, so you will see slight variations in the lengths of diploid
pairs.

EOF
pause
docommand "$RTG" sdfstats --lengths genome-son1.sdf
pause



cat<<EOF

Read Simulation
---------------

Now we have a reference genome and have also generated simulated
genomes for the members of our family, we can simulate next generation
sequencing of the sample genomes, using \`rtg readsim\`.  We will
simulate 2 x 100bp paired-end Illumina sequencing using a mean
fragment size of 300bp, at a sequencing coverage of about 20x for most
of our samples (since our sample genome SDFs include two copies of
each diploid chromosome, we instruct the \`readsim\` command to use
coverage 10).  One of the samples will be generated at a lower
coverage.  The simulated reads include sequencer errors according to
the selected machine type.  The results of read simulation will be
stored in an SDF containing the reads for each sample.

EOF
pause
#readsim_opts="--machine=illumina_pe -L 100 -R 100 -m 200 -M 400 --coverage 10 --qual-range 2-20 --Xmnp-event-rate=0.02 --Xinsert-event-rate=0.005 --Xdelete-event-rate=0.005"
readsim_opts=("--machine=illumina_pe" -L 100 -R 100 -m 200 -M 400 --qual-range 2-20)
rgcommon="PL:ILLUMINA\\tPI:300\\tDS:Simulated dataset"
seed=5643
for genome in father mother son2 daughter1 daughter2; do
    seed=$((seed + 5))
    docommand "$RTG" readsim --input genome-$genome.sdf --output reads-$genome.sdf --seed $seed --sam-rg "@RG\\tID:rg_$genome\\tSM:$genome\\t$rgcommon" --coverage 10 "${readsim_opts[@]}" || exit 1
done
genome=son1
seed=$((seed + 5))
docommand "$RTG" readsim --input genome-$genome.sdf --output reads-$genome.sdf --seed $seed --sam-rg "@RG\\tID:rg_$genome\\tSM:$genome\\t$rgcommon" --coverage 5 "${readsim_opts[@]}"|| exit 1

cat<<EOF

The \`sdfstats\` command can also be used to retrieve information about
read sets as we have included SAM read group information directly in the
SDF.  Let's also extract some of the reads in SAM format using
\`rtg sdf2sam\` so you can see what they look like.  Similarly, the reads
could be extracted using \`rtg sdf2fastq\` in order to generate FASTQ
files for read alignment tools.

EOF

pause
docommand "$RTG" sdfstats reads-son1.sdf
docommand "$RTG" sdf2sam --input reads-son1.sdf --output - --start-id 0 --end-id 5


cat<<EOF

At this point the VCF and read sets we have created could be used as the
basis of running alignment and variant calling tests to simulate various
scenarios.  RTG Core provides a full suite of alignment and variant
calling modules, but here we continue with the processing and analysis
available in RTG Tools.



Variant Set Processing and Comparison
-------------------------------------

Now let's run some variant manipulation commands that you might
typically use (although usually these commands would be run on various
VCF files that have been obtained from a variant caller or other
sources).

First let's run a quick check to see whether the calls in the VCF obey
pedigree by running our VCF file through \`rtg mendelian\`.  This
command outputs statistics on parental concordance and calls
inconsistent with Mendelian inheritance.  In this case we use the
explicit pedigree file we created earlier rather than the pedigree
embedded in the VCF header.  There will be be some non-mendelian
variants corresonding to the de novo events in the second daughter.

EOF
pause
docommand "$RTG" mendelian --template reference.sdf --pedigree family.ped --input family.vcf.gz
pause

cat<<EOF

RTG includes a sophisticated variant comparison tool called \`vcfeval\`,
which is able to deal with the fact that there are multiple equivalent
ways to represent the same variants, particularly those involving
complex situations such as block substitutions or indels.  Unlike other
tools that compare variant positions, alleles, and genotypes directly,
\`vcfeval\` performs comparison at the level of haplotypes by replaying
the variants into the reference.  This level of comparison is important
in real world datasets, particularly when comparing calls produced by
different calling algorithms, when comparing calls between samples that
have not been jointly called, or when comparing calls against a gold
standard baseline.  In this demo we are dealing with simulated variants
that have a consistent representation across the samples, so this aspect
of \`vcfeval\` is not important.  Nonetheless we will show how you might use
\`vcfeval\` for general variant comparisons and intersections.

EOF
pause

cat<<EOF

In most comparisons we use two separate VCF files, so \`vcfeval\` asks
for a "baseline" VCF and a "calls" VCF.  Variants that are matched
between both files are called "true positives", those contained in the
baseline but not in the calls are called "false negatives", and those
contained in the calls but not in the baseline are called "false
positives".  This terminology follows from the common practice of
comparing a call set with a set of known gold-standard variants for the
sample, but the actual process of finding matches between two samples is
quite general.

First off, let's compare two of our samples to find the shared
genotypes.  This is the default comparison mode for \`vcfeval\`, and is
appropriate when you want to compare a call set against the gold
standard for that sample, such as the commonly used NA12878 sample.
Thus, in this mode, the variants must also have the same zygosity in
order to match.  Let's find the genotypes shared by the father and son.
Note that in this case we will use the same VCF file for both, and since
the input is a multi-sample VCF, we need to specify which samples we
want to involve in the comparison.  In cases where the sample name is
not the same between the baseline and calls, you can use a
comma-separated notation to indicate the two sample names, with the
baseline sample name first.

EOF
pause
docommand "$RTG" vcfeval --template reference.sdf --baseline family.vcf.gz --calls family.vcf.gz --sample father,son1 -o father-son1-genotypes

cat<<EOF

We can see how many genotype matches there are between the father and
son, and some common benchmarking accuracy metrics are also included
in the summary.  If we want to see how many variants are shared,
regardless of zygosity, we can use the \`--squash-ploidy\` option
(this is also an appropriate option to use when looking to match
somatic variant calls against a variant database).

EOF
pause
docommand "$RTG" vcfeval --template reference.sdf --baseline family.vcf.gz --calls family.vcf.gz --sample father,son1 --squash-ploidy -o father-son1-alleles

cat<<EOF

As well as a summary of the overall calling accuracy, \`vcfeval\`
creates an output file for performing ROC analysis.  Variant callers
often err on the side of outputting too many variants (favoring
sensitivity), since it's always possible to post-filter to remove false
positives.  ROC analysis assists with choosing appropriate filter
thresholds, by demonstrating the trade-off between sensitivity and
precision with respect to a filter threshold.  The default is to analyse
using the GQ FORMAT field, which is not present in our simulated VCF,
but when you run \`vcfeval\`, you can supply the \`--vcf-score-field\`
option to use a different attribute that should be correlated with the
correctness of the variant (such as QUAL or AVR).

For our simulation we only have the allele frequency INFO annotation
created during the initial \`popsim\` command.  However, we expect
alleles common to both parents to more likely be alleles with higher
frequency in the population, and that those not in common to be those
with lower population allele frequency.  Let's see if that is true,
first by running the \`vcfeval\` using the AF field as the ROC scoring
attribute, and then generating the ROC plot using the \`rocplot\`
command.

EOF
pause
docommand "$RTG" vcfeval --template reference.sdf --baseline family.vcf.gz --calls family.vcf.gz --sample father,mother --squash-ploidy --vcf-score-field INFO.AF -o father-mother-alleles
docommand "$RTG" vcfeval --template reference.sdf --baseline family.vcf.gz --calls family.vcf.gz --sample father,daughter1 --squash-ploidy --vcf-score-field INFO.AF -o father-daughter-alleles


cat<<EOF

The \`rocplot\` command allows ROC plots for multiple ROC data files to
be viewed using either an interactive GUI, or via creation of a static
image.  We'll generate a static image now, but encourage you to try the
interactive GUI.  (As an alternative, you could also decompress
\`weighted_roc.tsv.gz\` file and load it as tab-separated file into a
spreadsheet or other graphing package.)

EOF
pause
docommand "$RTG" rocplot --title Shared-by-AF father-{mother,daughter}-alleles/weighted_roc.tsv.gz --png=roc-father-af.png
doimage roc-father-af.png

cat<<EOF

In an ideal ROC curve, sorting variants by the scoring field would
result in all of the true positive variants coming first (on the left
hand side of the curve), resulting in an initial steep incline.  The
false positive variants would come in toward the end of the set
(i.e. resulting in a flattening of the curve heading to the right).  You
can see from the shape of the ROC curve above that indeed, AF is a
somewhat reasonable indicator of the likelihood of the variants being
shared between the samples that we compared.

That's it for the demo, hopefully it gave you a taste of what you can
do with RTG Tools.  Feel free to look around in the various output
files that were created inside this ${DEMODIR} directory, and try out
RTG on some real data of your own.

EOF

