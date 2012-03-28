package capsules.checker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class Checker {
	
	static String capsulesPackageName = "capsules";
	static String capsuleClassName = capsulesPackageName+"/Capsule";
	static String capsuleClassDesc = classNameDesc(capsuleClassName); 
	
    static String classNameDesc(String className) { return "L"+className+";"; }

    static enum MemberKind {
    	Method {
    		String getMemberKey(String name, String desc) {
    			return name+"."+desc;
    		}
    	},
    	Field {
    		String getMemberKey(String name, String desc) {
    			return name+"("+desc;
    		}
    	};
    	
    	abstract String getMemberKey(String name, String desc);
    }
    
    private Checker() {}
    
	boolean verbose;
	String[] classPath;
	
	String findPath(String filePath, String[] paths) {
		for (String path : paths) {
			File file = new File(path, filePath);
			if (file.exists())
				return file.toString();
		}
		return null;
	}
	
	InputStream findClassFile(String filePath) {
		try {
			String path = findPath(filePath, classPath);
			return path == null ? null : new FileInputStream(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	class Capsule {
		String packageName;
		String keywordName;
		HashMap<String, CapsuleClass> classes = new HashMap<String, CapsuleClass>();
		
		class CapsuleClass {
			ArrayList<String> supertypes = new ArrayList<String>(); 
			HashSet<String> members = new HashSet<String>();
			boolean exported;
		}
		
		Capsule(String packageName, String keywordName) {
			this.packageName = packageName;
			this.keywordName = keywordName;
		}
		
		CapsuleClass getCapsuleClass(String className) {
			try {
				CapsuleClass capsuleClass = classes.get(className);
				if (capsuleClass == null) {
					final CapsuleClass cc = capsuleClass = new CapsuleClass();
					InputStream stream = findClassFile(className+".class");
					if (stream != null) {
						ClassReader reader = new ClassReader(stream);
						ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4) {
							
							@Override
							public void visit(int version, int access, String name,
									String signature, String superName, String[] interfaces) {
								if (!superName.equals("java/lang/Object"))
									cc.supertypes.add(superName);
								if (interfaces != null)
									cc.supertypes.addAll(Arrays.asList(interfaces));
							}

							@Override
							public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
								if (desc.equals(keywordName))
									cc.exported = true;
								return null;
							}
							
							@Override
							public FieldVisitor visitField(int access, final String fieldName, final String fieldDesc, String signature, Object value) {
								return new FieldVisitor(Opcodes.ASM4) {
									@Override
									public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
										if (desc.equals(keywordName))
											cc.members.add(MemberKind.Field.getMemberKey(fieldName, fieldDesc));
										return null;
									}
								};
							}
							
							@Override
							public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature, String[] exceptions)  {
								return new MethodVisitor(Opcodes.ASM4) {
									@Override
									public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
										if (desc.equals(keywordName))
											cc.members.add(MemberKind.Method.getMemberKey(methodName, methodDesc));
										return null;
									}
								};
							}
						};
						reader.accept(visitor, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG);
					}
					classes.put(className, capsuleClass);
				}
				return capsuleClass;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		boolean checkMemberAccessFrom(String owner, MemberKind kind, String name, String desc, boolean checkSupertypes, String fromPackage, String source, int line, boolean reportError) {
			if (fromPackage != null && (fromPackage+"/").startsWith(packageName+"/"))
				return true; // Intra-capsule access
			CapsuleClass capsuleClass = getCapsuleClass(owner);
			if (capsuleClass.members.contains(kind.getMemberKey(name, desc)))
				return true;
			
			if (checkSupertypes) {
				for (String supertype : capsuleClass.supertypes) {
					if (Checker.this.checkMemberAccessFrom(supertype, kind, name, desc, checkSupertypes, fromPackage, source, line, false)) {
						return true;
					}
				}
			}
			
			if (reportError) {
				String sourceFile = fromPackage+"/"+source;
				reportError("("+sourceFile+":"+line+"): "+kind+" not exported by capsule "+packageName);
			}
			
			return false;
		}
	}

	public static void main(String[] args) throws IOException {
		new Checker().run(args);
	}
	
	int errorCount;
	
	void reportError(String msg) {
		System.out.println(msg);
		errorCount++;
	}
	
	void run(String[] args) throws IOException {
		String[] positionalArgs = new String[1];
		int p = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-verbose"))
				verbose = true;
			else
				positionalArgs[p++] = args[i];
		}
		classPath = positionalArgs[0].split(File.pathSeparator);
		for (String path : classPath) {
			checkClasses(new File(path));
		}
		System.out.println(errorCount+" errors found");
		System.exit(errorCount == 0 ? 0 : 1);
	}
	
	void checkClasses(File path) throws IOException {
		if (path.isDirectory()) {
			String[] children = path.list();
			for (String child : children) {
				checkClasses(new File(path, child));
			}
		} else if (path.toString().endsWith(".class"))
			checkClass(path.toString());
	}
	
	void checkClass(String classFilePath) throws IOException {
		if (verbose)
			System.out.println("Checking class "+classFilePath);
		FileInputStream stream = new FileInputStream(classFilePath);
		checkClass(stream);
		stream.close();
	}
	
	static String getParentName(String name) {
		int lastSlash = name.lastIndexOf('/');
		if (lastSlash < 0)
			return null;
		return name.substring(0, lastSlash);
	}
	
	static boolean nameEquals(String name1, String name2) {
		return name1 == null || name2 == null ? name1 == name2 : name1.equals(name2);
	}
	
	HashMap<String, List<Capsule>> packageCapsules = new HashMap<String, List<Capsule>>();
	
	List<Capsule> getPackageCapsules(final String packageName) {
		if (packageName == null) return Collections.emptyList();
		List<Capsule> capsules = packageCapsules.get(packageName);
		if (capsules == null) {
			final List<Capsule> cs = capsules = new ArrayList<Capsule>();
			// First, copy parent capsules
			String parentName = getParentName(packageName);
			List<Capsule> parentCapsules = getPackageCapsules(parentName);
			capsules.addAll(parentCapsules);
			// Then, check if we have any capsules of our own
			try {
				InputStream packageInfoFile = findClassFile(packageName + "/package-info.class");
				if (packageInfoFile != null) {
					ClassReader reader = new ClassReader(packageInfoFile);
				 	ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4) {
						@Override
						public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
							if (desc.equals(capsuleClassDesc)) {
								return new AnnotationVisitor(Opcodes.ASM4) {
									@Override
									public void visit(String name, Object value) {
										Type exportKeyword = (Type)value;
										cs.add(new Capsule(packageName, exportKeyword.toString()));
										if (verbose)
											System.out.println("Detected capsule "+packageName+" with export keyword "+exportKeyword);
									}
								};
							}
							return null;
						}
					};
					reader.accept(visitor, ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG);
				}
				packageCapsules.put(packageName, capsules);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return capsules;
	}
	
	boolean checkMemberAccessFrom(String owner, MemberKind kind, String name, String desc, boolean checkSupertypes, String fromPackage, String source, int line, boolean reportError) {
		//System.err.println("Checking access of "+kind+" "+owner+"#"+name+":"+desc+" from "+fromPackage);
		String ownerPackage = getParentName(owner);
		if (nameEquals(ownerPackage, fromPackage)) {
			return true;
		} else {
			List<Capsule> capsules = getPackageCapsules(ownerPackage);
			for (Capsule capsule : capsules) {
				if (!capsule.checkMemberAccessFrom(owner, kind, name, desc, checkSupertypes, fromPackage, source, line, reportError))
					return false;
			}
			return true;
		}
	}
	
	void checkClass(InputStream classFile) throws IOException {
		ClassReader reader = new ClassReader(classFile);
		String className = reader.getClassName();
		final String currentPackage = getParentName(className);
		ClassVisitor checker = new ClassVisitor(Opcodes.ASM4) {
			
			String source;
			
			@Override
			public void visitSource(String source, String debug) {
				this.source = source;
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) { 
				return new MethodVisitor(Opcodes.ASM4) {
					
					int line;
					
					@Override
					public void visitLineNumber(int line, Label label) {
						this.line = line;
					}
					
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String desc)  {
						checkMemberAccessFrom(owner, MemberKind.Field, name, desc, false, currentPackage, source, line, true);
					}
					
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String desc)  {
						checkMemberAccessFrom(owner, MemberKind.Method, name, desc, opcode != Opcodes.INVOKESTATIC, currentPackage, source, line, true);
					}
				};
			}
			
		};
		reader.accept(checker, 0);
	}

}
