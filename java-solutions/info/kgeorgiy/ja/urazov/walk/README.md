## Recursive file hasher
**Task:** Develop RecursiveWalk class that calculates the hash sums of files in directories.  
The input file contains a list of files and directories that you need to walk. Directories are traversed recursively.

_Example:_

Input file

>samples/binary  
>samples  
>samples/no-such-file  

Output file

>005501015554abff samples/binary  
>0000000000000031 samples/1  
>0000000000003132 samples/12  
>0000000000313233 samples/123  
>0000000031323334 samples/1234  
>005501015554abff samples/binary  
>0000000000000000 samples/no-such-file  

When completing the task, you should pay attention to:

1. Design and exception handling, error diagnostics. The program must terminate correctly even in the event of an error. 
2. Correct I / O operation.
3. No resource leakage.
