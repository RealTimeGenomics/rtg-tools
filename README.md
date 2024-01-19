# RTG Tools

Copyright (c) 2018 Real Time Genomics Ltd

This software is provided under the Simplified BSD License. See
[LICENSE.txt](LICENSE.txt)

## Introduction

RTG Tools is a subset of
[RTG Core](https://github.com/RealTimeGenomics/rtg-core)
that includes several useful utilities for dealing with VCF files and
sequence data.  Probably the most interesting is the `vcfeval`
command which performs sophisticated comparison of VCF files.

Conventional tools attempt comparison by directly comparing variant
positions, alleles, and genotypes, however they are inherently unable
to deal with differences in representation that commonly arise,
particularly when dealing with complex variants or when comparing
variants produced by different callers.  More details are in this
presentation on
[slideshare](http://www.slideshare.net/GenomeInABottle/140127-rtg-vcfeval-vcf-comparison-tool)
and this manuscript on
[bioRxiv](http://biorxiv.org/content/early/2015/08/02/023754).
Comparison approaches based on normalization or decomposition can
alleviate these problem but often fail to deal with more complex
situations.

RTG `vcfeval` performs variant comparison at the haplotype level, that
is, it determines whether the genotypes asserted in the VCFs under
comparison result in the same genomic sequence when applied to the
reference genome.  This in itself is a non-trivial problem and naive
approaches face a combinatorial explosion to determine the most
accurate analysis.  To date, no other tool is capable of performing
this analysis as accurately and as fast as RTG `vcfeval`.  RTG
developed `vcfeval` for in-house use in 2010, and through our
collaborations we found this tool to be highly useful outside of
RTG. RTG vcfeval outputs VCF files containing the results of
comparison, summary metrics, and ROC curve data files.

In conjunction with `vcfeval`, the `rocplot` command provides an easy
way to interactively examine the ROC curves from one or more `vcfeval`
runs as an aid to selecting appropriate scoring attributes and
filtering thresholds. A quick screen capture is shown below:

![rocplot-screencap](rocplot-screencap.gif)

In order to encourage wider adoption of best-practise methods for
variant comparison and benchmarking, Real Time Genomics made RTG Tools
freely available, and now this includes the source code under an OSI
approved open source licence.  RTG Tools are mature, well tested, and
under ongoing development.

RTG Tools is available pre-packaged directly from our
[website](http://realtimegenomics.com/products/rtg-tools/), or follow
the instructions below to build from this repository.

If you have a need for variant calling or metagenomics analysis,
please consider our full analysis suite
[RTG Core](http://realtimegenomics.com/products/rtg-core/).


## Support

A user manual is included within the installation in both PDF and HTML
versions. These may also be viewed
online ([HTML](https://realtimegenomics.github.io/rtg-tools/index.html),
[PDF](https://cdn.rawgit.com/RealTimeGenomics/rtg-tools/master/installer/resources/tools/RTGOperationsManual.pdf)).

You can use the commands in RTG Tools to format your own reference
datasets, or download common
[pre-formatted references](http://realtimegenomics.com/news/pre-formatted-reference-datasets/)
from our website.

An
[rtg-users](https://groups.google.com/a/realtimegenomics.com/forum/#!forum/rtg-users)
discussion group is now available for general questions, tips, and
other discussions.

To be informed of new software releases, subscribe to the low-traffic
[rtg-announce](https://groups.google.com/a/realtimegenomics.com/forum/#!forum/rtg-announce)
group.

---

## Prerequisites for building from source

* Java 1.8 or later
* apache ant 1.9 or later

## Check out source code for RTG Tools

    $ git clone https://github.com/RealTimeGenomics/rtg-tools.git
    $ cd rtg-tools

## Compile / run unit tests

    $ ant runalltests

## Build RTG Tools package

To build the RTG Tools package which can be locally installed and run:

    $ ant zip-nojre

This will create an installation zip file under `dist`.

## Installation

Uncompress the installation zip:

    $ cd /my/install/dir/
    $ unzip /path/to/rtg-tools/dist/rtg-tools-VERSION-nojre.zip

Follow the instructions contained in the `README.txt`. This build will
use the system Java by default, so ensure that it is Java 1.8 or
later.

For a quick demonstration of the features of RTG Tools for simulation
and VCF processing on data generated from scratch, run the
`demo-tools.sh` script contained in the scripts subdirectory of the
installation directory:

    $ cd /my/install/dir/rtg-tools-VERSION/
    $ ./scripts/demo-tools.sh $PWD/rtg

