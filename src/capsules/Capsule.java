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
 * 
 * Specify a list of package names as the value of the <code>friends</code> attribute
 * to give those packages, and their subpackages, unrestricted access to this capsule.
 * For example,
 * <pre>
 * {@literal @}capsules.Capsule(exportKeyword=SystemAPI.class, friends={"org.helper1", "org.helper2"})
 * package org.system;
 * </pre>
 * allows code in packages <code>org.helper1</code> and <code>org.helper2</code> to access non-exported
 * elements below <code>org.system</code>.
 */
@Documented
public @interface Capsule {
	Class<?> exportKeyword();
	String[] friends() default {};
}
