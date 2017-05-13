package ru.l2gw.gameserver.skills;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Skill;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class SkillsEngine
{

	protected static Log _log = LogFactory.getLog(SkillsEngine.class.getName());

	private static final SkillsEngine _instance = new SkillsEngine();

	private List<File> _armorFiles;
	private List<File> _weaponFiles;
	private List<File> _etcitemFiles;
	private List<File> _skillFiles;

	public static SkillsEngine getInstance()
	{
		return _instance;
	}

	private SkillsEngine()
	{
		_armorFiles = new LinkedList<File>();
		_weaponFiles = new LinkedList<File>();
		_skillFiles = new LinkedList<File>();
		_etcitemFiles = new LinkedList<File>();
		hashFiles("data/stats/etcitem", _etcitemFiles);
		hashFiles("data/stats/armor", _armorFiles);
		hashFiles("data/stats/weapon", _weaponFiles);
		hashFiles("data/stats/skills", _skillFiles);
	}

	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(dirname);
		if(!dir.exists())
		{
			_log.warn("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for(File f : files)
			if(f.getName().endsWith(".xml"))
				hash.add(f);
	}

	public List<L2Skill> loadSkills(File file)
	{
		if(file == null)
		{
			_log.warn("SkillsEngine: File not found!");
			return null;
		}
		DocumentSkill doc = new DocumentSkill(file);
		doc.parse();
		return doc.getSkills();
	}

	public void loadAllSkills(TIntObjectHashMap<L2Skill> allSkills)
	{
		int count = 0;
		for(File file : _skillFiles)
		{
			List<L2Skill> s = loadSkills(file);
			if(s == null)
				continue;
			for(L2Skill skill : s)
			{
				allSkills.put(skill.getIndex(), skill);
				count++;
			}
		}
		_log.info("SkillsEngine: Loaded " + count + " Skill templates from XML files.");
	}

	public void loadArmors()
	{
		loadData(_armorFiles);
	}

	public void loadWeapons()
	{
		loadData(_weaponFiles);
	}

	public void loadItems()
	{
		loadData(_etcitemFiles);
	}

	public void loadData(List<File> files)
	{
		for(File f : files)
		{
			DocumentItem document = new DocumentItem(f);
			document.parse();
		}
	}
}