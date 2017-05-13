package ru.l2gw.util;

import org.apache.commons.logging.*;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.regex.Pattern;

public class Strings
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(Strings.class.getName());

	private static final char hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String bytesToString(byte[] b)
	{
		String ret = "";
		for(byte element : b)
		{
			ret += String.valueOf(hex[(element & 0xF0) >> 4]);
			ret += String.valueOf(hex[element & 0x0F]);
		}
		return ret;
	}

	public static String addSlashes(String s)
	{
		if(s == null)
			return "";

		s = s.replace("\\", "\\\\");
		s = s.replace("\"", "\\\"");
		s = s.replace("@", "\\@");
		s = s.replace("'", "\\'");
		return s;
	}

	public static String stripSlashes(String s)
	{
		if(s == null)
			return "";
		s = s.replace("\\'", "'");
		s = s.replace("\\\\", "\\");
		return s;
	}

	public static Integer parseInt(Object x)
	{
		if(x == null)
			return 0;

		if(x instanceof Integer)
			return (Integer) x;

		if(x instanceof Double)
			return ((Double) x).intValue();

		if(x instanceof Boolean)
			return (Boolean) x ? -1 : 0;

		Integer res = 0;
		try
		{
			res = Integer.parseInt("" + x);
		}
		catch(Exception e)
		{}
		return res;
	}

	public static Double parseFloat(Object x)
	{
		if(x instanceof Double)
			return (Double) x;

		if(x instanceof Integer)
			return 0.0 + (Integer) x;

		if(x == null)
			return 0.0;

		Double res = 0.0;
		try
		{
			res = Double.parseDouble("" + x);
		}
		catch(Exception e)
		{}
		return res;
	}

	public static Boolean parseBoolean(Object x)
	{
		if(x instanceof Integer)
			return (Integer) x != 0;

		if(x == null)
			return false;

		if(x instanceof Boolean)
			return (Boolean) x;

		if(x instanceof Double)
			return Math.abs((Double) x) < 0.00001;

		return !("" + x).equals("");
	}

	private static String[] tr;
	private static String[] trb;
	private static String[] trcode;

	public static void reload()
	{
		Files.cacheClean();
		String[] pairs = Files.read("data/translit.txt").split("\n");
		tr = new String[pairs.length * 2];
		for(int i = 0; i < pairs.length; i++)
		{
			String[] ss = pairs[i].split(" +");
			tr[i * 2] = ss[0];
			tr[i * 2 + 1] = ss[1];
		}

		pairs = Files.read("data/translit_back.txt").split("\n");
		trb = new String[pairs.length * 2];
		for(int i = 0; i < pairs.length; i++)
		{
			String[] ss = pairs[i].split(" +");
			trb[i * 2] = ss[0];
			trb[i * 2 + 1] = ss[1];
		}

		pairs = Files.read("data/transcode.txt").split("\n");
		trcode = new String[pairs.length * 2];
		for(int i = 0; i < pairs.length; i++)
		{
			String[] ss = pairs[i].split(" +");
			trcode[i * 2] = ss[0];
			trcode[i * 2 + 1] = ss[1];
		}
		_log.info("Loaded " + (tr.length + tr.length + trcode.length) + " translit entrys");
	}

	public static String translit(String s)
	{
		for(int i = 0; i < tr.length; i += 2)
			s = s.replace(tr[i], tr[i + 1]);

		return s;
	}

	public static String fromTranslit(String s, int type)
	{
		if(type == 1)
			for(int i = 0; i < trb.length; i += 2)
				s = s.replace(trb[i], trb[i + 1]);
		else if(type == 2)
			for(int i = 0; i < trcode.length; i += 2)
				s = s.replace(trcode[i], trcode[i + 1]);

		return s;
	}

	public static String replace(String str, String regex, int flags, String replace)
	{
		return Pattern.compile(regex, flags).matcher(str).replaceAll(replace);
	}

	public static boolean matches(String str, String regex, int flags)
	{
		return Pattern.compile(regex, flags).matcher(str).matches();
	}

	public static String bbParse(String s)
	{
		if(s == null)
			return null;

		s = s.replace("\r", "");
		s = s.replaceAll("(\\s|\"|\'|\\(|^|\n)\\*(.*?)\\*(\\s|\"|\'|\\)|\\?|\\.|!|:|;|,|$|\n)", "$1<font color=\"LEVEL\">$2</font>$3");
		s = replace(s, "^!(.*?)$", Pattern.MULTILINE, "<font color=\"LEVEL\">$1</font>\n\n");
		s = s.replaceAll("%%\\s*\n", "<br1>");
		s = s.replaceAll("\n\n+", "<br>");
		s = replace(s, "\\[([^\\]\\|]*?)\\|([^\\]]*?)\\]", Pattern.DOTALL, "<a action=\"bypass -h $1\">$2</a>");
		s = s.replaceAll(" @", "\" msg=\"");

		return s;
	}

	public static String utf2win(String utfString)
	{
		try
		{
			return new String(utfString.getBytes("Cp1251"));
		}
		catch(UnsupportedEncodingException uee)
		{
			return utfString;
		}
	}

	public static String FormatTable(Collection<String> tds, int rows, boolean appendTD)
	{
		String result = "";
		int i = 0;
		for(String td : tds)
		{
			if(i == 0)
				result += "<tr>";
			result += appendTD ? "<td>" + td + "</td>" : td;
			i++;
			if(i == rows)
			{
				result += "</tr>";
				i = 0;
			}
		}
		if(i > 0 && i < rows)
		{
			while(i < rows)
			{
				result += "<td></td>";
				i++;
			}
			result += "</tr>";
		}
		return result;
	}

	/**
	 * кроме обычного trim, заменяет кавычки на нестандартные UTF-8, удяляет ВСЕ двойные пробелы, убирает символы <>
	 */
	public static String trimHtml(String s)
	{
		int i;
		s = s.trim().replaceAll("\"", "?").replaceAll("'", "?").replaceAll("<", "").replaceAll(">", "");
		do
		{
			i = s.length();
			s = s.replaceAll("  ", " ");
		} while(i > s.length());
		return s;
	}

	public static String htmlButton(String value, String action, int width)
	{
		return htmlButton(value, action, width, 22);
	}

	public static String htmlButton(String value, String action, int width, int height)
	{
		return String.format("<button value=\"%s\" action=\"%s\" back=\"L2UI_CT1.Button_DF_Small_Down\" width=%d height=%d fore=\"L2UI_CT1.Button_DF_Small\">", value, action, width, height);
	}

	public static String getFString(int id, String... params)
	{
		StringBuffer param = new StringBuffer("");
		for(int i = 0; i < 5; i++)
			if(params.length > i && params[i] != null)
				param.append(" p").append(i + 1).append("=\"").append(params[i]).append("\"");

		return "<fstring" + param + ">" + id + "</fstring>";
	}
}