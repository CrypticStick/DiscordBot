package com.Stickles.ModularCoding;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class DynamicModuleLoader {

	public static class NoModuleDirException extends Exception {
		private static final long serialVersionUID = -1807645354565253022L;
	}

	static Path tempDir = null;
	static Path moduleDir = null;
	static List<JarClassLoader> jcls = new ArrayList<JarClassLoader>();
	static boolean watcherRunning = false;
	
	protected static void setModuleDirectory(String dir) {
		try {
			tempDir = Files.createTempDirectory("modules");
		} catch (IOException e1) {
			System.err.println("Error: Failed to create temporary directory.");
			return;
		}
		
		if (!new File(dir).exists())	//create modules folder if needed
			new File(dir).mkdir();
		
		moduleDir = Paths.get(dir);
	}
	
	private static boolean isCompletelyWritten(File file) {
	    RandomAccessFile stream = null;
	    try {
	        stream = new RandomAccessFile(file, "rw");
	        return true;
	    } catch (Exception e) {
	        System.out.println("[info] Waiting for file " + file.getName() + " to finish writing...");
	    } finally {
	        if (stream != null) {
	            try {
	                stream.close();
	            } catch (IOException e) {
	            	//too bad so sad
	            }
	        }
	    }
	    return false;
	}
	
	protected static void startWatcher() throws NoModuleDirException {
		if (moduleDir == null || tempDir == null)
			throw new NoModuleDirException();
		
		watcherRunning = true;
		
		new Thread(() -> {
			WatchService watcher;
			WatchKey watchKey = null;
			HashMap<File,Long> modified = new HashMap<>();
			
			try {
				watcher = moduleDir.getFileSystem().newWatchService();
				moduleDir.register(watcher, 
						StandardWatchEventKinds.ENTRY_DELETE, 
						StandardWatchEventKinds.ENTRY_MODIFY
						);
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
			
			while (watcherRunning) {
				watchKey = watcher.poll();
			    if(watchKey != null) {
			        for (WatchEvent<?> event : watchKey.pollEvents()) {
			        	Kind<?> kind = event.kind();
			        	final Path changed = (Path) event.context();
			        	
		        		if (changed.toString().endsWith(".jar")) {
				        	
		        			try {
					        	if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
					        		unloadJar(getLocalJar(changed.toString(),false));
					        	}
					        	
					        	else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
					        		unloadJar(getLocalJar(changed.toString(),false));
					        		modified.put(getLocalJar(changed.toString(),false), System.currentTimeMillis());	//add file to modified list; file will continue to be modified until completely loaded
					        	}
		        			} catch (NoModuleDirException e) {
		        				e.printStackTrace();
		        			}
		        		}
			        }
				    watchKey.reset();
			    }
			    
			    File[] keySet = new File[modified.size()];
			    modified.keySet().toArray(keySet);	//copy elements to array to avoid ConcurrentModificationException
			    for (File file : keySet) {	// for all files being modified...
					
					if (isCompletelyWritten(file)) {	//if the file is ready...
						modified.remove(file);	//file is no longer being modified
						try {
							loadJar(file,true);
						} catch (NoModuleDirException e) {
							e.printStackTrace();
						}	//load file
					}	
			    }
			    
			    try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			    
			}
		}).start();
	}
	
	protected static void stopWatcher() {
		watcherRunning = false;
	}
	
	static List<File> getAllJarFiles() throws NoModuleDirException {
		if (moduleDir == null || tempDir == null)
			throw new NoModuleDirException();
		
		File[] filesArr = new File(moduleDir.toString()).listFiles(new FilenameFilter() {	//create a list of .jar files
		    @Override
		    public boolean accept(File dir, String name) {
		        return name.endsWith(".jar");
		    }
		});
		
		List<File> files = new ArrayList<>();
		files.addAll(Arrays.asList(filesArr));
		return files;
	}
	
	protected static void loadAllModules() throws NoModuleDirException {
		List<File> files = getAllJarFiles();
		List<String> unloaded = getUnloadedJars();
		
		Iterator<File> i = files.iterator();
		while (i.hasNext()) {	//sort through file list, removing any files already loaded
			File f = i.next();
			for (JarClassLoader jcl : jcls) {
				if (f.getName().replace(".jar","").equals(jcl.getFileName()))
					i.remove();
			}
		}

		for (File module : files) {	//for each .jar file...
		if (unloaded.contains(module.getName().replace(".jar","")))
			loadJar(module,false);
		}
		
		initializeAllModules();	//initialize all modules after adding to ensure dependencies are sorted out
	}
	
	protected static void reloadAllModules() throws NoModuleDirException {
		unloadAllModules();
		loadAllModules();
	}
	
	protected static void unloadAllModules() throws NoModuleDirException {
		List<File> files = getAllJarFiles();
		
		for (File module : files) {	//for each .jar file...
			if (getLoadedJars().contains(module.getName().replace(".jar","")))	//get loaded every iteration in case dependencies are unloaded
			unloadJar(module);
		}
	}
	
	static void initializeAllModules() {
		Collections.sort(jcls,new Comparator<JarClassLoader>()	//Sorting the modules so that dependencies are loaded in the correct order
				  {
				     public int compare(JarClassLoader o1, JarClassLoader o2)	//-1 means o1 goes first; 1 means o2 goes first
				     {
				    	 if(o1 == null || o2 == null)	//if either JarClassLoader is null, there's no reason to move them
				    		 return 0;
				    	 if (o1.getModule() == null || o2.getModule() == null)	//if either module is null, there's no reason to move them
				    		 return 0;
				    	 if (o1.getModule().getDependencies() == null)			//if o1 has no dependencies, load it first
				    		 return -1;
				    	 if (o2.getModule().getDependencies() == null)			//if o2 has no dependencies, load it first
				    		 return 1;
				    	 
				    	 List<String> depNames1 = new ArrayList<String>();
				    	 List<String> depNames2 = new ArrayList<String>();
				    	 for (String moduleName : o1.getModule().getDependencies()) 
				    		 depNames1.add(moduleName);
				    	 for (String moduleName : o2.getModule().getDependencies()) 
				    		 depNames2.add(moduleName);
				    	 
				         if (depNames1.contains(o2.getModuleName()))	//if o1 relies on o2, o2 is first
				        	 return 1;
				         else if (depNames2.contains(o1.getModuleName())) //if o2 relies on o1, o1 is first
				        	 return -1;
				         else													//if neither depend on each other, don't change anything
				        	 return 0;
				     }
				});	
		for (JarClassLoader jcl : jcls) {
			jcl.initModule();
		}
	}
	
	public static Module getModule(String moduleName) {
		for (JarClassLoader jcl : jcls) {
			if (jcl.getModule() == null) continue;
			if (jcl.getModule().getName() == moduleName)
				return jcl.getModule();
		}
		return null;
	}
	
	public static boolean unloadJar(File jar) throws NoModuleDirException {
		JarClassLoader jcl = getJCL(getTemporaryJar(jar.getName()));
		if (jcl == null)
			return false;
		if (jcl.unloadJar()) {
			jcls.remove(jcl);
			jcl = null;
			System.gc();
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean unloadJar(Module module) {
		JarClassLoader jcl = getJCL(module);
		if (jcl == null)
			return false;
		if (jcl.unloadJar()) {
			jcls.remove(jcl);
			jcl = null;
			System.gc();
			return true;
		} else {
			return false;
		}
	}
	
	static JarClassLoader getJCL(File jar) {
		for (JarClassLoader jcl : jcls) {
			try {
			if (jcl.getURL().equals(jar.toURI().toURL()))
				return jcl;
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		return null;
	}
	
	static JarClassLoader getJCL(Module module) {
		for (JarClassLoader jcl : jcls) {
			if (jcl.getModule().equals(module))
				return jcl;
		}
		return null;
	}
	
	public static boolean loadJar(File jar, boolean initialize) throws NoModuleDirException {
		if (getLoadedJars().contains(jar.getName().replace(".jar","")))	//if file is already loaded, quit immediately
			return false;
		
		File newJar = createTemporaryJar(jar.getName());
		if (newJar != null) {
			JarClassLoader jcl = null;
			try {
				jcl = new JarClassLoader(newJar.toURI().toURL());	//create new jcl for new jar
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			
			jcl.createModule();
			for (JarClassLoader _jcl : jcls) {
				if (jcl.getModuleName() == _jcl.getModuleName()) {
					System.out.println("ERROR: Multiple modules with the name \'"+jcl.getModuleName()+"\'. Unloading both!");
					unloadJar(jcl.getModule());
					unloadJar(_jcl.getModule());
					return false;
				}
			}
			jcls.add(jcl);	//only save jcl if successfully created
			
			
			if (initialize) 
				jcl.initModule();
			
			return true;
		}
		else
			return false;
	}
	
	static File getLocalJar(String filename, boolean mustExist) throws NoModuleDirException {
		File file = new File(moduleDir.toString() + "/" + filename);
		if (!file.exists() && mustExist)
			return null;
		else
			return file;
	}
	
	static File getTemporaryJar(String filename) throws NoModuleDirException {
		File file = new File(tempDir.toString() + "/" + filename);
		if (!file.exists())
			return null;
		else
			return file;
	}
	
	static File createTemporaryJar(String filename) throws NoModuleDirException {
		if (getLocalJar(filename, true) == null) {
			return null;
		}
		
		File newJar = new File(tempDir.toString() + "/" + filename);	//creates directory for running copy
		boolean success = false;
		for (int i = 0; i < 5; i++) {	//it may take a moment for gc to finish, so try a few times if needed
			try {
				Files.copy(getLocalJar(filename,true).toPath(), newJar.toPath(),StandardCopyOption.REPLACE_EXISTING);	//copy file to run in a separate directory
				success = true;
				break;
			} catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		if (success)
			return newJar;
		else
			return null;
	}
	
	public static ArrayList<String> getLoadedJars() {
		ArrayList<String> loaded = new ArrayList<String>();
		for (JarClassLoader jcl : jcls)
			loaded.add(jcl.getFileName());
		return loaded;
	}
	
	public static ArrayList<String> getUnloadedJars() throws NoModuleDirException {
		ArrayList<String> unloaded = new ArrayList<String>();
		ArrayList<String> loaded = getLoadedJars();
		
		for (File jar : getAllJarFiles()) {
			String file = jar.getName().replace(".jar", "");
			if (!loaded.contains(file))
				unloaded.add(file.replace(".jar", ""));
		}
		return unloaded;
	}
	
}
