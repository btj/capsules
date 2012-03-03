package capsules;

import java.lang.annotation.Documented;

/**
 * When applied to a package, indicates that code outside of the package and its subpackages
 * should access only those types and members that carry the annotation specified by the
 * <code>exportKeyword</code> attribute.
 * 
 * To check that a program complies with its <code>@Capsule</code> annotations, run
 * capsules.jar with the root of the program's class tree as an argument. For example:  
 * <pre>
 * java -jar capsules.jar C:\myproject\bin
 * </pre>
 */
@Documented
public @interface Capsule {
	Class<?> exportKeyword();
}
