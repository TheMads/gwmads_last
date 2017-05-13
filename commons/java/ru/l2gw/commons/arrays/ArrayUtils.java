package ru.l2gw.commons.arrays;

/**
 * @author: rage
 * @date: 02.03.12 18:44
 */
public class ArrayUtils
{
	public static final String[] EMPTY_STRING_ARRAY = new String[0];
	public static final int[] EMPTY_INT_ARRAY = new int[0];
	public static final long[] EMPTY_LONG_ARRAY = new long[0];
	public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
	public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

	public static String[] toStringArray(String array)
	{
		if(array == null || array.isEmpty())
			return EMPTY_STRING_ARRAY;

		String[] a = array.split("[,;]");
		if(a.length < 1)
			return EMPTY_STRING_ARRAY;

		return a;
	}

	public static int[] toIntArray(String array)
	{
		String[] a = toStringArray(array);
		if(a == EMPTY_STRING_ARRAY || a.length < 1)
			return EMPTY_INT_ARRAY;

		int[] res = new int[a.length];
		for(int i = 0; i < a.length; i++)
			if(!a[i].isEmpty())
				res[i] = Integer.parseInt(a[i]);

		return res;
	}

	public static long[] toLongArray(String array)
	{
		String[] a = toStringArray(array);
		if(a == EMPTY_STRING_ARRAY || a.length < 1)
			return EMPTY_LONG_ARRAY;

		long[] res = new long[a.length];
		for(int i = 0; i < a.length; i++)
			if(!a[i].isEmpty())
				res[i] = Long.parseLong(a[i]);

		return res;
	}

	public static float[] toFloatArray(String array)
	{
		String[] a = toStringArray(array);
		if(a == EMPTY_STRING_ARRAY || a.length < 1)
			return EMPTY_FLOAT_ARRAY;

		float[] res = new float[a.length];
		for(int i = 0; i < a.length; i++)
			if(!a[i].isEmpty())
				res[i] = Float.parseFloat(a[i]);

		return res;
	}

	public static double[] toDoubleArray(String array)
	{
		String[] a = toStringArray(array);
		if(a == EMPTY_STRING_ARRAY || a.length < 1)
			return EMPTY_DOUBLE_ARRAY;

		double[] res = new double[a.length];
		for(int i = 0; i < a.length; i++)
			if(!a[i].isEmpty())
				res[i] = Double.parseDouble(a[i]);

		return res;
	}

	public static <T> boolean contains(T[] array, T value)
	{
		if(array == null)
			return false;

		for(T v : array)
			if(value == v)
				return true;
		return false;
	}

	public static boolean contains(int[] array, Object value)
	{
		if(array == null || !(value instanceof Integer))
			return false;

		for(int v : array)
			if((Integer) value == v)
				return true;
		return false;
	}
}