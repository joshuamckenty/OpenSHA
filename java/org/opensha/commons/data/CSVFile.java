package org.opensha.commons.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensha.commons.util.FileUtils;

public class CSVFile<E> {
	
	private List<String> colNames;
	private Map<String, ? extends List<E>> values;
	private int listSize;
	
	public CSVFile(List<String> colNames, Map<String, ? extends List<E>> values) {
		if (colNames.size() != values.keySet().size())
			throw new IllegalArgumentException("column names must be the same size as values!");
		for (String colName : colNames) {
			if (!values.keySet().contains(colName))
				throw new IllegalArgumentException("Column '"+colName+"' not found in values!");
		}
		listSize = -1;
		for (List<E> list : values.values()) {
			if (listSize < 0)
				listSize = list.size();
			else if (listSize != list.size())
				throw new IllegalArgumentException("Values lists aren't the same size!");
		}
		this.colNames = colNames;
		this.values = values;
	}
	
	public int getNumLines() {
		return listSize;
	}
	
	public int getNumCols() {
		return colNames.size();
	}
	
	public ArrayList<E> getLine(int i) {
		ArrayList<E> line = new ArrayList<E>();
		for (String colName : colNames) {
			E val = values.get(colName).get(i);
			line.add(val);
		}
		return line;
	}
	
	public String getLineStr(int i) {
		String line = null;
		for (String colName : colNames) {
			E val = values.get(colName).get(i);
			if (line == null)
				line = "";
			else
				line += ",";
			line += val.toString();
		}
		return line;
	}
	
	public String getHeader() {
		String line = null;
		for (String colName : colNames) {
			if (line == null)
				line = "";
			else
				line += ",";
			line += colName;
		}
		return line;
	}
	
	public void writeToFile(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		
		fw.write(getHeader() + "\n");
		for (int i=0; i<getNumLines(); i++) {
			fw.write(getLineStr(i) + "\n");
		}
		fw.close();
	}
	
	private static ArrayList<String> loadLine(String line, int num) {
		line = line.trim();
		String[] split = line.split(",");
		ArrayList<String> vals = new ArrayList<String>();
		for (String str : split)
			vals.add(str);
		while (vals.size() < num)
			vals.add("");
		return vals;
	}
	
	public static CSVFile<String> readFile(File file) throws IOException {
		ArrayList<String> colNames = null;
		HashMap<String, ArrayList<String>> values = new HashMap<String, ArrayList<String>>();
		for (String line : FileUtils.loadFile(file.toURI().toURL())) {
			if (colNames == null) {
				colNames = loadLine(line, -1);
				for (String colName : colNames)
					values.put(colName, new ArrayList<String>());
				continue;
			}
			ArrayList<String> vals = loadLine(line, colNames.size());
			for (int i=0; i<colNames.size(); i++) {
				values.get(colNames.get(i)).add(vals.get(i));
			}
		}
		
		return new CSVFile<String>(colNames, values);
	}

}
