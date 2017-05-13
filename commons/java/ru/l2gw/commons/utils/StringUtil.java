package ru.l2gw.commons.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class StringUtil
{
	private StringUtil()
	{
	}

	public static String concat(final String... strings)
	{
		final StringBuilder sbString = new StringBuilder(getLength(strings));

		for (final String string : strings)
			sbString.append(string);

		return sbString.toString();
	}

	public static StringBuilder startAppend(final int sizeHint, final String... strings)
	{
		final int length = getLength(strings);
		final StringBuilder sbString = new StringBuilder(sizeHint > length ? sizeHint : length);

		for (final String string : strings)
			sbString.append(string);

		return sbString;
	}

	public static void append(final StringBuilder sbString, final String... strings)
	{
		sbString.ensureCapacity(sbString.length() + getLength(strings));

		for (final String string : strings)
			sbString.append(string);
	}

	private static int getLength(final String[] strings)
	{
		int length = 0;

		for (final String string : strings)
			if (string == null)
				length += 4;
			else
				length += string.length();

		return length;
	}

	public static String toUpperCaseAddSpaceAndLower(String st)
	{
		char[] chars = st.toCharArray();
		StringBuilder buf = new StringBuilder(chars.length);

		for (char ch : chars)
		{
			if (Character.isUpperCase(ch))
			{
				buf.append(" ");
			}

			buf.append(Character.toLowerCase(ch));
		}

		return buf.toString();
	}

	/**
	 * Проверяет строку на соответсвие регулярному выражению
	 * @param text Строка-источник
	 * @param template Шаблон для поиска
	 * @return true в случае соответвия строки шаблону
	 */
	public static boolean isMatchingRegexp(String text, String template)
	{
		Pattern pattern = null;
		try
		{
			pattern = Pattern.compile(template);
		}
		catch(PatternSyntaxException e) // invalid template
		{
			e.printStackTrace();
		}
		if(pattern == null)
			return false;
		Matcher regexp = pattern.matcher(text);
		return regexp.matches();
	}

	/**
	 * Производит замену в строке по регулярному выражению
	 * @param source Строка-источник
	 * @param template Шаблон для замены
	 * @param replacement Строка замена
	 * @return Замененную строку
	 */
	public static String replaceRegexp(String source, String template, String replacement)
	{
		Pattern pattern = null;
		try
		{
			pattern = Pattern.compile(template);
		}
		catch(PatternSyntaxException e) // invalid template
		{
			e.printStackTrace();
		}
		if(pattern != null)
		{
			Matcher regexp = pattern.matcher(source);
			source = regexp.replaceAll(replacement);
		}
		return source;
	}

	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 * @param startIdx - начальный индекс, если указать отрицательный то он отнимется от количества строк
	 * @param maxCount - мескимум элементов, если 0 - вернутся пустая строка, если отрицательный то учитыватся не будет
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx, int maxCount)
	{
		String result = "";
		if(startIdx < 0)
		{
			startIdx += strings.length;
			if(startIdx < 0)
				return result;
		}
		while(startIdx < strings.length && maxCount != 0)
		{
			if(!result.isEmpty() && glueStr != null && !glueStr.isEmpty())
				result += glueStr;
			result += strings[startIdx++];
			maxCount--;
		}
		return result;
	}

	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 * @param startIdx - начальный индекс, если указать отрицательный то он отнимется от количества строк
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx)
	{
		return joinStrings(glueStr, strings, startIdx, -1);
	}

	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 */
	public static String joinStrings(String glueStr, String[] strings)
	{
		return joinStrings(glueStr, strings, 0);
	}
}
