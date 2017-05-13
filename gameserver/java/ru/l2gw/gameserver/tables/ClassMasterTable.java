package ru.l2gw.gameserver.tables;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.CubicManager;
import ru.l2gw.gameserver.model.base.ClassId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 20.05.2009
 * Time: 12:24:31
 */
public class ClassMasterTable
{
	protected static Log _log = LogFactory.getLog(CubicManager.class.getName());

	private static ClassMasterTable _instance;
	private static Map<String, List<ClassId>> _classMasterTypes;

	private ClassMasterTable()
	{
		_classMasterTypes = new HashMap<>();
		_log.info("ClassMasterTable: Initializing");
		load();
		_log.info("ClassMasterTable: Loaded " + _classMasterTypes.size() + " class master templates.");

	}

	public static ClassMasterTable getInstance()
	{
		if(_instance == null)
			_instance = new ClassMasterTable();

		return _instance;
	}

	public static void reload()
	{
		_instance = new ClassMasterTable();
	}

	private void load()
	{
		try
		{
			File classFile = new File(Config.DATAPACK_ROOT, "data/classmasters.csv");
			LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(classFile)));

			// type_name;classid,classid,classid...
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				StringTokenizer st = new StringTokenizer(line, ";");
				if(st.countTokens() < 2)
				{
					_log.warn("ClassMasterTable: parse error in line " + lnr.getLineNumber() + ": \"" + line + "\"");
					continue;
				}

				int c = 1;
				String currentToken = null;
				try
				{
					currentToken = st.nextToken().trim();
					String type = currentToken;
					c++;
					currentToken = st.nextToken().trim();
					StringTokenizer sc = new StringTokenizer(currentToken, ",");
					if(sc.countTokens() < 1)
					{
						_log.warn("ClassMasterTable: parse error in line " + lnr.getLineNumber() + ": type \""+type+"\" has no classId defined!");
						continue;
					}

					FastList<ClassId> classList = new FastList<ClassId>();

					while(sc.hasMoreTokens())
					{
						currentToken = sc.nextToken().trim();
						c++;
						int id = Integer.parseInt(currentToken);
						classList.add(ClassId.values()[id]);
					}

					if(classList.size() < 1)
					{
						_log.warn("ClassMasterTable: " + type + " has no class id's! skip...");
						continue;
					}

					_classMasterTypes.put(type, classList);
				}
				catch(NumberFormatException e)
				{
					_log.warn("ClassMasterTable: parse error in line " + lnr.getLineNumber() + " field " + c + " \"" + currentToken + "\"");
				}
			}
		}
		catch(final Exception e)
		{
			_log.warn("ClassMasterTable: Error parsing " + Config.DATAPACK_ROOT + "data/cubics.csv file. " + e);
		}
	}

	public List<ClassId> getClassListForType(String type)
	{
		if(_classMasterTypes.containsKey(type))
			return _classMasterTypes.get(type);

		return null;
	}
}
