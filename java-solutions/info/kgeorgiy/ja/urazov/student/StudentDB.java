package info.kgeorgiy.ja.urazov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {
    private final static Comparator<Student> NAME_COMPARATOR =
            Comparator
                    .comparing(Student::getLastName)
                    .thenComparing(Student::getFirstName)
                    .reversed()
                    .thenComparingInt(Student::getId);

    public static final Comparator<Student> ID_COMPARATOR = Comparator.comparingInt(Student::getId);

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return getCommonInfo(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return getCommonInfo(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return getCommonInfo(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return getCommonInfo(students, GET_FULL_NAME);
    }

    private static <T> List<T> getCommonInfo(final List<Student> students, final Function<Student, T> mappingFunction) {
        return students.stream().map(mappingFunction).collect(Collectors.toList());
    }

    // :NOTE: Константа
    private static final Function<Student, String> GET_FULL_NAME
            = student -> student.getFirstName() + " " + student.getLastName();

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return students.stream().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return getMaxAndMap(students, ID_COMPARATOR, Student::getFirstName, "");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        // :NOTE: Дубли
        return sortStudents(students, ID_COMPARATOR);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudents(students, NAME_COMPARATOR);
    }

    private static <T> List<T> sortStudents(final Collection<T> students, final Comparator<? super T> comp) {
        return students.stream().sorted(comp).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return find(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return find(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return find(students, group, Student::getGroup);
    }

    private static List<Group> getGroups(final Collection<Student> students, final UnaryOperator<List<Student>> sort) {
        return getGroupsMap(students, TreeMap::new).entrySet().stream()
                .map(e -> new Group(e.getKey(), sort.apply(e.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return students.stream().filter(matching(group, Student::getGroup))
                .collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private static <T> List<Student> find(
            final Collection<Student> students,
            final T element,
            final Function<Student, T> function) {
        return students.stream().filter(matching(element, function))
                .sorted(NAME_COMPARATOR).collect(Collectors.toList());
    }

    private static <T> Predicate<Student> matching(final T element, final Function<Student, T> matcher) {
        return student -> matcher.apply(student).equals(element);
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroups(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroups(students, this::sortStudentsById);
    }

    private static Map<GroupName, List<Student>> getGroupsMap(
            final Collection<Student> students,
            final Supplier<Map<GroupName, List<Student>>> map) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, map, Collectors.toList()));
    }

    private static Map<String, Set<GroupName>> getNamesMap(final Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::getFirstName,
                Collectors.mapping(Student::getGroup, Collectors.toSet())));
    }

    private static <T, R> T getMost(
            final Map<T, R> collection,
            final Function<R, Integer> function,
            final Comparator<T> order, final T def) {
        return getMaxAndMap(
                collection.entrySet(),
                Comparator.<Map.Entry<T, R>>comparingInt(group -> function.apply(group.getValue()))
                        .thenComparing(Map.Entry::getKey, order), Map.Entry::getKey, def);
    }

    private static <T, E> E getMaxAndMap(
            final Collection<T> collection, final Comparator<T> comparator,
            final Function<T, E> mappingFunction, final E def) {
        return collection.stream().max(comparator).map(mappingFunction).orElse(def);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> collection) {
        return getMost(getGroupsMap(collection, HashMap::new), List::size, Comparator.naturalOrder(), null);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> collection) {
        return getMost(getGroupsMap(collection, HashMap::new),
                students -> getDistinctFirstNames(students).size(), Comparator.reverseOrder(), null);
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return getMost(getNamesMap(students), Set::size, Comparator.naturalOrder(), "");
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return indicesQuery(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return indicesQuery(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] indices) {
        return indicesQuery(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return indicesQuery(students, indices, GET_FULL_NAME);
    }

    private static <T> List<T> indicesQuery(final Collection<Student> students, final int[] indices, final Function<Student, T> function) {
        return Arrays.stream(indices).mapToObj(new ArrayList<>(students)::get).map(function).collect(Collectors.toList());
    }
}
