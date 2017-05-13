package ru.l2gw.commons.crontab;

import java.util.ArrayList;

/**
 * <p>
 * A ValueMatcher whose rules are in a plain array of integer values. When asked
 * to validate a value, this ValueMatcher checks if it is in the array.
 * </p>
 *
 * @author Carlo Pelliccia
 */
class IntArrayValueMatcher implements ValueMatcher
{

	/**
	 * The accepted values.
	 */
	private int[] values;

	/**
	 * Builds the ValueMatcher.
	 *
	 * @param integers An ArrayList of Integer elements, one for every value accepted
	 *                 by the matcher. The match() method will return true only if
	 *                 its parameter will be one of this list.
	 */
	public IntArrayValueMatcher(ArrayList<Integer> integers)
	{
		int size = integers.size();
		values = new int[size];
		for(int i = 0; i < size; i++)
		{
			try
			{
				values[i] = integers.get(i);
			}
			catch(Exception e)
			{
				throw new IllegalArgumentException(e.getMessage());
			}
		}
	}

	/**
	 * Returns true if the given value is included in the matcher list.
	 */
	public boolean match(int value)
	{
		for(int i = 0; i < values.length; i++)
		{
			if(values[i] == value)
			{
				return true;
			}
		}
		return false;
	}

}
