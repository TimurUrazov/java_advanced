## Implementor

**Task**: Develop an ``Implementor`` class which will generate implementations of classes and interfaces.  

* Command-line argument: The full name of the class / interface to generate the implementation for.  
* As a result, the java code of the class with the Impl suffix must be generated, extending (implementing) the specified class (interface).
* The generated class should compile without errors.
* The generated class must not be abstract.
* Methods of the generated class should ignore their arguments and return default values.  

In the task, there are three options:

The Implementor must be able to implement both classes and interfaces. Generics support is not required.

**Jar Implementor**: 

1. Create it `.jar` file containing the compiled Implementor and associated classes.
2. Created  `.jar` file should be launched with the java `-jar` command.
3. Runable `.jar` file must accept the same command-line arguments as the `Implementor` class.
Implementor with arguments `-jar <class-name> <filename.jar>` should pack the generated implementation to given `<filename.jar>` file.
The solution must be modularized.

**Java doc**:

1. Document the `Implementor` class and related classes using Javadoc.
    - All classes and all class members, including `private`, must be documented.
    - Documentation should be generated without warnings.
    - The generated documentation must contain correct references to the standard library classes.
2. You must also submit:
    - a script for generating documentation;
    - generated documentation.
3. This homework assignment is given only together with the previous one. You will not be able to pass the previous homework separately.
