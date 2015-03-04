# Noisy Gold Miners

	Hic sunt dracones

This is a library to help research with agent perception correction using probabilistic methods.
In particular this library uses [Jason](http://jason.sf.net) to simulate some Gold Miners competitions of the [multi-agent programming contest](https://multiagentcontest.org/).

The original simulation is changed by adding some noise to agent perception (_ie_ with a given probability the value of a cell perception is changed). Then agent/team performance is evaluated on the number of collected golds. As expected, the greater the noise the smallest the performance.

Probabilistic methods are supported in [jpgm](https://github.com/fmgc/jpgm), a sister project to provide the sparse matrix computations required by the Dynamic Bayesian Network models used here.

## Instalation

1. clone this repository
2. create a sub-directory `lib`
3. place there `jason.jar` and `search.jar` (that come with [Jason](http://jason.sf.net)) and `jpgm.jar` (see the building instructions in [jpgm](https://github.com/fmgc/jpgm))
4. `ant` should work 

## Documentation

`WIP`