package capsules.doclet;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;

public class Doclet {
	
	public static void main(String[] args) {
		String name = Doclet.class.getName();
		int exitCode = Main.execute(name, name, args);
		System.exit(exitCode);
	}
    
    public static int optionLength(String option)
    {
    	if (option.equals("-exportKeyword"))
    		return 2;
    	else
    		return Standard.optionLength(option);
    }
    
    static class Options {
    	String[][] javadocOptions;
    	String exportKeyword;
    	
    	Options(String[][] options) {
        	ArrayList<String[]> javadocOptions = new ArrayList<String[]>();
        	for (String[] option : options) {
        		if (option[0].equals("-exportKeyword")) {
        			exportKeyword = option[1];
        		} else {
        			javadocOptions.add(option);
        		}
        	}
        	this.javadocOptions = javadocOptions.toArray(new String[javadocOptions.size()][]);
    	}
    }
	
    public static boolean validOptions(String[][] options, DocErrorReporter reporter)
    	throws java.io.IOException
    {
    	Options opts = new Options(options);
    	if (opts.exportKeyword == null) {
    		reporter.printError("-exportKeyword option required");
    		return false;
    	}
        return Standard.validOptions(opts.javadocOptions, reporter);
    }
    
    public static boolean start(RootDoc root)
    	throws java.io.IOException
    {
        return Standard.start((RootDoc)new Doclet(root).process(root, RootDoc.class));
    }
    
    public static LanguageVersion languageVersion() { 
    	return LanguageVersion.JAVA_1_5; 
	}
    
    private String exportKeyword;
    private IdentityHashMap<Object, Object> proxyMap = new IdentityHashMap<Object, Object>();
    
    private Doclet(RootDoc root) {
    	Options options = new Options(root.options());
    	exportKeyword = options.exportKeyword;
    }
    
    private Object getProxy(Object target) {
    	Object proxy = proxyMap.get(target);
    	if (proxy == null) {
    		proxy = Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), new MyInvocationHandler(target));
    		proxyMap.put(target, proxy);
    	}
    	return proxy;
    }
    
    private boolean containsExportKeyword(AnnotationDesc[] annotations) {
    	for (AnnotationDesc a : annotations) {
    		if (a.annotationType().qualifiedName().equals(exportKeyword)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private boolean isExported(Object target) {
    	if (target instanceof ClassDoc)
    		return containsExportKeyword(((ClassDoc)target).annotations());
    	else if (target instanceof MemberDoc)
    		return containsExportKeyword(((MemberDoc)target).annotations());
    	else
    		return true;
    }
    
    private Object process(final Object target, Class<?> expectedType) {
    	if (target == null)
    		return null;
    	if (expectedType.isArray() && expectedType.getComponentType().getName().startsWith("com.sun.javadoc.")) {
    		Class<?> componentType = expectedType.getComponentType();
    		Object[] targetArray = (Object[])target;
    		ArrayList<Object> newElements = new ArrayList<Object>(targetArray.length);
    		for (Object targetElement : targetArray) {
    			if (isExported(targetElement))
    				newElements.add(process(targetElement, componentType));
    		}
    		return newElements.toArray((Object[])Array.newInstance(componentType, newElements.size()));
    	} else if (expectedType.getName().startsWith("com.sun.javadoc.")) {
	    	return getProxy(target);
    	} else {
    		return target;
    	}
    }
    
    private class MyInvocationHandler implements InvocationHandler {

    	final Object target;
    	
    	MyInvocationHandler(Object target) {
    		this.target = target;
    	}
    	
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (args[i] != null) {
						if (args[i] instanceof Proxy) {
							Object handler = Proxy.getInvocationHandler(args[i]);
							if (handler instanceof MyInvocationHandler) {
								args[i] = ((MyInvocationHandler)handler).target;
							}
						}
					}
				}
			}
			return process(method.invoke(target, args), method.getReturnType());
		}
    	
    }
    
}
