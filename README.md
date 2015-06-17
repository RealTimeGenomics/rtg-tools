# RTG Tools

Copyright (c) 2014 Real Time Genomics Ltd

This software is provided under the Simplified BSD License. See [LICENSE.txt](LICENSE.txt)

---

## Prerequisites

* Java 1.7 or later
* apache ant 1.9 or later

## Check out source code for RTG Tools

    $ git clone https://github.com/RealTimeGenomics/rtg-tools.git
    $ cd rtg-tools

## Compile / run unit tests

    $ ant runalltests

## Building RTG Tools

To build the RTG Tools package which can be locally installed and run:

    $ ant zip-nojre

This will create an installation zip file under 'dist'.

## Installation

Unzip the installation zip file in your installation location and follow the instructions contained in the README.txt. This build will use the system Java by default, so ensure it is Java 1.7 or later.

## Release history

See [doc/ReleaseNotes.txt](doc/ReleaseNotes.txt) for release history details.

## Support

An [rtg-users](https://groups.google.com/a/realtimegenomics.com/forum/#!forum/rtg-users) discussion group is now available for general questions, tips, and other discussions.

To be informed of new software releases, subscribe to the low-traffic [rtg-announce](https://groups.google.com/a/realtimegenomics.com/forum/#!forum/rtg-announce) group.

