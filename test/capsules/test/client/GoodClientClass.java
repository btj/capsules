package capsules.test.client;

import capsules.test.system.SystemRoot;
import capsules.test.system.sub1.Subsystem1ExportedClass;
import capsules.test.system.sub2.Subsystem2ExportedClass;

public class GoodClientClass {
	public static void goodClientMethod() {
		SystemRoot.exportedMethod();
		Subsystem1ExportedClass.exportedMethod();
		Subsystem2ExportedClass.exportedMethod();
		System.out.println(SystemRoot.exportedField);
		System.out.println(Subsystem1ExportedClass.exportedField);
		System.out.println(Subsystem2ExportedClass.exportedField);
	}
}
