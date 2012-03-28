package capsulesexample.client;

import capsulesexample.system.InternalHelper;
import capsulesexample.system.SystemRoot;
import capsulesexample.system.sub1.Subsystem1ExportedClass;
import capsulesexample.system.sub1.Subsystem1InternalClass;
import capsulesexample.system.sub2.Subsystem2ExportedClass;
import capsulesexample.system.sub2.Subsystem2ExportedSubclass;
import capsulesexample.system.sub2.Subsystem2InternalClass;

public class BadClientClass {
	public static void goodClientMethod() {
		SystemRoot.exportedMethod();
		Subsystem1ExportedClass.exportedMethod();
		Subsystem2ExportedClass.exportedMethod();
		System.out.println(SystemRoot.exportedField);
		System.out.println(Subsystem1ExportedClass.exportedField);
		System.out.println(Subsystem2ExportedClass.exportedField);
	}
	public static void badClientMethod() {
		SystemRoot.internalMethod();
		InternalHelper.internalHelperMethod();
		Subsystem1ExportedClass.internalMethod();
		Subsystem1InternalClass.internalMethod();
		Subsystem2ExportedClass.internalMethod();
		Subsystem2InternalClass.internalMethod();
		System.out.println(SystemRoot.internalField);
		System.out.println(InternalHelper.internalField);
		System.out.println(Subsystem1ExportedClass.internalField);
		System.out.println(Subsystem1InternalClass.internalField);
		System.out.println(Subsystem2ExportedClass.internalField);
		System.out.println(Subsystem2InternalClass.internalField);
		
		Subsystem2ExportedSubclass sub = Subsystem2ExportedSubclass.create();
		System.out.println(sub.internalField);
		sub.internalMethod();
	}
}
