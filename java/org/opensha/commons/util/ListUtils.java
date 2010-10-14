package org.opensha.commons.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opensha.commons.data.NamedObjectAPI;

public class ListUtils {
	
	public static NamedObjectAPI getObjectByName(Collection<? extends NamedObjectAPI> objects, String name) {
		for (NamedObjectAPI object : objects) {
			if (object.getName().equals(name))
				return object;
		}
		return null;
	}
	
	public static int getIndexByName(List<? extends NamedObjectAPI> list, String name) {
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).getName().equals(name))
				return i;
		}
		return -1;
	}
	
	public static ArrayList<String> getNamesList(Collection<? extends NamedObjectAPI> objects) {
		ArrayList<String> names = new ArrayList<String>();
		for (NamedObjectAPI object : objects) {
			names.add(object.getName());
		}
		return names;
	}

}