# References
This is a just running list of useful references found during the development of this project.

## Java/JVM Talks
- [invokedynamic for Mere Mortals](https://www.youtube.com/watch?v=Q5mVy0BAxG0#t=6h15m30s)
- [The Adventurous Developer’s Guide to JVM Languages](https://www.youtube.com/watch?v=Q5mVy0BAxG0#t=7h44m20s)
- [Black Hat USA 2002 - Security Aspects in Java Bytecode Engineering](https://www.youtube.com/watch?v=DYY0FSnaQXE)
- [Black Hat USA 2012 - Recent Java Exploitation Trends and Malware](https://www.youtube.com/watch?v=5JN65JZmHjM)
- [Black Hat USA 2013 - Java Every-Days: Exploiting Software Running on 3 Billion Devices](https://www.youtube.com/watch?v=HO0CkhndCQQ)

## Runtime Patching
- [Rubah is a Dynamic Software Updating (DSU) system for Java that works on stock JVMs](https://github.com/plum-umd/rubah), [Quick Start](http://web.ist.utl.pt/~luis.pina/oopsla14/aec/getting-started.html), [Steps](http://web.ist.utl.pt/~luis.pina/oopsla14/aec/step-by-step.html), [Slides](https://www.infoq.com/presentations/rubah), [Paper](http://www.cs.umd.edu/~mwh/papers/rubah-oopsla14.pdf)
- [Java Geeks Using the BootClasspath - Tweaking the Java Runtime API](http://www.tedneward.com/files/Papers/BootClasspath/BootClasspath.pdf)
- [Covert Java: Techniques for Decompiling, Patching, and Reverse Engineering](http://www.amazon.com/gp/product/0672326388/ref=as_li_tl?ie=UTF8&camp=1789&creative=390957&creativeASIN=0672326388&linkCode=as2&tag=zombiest-20&linkId=6WARWI6KSNMYLBWS)
- [-Xbootclasspath Oracle Docs](https://docs.oracle.com/cd/E15289_01/doc.40/e15062/optionx.htm#i1018570)
- Hotpatching a Java 6 Application ([Part 1](http://www.fasterj.com/articles/hotpatch1.shtml) and [Part 2](http://www.fasterj.com/articles/hotpatch2.shtml))
- [Java Endorsed Standards Override Mechanism](https://docs.oracle.com/javase/7/docs/technotes/guides/standards/)
- [JRebel Explained](http://zeroturnaround.com/rebellabs/reloading-objects-classes-classloaders/)
- [Stack Overflow Question on Editing rt.jar](https://stackoverflow.com/questions/8433047/overriding-single-classes-from-rt-jar)
- [JEP 159: Enhanced Class Redefinition](http://openjdk.java.net/jeps/159)
- [HotswapAgent](http://www.hotswapagent.org/quick-start) + [Hotswap Projects](https://github.com/HotswapProjects)
- [Oracle FPUpdater Tool](http://www.oracle.com/technetwork/java/javase/fpupdater-tool-readme-305936.html)
- [ClassLoader to Reload Class Definitions](https://stackoverflow.com/questions/3971534/how-to-force-java-to-reload-class-upon-instantiation)
- [DYNAMIC SOFTWARE UPDATING (Dissertation)](http://www.cs.umd.edu/~mwh/papers/thesis.pdf)
- [Dynamic Software Updating for Java](http://www.luispina.me/projects/rubah.html)

## Bytecode Manipulations
- [ASM Whitepaper](http://asm.ow2.org/current/asm-eng.pdf)
- [ASM Transformations Whitepaper](http://asm.ow2.org/current/asm-transformations.pdf), [MIRROR](https://src.fedoraproject.org/lookaside/extras/asm2/asm-transformations.pdf/991a1ccdb3e79fe393aed7477f4f7ca5/asm-transformations.pdf)
- [Merging Classes with ASM](http://www.jroller.com/eu/entry/merging_class_methods_with_asm)
- [Updated Version of ASM Bytecode Outline Plugin](http://andrei.gmxhome.de/bytecode/index.html)
- [Konloch Bytecode Viewer](https://github.com/Konloch/bytecode-viewer)
- [BCEL](https://commons.apache.org/proper/commons-bcel/manual.html)

## Intermediate Languages
- [Soot](https://sable.github.io/soot/)
- [Soot Command Line Options](https://ssebuild.cased.de/nightly/soot/doc/soot_options.htm)

## Eclipse Plugin Development
- [Project Builders and Natures](https://eclipse.org/articles/Article-Builders/builders.html)
- [Incremental Builders](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2FresAdv_builders.htm)
- [Eclipse Launch Frameworker](https://www.eclipse.org/articles/Article-Launch-Framework/launch.html)
- [Eclipse Launchers](http://alvinalexander.com/java/jwarehouse/eclipse/org.eclipse.jdt.launching/launching/org/eclipse/jdt/internal/launching/JavaAppletLaunchConfigurationDelegate.java.shtml)
- [Eclipse Java Launcher Example](https://eclipse.org/articles/Article-Java-launch/launching-java.html)
- [Eclipse Launch Shortcuts](http://opensourcejavaphp.net/java/eclipse/org/eclipse/jdt/internal/debug/ui/launcher/JavaLaunchShortcut.java.html)
- [Launch Shortcut Example](http://grepcode.com/file_/repository.grepcode.com/java/eclipse.org/3.5.2/org.eclipse.jdt.debug/ui/3.4.1/org/eclipse/jdt/debug/ui/launchConfigurations/JavaApplicationLaunchShortcut.java/?v=source)

## Other
- [JVM Internals](http://blog.jamesdbloom.com/JVMInternals.html)
