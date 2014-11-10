# CFGScanDroid #
CFGScanDroid is a utility for comparing control flow graph (CFG) signatures to the control flow graphs of Android methods.

It was designed as a scanner for malicious applications. 

Talk here: http://www.irongeek.com/i.php?page=videos/derbycon4/t420-control-flow-graph-based-virus-scanning-douglas-goddard

## Building ##

If you do not have Maven installed:

`sudo apt-get install maven`

(If you're on a non-Debian OS, I believe in you and your ability to get Maven installed.)

If you have Maven, the build script should run the correct command:

`./build.sh`

This will create a file:

`target/CFGScanDroid-0.1-jar-with-dependencies.jar`

## Running ##

`java -jar target/CFGScanDroid-0.1-jar-with-dependencies.jar`

	Usage: <main class> [options]
	  Options:
	    -f, -dex-files
	       DEX file(s) to run
	       Default: []
	    -d, -dump-sigs
	       Dump signature for each method of each DEX file
	       Default: false
	    -e, -exact-match
	       Only match complete signature CFG to function CFG
	       Default: true
	    -n, -normalize
	       Normalize the control flow graph to have a single entry point
	       Default: false
	    -o, -one-match
	       Only match once on a method
	       Default: false
	    -p, -partial-match
	       Find the signature graph within the function graph
	       Default: false
	    -m, -print-matched
	       Print when a match is found
	       Default: true
	    -t, -print-statistics
	       Print signature statistics after scan
	       Default: true
	    -u, -print-unmatched
	       Print when no match is found
	       Default: true
	    -r, -raw-signature
	       Pass a signature in raw on the command line
	       Default: []
	    -h, -short-identifier
	       Do not print full CFG identifier
	       Default: false
	    -s, -sig-file
	       A file containing signatures
	       Default: []
	    -i, -simple-match
	       Match exact on vertex and edge count only (fp prone)
	       Default: false
	    -g, -subgraph
	       Tries to match signature depth 0 to each function vertex
	       Default: false

Basic usage will be:

`java -jar target/CFGScanDroid-0.1-jar-with-dependencies.jar -s path/to/sigfile.adb -f path/to/files/`

If you don't have any signatures, you can dump the signatures for a file with `-d`:

`java -jar target/CFGScanDroid-0.1-jar-with-dependencies.jar -d -f path/to/file/classes.dex`

Files can be APKs or DEX files. Obviously DEX will be faster as it does not have to unzip to reach the code.

If you don't want to store your signature in a file, you can use `-r "SOMESIGNATURE"` to pass a signature in on the command line.

If the code is running to fast for you, there are some good ways to slow it down.

The `-n` option normalizes the control flow graph, removing any vertices not reachable from V0 (entry point). This is to get rid of catch blocks that have no branch to them.

The `-p` option enables partial graph matching. This means a signature will be compared to any function with >= the number of vertices and edges as it.

The `-g` option enables subgraph matching. This will normalize the graphs and try to match the signature's V0 with every node in a graph. This has not been optimized at all so it is considerably slow for a large number of samples and signatures.

If there is too stuff printing to the screen, there are some solutions to that.

Using `-u false` will hide unmatched samples, likewise `-m false` will hide matches.

Stats are printed at the end, if you don't like this you can use `-t false` to hide them.

If the class/path.functionName() is too long of an identifier for you, `-h` will shorten it to path.functionName(). Truncating anything before the final /.

If you don't need multiple alerts on the same method, you can use `-o` to stop scanning a method once it is identified with one signature.

## Signature Format ##

The signatures are of format NAME;VERTEX_COUNT;ADJACENCY_LIST -

The adjacency list is of the format PARENT_BB:CHILD_BB[,CHILD_BB,...];

An example:

`interceptSMS;4;0:1,2;1:3;2:3,0`

The signature name is `interceptSMS`, the next fields indicates there are 4 vertices.

Breaking out the adjacency list:

    0:1,2
    1:3
    2:3,0

Vertex 0 branches to V1 and V2. V1 branches to V3. V2 branches to V3 and V0.

Or if you prefer crappy diagrams:

        0 <----
      /   \   |
     1     2__|
      \   /
        3

## Contact ##

Feel free to open an issue here for any problems or feature requests.

Thanks!
