package ru.l2gw.util;

import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;

import java.io.*;
import java.util.HashMap;

public class Files
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(Strings.class.getName());

	private static HashMap<String, String> cache = new HashMap<String, String>();

	public static String read(String name)
	{
		if(name == null)
			return null;

		if(Config.USE_FILE_CACHE && cache.containsKey(name))
			return cache.get(name);

		File file = new File("./" + name);

		//		_log.info("Get file "+file.getPath());

		if(!file.exists())
			return null;

		String content = null;

		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new UnicodeReader(new FileInputStream(file), "UTF-8"));
			StringBuffer sb = new StringBuffer();
			String s = "";
			while((s = br.readLine()) != null)
				sb.append(s).append("\n");
			content = sb.toString();
			sb = null;
		}
		catch(Exception e)
		{ /* problem are ignored */}
		finally
		{
			try
			{
				if(br != null)
					br.close();
			}
			catch(Exception e1)
			{ /* problems ignored */}
		}

		if(Config.USE_FILE_CACHE)
			cache.put(name, content);

		return content;
	}

	public static void cacheClean()
	{
		cache = new HashMap<String, String>();
	}

	public static long lastModified(String name)
	{
		if(name == null)
			return 0;

		return new File(name).lastModified();
	}

	public static String read(String name, L2Player player)
	{
		return read(name, player, true);
	}

	public static String read(String name, String lang)
	{
		return read(name, lang, true);
	}

	public static String read(String name, L2Player player, boolean parseBB)
	{
		if(player == null)
			return "";
		return read(name, player.getVar("lang@"), parseBB);
	}

	public static String langFileName(String name, String lang)
	{
		if(lang == null || lang.equalsIgnoreCase("en"))
			lang = "";

		String tmp;

		tmp = name.replaceAll("(.+)(\\.htm)", "$1-" + lang + "$2");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		tmp = name.replaceAll("(.+)(/[^/].+\\.htm)", "$1/" + lang + "$2");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		tmp = name.replaceAll("(.+?/html)/", "$1-" + lang + "/");
		if(Config.DEBUG)
			_log.info("Try load file " + tmp);
		if(!tmp.equals(name) && lastModified(tmp) > 0)
			return tmp;

		if(lastModified(name) > 0)
			return name;

		return null;
	}

	public static String read(String name, String lang, boolean parseBB)
	{
		String tmp = langFileName(name, lang);

		long last_modif = lastModified(tmp); // время модификации локализованного файла
		if(last_modif > 0) // если он существует
		{
			if(last_modif >= lastModified(name) || !Config.CHECK_LANG_FILES_MODIFY) // и новее оригинального файла
				return parseBB ? Strings.bbParse(read(tmp)) : read(tmp); // то вернуть локализованный

			_log.warn("Last modify of " + name + " more then " + tmp); // если он существует но устарел - выругаться в лог
		}

		return parseBB ? Strings.bbParse(read(name)) : read(tmp); // если локализованный файл отсутствует вернуть оригинальный
	}

	/**
	 * Сохраняет строку в файл в кодировке UTF-8.<br>
	 * Если такой файл существует, то перезаписывает его.
	 * @param path путь к файлу
	 * @param string сохраняемая строка
	 */
	public static void writeFile(String path, String string)
	{
		if(string == null || string.length() == 0)
			return;

		File target = new File(path);

		if(!target.exists())
			try
			{
				target.createNewFile();
			}
			catch(IOException e)
			{
				e.printStackTrace(System.err);
			}

		try
		{
			FileOutputStream fos = new FileOutputStream(target);
			fos.write(string.getBytes("UTF-8"));
			fos.close();
		}
		catch(IOException e)
		{
			e.printStackTrace(System.err);
		}
	}

	public static byte[] readFile(String filePath)
	{
		File file = new File(filePath);

		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
			byte[] data = new byte[fis.available()];
			fis.read(data);
			return data;
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(fis != null)
				{
					fis.close();
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

		return null;
	}

	public static void dumpToFile(byte[] data, String fileName)
	{
		File out = new File(fileName);
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(out);
			fos.write(data);
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(fos != null)
			{
				try
				{
					fos.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}