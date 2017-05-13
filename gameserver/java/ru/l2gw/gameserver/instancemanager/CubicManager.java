package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.CubicTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.StringTokenizer;

public class CubicManager
{
	protected static Log _log = LogFactory.getLog(CubicManager.class.getName());

	private static CubicManager _instance;
	static Map<Integer, CubicTemplate> _cubics;

	public static CubicManager getInstance()
	{
		if(_instance == null)
		{
			_log.info("Initializing CubicManager");
			_instance = new CubicManager();
			_instance.load();
		}
		return _instance;
	}

	public static void reload()
	{
		_instance = new CubicManager();
	}

	public CubicManager()
	{
		_cubics = new FastMap<Integer, CubicTemplate>();

		_log.info("CubicManager: Initializing");
		load();
		_log.info("CubicManager: Loaded " + _cubics.size() + " Cubic templates.");
	}

	private void load()
	{
		try
		{
			File cubicFile = new File(Config.DATAPACK_ROOT, "data/cubics.csv");
			LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(cubicFile)));

			//id;level;live time;m.attck;attck count;reuse;activateRate;skillId-skillLvl;skillId-skillLvl;
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				StringTokenizer st = new StringTokenizer(line, ";");
				if(st.countTokens() < 8)
				{
					_log.warn("CubicManager: parse error in line " + lnr.getLineNumber() + ": \"" + line + "\"");
					continue;
				}

				int c = 1;
				String currentToken = null;
				try
				{
					currentToken = st.nextToken().trim();
					int id = Integer.parseInt(currentToken);
					c++;
					currentToken = st.nextToken().trim();
					c++;
					int level = Integer.parseInt(currentToken);
					currentToken = st.nextToken().trim();
					c++;
					int liveTime = Integer.parseInt(currentToken);
					currentToken = st.nextToken().trim();
					c++;
					int power = Integer.parseInt(currentToken);
					currentToken = st.nextToken().trim();
					c++;
					int attackCount = Integer.parseInt(currentToken);
					currentToken = st.nextToken().trim();
					c++;
					int reuseDelay = Integer.parseInt(currentToken);
					currentToken = st.nextToken().trim();
					c++;
					int activateRate = Integer.parseInt(currentToken);

					CubicTemplate ct = new CubicTemplate(id, level, liveTime, power, attackCount, reuseDelay, activateRate);

					while(st.hasMoreTokens())
					{
						currentToken = st.nextToken().trim();
						c++;

						String[] skillInfo = currentToken.split("-");
						int skillId = Integer.parseInt(skillInfo[0]);
						int skillLevel = Integer.parseInt(skillInfo[1]);

						L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
						if(skill == null)
						{
							_log.warn("CubicManager: no skill found for " + ct + " skillId=" + skillId + " skillLevel=" + skillLevel);
							continue;
						}

						ct.addSkill(skill);
					}

					if(ct.getSkills().size() < 1)
					{
						_log.warn("CubicManager: "+ct+" has no skills! skip...");
						continue;
					}

					_cubics.put(ct.hashCode(), ct);
				}
				catch(NumberFormatException e)
				{
					_log.warn("CubicManager: parse error in line " + lnr.getLineNumber() + " field " + c + " \"" + currentToken + "\"");
				}
			}
		}
		catch(final Exception e)
		{
			_log.warn("CubicManager: Error parsing " + Config.DATAPACK_ROOT + "data/cubics.csv file. " + e);
		}
	}

	public CubicTemplate getCubicTemplate(int id, int level)
	{
		if(_cubics.containsKey(id * 300 + level))
			return _cubics.get(id * 300 + level);

		_log.info("CubicManager: no CubicTemplate for id=" + id + ";level=" + level);
		return null;
	}
}
