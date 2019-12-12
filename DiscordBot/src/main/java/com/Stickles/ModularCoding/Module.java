package com.Stickles.ModularCoding;

import java.util.List;

public interface Module {
	void initialize() throws Exception;
	void shutdown();
	void addDependants(Module toAdd);
	void removeDependants(Module toRemove);
	List<Module> getDependants();
	List<String> getDependencies();
	String getName();
}
