# Scala parser benchmarks

This repo aims to provide a standardised JMH benchmark for comparing the performance
of common Scala parsing libraries. 

Included:

 * [FastParse1](https://github.com/lihaoyi/fastparse)
 * [FastParse2](https://github.com/lihaoyi/fastparse)
 * [Parsley](https://github.com/J-mie6/Parsley)
 * [atto](http://tpolecat.github.io/atto/)
 * [scala-parser-combinators](https://github.com/scala/scala-parser-combinators)
 * [parsec-for-scala](https://bitbucket.org/J_mie6/parsec-for-scala)
 * [Meerkat](http://meerkat-parser.github.io/index.html)
 * [parboiled2](https://github.com/sirthias/parboiled2)

Attempted:

 * [parseback](https://github.com/djspiewak/parseback) - failed to parse a BF grammar even after 
 transformation

Pending:

 * [fcd](https://github.com/b-studios/fcd)
 

**Note**

*parsec-for-scala* contains two branches of which one is a port of the Haskell 
[parsec](https://github.com/haskell/parsec) library and the other is an 
implementation of the shift-reduce parser(needs confirmation). This specific library is still being
actively developed so changes are tracked using submodules.

## Running

Make sure tests are all green, run:

    ./sbt test

To start benchmarking all modules:

    ./sbt benchmarkAll
    
The arguments to JMH is `-i 10 -wi 10 -f 1 -t 1 -rf json`, benchmarks will take ~1 hour per module.
The results will be written to the `docs` directory as JSON files with the `jmh_` prefix.

The `docs` directory has all the required files for the benchmark to be viewable in a browser. Start a web server in the `docs` directory and navigate to `report.html`

## Methodology 

Each benchmark lives in a separate module as described in `build.sbt` with the JMH plugin enabled for benchmarking. 

Previously, all benchmarks was written in one module with all required parsers as library dependencies but this was problematic for the following reason: 

 * When multiple version of the same library are required the benchmark may not be accurate.
 * Benchmarking parsers with the same package name(e.g different versions of the same parser) involves shading which could be unfair.


## Results

Latest JMH reports are [here](https://tom91136.github.io/scala-parser-benchmarks/report.html)

<!---
Latest PDF results are [here](results/data.pdf)
-->

Benchmark machine:
```
# JMH version: 1.21
# VM version: JDK 1.8.0_161, VM 25.161-b12
# VM invoker: /usr/java/jdk1.8.0_161/jre/bin/java
# Warmup: 15 iterations, 1 s each
# Measurement: 15 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op

Linux 4.18.16-200.fc28.x86_64 #1 SMP Sat Oct 20 23:53:47 UTC 2018 x86_64 x86_64 x86_64 GNU/Linux
Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz OC'ed @ 4.70Ghz
32GB RAM
 
```

Currently implemented benchmark fixtures are:

 * Left recursive simple
 	```
 	<one>  ::= "1"
 	<rule> ::= <one> "+" <one> | <one>
 	```
 * [BrainFuck](https://github.com/brain-lang/brainfuck) syntax
 * [JSON](https://www.json.org/) syntax
 
	
More meaningful fixtures will be added in the future; pull requests welcome.
Fixtures can be found in [this](src/main/scala/net/kurobako/spb) package.

