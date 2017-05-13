package ru.l2gw.commons.crontab;

/**
 * <p>
 * This kind of exception is thrown if an invalid scheduling pattern is
 * encountered by the scheduler.
 * </p>
 *
 * @author Carlo Pelliccia
 */
public class InvalidPatternException extends RuntimeException
{

	private static final long serialVersionUID = 1L;

	/**
	 * Package-reserved construction.
	 */
	InvalidPatternException()
	{
	}

	/**
	 * Package-reserved construction.
	 */
	InvalidPatternException(String message)
	{
		super(message);
	}

}
