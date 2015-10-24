# JReFrameworker
A practical tool for creating Managed Code Rootkits (MCRs) in the Java Runtime Environment

![Java Evil Edition](./images/Java-Evil-Edition-Horizontal.jpg)

## Overview
This project aims to extend the work done by Erez Metula in his book [Managed Code Rootkits: Hooking into Runtime Environments](http://amzn.to/1LuFMaF). The work outlines a tool [ReFrameworker](https://appsec-labs.com/managed_code_rootkits/#Tool) that claims to be a framework modification tool capable of performing *any* modification task, however the tool falls short in usability. Developing new attack modules is difficult as most users are not familiar with working in the intermediate representations (IR) required by the tool.  Worse yet, the "write once, run everywhere" motto of managed languages is violated when dealing with runtime libraries, forcing the attacker to write new exploits for each target platform. The current version of ReFrameworker (version 1.1) does not have the ability to manipulate Java bytecode, although Erez Metula points out that the same techniques of using IRs such as [Soot's Jimple](https://sable.github.io/soot/) or the [Jasmin](http://jasmin.sourceforge.net/) assembler can be used to create Java MCRs.

## JReFrameworker
Since ReFrameworker is no longer maintained, this project aims to extend previous works by introducing JReFrameworker, a tool to extend MCR capabilities to the Java Runtime Environment in a user-friendly way. 

JReFrameworker is an Eclipse plugin for creating and building projects that allow the user to write annotated Java source that is automatically merged or inserted into the runtime, which also allows the user to develop and debug attack modules. Working at the intended abstraction level of source code allows the attacker to "write once, exploit everywhere"".

For more details visit: [ben-holland.com/JReFrameworker/](https://ben-holland.com/JReFrameworker/)
