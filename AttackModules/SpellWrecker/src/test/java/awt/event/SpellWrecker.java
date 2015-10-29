package test.java.awt.event;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

import jreframeworker.annotations.types.DefineType;

@DefineType
public class SpellWrecker {

	private final long WINDOW_LENGTH = 1000; // 1 second window
	private final int WINDOW_HISTORY = 5; // 5 window history
	private final double MIN_OBSERVATIONS_AVERAGE = 3; // 3 characters per second average
	private final double MAX_STANDARD_DEVIATION = 3.5; // windows deviate at most 3.5 characters per second deviation
	private final long ACTION_DELAY = 700; // half second delay between typo injections
	
	private final static char[][] LOWERCASE_KEYBOARD = {{'`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', ' '},
												 {' ', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'},
												 {' ', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', ' ', ' '},
												 {' ', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', ' ', ' ', ' '}};

	private final static char[][] UPPERCASE_KEYBOARD = {{'~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', ' '},
												 {' ', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '{', '}', '|'},
												 {' ', 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"', ' ', ' '},
												 {' ', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?', ' ', ' ', ' '}};
	
	private int[] observations;
	private long[] timestamps;
	private int index = 0;
	private long lastAction = System.currentTimeMillis();

	private static SpellWrecker instance = null;
	
	private SpellWrecker(){
		this.observations = new int[WINDOW_HISTORY + 1];
		this.timestamps = new long[WINDOW_HISTORY + 1];
		
		// initialize the timestamps array
		long currentTimestamp = System.currentTimeMillis();
		for(int i=1; i<=timestamps.length; i++){
			timestamps[i-1] = currentTimestamp + (i * WINDOW_LENGTH);
		}
	}
	
	private static SpellWrecker getInstance() {
		if (instance == null) {
			instance = new SpellWrecker();
		}
		return instance;
	}

	private int getCurrentObservations() {
		return observations[index];
	}
	
	private double getHistoricalAverage(){
		double result = 0.0;
		int[] history = getHistoricalObservations();
		for(int i=0; i<history.length; i++){
			result += history[i];
		}
		return result / (double) history.length;
	}
	
	// TODO: Don't use this algorithm as advised on http://stackoverflow.com/a/14839593/475329
	private double getHistoricalStandardDeviation(){
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
	
	private int[] getHistoricalObservations(){
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
		
		if(new Random().nextInt(100) == 0){
			return _spellwreck(input);
		}
		
		// There are some bugs, for now just spellwreck 1/100 times
		/*
		// there can only be one SPELLWRECKER!!!!!
		SpellWrecker monitor = getInstance();
		
		System.out.println(monitor.toString());
		
		// record the observation
		long currentTimestamp = System.currentTimeMillis();
		if(currentTimestamp < monitor.timestamps[monitor.index]){
			monitor.observations[monitor.index]++;
		} else {
			while(currentTimestamp >= monitor.timestamps[monitor.index]){
				long lastTimestamp = monitor.timestamps[monitor.index];
				monitor.index = (monitor.index + 1) % monitor.timestamps.length;
				// extend history forward  by one cell if needed
				if(lastTimestamp > monitor.timestamps[monitor.index]){
					monitor.timestamps[monitor.index] = lastTimestamp + monitor.WINDOW_LENGTH;
					monitor.observations[monitor.index] = 0;
				}
			}
			monitor.observations[monitor.index]++;
		}
		
		// if its the right time to insert a typo go for it
		double historicalAverage = monitor.getHistoricalAverage();
		if(historicalAverage > monitor.MIN_OBSERVATIONS_AVERAGE){
			if(monitor.getCurrentObservations() > historicalAverage){
				if(monitor.getHistoricalStandardDeviation() < monitor.MAX_STANDARD_DEVIATION){
					if((currentTimestamp - monitor.lastAction) > monitor.ACTION_DELAY){
						monitor.lastAction = currentTimestamp;
						return monitor._spellwreck(input);
					}
				}
			}
		}
		*/
		
		return input;
	}
	
	// helper method to spellwreck a given character
	private static char _spellwreck(char input){
		
		if(input == ' '){
			return input;
		}
		
		// create some character arrays to hold the keyboard layouts
		// note a space is used as a filler character and will be ignored
		char[][] keyboard = LOWERCASE_KEYBOARD;
		char[][] shiftKeyboard = UPPERCASE_KEYBOARD;
		
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
					
					// another neighbor for a character is the lower case character (user let go of shift key too early)
					neighbors += LOWERCASE_KEYBOARD[row][column];
					
					// randomly pick a possible typo
					char[] typos = neighbors.toCharArray();
					char typo = typos[new java.util.Random().nextInt(typos.length)];
					
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
	
	@Override
	public String toString(){
		DecimalFormat df = new DecimalFormat("#.##");
		return "Keystrokes Per Second=" + getCurrentObservations()
		+ ", History Std Deviation=" + df.format(getHistoricalStandardDeviation())
		+ ", History=" + Arrays.toString(getHistoricalObservations());
	}
	
}
