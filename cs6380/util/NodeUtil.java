package cs6380.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeUtil {

	public static void main(String[] args) {
		HashMap<Integer, String> res = new HashMap<>();
		HashMap<Integer, Integer> pos = new HashMap<>();
		Map<Integer, Integer> map = readConfig("config.txt", res, pos, "8");
		System.out.println(res);
		System.out.println(pos);
		System.out.println(map);
	}

	/**
	 * read node info from config file and initialize client instance according to
	 * config format
	 * 
	 * @param fileName
	 *            given config file name
	 * @return return a list of client info
	 * @throws FileNotFoundException
	 */
	public static Map<Integer, Integer> readConfig(String fileName, Map<Integer, String> id_to_addr, Map<Integer, Integer> id_to_index, String id) {
		BufferedReader scanner = null;
		Map<Integer, Integer> map = new HashMap<>();
		try {
			File file = new File(fileName);
			scanner = new BufferedReader(new FileReader(file));
			//scanner = new Scanner(file);
			
			if (scanner != null) {
				// read number of nodes
				int nodeNums = 0;
				String line = null;
				while ((line = scanner.readLine()) != null) {
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						nodeNums = Integer.parseInt(line.trim());
						break;
					}
				}

				int index = 0;
				while (index < nodeNums && (line = scanner.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0) {
						continue;
					}
					if (!line.startsWith("#")) {
						String[] info = line.split("\\s+");
						id_to_addr.put(Integer.parseInt(info[0]), info[2] + ":" + info[1]);
						id_to_index.put(Integer.parseInt(info[0]), index);
						index++;
					}
				}
				System.out.println("Reading Neighbors");
				
				while((line = scanner.readLine()) != null) {
					line = line.trim();
					if(line.length() == 0) {
						continue;
					}
					
					if (!line.startsWith("#")) {
						String[] info = line.split("\\s+");
						Matcher m = Pattern.compile(".*\\((\\S+)\\).*").matcher(info[0]);
						int weight = Integer.parseInt(info[1]);
						if(m.find()) {
							String pair = m.group(1);
							String[] ends = pair.split(",");
							if(ends[0].equals(id)) {
								map.put(Integer.parseInt(ends[1]), weight);
							} else if(ends[1].equals(id)) {
								map.put(Integer.parseInt(ends[0]), weight);
							}
						}else {
							System.err.println("Config file has wrong format");
						}
					}
				}
				
				System.out.println("Reading Config Ends");
				scanner.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
}
