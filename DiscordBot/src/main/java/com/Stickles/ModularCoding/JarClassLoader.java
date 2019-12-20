package com.Stickles.ModularCoding;
/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.net.URL;
import java.net.URLClassLoader;
import java.net.JarURLConnection;
import java.util.Iterator;
import java.util.jar.Attributes;

import com.Stickles.DiscordBot;

import java.io.IOException;

/**
 * A class loader for loading jar files, both local and remote.
 */
class JarClassLoader extends URLClassLoader {
	
	/**
     * Exception thrown when the JarClassLoader fails to initialize.
     */
    class JarFailureException extends Exception {
		private static final long serialVersionUID = 7787733354466853696L;
	}

	private Module module;
    private Class<?> klass;
    private URL url;
    private boolean moduleReady = false;
    
    /**
     * Creates a new JarClassLoader for the specified url.
     *
     * @param url the jar being loaded
     */
    public JarClassLoader(URL url) throws JarFailureException {
        super(new URL[] { url });
        this.url = url;
        if (!loadJar()) {
        	unloadJar();
        	throw new JarFailureException();
        }
    }

    /**
     * Returns the name of the jar file main class, or null if
     * no "Main-Class" manifest attributes was defined.
     */
    String getMainClassName(URL file) throws IOException {
        URL u = new URL("jar", "", file + "!/");
        JarURLConnection uc = (JarURLConnection)u.openConnection();
        uc.setUseCaches(false);
        Attributes attr = uc.getMainAttributes();
        return attr != null ? attr.getValue(Attributes.Name.MAIN_CLASS) : null;
    }
    
    /**
     * Initializes the module from the loaded jar. If already initialized, this does nothing.
     *
     * @return True if successful
     */
	public boolean initModule() {
    	try {
    		if (module == null)
    			if (!createModule()) {
    				System.out.println("Failed to instantiate a module!");
    	    		return false;
    			}
	        if (!moduleReady) {
	    		if (module.getDependencies() != null)
		        	for (String dependency : module.getDependencies()) {
		        		if (DiscordBot.getModule(dependency) == null) {	//don't initialize if dependencies are missing
		        			System.out.println(String.format("%s is missing dependency \"%s\"", module.getName(), dependency));
		        			module = null;
		        			return false;
		        		}
		        	}
	    		
	        	module.initialize();	//initialize after checking
	        	
	    		if (module.getDependencies() != null)
		        	for (String dependency2 : module.getDependencies()) {
		        		if (DiscordBot.getModule(dependency2) != null)
		        			DiscordBot.getModule(dependency2).addDependants(module);	//let dependencies know they have a new dependant.
		        	}
	    		
	    		moduleReady = true;
		        System.out.println(String.format("%s has loaded!", module.getName()));
		        return true;
		        
	        } else {
	        	System.out.println(String.format("%s was already loaded!", module.getName()));
		        return true;
	        }
	        
    	} catch (Exception e) {
    		e.printStackTrace();
	        module = null;
    		System.out.println("Failed to instantiate a module!");
    		return false;
    	}
    }
    
	/**
     * Creates a new instance of a module from the loaded jar. If already created, this does nothing.
     *
     * @return True if successful
     */
    public boolean createModule() {
		if (module == null) {
    	try {
	    	module = (Module) klass.getDeclaredConstructor().newInstance();
	    	if (module == null) {
		        System.out.println("Failed to instantiate a module!");
		        return false;
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
	        module = null;
    		System.out.println("Failed to instantiate a module!");
    		return false;
    	}
    	return true;
		} else {
			System.out.println("Module was already created!");
			return true;
		}
    }
    
    /**
     * Gets the main class from the jar. The
     * class is saved until it is unloaded.
     *
     * @return True if successful
     */
    boolean loadJar() {
    	try {
    		klass = loadClass(getMainClassName(url));
	        if (Module.class.isAssignableFrom(klass)) {
	        	return true;
	        }
	        return false;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return false;
    	}
    }
    
    /**
     * Prepares the jcl for removal. If unload is successful, jcl can be safely deleted.
     *
     * @return True if successful
     */
    public boolean unloadJar() {
    	if (module != null) {
    		if (module.getDependants() != null) {
    			Iterator<Module> dependant_i = module.getDependants().iterator();
	    		while (dependant_i.hasNext())	{	//shut down modules that rely on this one
	    			Module dependant = dependant_i.next();
	        		if (dependant != null) {
	        			dependant_i.remove();
	        			DiscordBot.unloadJar(dependant);
	        		}
	    		}
    		}
    		
    		if (module == null) return true;
    		
    		if (module.getDependencies() != null)
		    	for (String dependency : module.getDependencies())	//let dependencies know they're losing a dependant
		    		if (DiscordBot.getModule(dependency) != null) 
		    			DiscordBot.getModule(dependency).removeDependants(module);
	    	
	    	String name = module.getName();
	    	
			module.shutdown();	//shutdown module
	    	module = null; 
	    	System.out.println(String.format("%s has shut down!", name));
    	}
    	
    	klass = null;
    	try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.gc();
		return true;
	}

    public Module getModule() {
    	return module;
    }
    
    public String getModuleName() {
    	if (module != null)
    		return module.getName();
    	return "Unknown";
    }
    
    public String getFileName() {
    	String path = url.getFile();
		int back = path.lastIndexOf("\\");
		int forward = path.lastIndexOf("/");
		int start = (back > forward) ? back : forward;
		return path.substring(start+1).replace(".jar", "");
    }
    
    public URL getURL() {
    	return url;
    }
    
    /**
     * Returns whether or not a working module exists.
     *
     * @return True if module exists
     */
    public boolean isModuleReady() {
    	return moduleReady;
    }
}
