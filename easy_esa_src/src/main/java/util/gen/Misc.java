package util.gen;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * A variety of static methods.
 *
 */
public class Misc {
	
	/**Calls garbage collection then returns total - free.*/
	public static long fetchUsedMemory(){
	    System.gc(); System.gc(); System.gc(); System.gc();
	    System.gc(); System.gc(); System.gc(); System.gc();
	    System.gc(); System.gc(); System.gc(); System.gc();
	    System.gc(); System.gc(); System.gc(); System.gc();
	    return Runtime.getRuntime().totalMemory() -
	      Runtime.getRuntime().freeMemory();
	}
	
	/**Flips key:value to value:key.*/
	public static HashMap invert(HashMap h){
		HashMap inverted = new HashMap (h.size());
		Iterator it = h.keySet().iterator();
		while (it.hasNext()){
			Object o = it.next();
			inverted.put(h.get(o), o);
		}
		return inverted;
	}
	
	/**Reverses a String using a StringBuilder.*/
	public static String reverse(String x){
		return new StringBuilder(x).reverse().toString();
	}
	
	/**Randomizes the array by permutating indexes and swapping. Fast.
	 * Can set seedMultiplier  or vary it to be sure and get diff*/
	public static void randomize(String[] f, long seed){
		int len = f.length;
		String current;
		String random;
		int index;
		Random generator = new Random(seed);
		for (int i=0; i<len; i++){
			index = generator.nextInt(len);
			current = f[i];
			random = f[index];
			f[i] = random;
			f[index] = current;
		}
	}
	
	/**Randomizes the array by permutating indexes and swapping. Fast.
	 * Can set seed or vary it to be sure and get diff*/
	public static void randomize(Object[] f, long seed){
		int len = f.length;
		Object current;
		Object random;
		int index;
		Random generator = new Random(seed);
		for (int i=0; i<len; i++){
			index = generator.nextInt(len);
			current = f[i];
			random = f[index];
			f[i] = random;
			f[index] = current;
		}
	}
	
	/**Does not check if indexes are out of bounds!
	 * @return the number of trues between the start and stop
	 * @param startIndex is included
	 * @param stopIndex is excluded
	 * */
	public static int countTrues(boolean[] b, int startIndex, int stopIndex){
		int trueCount = 0;
		for (int i=startIndex; i< stopIndex; i++) if (b[i]) trueCount++;
		return trueCount;
	}
	
	/**Prints key + seperator + value+ return to out.*/
	public static void printHashMap(HashMap hash, String seperator){
		Iterator it = hash.keySet().iterator();
		while (it.hasNext()){
			String key = (String) it.next();
			System.out.println(key+seperator+hash.get(key));
		}
	}
	
	/**Converts a hash to a String[] by direct cast.*/
	public static String[] hashSetToStringArray(HashSet hash){
		Iterator it = hash.iterator();
		String[] s = new String[hash.size()];
		int counter =0;
		while (it.hasNext()){
			s[counter++] = (String)it.next();
		}
		return s;
	}
	
	/**Sleeps for given number of seconds*/
	public static void sleep(int seconds){
		try{
			java.lang.Thread.sleep( seconds * 1000 );
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**Looks for key, if not found prints error message and exists.*/
	public static String fetchExit(String key, HashMap map){
		if (map.containsKey(key) == false) Misc.printExit("\nError: the key is not present in the HashMap -> "+ key);
		return (String) map.get(key);
	}
	
	/**Returns a new array with the newValue appended on the beginning.*/
	public static String[] prependString (String[] d, String newValue){
		String[] added = new String[d.length+1];
		System.arraycopy(d,0,added,1,d.length);
		added[0] = newValue;
		return added;
	}
	
	/**Capitalizes the first letter in a String.*/
	public static String capitalizeFirstLetter(String s){
		char[] first = s.toCharArray();		
		if (Character.isLetter(first[0])){
			first[0] = Character.toUpperCase(first[0]);
			return new String(first);
		}
		return s;
	}
	
	/**Removes an extension if found xxx.txt -> xxx.
	 * If none found returns the original.
	 */
	public static String removeExtension(String txt) {
		int index = txt.lastIndexOf(".");
		if (index != -1)  return txt.substring(0,index);
		return txt;
	}
	
	/**Given a String of 'treat1=/file1,/file2*treat2=/file3,/file4*treat3=/file5,file6'
	 * Will return an ArrayList contining first a String[] of the keys {treat1,treat2,treat3}
	 * and second a String[][] of the parsed values matching the keys.
	 * Returns null if an error is found.*/
	public static ArrayList parseKeyValueGroups(String line){		
		String[] groups = line.split("\\*");
		String[] names = new String[groups.length];
		String[][] items = new String[groups.length][];		
		for (int x=0; x<groups.length; x++){
			String[] keyValue = groups[x].split("=");
			if (keyValue.length !=2 )return null;
			names[x] = keyValue[0];
			items[x] = keyValue[1].split(",");
		}
		ArrayList al = new ArrayList(2);
		al.add(names);
		al.add(items);
		return al;
	}
	
	/**Trims characters common to all from start of lines.*/
	public static String[] trimCommonStart(String[] lines){
		//trim front
		boolean go = true;
		int clip = 0;
		while (go){
			//check length
			if (lines[0].length() <= clip){
				clip--;
				break;
			}
			char test = lines[0].charAt(clip);
			//for each line
			for (int i=1; i< lines.length; i++){
				//check if long enough
				if (lines[i].length() <= clip) {
					clip--;
					go = false;
					break;
				}
				
				else if (test != lines[i].charAt(clip) ){
					go = false;
					break;
				}
			}
			if (go) clip++;
		}
		String[] clipped = new String[lines.length];
		for (int i=0; i< lines.length; i++){
			clipped[i] = lines[i].substring(clip);
		}
		return clipped;
	}
	
	/**Trims the comon chars from the front and back of each line, won't entirely delete line.*/
	public static String[] trimCommon(String[] lines){
		//this is sort of lazy Nix! :)
		//trim front
		String[] trimmed = trimCommonStart(lines);
		//reverse lines
		for (int i=0; i< lines.length; i++){
			trimmed[i] = new StringBuffer(trimmed[i]).reverse().toString();
		}
		//trim back
		trimmed = trimCommonStart(trimmed);
		//reverse lines
		for (int i=0; i< lines.length; i++){
			trimmed[i] = new StringBuffer(trimmed[i]).reverse().toString();
		}
		return trimmed;
	}
	
	/**Given a String of 'treat1=/file1,treat2=/file3, etc', no spaces,
	 * returns a LinkedHashMap of keys=values.
	 * Returns null if an error is found.*/
	public static LinkedHashMap parseKeyValues(String line){
		String[] keyValues = line.split(",");
		if (keyValues.length==0)return null;
		LinkedHashMap hash = new LinkedHashMap(keyValues.length);
		for (int i=0; i<keyValues.length; i++){
			String[] kv = keyValues[i].split("=");
			if (kv.length!=2)return null;			
			hash.put(kv[0],kv[1]);
		}
		return hash;
	}
	
	/**Attempts to match the end of the word with the match, case insensitive, replaces it if found,
	 * otherwise returns null.*/
	public static String replaceEnd(String word, String match, String replacement){
		Pattern p = Pattern.compile(match +"$", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(word);
		if (m.find()){
			return m.replaceFirst(replacement);
		}
		return null;
	}
	
	/**Prints message to screen, then exits.*/
	public static void printErrAndExit (String message){
		System.err.println (message);
		System.exit(0);
	}
	/**Prints message to screen, then exits.*/
	public static void printExit (String message){
		System.out.println (message);
		System.exit(0);
	}
	/**Counts the total length of an Object[][]*/
	public static int totalLength(Object[][] array){
		int length = 0;
		int num = array.length;
		for (int i=0; i<num; i++){
			length += array[i].length;
		}
		return length;
	}
	
	/**Removes .txt from end of String if present*/
	public static String trimTxt(String s){
		if (s.endsWith(".txt")) return s.substring(0,s.length()-4);
		return s; 
	}
	
	/**Prepends zeros to a string such that its total length = 10.  Use for converting a String number to a form that can be 
	 * correctly sorted.  ie would convert 39 to 0000000039*/
	public static String addZeros(String x){
		int numDigits = 10;
		String[] zeros = {"", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000", "0000000000"};
		int length = numDigits - x.length();
		if (length !=0 ) x = zeros[length]+x;
		return x;
	}

	/**Prepends zeros to each String such that its total length = 10.  Use for converting a String number to a form that can be 
	 * correctly sorted.  ie would convert 39 to 0000000039*/
	public static String[] addZeros(String[] x){
		int num = x.length;
		int numDigits = 10;
		String[] zeros = {"", "0", "00", "000", "0000", "00000", "000000", "0000000", "00000000", "000000000", "0000000000"};
		for (int i=0; i<num; i++){
			int length = numDigits - x[i].length();
			if (length !=0 ) x[i] = zeros[length]+x[i];
		}
		return x;
	}
	
	/**Loads a HashSet with and Object array.*/
	public static HashSet loadHashSet(Object[] s){
		int num = s.length;
		HashSet hash = new HashSet(num);
		for (int i=0; i< num; i++) hash.add(s[i]);
		return hash;
	}
	
	/**Trims "" from the beginning and end of String if both are found.  
	 * Excel when saving as a tab delimited file, adds quotes !$#%#$!!!*/
	public static String trimQuotes(String cell){
		if (cell.matches("^\".*\"$")) return cell.substring(1, cell.length()-1);
		return cell;
	}
	
	/**Splits a String on a regular expression, watching out for escaped characters
	 * ie Cat\=Mouse would not be split when = is given as the regex, a String[0], 
	 * {"Cat\=Mouse"} would be returned.  The method will not
	 * return empty Strings, Cat===Mouse would return {"Cat","Mouse}. Lastly,
	 * each String in the String[] is .trim()ed prior to saving, so no flanking 
	 * white space. Use this method when you need to allow for character escaping.
	 */
	public static String[] splitString (String concat, String regex){
		if (Misc.isEmpty(regex)) return new String[]{concat};
		
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(concat);
		ArrayList al = new ArrayList();
		int start = 0;
		int stop = 0;
		char slash = '\\';
		String token;
		
		//scan seq for match
		while (m.find()){
			//find a match
			stop = m.start();
			//System.out.println(stop);
			//is it escaped with a \
			if (stop==0) { start = 1;}
			else if (concat.charAt(stop-1) != slash ) {
				token = concat.substring(start, stop).trim();
				if (token.equals("") == false) al.add(token);
				start = stop +1;
			}
		}
		
		//add last
		token = concat.substring(start).trim();
		if (token.equals("") == false) al.add(token);

		//convert to String[]
		String [] cells = new String[al.size()];
		al.toArray(cells);
		return cells;
	}
	
	
	/**Loose matching of substring guess from larger String[]s.  
	 * Returns index of first matching choice, -1 if nothing found.
	 * Case insensitive. */
	public static int fetchMatchingStringArrayIndex(String guess, String[] choices){
		int num = choices.length;
		String lcGuess = guess.toLowerCase().trim();
		for (int i=0; i< num; i++){
			 if (choices[i].toLowerCase().indexOf(lcGuess)!=-1) return i;
		}
		return -1;
	}
	
	/**Lower cases a String[]*/
	public String[] toLowerCase(String[] strings){
		int num = strings.length;
		String[] ls = new String[num];
		for (int i=0; i<num; i++) ls[i] = strings[i].toLowerCase();
		return ls;
	}

	/**Prints a Object[] to System.out*/
	public static void printArray(Object[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.println(array[i]);
	}
	
	/**Prints a byte[] to System.out*/
	public static void printArray(byte[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.println(array[i]);
	}
	
	/**Prints a boolean[] to System.out*/
	public static void printArray(boolean[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.println(array[i]);
	}

	/**Prints a Object[][] to System.out*/
	public static void printArray(Object[][] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) {
			int len2= array[i].length;
			for (int j=0; j< len2; j++){
				System.out.println(array[i][j].toString());
			}
		}
	}

	/**Prints a String[] to System.out*/
	public static void printArray(String[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.println(array[i]);
	}
	
	/**Prints a String[] to System.out*/
	public static void printArrayLine(String[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.print(array[i]+" ");
		System.out.println();
	}

	/**Prints ArrayList to System.out*/
	public static void printArray(ArrayList array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.size();
		for (int i=0; i<len; i++) System.out.println(array.get(i));
	}

	/**Prints a int[] to System.out*/
	public static void printArray(int[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.print(array[i]+" ");
		System.out.println();
	}
	/**Prints a int[] to System.out*/
	public static void printArray(float[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.print(array[i]+ " ");
		System.out.println();
	}	
	/**Prints a double[] to System.out on a single line.*/
	public static void printArray(double[] array){
		if (array==null){
			System.out.println("null");
			return;
		}
		int len = array.length;
		for (int i=0; i<len; i++) System.out.print(array[i]+" ");
		System.out.println();
	}


	/**Trims all the strings in the array String.trim()*/
	public static void trim(String[] s){
		for (int i=0; i< s.length; i++) s[i] = s[i].trim();
	}

	/**Removes the random digit_ proceeding a string, (ie 2432344322_fileName.txt to fileName.txt)*/
	public static String clipUnderScore(String string){
		int pos = string.indexOf("_");
		if (pos == -1) return string;
		return string.substring(pos+1);
	}

	/**Truncates a String adding three periods if too long.*/
	public static String truncate(String string, int cutOff){		
		if (string.length()>cutOff){
			StringBuffer sb = new StringBuffer (string.substring(0,cutOff-3));
			sb.append("...");
			return sb.toString();
		}
		return string;
	}

	/**Returns a nicely formated time (15 May 2004).
	 * (Note, all I can say is that the GC DateFormat Date classes are so convoluted as to be utterly useless. Shame!)*/
	public static String getDate(){
		String[] months = {"Jan","Feb","Mar","Apr","May","June","July", "Aug","Sept","Oct","Nov","Dec"};
		GregorianCalendar c = new GregorianCalendar();
		return c.get(Calendar.DAY_OF_MONTH)+ " "+months[c.get(Calendar.MONTH)]+ " "+ c.get(Calendar.YEAR);
	}
	/**Returns a nicely formated time (15May2004).
	 * (Note, all I can say is that the GC DateFormat Date classes are so convoluted as to be utterly useless. Shame!)*/
	public static String getDateNoSpaces(){
		String[] months = {"Jan","Feb","Mar","Apr","May","June","July", "Aug","Sept","Oct","Nov","Dec"};
		GregorianCalendar c = new GregorianCalendar();
		return c.get(Calendar.DAY_OF_MONTH)+months[c.get(Calendar.MONTH)]+ c.get(Calendar.YEAR);
	}
	
	/**Returns a formatted time 2:15:00 given 2.25*/
	public static String getFormattedTimeFromFraction (double fractionalTime){
		int whole = new Double (fractionalTime).intValue();
		double fraction = fractionalTime - whole;
		int min = (int)(fraction * 60);
		if (min<10) return whole+":0"+min+":00";
		return whole+":"+min+":00";
	}

	/**Takes a String like testSlide:33328:testslide desc...:1 or test: gip: 2,
	 * splits on the : and looks for ID in the last String[x], if not found it returns zero, otherwise
	 * it returns a parsed int.*/			
	public static int extractIdFromConcat(String concat){
		if (concat==null) return 0;
		String[] cells = concat.split(":");
		int numCells = cells.length;
		if (numCells!=0){
			String id = cells[numCells-1].trim();
			//check that a number is present
			try{
				return Integer.parseInt(id);
			} catch (NumberFormatException e){	
				System.err.println("Returning zero from extractIdFromConcat(), concat -> "+concat);
				return 0;
			}
		}
		return 0;
	}

	/**Takes a String like 1: testSlide: 33328:testslide desc etc or 2: test: gip,
	 * splits on the : and looks for ID in the first String[x], if not found it returns zero, otherwise
	 * it returns a parsed int.*/			
	public static int extractIdFromFrontOfConcat(String concat){
		if (concat==null) return 0;
		String[] cells = concat.split(":");
		int numCells = cells.length;
		if (numCells!=0){
			String id = cells[0].trim();
			//check that a number is present
			try{
				return Integer.parseInt(id);
			} catch (NumberFormatException e){
				System.err.println("Returning zero from extractIdFromFrontOfConcat(), concat -> "+concat);
				return 0;
			}
		}
		return 0;
	}

	/**Ditto to extractIDFromConcat but works on String[] returning IDs as a String[].
	 * Pulls off an int from the first cel of a colon delimited String.*/			
	public static String[] extractIDsFromConcats(String[] concats){
		int len = concats.length;
		String[] IDs = new String[len];
		for (int i=0; i<len; i++){
			IDs[i]=concats[i].split(":")[0].trim();
		} 
		return IDs;
	}

	/**Returns a String[2] split on first colon, if no colon returns null.*/	
	public static String[] splitOnFirstColon(String idColonName){
		int f = idColonName.indexOf(":");
		if (f==-1) return null;
		else return new String[]{idColonName.substring(0,f).trim(), idColonName.substring(f+1).trim()};
		
	}
	
	/**Method to check if a string is null or empty after trimming.*/
	public static boolean isEmpty(String text) {
		if (text == null || text.trim().equals("")) return true;
		return false;
	}

	/**Method to check if a string is not null and contains more than "".*/
	public static boolean isNotEmpty(String text) {
		if (text == null || text.trim().equals("")) return false;
		return true;
	}		
	/**Converts an ArrayList of Integer to int[]. Returns null if empty.*/
	public static int[] integerArrayListToIntArray(ArrayList integerAL){
		if (integerAL == null) return null;
		int num = integerAL.size();
		if (num ==0) return null;
		int[] ints = new int[num];
		try {
			for (int i=0; i< num; i++){
				ints[i]= ((Integer)integerAL.get(i)).intValue();
			}
			return ints;
		}
		catch(Exception e){
			System.err.println("Returning null from integerArrayListToIntArray(), ArrayList -> "+integerAL);
			return null;
		}
	}
	/**Returns a String[] given an ArrayList of Strings.*/
	public static String[] stringArrayListToStringArray(ArrayList stringAL){
		String[] s = new String[stringAL.size()];
		stringAL.toArray(s);
		return s;
	}

	
	
	/**Returns a String separated by separator for each int[i].
	 * Returns "" if null or uninitialized.*/
	public static String intArrayToString(int[] i, String separator){
		if (i==null) return "";
		int len = i.length;
		if (len==1) return i[0]+"";
		if (len==0) return "";
		StringBuffer sb = new StringBuffer(i[0]+"");
		for (int j=1; j<len; j++){
			sb.append(separator);
			sb.append(i[j]);
		}
		return sb.toString();
	}		
	
	/**Returns a String separated by separator for each float[i].
	 * Returns "" if null or uninitialized.*/
	public static String floatArrayToString(float[] i, String separator){
		if (i==null) return "";
		int len = i.length;
		if (len==1) return i[0]+"";
		if (len==0) return "";
		StringBuffer sb = new StringBuffer(i[0]+"");
		for (int j=1; j<len; j++){
			sb.append(separator);
			sb.append(i[j]);
		}
		return sb.toString();
	}	
	
	/**Returns a String separated by separator for each double[i].
	 * Returns "" if null or uninitialized.*/
	public static String doubleArrayToString(double[] i, String separator){
		if (i==null) return "";
		int len = i.length;
		if (len==1) return i[0]+"";
		if (len==0) return "";
		StringBuffer sb = new StringBuffer(i[0]+"");
		for (int j=1; j<len; j++){
			sb.append(separator);
			sb.append(i[j]);
		}
		return sb.toString();
	}	


	/**Creates a LinkedHashMap from a String[] where the array is ordered such as key,value,key,value...
	 * Direction 0 forward, 1 = reverse of making the Map. Returns null if odd number of Strings*/
	public static LinkedHashMap createLinkedHashMap(String[] s, int direction){
		int len = s.length;
		LinkedHashMap hash= null;
		if (len % 2 !=0) return hash;
		hash = new LinkedHashMap(len/2);
		if (direction == 0){
			for (int i=len-1; i>=0; i= i-2){
				hash.put(s[i-1], s[i]);
			}
		}
		else {
			for (int i=len-1; i>=0; i= i-2){
				hash.put(s[i], s[i-1]);
			}			
		}
		return hash;
	}
	/**Prints a HashSet to a file. Each entry on a new line.*/
	public static boolean printHashSetToFile(HashSet hash, String file){
		try{
			PrintWriter out = new PrintWriter(new FileWriter(file));
			Iterator it = hash.iterator();
			while(it.hasNext()){
				out.println(it.next());
			}
			out.close();
			return true;
		}catch(Exception e){
			System.err.println("Problem with printHashSetToFile()");
			e.printStackTrace();
			return false;
		}
	}
	
	/**Converts an Object[] to an ArrayList*/
	public static ArrayList objectArrayToArrayList(Object[] ob){
		int len = ob.length;
		ArrayList al = new ArrayList(len);
		for (int i=0; i<len; i++){
			al.add(ob[i]);
		}
		return al;
	}
			
	/**Print to System.out a double[][]*/
	public static void printArray(double[][] d){
		int numColumns = d.length;
		int numRows = d[0].length;
		for (int i=0; i<numColumns; i++){
			System.out.println("\n\nColumn: "+i);
			for (int j=0; j<numRows; j++){
				System.out.println("Row: "+j+"\tValue: "+d[i][j]);
			}
		}
	}

	/**Print to System.out a float[][]*/
	public static void printArray(float[][] d){
		int numColumns = d.length;
		int numRows = d[0].length;
		for (int i=0; i<numColumns; i++){
			System.out.println("\n\nColumn: "+i);
			for (int j=0; j<numRows; j++){
				System.out.println("Row: "+j+"\tValue: "+d[i][j]);
			}
		}
	}
	
	/**Print to System.out a int[][]*/
	public static void printArray(int[][] d){
		if (d==null || d[0] == null){
			System.out.println("null");
			return;
		}
		int numColumns = d.length;
		int numRows = d[0].length;
		for (int i=0; i<numColumns; i++){
			System.out.println("\nColumn: "+i);
			for (int j=0; j<numRows; j++){
				System.out.println("Row: "+j+"\tValue: "+d[i][j]);
			}
		}
	}	
	
	/**Returns an ArrayList of String given a String[]*/
	public static ArrayList stringArrayToArrayList(String[] s){
		int num = s.length;
		ArrayList al = new ArrayList(num);
		for (int i=0; i<num; i++){
			al.add(s[i]);
		}
		return al;
	}

	/**Returns a String separated by the separator given an ArrayList of String.*/
	public static String stringArrayListToString(ArrayList stringAL, String separator){
		int len = stringAL.size();
		if (len==0) return "";
		if (len==1) return (String)stringAL.get(0);
		StringBuffer sb = new StringBuffer((String)stringAL.get(0));
		for (int i=1; i<len; i++){
			sb.append(separator);
			sb.append((String)stringAL.get(i));
		}
		return sb.toString();
	}

	/**Returns a String separated by commas for each bin.*/
	public static String stringArrayToString(String[] s, String separator){
		if (s==null) return "";
		int len = s.length;
		if (len==1) return s[0];
		if (len==0) return "";
		StringBuffer sb = new StringBuffer(s[0]);
		for (int i=1; i<len; i++){
			sb.append(separator);
			sb.append(s[i]);
		}
		return sb.toString();
	}	
	
	/**Creates a HashMap from a String[] where the array is ordered such as key,value,key,value...
	 * Returns null if odd number of Strings*/
	public static HashMap createHashMap(String[] s){
		int len = s.length;
		HashMap hash= null;
		if (len % 2 !=0) return hash;
		hash = new HashMap(len/2);
		for (int i=len-1; i>=0; i= i-2){
			hash.put(s[i-1], s[i]);
		}
		return hash;
	}
    /**Returns true if a String contains just white space or is empty.*/
	public static boolean isStringEmpty(String string){
		if (string==null || string.trim().equals("")) return true;
		return false;
	}	
}
