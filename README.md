# Scala parser benchmarks

This repo aims to provide a standardised JMH benchmark for comparing the performance
of common Scala parsing libraries. 

Included:

 * [FastParse](https://github.com/lihaoyi/fastparse)
 * [Parsley](https://github.com/J-mie6/Parsley)
 * [atto](http://tpolecat.github.io/atto/)
 * [scala-parser-combinators](https://github.com/scala/scala-parser-combinators)
 * [parsec-for-scala](https://bitbucket.org/J_mie6/parsec-for-scala)
 * [Meerkat](http://meerkat-parser.github.io/index.html)

Pending:

 * [parboiled2](https://github.com/sirthias/parboiled2)
 * [parseback](https://github.com/djspiewak/parseback)
 * [fcd](https://github.com/b-studios/fcd)
 

**Note**

*parsec-for-scala* contains two branches of which one is a port of the Haskell 
[parsec](https://github.com/haskell/parsec) library and the other is an 
implementation of the shift-reduce parser(needs confirmation). This specific library is still being
actively developed so changes are tracked using submodules.


## Results

Latest results are [here](results/data.pdf)

Benchmark machine:
```
# JMH version: 1.20
# VM version: JDK 1.8.0_161, VM 25.161-b12
# VM invoker: /usr/java/jdk1.8.0_161/jre/bin/java
# Warmup: 15 iterations, 1 s each
# Measurement: 15 iterations, 1 s each
# Timeout: 10 min per iteration
# Threads: 1 thread, will synchronize iterations
# Benchmark mode: Average time, time/op

Linux 4.15.4-300.fc27.x86_64 #1 SMP Mon Feb 19 23:31:15 UTC 2018 x86_64 x86_64 x86_64 GNU/Linux
Intel(R) Core(TM) i7-6700K CPU @ 4.00GHz OC'ed @ 4.50Ghz
32GB RAM
 
```

Currently implemented benchmark fixtures are:

 * Left recursive simple
 	```
 	<one>  ::= "1"
 	<rule> ::= <one> "+" <one> | <one>
 	```
More meaningful fixtures will be added in the future; pull requests welcome.
Fixtures can be found in [this](src/main/scala/net/kurobako/spb) package.

To run benchmarks:

    ./sbt jmh:run