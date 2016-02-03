Modular software development implies a **distinction between interface and implementation**. Java supports treating classes and packages as modules through its access modifiers. However, Java lacks such support for multi-package modules.

This project defines a **convention** for identifying multi-package modules in the form of _capsules_. A capsule is defined by a _root package_ and an _export keyword_. An export keyword is an annotation type. A capsule consists of its root package and all descendant packages in the package tree. The _exported elements_ of a capsule are the types and members in the capsule that are annotated with the capsule's export keyword. A root package is associated with an export keyword using the @capsules.Capsule annotation.

The project offers **tool support** for capsules in the form of a static checker and a Javadoc doclet.

The **static checker**, given a set of .class files, checks that they respect capsule encapsulation, i.e. that all code not in a capsule accesses only the capsule's exported types and members.

The **doclet** generates Javadoc documentation for a given capsule, including only its exported types and members.

## Example ##

Consider a module consisting of the packages `foo` and `foo.bar`, and some client code in `fooclient`:

```java
// foo/Foo.java
package foo;

@FooAPI
public class Foo {

    @FooAPI
    public static void apiMethod() {}

    public static void internalMethod() {
        foo.bar.Bar.internalMethod(); // OK
    }

}
```

```java
// foo/FooAPI.java
package foo;

@java.lang.annotation.Documented
@FooAPI
public @interface FooAPI {}
```

```java
// foo/package-info.java
@capsules.Capsule(exportKeyword=FooAPI.class)
package foo;
```

```java
// foo/bar/Bar.java
package foo.bar;

@foo.FooAPI
public class Bar {

    @foo.FooAPI
    public static void apiMethod() {}

    public static void internalMethod() {
        foo.Foo.internalMethod(); // OK
    }

}
```

```java
// fooclient/FooClient.java
package fooclient;

public class FooClient {

    public static void main(String[] args) {
        foo.Foo.apiMethod(); // OK
        foo.Foo.internalMethod(); // Not OK
        foo.bar.Bar.apiMethod(); // OK
        foo.bar.Bar.internalMethod(); // Not OK
    }

}
```

Method `foo.Foo.internalMethod` is used within the module by `foo.bar.Bar.internalMethod` but is not supposed to be accessed by client code. Similarly, method `foo.bar.Bar.internalMethod` is used within the module by `foo.Foo.internalMethod` but is not supposed to be accessed by client code.

We can indicate that package `foo` and its subpackages form a module by applying the `@Capsule` annotation to package `foo`. Furthermore, to indicate which types and members are supposed to be used by client code and which are not, we define an annotation type `FooAPI` and we annotate all types and members that we want to export with it. We indicate that `FooAPI` serves as the export keyword for the capsule using the `@Capsule` annotation's `exportKeyword` element.

We can now use the static checker to check that client code uses only exported elements. Since the checker operates on .class files, we first need to compile the codebase:

```
javac -g -classpath capsules.jar -d bin -sourcepath src src/fooclient/*.java src/foo/*.java src/foo/bar/*.java
```
Note: be sure to include the `-g` flag so that the checker can report the source locations of the errors it finds.

Now we can run the checker, passing the directory with the .class file tree as an argument:

```
java -jar capsules.jar bin
```

It will produce the following output:
```
(fooclient/FooClient.java:7): Method not exported by capsule foo
(fooclient/FooClient.java:9): Method not exported by capsule foo
2 errors found
```

We can generate Javadoc documentation for the exported elements of our capsule using the following command line:

```
javadoc -docletpath capsules.jar -doclet capsules.doclet.Doclet -d foodocs
    -classpath capsules.jar -sourcepath src -subpackages foo -exportKeyword foo.FooAPI
```

## Friends and multi-root capsules ##

A capsule may optionally specify a list of friend packages. For example:

```java
@capsules.Capsule(exportKeyword=SystemAPI.class, friends={"org.friend1", "org.friend2"})
package org.system;
```

The effect is that code in the friend packages and their subpackages gets unrestricted access to the capsule.

This feature can be used to more easily use the capsules toolkit on an existing codebase where the code for the capsule is not below a single root package. Proceed as follows:
  1. Declare each root package as a capsule separately
  1. Use the same export keyword for all capsules
  1. Make each capsule a friend of the other capsules

For example:
```java
// org/root1/package-info.java
@capsules.Capsule(exportKeyword=org.root1.SystemAPI.class, friends={"org.root2"})
package org.root1;
```

```java
// org/root2/package-info.java
@capsules.Capsule(exportKeyword=org.root1.SystemAPI.class, friends={"org.root1"})
package org.root2;
```

This way, org.root1 and org.root2 effectively constitute a single capsule.
