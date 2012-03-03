package capsules.test.system.sub2;

import capsules.test.system.InternalHelper;
import capsules.test.system.SystemRoot;
import capsules.test.system.sub1.Subsystem1ExportedClass;
import capsules.test.system.sub1.Subsystem1InternalClass;

public class Subsystem2InternalClass {
	public static int internalField;
	
	public static void internalMethod() {
		SystemRoot.exportedMethod();
		SystemRoot.internalMethod();
		InternalHelper.internalHelperMethod();
		Subsystem1ExportedClass.exportedMethod();
		Subsystem1ExportedClass.internalMethod();
		Subsystem1InternalClass.internalMethod();
		Subsystem2ExportedClass.exportedMethod();
		Subsystem2ExportedClass.internalMethod();
		Subsystem2InternalClass.internalMethod();
		System.out.println(SystemRoot.exportedField);
		System.out.println(SystemRoot.internalField);
		System.out.println(InternalHelper.internalField);
		System.out.println(Subsystem1ExportedClass.exportedField);
		System.out.println(Subsystem1ExportedClass.internalField);
		System.out.println(Subsystem1InternalClass.internalField);
		System.out.println(Subsystem2ExportedClass.exportedField);
		System.out.println(Subsystem2ExportedClass.internalField);
		System.out.println(Subsystem2InternalClass.internalField);
	}
}
