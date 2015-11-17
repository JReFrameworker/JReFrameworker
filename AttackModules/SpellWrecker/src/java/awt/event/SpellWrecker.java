package java.awt.event;

import java.util.Random;

import jreframeworker.annotations.types.DefineType;

@DefineType
public class SpellWrecker {

	public static boolean initialized = false;
	
	public static long windowLength;
	public static int[] observations;
	public static long[] timestamps;
	public static int index;
	public static long lastAction;

	public static void init() {
		// 1 second window, with 5 second history
		int windowHistory = 5;
		
		windowLength = 1000;
		observations = new int[windowHistory + 1];
		timestamps = new long[windowHistory + 1];
		index = 0;
		
		// initialize the timestamps array
		long currentTimestamp = System.currentTimeMillis();
		for(int i=1; i<=timestamps.length; i++){
			timestamps[i-1] = currentTimestamp + (i * windowLength);
		}
		
		initialized = true;
   }

	public static void observe(){
		
		if(!initialized){
			init();
		}
		
		// record the observation
		long currentTimestamp = System.currentTimeMillis();
		if(currentTimestamp < timestamps[index]){
			observations[index]++;
		} else {
			while(currentTimestamp >= timestamps[index]){
				long lastTimestamp = timestamps[index];
				index = (index + 1) % timestamps.length;
				// extend history forward  by one cell if needed
				if(lastTimestamp > timestamps[index]){
					timestamps[index] = lastTimestamp + windowLength;
					observations[index] = 0;
				}
			}
			observations[index]++;
		}
	}
	
	public static int getCurrentObservations(){
		return observations[index];
	}

	public static double getAverageObservations(){
		double result = 0.0;
		for(int i=0; i<observations.length; i++){
			result += observations[i];
		}
		return result / (double) observations.length;
	}

	public static int getMaxObservations() {
		int result = 0;
		for(int i=0; i<observations.length; i++){
			if(observations[i] > result){
				result = observations[i];
			}
		}
		return result;
	}
	
	// TODO: Don't use this algorithm as advised on http://stackoverflow.com/a/14839593/475329
	public static double getHistoricalStandardDeviation(){
		int[] history = getHistoricalObservations();
		
		// calculate the mean
		double sum = 0.0;
        for(int i=0; i<history.length; i++){
        	sum += observations[i];
        }
		double mean = ((double) sum) / ((double) history.length);
		
		// calculate the standard variance
		double temp = 0;
        for(int i=0; i<history.length; i++){
        	double x = (double) observations[i];
        	temp += (mean-x)*(mean-x);
        }
        double variance = temp/((double)history.length);
        
		return Math.sqrt(variance);
	}
	
	public static int[] getHistoricalObservations(){
		int size = observations.length-1;
		int offset = (index + 1) % observations.length;;
		int[] historicalObservations = new int[size];
		for(int i=0; i<size; i++){
			historicalObservations[i] = observations[offset];
			offset = (offset + 1) % observations.length;
		}
		return historicalObservations;
	}
	
	public static char spellwreck(char input){
		if(!initialized){
			init();
		}
		observe();
		char result = input;
		if(getAverageObservations() > 3){
			if(getHistoricalStandardDeviation() < 3.5){
				if(System.currentTimeMillis() - lastAction > 700){
					result = _spellwreck(input);
					lastAction = System.currentTimeMillis();
				}
			}
		}
		return result;
	}
	
	public static char _spellwreck(char input){
		if(input == ' '){
			return input;
		}
		
		// create some character arrays to hold the keyboard layouts
		// note a space is used as a filler character and will be ignored
		char[][] keyboard = {{'`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', ' '},
				{' ', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'},
				{' ', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', ' ', ' '},
				{' ', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', ' ', ' ', ' '}};

		char[][] shiftKeyboard = {{'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', ' '},
					 {' ', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '|'},
					 {' ', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', ' ', ' '},
					 {' ', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?', ' ', ' ', ' '}};
		
		// search both keyboards for the input character
		for(int row=0; row<keyboard.length; row++){
			for(int column=0; column<keyboard[row].length; column++){
				if(input == keyboard[row][column] || input == shiftKeyboard[row][column]){
					// for convenience lets just work with the keyboard the input is in from now on
					if(input == shiftKeyboard[row][column]){
						keyboard = shiftKeyboard;
					}
					// compute the minimum and maximum key locations we could choose from
					int rowLowerBound = row == 0 ? 1 : (row-1);
					int rowUpperBound = row == (keyboard.length-1) ? (keyboard.length-2) : (row+1);
					int columnLowerBound = column == 0 ? 1 : (column-1);
					int columnUpperBound = column == (keyboard[row].length-1) ? (keyboard[row].length-2) : (column+1);
					
					// create an array of unique typo characters
					String neighbors = "";
					char rowLowerBoundCharacter = keyboard[rowLowerBound][column];
					if(rowLowerBoundCharacter != ' ' && !neighbors.contains("" + rowLowerBoundCharacter)){
						neighbors += rowLowerBoundCharacter;
					}
					char rowUpperBoundCharacter =  keyboard[rowUpperBound][column];
					if(rowUpperBoundCharacter != ' ' && !neighbors.contains("" + rowUpperBoundCharacter)){
						neighbors += rowUpperBoundCharacter;
					}
					char columnLowerBoundCharacter = keyboard[row][columnLowerBound];
					if(columnLowerBoundCharacter != ' ' && !neighbors.contains("" + columnLowerBoundCharacter)){
						neighbors += columnLowerBoundCharacter;
					}
					char columnUpperBoundCharacter = keyboard[row][columnUpperBound];
					if(columnUpperBoundCharacter != ' ' && !neighbors.contains("" + columnUpperBoundCharacter)){
						neighbors += columnUpperBoundCharacter;
					}
					
					// another neighbor for upper case letter is the lower case letter (user let go of shift key too early)
					if(Character.isLetter(input) && Character.isUpperCase(input)){
						neighbors += Character.toLowerCase(input);
					}
					
					// randomly pick a possible typo
					char[] typos = neighbors.toCharArray();
					char typo = typos[new Random().nextInt(typos.length)];
					
					// don't convert letters to symbols, it is too noticeable
					if(Character.isLetter(input) && !Character.isLetter(typo)){
						return input;
					} else {
						return typo;
					}
				}
			}
		}
		
		// just return the original input if a typo cannot be generated
		return input;
	}
	
}
