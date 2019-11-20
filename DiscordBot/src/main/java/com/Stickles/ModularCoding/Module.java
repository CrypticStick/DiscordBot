package com.Stickles.ModularCoding;

import java.util.List;

public interface Module {
	void initialize() throws Exception;
	void shutdown();
	void addDependants(Module toAdd);
	void removeDependants(Module toRemove);
	List<Module> getDependants();
	List<Class<? extends Module>> getDependencies();
	String getName();
}
