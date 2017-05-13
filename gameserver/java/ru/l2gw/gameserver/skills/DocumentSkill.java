package ru.l2gw.gameserver.skills;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Skill.SkillType;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.templates.StatsSet;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

final class DocumentSkill extends DocumentBase
{

	public class Skill
	{
		public int id;
		public String name;
		public StatsSet[] sets;
		public int currentLevel;
		public ArrayList<L2Skill> skills = new ArrayList<L2Skill>();
		public ArrayList<L2Skill> currentSkills = new ArrayList<L2Skill>();
	}

	private Skill currentSkill;
	private List<L2Skill> skillsInFile = new LinkedList<L2Skill>();

	DocumentSkill(File file)
	{
		super(file);
	}

	private void setCurrentSkill(Skill skill)
	{
		currentSkill = skill;
	}

	protected List<L2Skill> getSkills()
	{
		return skillsInFile;
	}

	@Override
	protected Object getTableValue(String name)
	{
		try
		{
			return tables.get(name)[currentSkill.currentLevel];
		}
		catch(RuntimeException e)
		{
			_log.warn("error in table of skill Id: " + currentSkill.id + " level: " + currentSkill.currentLevel + " table: " + name, e);
			return 0;
		}
	}

	@Override
	protected Object getTableValue(String name, int idx)
	{
		try
		{
			return tables.get(name)[idx - 1];
		}
		catch(RuntimeException e)
		{
			_log.warn("wrong level count in skill Id: " + currentSkill.id + " level: " + idx + " table: " + name , e);
			return 0;
		}
	}

	@Override
	protected void parseDocument(Document doc)
	{
		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("skill".equalsIgnoreCase(d.getNodeName()))
					{
						setCurrentSkill(new Skill());
						parseSkill(d);
						skillsInFile.addAll(currentSkill.skills);
						resetTable();
					}
			}
			else if("skill".equalsIgnoreCase(n.getNodeName()))
			{
				setCurrentSkill(new Skill());
				parseSkill(n);
				skillsInFile.addAll(currentSkill.skills);
			}
	}

	protected void parseSkill(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		int skillId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
		String skillName = attrs.getNamedItem("name").getNodeValue();
		String levels = attrs.getNamedItem("levels").getNodeValue();
		int lastLvl = Integer.parseInt(levels);

		currentSkill.id = skillId;
		currentSkill.name = skillName;
		currentSkill.sets = new StatsSet[lastLvl];

		for(int i = 0; i < lastLvl; i++)
		{
			currentSkill.sets[i] = new StatsSet();
			currentSkill.sets[i].set("skill_id", currentSkill.id);
			currentSkill.sets[i].set("level", i + 1);
			currentSkill.sets[i].set("name", currentSkill.name);
		}

		if(currentSkill.sets.length != lastLvl)
			throw new RuntimeException("Skill id=" + skillId + " number of levels missmatch, " + lastLvl + " levels expected");

		Node first = n.getFirstChild();
		for(n = first; n != null; n = n.getNextSibling())
			if("table".equalsIgnoreCase(n.getNodeName()))
			try
			{
				parseTable(n);
			}
			catch(IllegalArgumentException ia)
			{
				throw new RuntimeException("Skill id=" + skillId + " table parse error: " + ia.getMessage());
			}

		for(int i = 1; i <= lastLvl; i++)
			for(n = first; n != null; n = n.getNextSibling())
				if("set".equalsIgnoreCase(n.getNodeName()))
					try
					{
						parseBeanSet(n, currentSkill.sets[i - 1], i);
					}
					catch(Exception e)
					{
						throw new RuntimeException("Skill id=" + skillId + " skill level=" + i + " set parse error: " + e.getMessage());
					}

		makeSkills();
		for(int i = 0; i < lastLvl; i++)
		{
			currentSkill.currentLevel = i;
			for(n = first; n != null; n = n.getNextSibling())
			{
				if("cond".equalsIgnoreCase(n.getNodeName()))
				{
					Condition condition = parseCondition(n.getFirstChild(), currentSkill.currentSkills.get(i));
					Node msg = n.getAttributes().getNamedItem("msg");
					Node msgId = n.getAttributes().getNamedItem("msgId");
					if(condition != null && msg != null)
						condition.setMessage(msg.getNodeValue());
					else if(condition != null && msgId != null)
						condition.setMessageId(parseNumber(msgId.getNodeValue()).intValue());
					
					currentSkill.currentSkills.get(i).attach(condition);
				}
				if("for".equalsIgnoreCase(n.getNodeName()))
					parseTemplate(n, currentSkill.currentSkills.get(i));
			}
		}
		currentSkill.skills.addAll(currentSkill.currentSkills);
	}

	private void makeSkills()
	{
		currentSkill.currentSkills = new ArrayList<L2Skill>(currentSkill.sets.length);
		//System.out.println(sets.length);
		for(int i = 0; i < currentSkill.sets.length; i++)
			currentSkill.currentSkills.add(i, SkillType.valueOf(currentSkill.sets[i].getString("skillType", "SKILL")).makeSkill(currentSkill.sets[i]));
	}
}