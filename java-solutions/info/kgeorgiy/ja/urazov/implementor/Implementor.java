package info.kgeorgiy.ja.urazov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation class of {@link JarImpler} interface
 * @author Timur Urazov
 */

public class Implementor implements JarImpler {
    /**
     * Returns simple name of class including no package name
     *
     * @param token type token
     * @return {@link Class#getSimpleName()} with 'Impl' suffix
     */
    private static String getSimpleImplName(final Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns name of class including simple name and package name
     *
     * @param token type token
     * @return {@link Class#getPackageName()} concatenated with {@link #getSimpleImplName(Class)} by '.'
     */
    private static String getFullImplName(final Class<?> token) {
        return token.getPackageName() + "." + getSimpleImplName(token);
    }

    /**
     * Returns path to file, determined by {@link #getFullImplName(Class)}, separated by given
     * separator, which extension is determined by suffix
     *
     * @param token type token
     * @param separator separator of file path
     * @param suffix extension of file
     * @return path to file which is defined by {@link #getFullImplName(Class)} called on given
     * type token with '.', replaced by separator, and file suffix appended
     */
    private static String getFullImplName(final Class<?> token, final char separator,
                                          final String suffix) {
        return getFullImplName(token).replace('.', separator) + suffix;
    }

    /**
     * Returns path to java source file, determined by {@link #getFullImplName(Class)},
     * separated by {@link File#separatorChar}, resolved against given directory
     *
     * @param directory directory to resolve file name against
     * @param token type token
     * @return java source file path defined by {@link #getFullImplName(Class, char, String)}
     * called with type token, {@link File#separatorChar}, ".java" suffix
     */
    private static Path getFullName(final Path directory, final Class<?> token) {
        return directory.resolve(getFullImplName(token, File.separatorChar, Decoration.JAVA_FILE));
    }

    /**
     * Examine type token for existence of private access modifier, checks whether it represents
     * primitive type, array or enum, whether it is final, and retrieve non-private constructors
     *
     * @param token type token
     * @return non-private constructors
     * @throws ImplerException if class or interface is private, if class is primitive, final,
     * represents array or enum or has only private constructors
     */
    private static List<Constructor<?>> checkToken(final Class<?> token) throws ImplerException {
        final int modifiers = token.getModifiers();

        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException(
                    "Cannot extend(implement) private class(interface): " + token.getName());
        }

        if (token.isInterface()) {
            return null;
        }

        if (token.isPrimitive() || token.isArray()) {
            throw new ImplerException(
                    "Entity is primitive type or an array class: " + token.getName());
        }
        // :NOTE: + .isEnum
        if (token == Enum.class || Modifier.isFinal(modifiers)) {
            throw new ImplerException(
                    "Cannot extend final or enum class: " + token.getName());
        }

        final List<Constructor<?>> constructors = Arrays
                .stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers())
                        || constructor.getParameterTypes().length != 0)
                .collect(Collectors.toList());

        if (constructors.isEmpty()) {
            throw new ImplerException("No public constructors available: " + token.getName());
        }

        return constructors;
    }

    /**
     * Creates parent directory for given file if {@link Path#getParent()} exists
     *
     * @param outputFile file parent directory for that to create
     * @throws ImplerException if {@link IOException} occurred while creating directory or if
     * file with such a name as directory already exists
     */
    private static void createOutputFileParentDir(final Path outputFile) throws ImplerException {
        final Path outputFileParentDir = outputFile.getParent();
        if (outputFileParentDir != null) {
            try {
                Files.createDirectories(outputFileParentDir);
            } catch (FileAlreadyExistsException e) {
                throw new ImplerException(
                        "File with name '" + outputFileParentDir
                                + "' already exists. " + e.getMessage());
            } catch (IOException e) {
                throw new ImplerException("Error creating parent directory. " + e.getMessage());
            }
        }
    }

    /**
     * Creates path to java file resolved against given root, which is defined by
     * {@link #getFullName(Path, Class)} and, if necessary, creates its parent directory by
     * calling {@link #createOutputFileParentDir(Path)}
     *
     * @param token type token of
     * @param root directory to resolve file name against
     * @return path to java source file
     * @throws ImplerException if it is unable to parse path or if #createOutputFileParentDir(Path)
     * threw it
     */
    private static Path createOutputFile(final Class<?> token, final Path root) throws ImplerException {
        final Path path;
        try {
            path = getFullName(root, token);
        } catch (InvalidPathException e) {
            throw new ImplerException("Cannot parse output file root directory name '" + root + "'.");
        }
        createOutputFileParentDir(path);
        return path;
    }

    /**
     * Generates implementation of class defined by type token and writes it to writer as Unicode
     * escaped sequence
     *
     * @param writer writer where generated code is written to
     * @param token type token representing class to implement
     * @param validConstructors non-private constructors
     * @throws IOException if error occurs while generated code is being written to output file
     */
    private static void generateAndWrite(final BufferedWriter writer, final Class<?> token,
                                         final List<Constructor<?>> validConstructors)
            throws IOException {
        final StringBuilder impl = new StringBuilder();
        final String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            impl.append(Decoration.packageDeclaration(packageName));
        }
        impl.append(Decoration.classDeclaration(token));
        if (!token.isInterface()) {
            impl.append(generateConstructors(validConstructors));
        }
        impl.append(Decoration.LINE_SEP);
        impl.append(generateMethods(token));
        impl.append(Decoration.BLOCK_END);
        writer.write(impl.toString()
                .chars()
                .mapToObj(ch -> String.format("\\u%04x", ch))
                .collect(Collectors.joining()));
    }

    /**
     * Wrap non-private and non-public methods among given to {@link MethodToGenerate} wrapper
     *
     * @param methods methods to be filtered and wrapped
     * @return methods wrapped to {@link MethodToGenerate}
     */
    private static Set<MethodToGenerate> convertMethods(final Method[] methods) {
        return collectFilteredAndMapped(Arrays.stream(methods),
                method -> {
                    final int modifiers = method.getModifiers();
                    return !Modifier.isPublic(modifiers) && !Modifier.isPrivate(modifiers);
                },
                MethodToGenerate::new,
                Collectors.toCollection(HashSet::new));
    }

    /**
     * Retrieves wrapped methods determined by {@link #convertMethods(Method[])} by recursively
     * traversing ancestors in tree of extension
     *
     * @param token type token which ancestors to traverse
     * @return wrapped methods of this class and its ancestors determined by
     * {@link #convertMethods(Method[])}
     */
    private static Set<MethodToGenerate> retrieveMethods(Class<?> token) {
        final Set<MethodToGenerate> methods = convertMethods(token.getDeclaredMethods());
        token = token.getSuperclass();
        if (token != null) {
            methods.addAll(retrieveMethods(token));
        }
        return methods;
    }

    /**
     * Retrieve public wrapped to {@link MethodToGenerate} methods of class
     * or interface ancestors and, in case of class, all wrapped methods defined by
     * {@link #retrieveMethods(Class)}, leave abstract methods and generate methods as
     * defined by {@link #generateMethod(Method)}
     *
     * @param token token which methods to generate
     * @return string representation of code block containing all generated methods
     */
    private static String generateMethods(final Class<?> token) {
        final Set<MethodToGenerate> methods = collectMapped(
                token.getMethods(),
                MethodToGenerate::new,
                Collectors.toCollection(HashSet::new));

        if (!token.isInterface()) {
            methods.addAll(retrieveMethods(token));
        }

        return collectFilteredAndMapped(
                methods.stream().map(MethodToGenerate::method),
                method -> Modifier.isAbstract(method.getModifiers()),
                Implementor::generateMethod,
                Collectors.joining(Decoration.LINE_SEP));
    }

    /**
     * Filter a given stream of data by given predicate, then apply mapping function and collect it
     * by given collector as defined by {@link #collectMapped(Stream, Function, Collector)}
     *
     * @param stream stream to be filtered, then to map function to and collect
     * @param predicate filtering predicate
     * @param extractor function to be applied
     * @param collector collector that collects filtered and mapped stream
     * @param <T> stream type
     * @param <U> type of applied function values
     * @param <R> type of collection
     * @return collection of filtered and mapped stream
     */
    private static <T, U, R> R collectFilteredAndMapped(final Stream<T> stream,
                                                        final Predicate<? super T> predicate,
                                                        final Function<? super T, ? extends U> extractor,
                                                        final Collector<? super U, ?, R> collector) {
        return collectMapped(stream.filter(predicate), extractor, collector);
    }

    /**
     * Apply mapping function to a given stream of data and collect it as defined by given collector
     *
     * @param stream to map function to and collect
     * @param extractor function to be applied
     * @param collector collector that collects mapped stream
     * @param <T> stream type
     * @param <U> type of applied function values
     * @param <R> collector that collects mapped stream
     * @return collection of mapped stream
     */
    private static <T, U, R> R collectMapped(final Stream<T> stream,
                                             final Function<? super T, ? extends U> extractor,
                                             final Collector<? super U, ?, R> collector) {
        return stream.map(extractor).collect(collector);
    }

    /**
     * Apply mapping function to a stream of given array as defined by
     * {@link #collectMapped(Stream, Function, Collector)} and collect
     * it as defined by given collector
     *
     * @param array array which values form stream
     * @param extractor function to be applied
     * @param collector collector that collects mapped stream
     * @param <T> array type
     * @param <U> type of applied function values
     * @param <R> collector that collects mapped stream
     * @return collection of mapped stream of given array
     */
    private static <T, U, R> R collectMapped(final T[] array,
                                             final Function<? super T, ? extends U> extractor,
                                             final Collector<? super U, ?, R> collector) {
        return collectMapped(Arrays.stream(array), extractor, collector);
    }

    /**
     * By calling {@link #generateBody(Executable, Function, String, String)}
     * on given method, function that retrieves returned type and name of it and default value
     * defined by {@link Decoration#returnValue(Class)} generate method which overrides the given one
     *
     * @param method method to be overridden
     * @return string representation of overridden method
     */
    private static String generateMethod(final Method method) {
        return generateBody(method,
                mth -> mth.getReturnType().getCanonicalName() + Decoration.SPACE + mth.getName(),
                Decoration.returnValue(method.getReturnType()), "return");
    }

    /**
     * Generates constructors by applying function {@link #generateConstructor(Constructor)} to
     * given constructors and then joining them
     *
     * @param constructors super constructors to be called in generated constructors
     * @return string representation of generated constructors
     */
    private static String generateConstructors(final List<Constructor<?>> constructors) {
        return String.join(Decoration.LINE_SEP, collectMapped(constructors.stream(),
                Implementor::generateConstructor, Collectors.toList()));
    }

    /**
     * Generate body of executable by calling {@link Decoration#unite(String, String, String, boolean)}
     * on generated head defined by {@link #generateHead(Executable, String)} calling on executable
     * and applied function on it, keyword, inside content
     *
     * @param executable executable to generate body of
     * @param function function to be applied on executable
     * @param inside content of given executable body
     * @param keyWord string representation of keyword
     * @param <T> type of executable
     * @return executable generated body
     */
    private static <T extends Executable> String generateBody(final T executable,
                                                              final Function<T, String> function,
                                                              final String inside,
                                                              final String keyWord)  {
        return Decoration.unite(generateHead(executable, function.apply(executable)),
                keyWord, inside, !(executable instanceof Constructor<?>));
    }

    /**
     * By calling {@link #generateBody(Executable, Function, String, String)}
     * on given constructor, function that retrieves name of this constructor declaring class as
     * defined by {@link #getSimpleImplName(Class)} and arguments as defined by calling
     * {@link #getArguments(Parameter[])} on its parameters, generate constructor which calls its
     * super constructor with same parameters
     *
     * @param constructor super constructor to be called
     * @return string representation of generated constructor
     */
    private static String generateConstructor(final Constructor<?> constructor) {
        return generateBody(constructor, ctor -> getSimpleImplName(ctor.getDeclaringClass()),
                Decoration.parenthesis(getArguments(constructor.getParameters())), "super");
    }

    /**
     * Generate head of executable as defined by {@link Decoration#head(String, String, Class[])}
     * by calling this function on name of executable, declaration of parameters executable gets
     * as defined by {@link #getMultipleDeclaration(Parameter[])} and exceptions it throws
     *
     * @param executable executable which head to generate
     * @param name name of executable
     * @return generated head of executable
     */
    private static String generateHead(final Executable executable, final String name) {
        return Decoration.head(name, getMultipleDeclaration(executable.getParameters()),
                executable.getExceptionTypes());
    }

    /**
     * Collects result of function mapped to a stream of array by joining it by comma with space
     * using {@code Collectors.joining}
     *
     * @param array given array on which elements to map function
     * @param function function to map on elements of array
     * @param <T> type of array elements
     * @return result of function mapped to a stream of array separated by commas and spaces
     */
    private static <T> String enumerate(final T[] array, final Function<T, String> function) {
        return collectMapped(array, function, Collectors.joining(", "));
    }

    /**
     * Turn parameter to string representation of this parameter and, if enableType is {@code true},
     * its return type
     *
     * @param enableType flag determining whether to retrieve returned type of parameter or not
     * @return parameter and optional returned type
     */
    private static Function<Parameter, String> getSingleDeclaration(final boolean enableType) {
        return parameter -> {
            String result = parameter.getName();
            if (enableType) {
                result = parameter.getType().getCanonicalName() + Decoration.SPACE + result;
            }
            return result;
        };
    }

    /**
     * Return arguments string representation to be passed to constructor by calling
     * {@link #enumerate(Object[], Function)} on parameters and {@link #getSingleDeclaration(boolean)}
     * function where {@code false} passed
     *
     * @param parameters parameters, which types required for getting arguments
     * @return arguments string representation to be passed to constructor
     */
    private static String getArguments(final Parameter[] parameters) {
        return enumerate(parameters, getSingleDeclaration(false));
    }

    /**
     * Return parameters string representation for method declaration by calling
     * {@link #enumerate(Object[], Function)} on parameters and {@link #getSingleDeclaration(boolean)}
     * function where {@code true} passed
     *
     * @param parameters parameters, which types required for getting method declaration
     * @return parameters string representation for method declaration
     */
    private static String getMultipleDeclaration(final Parameter[] parameters) {
        return enumerate(parameters, getSingleDeclaration(true));
    }

    /**
     * Generate exceptions string representation by calling {@link #enumerate(Object[], Function)}
     * on given exception types and {@link Class#getCanonicalName()}
     *
     * @param exceptions exception types to be represented
     * @return exceptions string representation
     */
    private static String getExceptions(final Class<?>[] exceptions) {
        return enumerate(exceptions, Class::getCanonicalName);
    }

    /**
     * @throws ImplerException if some exceptions occurred in {@link #checkToken(Class)} function,
     * if output file can not be opened or created or error occurred while writing to output file
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        final List<Constructor<?>> validConstructors = checkToken(token);
        final Path outputFilePath = createOutputFile(token, root);
        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {
            try {
                generateAndWrite(writer, token, validConstructors);
            } catch (IOException e) {
                throw new ImplerException("Error while writing to output file: '" + outputFilePath + "'.");
            }
        }  catch (IOException e) {
            throw new ImplerException("Cannot open or create output file: '" + outputFilePath + "'.");
        }
    }

    /**
     * Compiles given file with dependencies determined by code source of token
     *
     * @param file to be compiled
     * @param token type token for code source
     * @throws ImplerException if compiler could not be found, if exit code of compiled
     * does not equal to zero or if error occurred parsing uri of classpath
     */
    public static void compileFile(final String file, Class<?> token) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        try {
            final String classpath = Path.of(token.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toString();
            final String[] args = new String[] {"-cp", classpath, file};
            final int exitCode = compiler.run(null, null, null, args);
            if (exitCode != 0) {
                throw new ImplerException("Compilation error, exit code = " + exitCode);
            }
        } catch (URISyntaxException e) {
            throw new ImplerException("Error parsing uri of classpath");
        }
    }

    /**
     * @throws ImplerException if error occurred while copying classfile to jarfile, while
     * making outputstream from jarfile or while making {@link JarOutputStream} from jarfile and
     * manifest
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path root = jarFile.getParent();
        implement(token, root);

        final String compiledClass = getFullName(root, token).toString();

        compileFile(compiledClass, token);

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (final OutputStream outputStream = Files.newOutputStream(jarFile)) {
            try (final JarOutputStream jarOutputStream = new JarOutputStream(outputStream, manifest)) {
                final String jarClass = getFullImplName(token, '/', Decoration.CLASS_FILE);
                final JarEntry jarEntry = new JarEntry(jarClass);
                jarOutputStream.putNextEntry(jarEntry);
                try {
                    Files.copy(Path.of(compiledClass.replace(Decoration.JAVA_FILE,
                            Decoration.CLASS_FILE)), jarOutputStream);
                } catch (IOException e) {
                    throw new ImplerException("Error while copying classfile to jarfile");
                }
            } catch (IOException e) {
                throw new ImplerException("Error while making outputstream from jarfile");
            }
        } catch (IOException e) {
            throw new ImplerException("Error while making jarOutputstream from jarfile and manifest");
        }
    }

    /**
     * The record wraps method to add it in set so that it can be distinguished from the other
     * method even if their {@link Method#getDeclaringClass()} coincide
     *
     * @param method method to be wrapped
     */

    private record MethodToGenerate(Method method) {
        /**
         * Returns hash of {@link #method}, which depends on {@link Method#getName()},
         * {@link Method#getReturnType()} and {@link Method#getParameterTypes()}
         *
         * @return hash of {@link #method}
         */
        @Override
        public int hashCode() {
            return Objects.hash(method.getName(),
                    Arrays.hashCode(method.getParameterTypes()),
                    method.getReturnType());
        }

        /**
         * Compare two {@link MethodToGenerate} by {@link Method#getName()},
         * {@link Method#getReturnType()} and {@link Method#getParameterTypes()}
         *
         * @param o object to be compared
         * @return flag defining equivalence
         */
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (o instanceof final MethodToGenerate that) {
                final Method thatMethod = that.method();
                return thatMethod.getName().equals(method.getName())
                        && Arrays.equals(thatMethod.getParameterTypes(), method.getParameterTypes())
                        && thatMethod.getReturnType().equals(method.getReturnType());
            }
            return false;
        }
    }

    /**
     * The class is created for design logic of a class to be generated
     */

    private final static class Decoration {
        /**
         * Java source file suffix
         */
        private static final String JAVA_FILE = ".java";

        /**
         * Compiled class file suffix
         */
        private static final String CLASS_FILE = ".class";

        /**
         * Line separator, which {@link System#lineSeparator()} returned
         */
        private static final String LINE_SEP = System.lineSeparator();

        /**
         * Whitespace
         */
        private static final String SPACE = " ";

        /**
         * Single tabulation
         */
        private static final String TAB = "    ";

        /**
         * Double tabulation
         */
        private static final String DOUBLE_TAB = TAB.concat(TAB);

        /**
         * Semicolon and line separator {@link Decoration#LINE_SEP}
         */
        private static final String LINE_BREAK = ";" + LINE_SEP;

        /**
         * Block closing parenthesis and line separator {@link Decoration#LINE_SEP}
         */
        private static final String BLOCK_END = "}" + LINE_SEP;

        /**
         * Block opening parenthesis and line separator {@link Decoration#LINE_SEP}
         */
        private static final String BLOCK_BEGIN = "{" + LINE_SEP;

        /**
         * {@code public} access modifier
         */
        private static final String PUBLIC = "public";

        /**
         * Returns {@code implements} or {@code extends} keyword string representation
         * according to the type of token, which is either interface or class respectively
         *
         * @param token given token
         * @return {@code implements} or {@code extends} keyword string representation
         */
        private static String extendsOrImplements(final Class<?> token) {
            return token.isInterface() ? "implements" : "extends";
        }

        /**
         * Generate {@code package} declaration for the class to be generated
         *
         * @param packageName name of package to be declared
         * @return {@code package} declaration string representation
         */
        private static String packageDeclaration(final String packageName) {
            return "package" + SPACE + packageName + LINE_BREAK + LINE_SEP;
        }

        /**
         * Generate class declaration string representation with class or interface to be
         * extended or implemented using {@link #extendsOrImplements(Class)}
         *
         * @param token given token
         * @return class declaration string representation with {@link Decoration#BLOCK_BEGIN}
         */
        private static String classDeclaration(final Class<?> token) {
            return String.join(SPACE, PUBLIC, "class", getSimpleImplName(token),
                    extendsOrImplements(token), token.getCanonicalName(), BLOCK_BEGIN);
        }

        /**
         * Wrap given content into parenthesis
         *
         * @param inside given content to be surrounded by parenthesis
         * @return given content surrounded by parenthesis
         */
        private static String parenthesis(final String inside) {
            return "(" + inside + ")";
        }

        /**
         * Generate method or constructor declaration string representation by compounding name
         * of it with its parameters and string representation of exceptions using
         * {@link #getExceptions(Class[])}
         *
         * @param name name of method or constructor
         * @param parameters parameters of method or constructor
         * @param exceptions exceptions method or constructor throws
         * @return method or constructor string representation declaration with exceptions it
         * throws and {@link Decoration#BLOCK_BEGIN}
         */
        private static String head(final String name, final String parameters,
                                   Class<?>[] exceptions) {
            String exc = "";
            if (exceptions.length != 0) {
                exc = SPACE + "throws" + SPACE + getExceptions(exceptions);
            }
            return PUBLIC + SPACE + name + parenthesis(parameters) + exc + SPACE + BLOCK_BEGIN;
        }

        /**
         * Generate the whole method or constructor string representation by compounding its head
         * (method or constructor string representation declaration with exceptions it throws) with
         * {@code return} or {@code super} string representation and arguments string
         * representation to be given to super constructor or returned value of function to be
         * overridden according to a flag
         *
         * @param head head of method or constructor
         * @param keyWord string representation of keyword
         * @param inside arguments or returned value string representation
         * @param overrides flag determining whether to override or not
         * @return generated method or constructor
         */
        private static String unite(final String head, final String keyWord,
                                    final String inside, final boolean overrides) {
            String body = TAB + head + DOUBLE_TAB + keyWord
                    + inside + LINE_BREAK + TAB + BLOCK_END;
            if (overrides) {
                body = TAB + "@Override" + LINE_SEP + body;
            }
            return body;
        }

        /**
         * Generates default return value string representation according to the token return type
         *
         * @param token given token
         * @return default return value string representation
         */
        private static String returnValue(final Class<?> token) {
            String result;
            if (token == boolean.class) {
                result = "false";
            } else if (token == void.class) {
                result = "";
            } else if (token.isPrimitive()) {
                result = "0";
            } else {
                result = "null";
            }
            if (!"".equals(result)) {
                result = SPACE.concat(result);
            }
            return result;
        }
    }

    /**
     * The starting point of execution of {@link Implementor}, which runs
     * {@link #implementJar(Class, Path)} and requires three non-null args
     * consisting of {@code -jar} name of class and path to jarfile
     *
     * @param args application running arguments ["-jar", "classname", "jarfile"]
     */
    public static void main(String[] args) {
        if (args == null || args.length != 3 || !"-jar".equals(args[0])) {
            System.err.println("Usage:" + System.lineSeparator()
                    + "    [-jar] <class name> <output path>");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Non-null arguments are required");
            return;
        }

        try {
            final JarImpler implementor = new Implementor();
            implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }  catch (ClassNotFoundException e) {
            System.err.println("Error loading class " + args[1] + e.getMessage());
        } catch (LinkageError e) {
            System.err.println("Class " + args[1] + " has incompatible dependency "
                    + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Cannot parse " + args[2]);
        }
    }
}
