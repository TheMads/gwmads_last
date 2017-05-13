package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;

import java.io.*;
import java.util.StringTokenizer;


public class ColorNameTable
{
	private static Log _log = LogFactory.getLog(ColorNameTable.class.getName());

	private static ColorNameTable _instance;
	private TIntObjectHashMap<NameColor> _colors;
	private TIntObjectHashMap<NameColor> _titleColors;

	public static ColorNameTable getInstance()
	{
		if(_instance == null)
			_instance = new ColorNameTable();

		return _instance;
	}

	public ColorNameTable()
	{
		_colors = new TIntObjectHashMap<>();
		_titleColors = new TIntObjectHashMap<>();
		load();
	}

	public class NameColor
	{
		private int _count;
		private int _color;
		private int _itemId;
		private String _name;
		private String _colorStr;

		public NameColor(int color, int itemId, int count, String name, String colorStr)
		{
			_color = color;
			_count = count;
			_itemId = itemId;
			_name = name;
			_colorStr = colorStr;
		}

		public int getColor()
		{
			return _color;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getCount()
		{
			return _count;
		}

		public String getName()
		{
			return _name;
		}

		public String getColorStr()
		{
			return _colorStr;
		}
	}

	private void load()
	{
		LineNumberReader lnr = null;
		_colors = new TIntObjectHashMap<>();
		_titleColors = new TIntObjectHashMap<>();
		try
		{
			File nameColorData = new File(Config.DATAPACK_ROOT, "data/namecolors.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(nameColorData)));

			String line = null;
			_log.warn("Loading name colors");

			int id = 1;

			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				NameColor nc = parseList(line);
				_colors.put(id, nc);
				id++;
			}

			_log.warn("Name colors loaded: " + (id - 1));

			File titleColorData = new File(Config.DATAPACK_ROOT, "data/titlecolors.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(titleColorData)));

			_log.warn("Loading title colors");

			id = 1;

			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				NameColor nc = parseList(line);
				_titleColors.put(id, nc);
				id++;
			}

			_log.warn("Title colors loaded: " + (id - 1));
		}
		catch(FileNotFoundException e)
		{
			_log.warn("namecolors.csv or titlecolors.csv is missing in data folder");
		}
		catch(IOException e)
		{
			_log.warn("error while creating name colors " + e);
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch(Exception e1)
			{ /* ignore problems */ }
		}

	}

	private NameColor parseList(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");

		String colorStr = st.nextToken();
		int color = Integer.decode("0x" + colorStr);
		int itemId = Integer.parseInt(st.nextToken());
		int count = Integer.parseInt(st.nextToken());
		String name = st.nextToken();
		return new NameColor(color, itemId, count, name, colorStr);
	}

	public NameColor getColorById(int id)
	{
		if(_colors == null) return null;
		return _colors.get(id);
	}

	public NameColor getTitleColorById(int id)
	{
		if(_titleColors == null) return null;
		return _titleColors.get(id);
	}

	public void reload()
	{
		_colors = null;
		_colors = new TIntObjectHashMap<>();
		_titleColors = null;
		_titleColors = new TIntObjectHashMap<>();
		load();
	}

	public int[] getColorsIds()
	{
		if(_colors == null) return null;
		return _colors.keySet().toArray(new int[_colors.size()]);
	}

	public int[] getTitleColorsIds()
	{
		if(_titleColors == null) return null;
		return _titleColors.keySet().toArray(new int[_titleColors.size()]);
	}
}
