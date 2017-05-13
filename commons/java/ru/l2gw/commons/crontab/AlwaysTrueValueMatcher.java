package ru.l2gw.commons.crontab;

/**
 * This ValueMatcher always returns true!
 *
 * @author Carlo Pelliccia
 */
class AlwaysTrueValueMatcher implements ValueMatcher
{

	/**
	 * Always true!
	 */
	public boolean match(int value)
	{
		return true;
	}

}
