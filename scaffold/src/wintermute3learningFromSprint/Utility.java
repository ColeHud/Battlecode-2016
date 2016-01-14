package wintermute3learningFromSprint;
import java.util.ArrayList;

import battlecode.common.*;

public class Utility 
{
	//constants 
	public static final int MINIMUM_TIME_TO_TURTLE = 100;

	//codes
	public static final int STRATEGY_CODE = 77525;

	//get an arraylist of directions
	public static ArrayList<Direction> arrayListOfDirections()
	{
		ArrayList<Direction> directions = new ArrayList<Direction>();
		Direction[] arrayDirections = Direction.values();
		for(Direction dir : arrayDirections)
		{
			directions.add(dir);
		}
		return directions;
	}
}
