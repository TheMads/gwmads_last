package ru.l2gw.gameserver.skills;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.skills.conditions.*;
import ru.l2gw.gameserver.skills.conditions.ConditionGameTime.CheckGameTime;
import ru.l2gw.gameserver.skills.conditions.ConditionPlayerRiding.CheckPlayerRiding;
import ru.l2gw.gameserver.skills.conditions.ConditionPlayerState.CheckPlayerState;
import ru.l2gw.gameserver.skills.conditions.ConditionTargetDirection.TargetDirection;
import ru.l2gw.gameserver.skills.effects.EffectTemplate;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2Armor.ArmorType;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;
import ru.l2gw.gameserver.templates.StatsSet;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

abstract class DocumentBase
{
	static Log _log = LogFactory.getLog(DocumentBase.class.getName());

	private File file;
	protected HashMap<String, Object[]> tables;

	DocumentBase(File file)
	{
		this.file = file;
		tables = new HashMap<String, Object[]>();
	}

	public Document parse()
	{
		Document doc;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch(Exception e)
		{
			_log.error("Error loading file " + file, e);
			return null;
		}
		try
		{
			parseDocument(doc);
		}
		catch(Exception e)
		{
			_log.error("Error in file " + file, e);
			return null;
		}
		return doc;
	}

	protected abstract void parseDocument(Document doc);

	protected abstract Object getTableValue(String name);

	protected abstract Object getTableValue(String name, int idx);

	protected void resetTable()
	{
		tables = new HashMap<String, Object[]>();
	}

	protected void setTable(String name, Object[] table)
	{
		tables.put(name, table);
	}

	protected void parseTemplate(Node n, Object template)
	{
		Condition condition = null;
		n = n.getFirstChild();
		if(n == null)
			return;
		if("cond".equalsIgnoreCase(n.getNodeName()))
		{
			condition = parseCondition(n.getFirstChild(), template);
			Node msg = n.getAttributes().getNamedItem("msg");
			if(condition != null && msg != null)
				condition.setMessage(msg.getNodeValue());
		}
		for(; n != null; n = n.getNextSibling())
		{
			String nodeName = n.getNodeName();
			if("add".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Add", condition);
			else if("sub".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Sub", condition);
			else if("mul".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Mul", condition);
			else if("div".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Div", condition);
			else if("set".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Set", condition);
			else if("enchant".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "Enchant", condition);
			else if("trigger_skill_by_dmg".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "TriggerSkillByDmg", condition);
			else if("trigger_skill_by_attack".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "TriggerSkillByAttack", condition);
			else if("trigger_skill_by_skill".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "TriggerSkillBySkill", condition);
			else if("trigger_skill_by_avoid".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "TriggerSkillByAvoid", condition);
			else if("block_buff".equalsIgnoreCase(nodeName))
				attachFunc(n, template, "BlockBuffSlot", condition);
			else if("effect".equalsIgnoreCase(nodeName))
			{
				if(template instanceof EffectTemplate)
					throw new RuntimeException("Nested effects");
				attachEffect(n, template);
			}
			else if("skill".equalsIgnoreCase(nodeName))
				attachSkill(n, (L2Item) template, condition);
			else if(n.getNodeType() == Node.ELEMENT_NODE)
				_log.error("Unsupported stat function: " + nodeName + " in " + template);
		}
	}

	protected void attachSkill(Node n, L2Item template, @SuppressWarnings("unused") Condition condition)
	{
		NamedNodeMap attrs = n.getAttributes();
		int skillId = Short.valueOf(attrs.getNamedItem("id").getNodeValue());
		int skillLevel = Byte.valueOf(attrs.getNamedItem("level").getNodeValue());
		byte chance = parseNumber(attrs.getNamedItem("chance").getNodeValue()).byteValue();
		String action = attrs.getNamedItem("action").getNodeValue();
		L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
		if("crit".equalsIgnoreCase(action))
			template.attachSkillOnAction(skill, chance, true);
		else if("magic".equalsIgnoreCase(action))
			template.attachSkillOnAction(skill, chance, false);
		else if("add".equalsIgnoreCase(action))
			template.attachSkill(skill);
		else
			throw new NoSuchElementException("Unsupported action type for weapon-attached skill");
	}

	protected void attachFunc(Node n, Object template, String name, Condition attachCond)
	{
		Stats stat = n.getAttributes().getNamedItem("stat") == null ? null : Stats.valueOfXml(n.getAttributes().getNamedItem("stat").getNodeValue());
		String order = n.getAttributes().getNamedItem("order") == null ? "0x00" : n.getAttributes().getNamedItem("order").getNodeValue();
		int ord = parseNumber(order).intValue();
		double val = 0;
		if(n.getAttributes().getNamedItem("val") != null)
		{
			Number num = parseNumber(n.getAttributes().getNamedItem("val").getNodeValue());
			if(num == null)
				throw new NoSuchElementException("No number value for: " + template + " stat: " + stat + " order: " + order);
			val = num.doubleValue();
		}

		NamedNodeMap attrs = n.getAttributes();
		StatsSet set = null;

		for(int i = 0; i < attrs.getLength(); i++)
		{
			String attName = attrs.item(i).getNodeName();
			if("val".equalsIgnoreCase(attName) || "stat".equalsIgnoreCase(attName) || "order".equalsIgnoreCase(attName))
				continue;

			if(set == null)
				set = new StatsSet();

			String value = attrs.item(i).getNodeValue();

			if(value.charAt(0) == '#')
				set.set(attName, getTableValue(value));
			else
				set.set(attName, value);
		}

		Condition applyCond = parseCondition(n.getFirstChild(), template);
		FuncTemplate ft = new FuncTemplate(attachCond, applyCond, name, stat, ord, val);
		ft._attr = set;

		if(template instanceof L2Item)
			((L2Item) template).attachFunction(ft, true);
		else if(template instanceof L2Skill)
			((L2Skill) template).attach(ft);
		else if(template instanceof EffectTemplate)
			((EffectTemplate) template).attach(ft);
		else if(template instanceof EnchantOption)
			((EnchantOption) template).addFuncTemplate(ft);
	}

	protected void attachEffect(Node n, Object template)
	{
		NamedNodeMap attrs = n.getAttributes();
		StatsSet set = new StatsSet();

		for(int i = 0; i < attrs.getLength(); i++)
		{
			String name = attrs.item(i).getNodeName();
			String value = attrs.item(i).getNodeValue();

			if(value.charAt(0) == '#')
			{
				Object val = getTableValue(value);
				set.set(name, val);
			}
			else
				set.set(name, value);
		}

		EffectTemplate lt = new EffectTemplate(set, (L2Skill) template);

		parseTemplate(n, lt);

		int minLevel = Integer.MIN_VALUE;
		if(attrs.getNamedItem("minSkillLevel") != null)
			minLevel = parseNumber(attrs.getNamedItem("minSkillLevel").getNodeValue()).intValue();

		int maxLevel = Integer.MAX_VALUE;
		if(attrs.getNamedItem("maxSkillLevel") != null)
			maxLevel = parseNumber(attrs.getNamedItem("maxSkillLevel").getNodeValue()).intValue();

		if(attrs.getNamedItem("skillLevel") != null)
		{
			String[] lvl = attrs.getNamedItem("skillLevel").getNodeValue().split("-");
			minLevel = Integer.parseInt(lvl[0]);
			maxLevel = lvl.length == 2 ? Integer.parseInt(lvl[1]) : minLevel;
		}

		if(template instanceof L2Skill)
			if(((L2Skill) template).getLevel() >= minLevel && ((L2Skill) template).getLevel() <= maxLevel)
				((L2Skill) template).attach(lt);
	}

	protected Condition parseCondition(Node n, Object template)
	{
		while(n != null && n.getNodeType() != Node.ELEMENT_NODE)
			n = n.getNextSibling();
		if(n == null)
			return null;
		if("and".equalsIgnoreCase(n.getNodeName()))
			return parseLogicAnd(n, template);
		if("or".equalsIgnoreCase(n.getNodeName()))
			return parseLogicOr(n, template);
		if("not".equalsIgnoreCase(n.getNodeName()))
			return parseLogicNot(n, template);
		if("player".equalsIgnoreCase(n.getNodeName()))
			return parsePlayerCondition(n);
		if("target".equalsIgnoreCase(n.getNodeName()))
			return parseTargetCondition(n, template);
		if("skill".equalsIgnoreCase(n.getNodeName()))
			return parseSkillCondition(n);
		if("has".equalsIgnoreCase(n.getNodeName()))
			return parseHasCondition(n);
		if("using".equalsIgnoreCase(n.getNodeName()))
			return parseUsingCondition(n);
		if("game".equalsIgnoreCase(n.getNodeName()))
			return parseGameCondition(n);
		if("chance".equalsIgnoreCase(n.getNodeName()))
			return parseChanceCondition(n);

		return parseOtherCondition(n);
	}

	protected Condition parseLogicAnd(Node n, Object template)
	{
		ConditionLogicAnd cond = new ConditionLogicAnd();
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
			{
				Condition c = parseCondition(n, template);
				Node msg = n.getAttributes().getNamedItem("msg");
				Node msgId = n.getAttributes().getNamedItem("msgId");
				if(c != null && msg != null)
					c.setMessage(msg.getNodeValue());
				else if(c != null && msgId != null)
					c.setMessageId(parseNumber(msgId.getNodeValue()).intValue());
				cond.add(c);
			}
		if(cond._conditions == null || cond._conditions.length == 0)
			_log.error("Empty <and> condition in " + file);
		return cond;
	}

	protected Condition parseLogicOr(Node n, Object template)
	{
		ConditionLogicOr cond = new ConditionLogicOr();
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
				cond.add(parseCondition(n, template));
		if(cond._conditions == null || cond._conditions.length == 0)
			_log.error("Empty <or> condition in " + file);
		return cond;
	}

	protected Condition parseLogicNot(Node n, Object template)
	{
		for(n = n.getFirstChild(); n != null; n = n.getNextSibling())
			if(n.getNodeType() == Node.ELEMENT_NODE)
				return new ConditionLogicNot(parseCondition(n, template));
		_log.error("Empty <not> condition in " + file);
		return null;
	}

	protected Condition parsePlayerCondition(Node n)
	{
		int[] forces = new int[2];
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			String nodeName = a.getNodeName();
			if("race".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionPlayerRace(a.getNodeValue()));
			else if("minLevel".equalsIgnoreCase(nodeName))
			{
				int lvl = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerMinLevel(lvl));
			}
			else if("maxLevel".equalsIgnoreCase(nodeName))
			{
				int lvl = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerMaxLevel(lvl));
			}
			else if("maxPK".equalsIgnoreCase(nodeName))
			{
				int pk = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerMaxPK(pk));
			}
			else if("resting".equalsIgnoreCase(nodeName))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RESTING, val));
			}
			else if("moving".equalsIgnoreCase(nodeName))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.MOVING, val));
			}
			else if("running".equalsIgnoreCase(nodeName))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.RUNNING, val));
			}
			else if("standing".equalsIgnoreCase(nodeName))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.STANDING, val));
			}
			else if("siting".equalsIgnoreCase(nodeName))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.SITING_ONLY, val));
			}
			else if("flying".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.FLYING, val));
			}
			else if("olympiad".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionPlayerState(CheckPlayerState.OLYMPIAD, val));
			}
			else if("percentHP".equalsIgnoreCase(nodeName))
			{
				int percentHP = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentHp(percentHP));
			}
			else if("minHP".equalsIgnoreCase(nodeName))
			{
				int minHP = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerMinHp(minHP));
			}
			else if("percentMP".equalsIgnoreCase(nodeName))
			{
				int percentMP = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentMp(percentMP));
			}
			else if("percentCPmore".equalsIgnoreCase(nodeName))
			{
				int percentCPmore = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerPercentCpMore(percentCPmore));
			}
			else if("riding".equalsIgnoreCase(nodeName))
			{
				String riding = a.getNodeValue();
				if("strider".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.STRIDER));
				else if("wyvern".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.WYVERN));
				else if("none".equalsIgnoreCase(riding))
					cond = joinAnd(cond, new ConditionPlayerRiding(CheckPlayerRiding.NONE));
			}
			else if("battle_force".equalsIgnoreCase(a.getNodeName()))
				forces[0] = parseNumber(a.getNodeValue()).intValue();
			else if("spell_force".equalsIgnoreCase(a.getNodeName()))
				forces[1] = parseNumber(a.getNodeValue()).intValue();
			else if("attackerNpcId".equalsIgnoreCase(a.getNodeName()))
			{
				int npcId = parseNumber(a.getNodeValue()).intValue();
				cond = joinAnd(cond, new ConditionPlayerAttackerNpc(npcId));
			}
			else if("zoneType".equalsIgnoreCase(a.getNodeName()))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ";");
				try
				{
					ZoneType zt = ZoneType.valueOf(st.nextToken().trim().toLowerCase());
					boolean val = st.hasMoreTokens() ? Boolean.valueOf(st.nextToken().trim()) : false;
					cond = joinAnd(cond, new ConditionPlayerZoneType(zt, val));
				}
				catch(IllegalArgumentException e)
				{
					_log.info("ConditionPlayerZoneType: error! no zone type: " + a.getNodeValue());
				}
			}
			else if("hasEffect".equalsIgnoreCase(a.getNodeName()))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue().startsWith("#") ? getTableValue(a.getNodeValue()).toString() : a.getNodeValue(), "-");
				Integer id = parseNumber(st.nextToken().trim()).intValue();
				short level = parseNumber(st.nextToken().trim()).shortValue();
				cond = joinAnd(cond, new ConditionPlayerHasEffect(id, level));
			}
			else if("class".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerClass(a.getNodeValue().split(";")));
			else if("hasHideOut".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerHasHideOut(parseNumber(a.getNodeValue()).intValue()));
			else if("sex".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerSex(parseNumber(a.getNodeValue()).byteValue()));
			else if("PetSummoned".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerIsPetSummoned(Boolean.valueOf(a.getNodeValue())));
			else if("SubJobActive".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerSubJob(Boolean.valueOf(a.getNodeValue())));
			else if("freeInventorySlot".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionFreeInventorySlot(Integer.parseInt(a.getNodeValue())));
			else if("karma".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerKarma(Boolean.parseBoolean(a.getNodeValue())));
			else if("minPledgeRank".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerPledgeRank(L2Clan.PledgeRank.valueOf(a.getNodeValue().toUpperCase()).ordinal()));
			else if("academyGraduated".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerAcademyComplete(Boolean.valueOf(a.getNodeValue())));
			else if("pledgeType".equalsIgnoreCase(a.getNodeName()))
			{
				String type = a.getNodeValue().toLowerCase();
				int pledgeType = 0;
				if("academy".equals(type))
					pledgeType = L2Clan.SUBUNIT_ACADEMY;
				else if("none".equals(type))
					pledgeType = L2Clan.SUBUNIT_NONE;
				else if("royal1".equals(type))
					pledgeType = L2Clan.SUBUNIT_ROYAL1;
				else if("royal2".equals(type))
					pledgeType = L2Clan.SUBUNIT_ROYAL2;
				else if("knight1".equals(type))
					pledgeType = L2Clan.SUBUNIT_KNIGHT1;
				else if("knight2".equals(type))
					pledgeType = L2Clan.SUBUNIT_KNIGHT2;
				else if("knight3".equals(type))
					pledgeType = L2Clan.SUBUNIT_KNIGHT3;
				else if("knight4".equals(type))
					pledgeType = L2Clan.SUBUNIT_KNIGHT4;
				cond = joinAnd(cond, new ConditionPlayerPledgeType(pledgeType));
			}
			else if("instance".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerInstance(a.getNodeValue()));
			else if("flying_transform".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerFlyingTransform(Boolean.parseBoolean(a.getNodeValue())));
			else if("transformId".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionPlayerTransform(a.getNodeValue()));
		}

		if(forces[0] + forces[1] > 0)
			cond = joinAnd(cond, new ConditionForceBuff(forces));

		if(cond == null)
			_log.error("Unrecognized <player> condition in " + file);
		return cond;
	}

	protected Condition parseSkillCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			String nodeName = a.getNodeName();
			if("isMagic".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionSkillIsMagic(a.getNodeValue().equalsIgnoreCase("true")));
		}
		if(cond == null)
			_log.error("Unrecognized <skill> condition in " + file);
		return cond;
	}

	protected Condition parseTargetCondition(Node n, @SuppressWarnings("unused") Object template)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			String nodeName = a.getNodeName();
			if("aggro".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetAggro(Boolean.valueOf(a.getNodeValue())));
			else if("pvp".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetPlayable(Boolean.valueOf(a.getNodeValue())));
			else if("pet".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetPet(Boolean.valueOf(a.getNodeValue())));
			else if("playerOnly".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetPlayer(Boolean.valueOf(a.getNodeValue())));
			else if("self".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetSelf(Boolean.valueOf(a.getNodeValue())));
			else if("mob".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetMob(Boolean.valueOf(a.getNodeValue())));
			else if("race".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetRace(a.getNodeValue()));
			else if("playerRace".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetPlayerRace(a.getNodeValue()));
			else if("castledoor".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetCastleDoor(Boolean.valueOf(a.getNodeValue())));
			else if("myparty".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetMyParty(Boolean.valueOf(a.getNodeValue())));
			else if("unlockable".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetUnlockable(Boolean.valueOf(a.getNodeValue())));
			else if("ballista".equalsIgnoreCase(nodeName))
				cond = joinAnd(cond, new ConditionTargetBallista(Boolean.valueOf(a.getNodeValue())));
			else if("direction".equalsIgnoreCase(nodeName))
			{
				String val = a.getNodeValue();
				if("behind".equalsIgnoreCase(val))
					cond = joinAnd(cond, new ConditionTargetDirection(TargetDirection.BEHIND));
				if("front".equalsIgnoreCase(val))
					cond = joinAnd(cond, new ConditionTargetDirection(TargetDirection.FRONT));
				if("side".equalsIgnoreCase(val))
					cond = joinAnd(cond, new ConditionTargetDirection(TargetDirection.FRONT));
			}
			else if("hasBuffId".equalsIgnoreCase(nodeName))
			{
				String[] values = a.getNodeValue().split(";");
				int id;
				HashMap<Integer, Integer> skills = new HashMap<Integer, Integer>();
				boolean val = Boolean.valueOf(values[values.length - 1]);
				values[values.length - 1] = null;

				for(String buff : values)
				{
					if(buff == null)
						continue;
					int level = -1;
					StringTokenizer st = new StringTokenizer(buff, ",");
					id = Integer.valueOf(st.nextToken()).intValue();
					if(st.hasMoreTokens())
						level = Integer.valueOf(st.nextToken()).intValue();
					skills.put(id, level);
				}
				cond = joinAnd(cond, new ConditionTargetHasBuffId(skills, val));
			}
			else if("hasSkill".equalsIgnoreCase(nodeName))
			{
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), "-");
				int id, lvl;
				lvl = -1;
				id = Integer.valueOf(st.nextToken());
				if(st.hasMoreTokens())
					lvl = Integer.valueOf(st.nextToken());

				cond = joinAnd(cond, new ConditionTargetHasSkill(id, lvl));
			}
			else if("hasEffectType".equalsIgnoreCase(nodeName))
			{
				String val = a.getNodeValue();
				cond = joinAnd(cond, new ConditionTargetHasEffectType(val));
			}
			else if("distanceToTarget".equalsIgnoreCase(nodeName))
			{
				Integer distance = parseNumber(a.getNodeValue().trim()).intValue();
				cond = joinAnd(cond, new ConditionDistanceToTarget(distance));
			}
			else if("minDistanceToTarget".equalsIgnoreCase(nodeName))
			{
				Integer distance = parseNumber(a.getNodeValue().trim()).intValue();
				cond = joinAnd(cond, new ConditionMinDistanceToTarget(distance));
			}
			else if("class_id_restriction".equalsIgnoreCase(a.getNodeName()) || "classId".equalsIgnoreCase(a.getNodeName()))
			{
				FastList<Short> array = new FastList<Short>();
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while(st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					array.add(parseNumber(item).shortValue());
				}
				cond = joinAnd(cond, new ConditionTargetClassId(array));
			}
			else if("energyMax".equalsIgnoreCase(a.getNodeName()))
			{
				Integer energyMax = parseNumber(a.getNodeValue().trim()).intValue();
				cond = joinAnd(cond, new ConditionTargetEnergyMax(Integer.valueOf(energyMax)));
			}
			else if("npcId".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionTargetNpcId(a.getNodeValue().trim().split(";")));
		}
		if(cond == null)
			_log.error("Unrecognized <target> condition in " + file);
		return cond;
	}

	protected Condition parseUsingCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			String nodeName = a.getNodeName();
			String nodeValue = a.getNodeValue();
			if("kind".equalsIgnoreCase(nodeName))
			{
				int mask = 0;
				StringTokenizer st = new StringTokenizer(nodeValue, ",");
				while(st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					for(WeaponType wt : WeaponType.values())
						if(wt.toString().equalsIgnoreCase(item))
						{
							mask |= wt.mask();
							break;
						}
				}
				cond = joinAnd(cond, new ConditionUsingItemType(mask));
			}
			else if("armor".equalsIgnoreCase(nodeName))
			{
				ArmorType armor = ArmorType.valueOf(nodeValue.toUpperCase());
				cond = joinAnd(cond, new ConditionUsingArmor(armor));
			}
			else if("skill".equalsIgnoreCase(nodeName))
			{
				int id = Integer.parseInt(nodeValue);
				cond = joinAnd(cond, new ConditionUsingSkill(id));
			}
			else if("slotitem".equalsIgnoreCase(nodeName))
			{
				StringTokenizer st = new StringTokenizer(nodeValue, ";");
				int id = Integer.parseInt(st.nextToken().trim());
				short slot = Short.parseShort(st.nextToken().trim());
				int enchant = 0;
				if(st.hasMoreTokens())
					enchant = Integer.parseInt(st.nextToken().trim());
				cond = joinAnd(cond, new ConditionSlotItemId(slot, id, enchant));
			}
			else if("direction".equalsIgnoreCase(nodeName))
			{
				TargetDirection Direction = TargetDirection.valueOf(nodeValue.toUpperCase());
				cond = joinAnd(cond, new ConditionTargetDirection(Direction));
			}
		}
		if(cond == null)
			_log.error("Unrecognized <using> condition in " + file);
		return cond;
	}

	protected Condition parseHasCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			String nodeName = a.getNodeName();
			String nodeValue = a.getNodeValue();
			if("skill".equalsIgnoreCase(nodeName))
			{
				StringTokenizer st = new StringTokenizer(nodeValue, ";");
				Integer id = parseNumber(st.nextToken().trim()).intValue();
				short level = parseNumber(st.nextToken().trim()).shortValue();
				cond = joinAnd(cond, new ConditionHasSkill(id, level));
			}
		}
		if(cond == null)
			_log.error("Unrecognized <has> condition in " + file);
		return cond;
	}

	protected Condition parseGameCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if("night".equalsIgnoreCase(a.getNodeName()))
			{
				boolean val = Boolean.valueOf(a.getNodeValue());
				cond = joinAnd(cond, new ConditionGameTime(CheckGameTime.NIGHT, val));
			}
		}
		if(cond == null)
			_log.error("Unrecognized <game> condition in " + file);
		return cond;
	}

	protected Condition parseChanceCondition(Node n)
	{
		Condition cond = null;
		NamedNodeMap attrs = n.getAttributes();
		for(int i = 0; i < attrs.getLength(); i++)
		{
			Node a = attrs.item(i);
			if("onSkillUse".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionChanceOnSkillUse(parseNumber(a.getNodeValue()).intValue()));
			else if("minDamage".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionChanceMinDamage(parseNumber(a.getNodeValue()).intValue()));
			else if("chanceMod".equalsIgnoreCase(a.getNodeName()))
				cond = joinAnd(cond, new ConditionChanceOnDamage(parseNumber(a.getNodeValue()).intValue()));
			else if("kind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while(st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					for(WeaponType wt : WeaponType.values())
						if(wt.toString().equalsIgnoreCase(item))
						{
							mask |= wt.mask();
							break;
						}
				}
				cond = joinAnd(cond, new ConditionChanceUsingKind(mask));
			}
			else if("notkind".equalsIgnoreCase(a.getNodeName()))
			{
				int mask = 0;
				StringTokenizer st = new StringTokenizer(a.getNodeValue(), ",");
				while(st.hasMoreTokens())
				{
					String item = st.nextToken().trim();
					for(WeaponType wt : WeaponType.values())
						if(wt.toString().equalsIgnoreCase(item))
						{
							mask |= wt.mask();
							break;
						}
				}
				cond = joinAnd(cond, new ConditionChanceUsingNotKind(mask));
			}

		}
		if(cond == null)
			_log.error("Unrecognized <chance> condition in " + file);
		return cond;
	}

	protected Condition parseOtherCondition(Node n)
	{
		Condition cond = null;

		if("canSummon".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanSummon());
		else if("canSummonPet".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanSummonPet());
		else if("canSummonSiegeGolem".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanSummonSiegeGolem());
		else if("CanSummonFriend".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanSummonFriend());
		else if("CanTransform".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanTransform());
		else if("CanFlyingTransform".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanFlyingTransform());
		else if("CanLanding".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanLanding());
		else if("CanRide".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanRide());
		else if("CanResurrect".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanResurrect());
		else if("consumeBody".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionConsumeBody());
		else if("isClanLeader".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionIsClanLeader());
		else if("isHero".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionIsHero());
		else if("agathion".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionAgathion());
		else if("holythingPossess".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionHolythingPossess());
		else if("flagpolePossess".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionFlagpolePossess());
		else if("WardPossess".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionWardPossess());
		else if("BuildCamp".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionBuildCamp());
		else if("BuildOutpost".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionBuildOutpost());
		else if("HasOutpost".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionHasOutpost());
		else if("SiegeRegistered".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionSiegeRegistered());
		else if("TerritoryWarOnly".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionTerritoryWarOnly());
		else if("CanSweep".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanSweep());
		else if("WeaponConvert".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionWeaponConvert());
		else if("FishingCast".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionFishingCast());
		else if("FishingPumping".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionFishingPumping());
		else if("FishingReeling".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionFishingReeling());
		else if("OpGround".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpGround());
		else if("OpClanGate".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpClanGate());
		else if("OpHome".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpHome(n.getAttributes().getNamedItem("type").getNodeValue()));
		else if("OpCloak".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpCloak());
		else if("OpTerritory".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpTerritory(n.getAttributes().getNamedItem("points").getNodeValue()));
		else if("CanEscape".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionCanEscape());
		else if("ClanAirship".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionClanAirship());
		else if("uc".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionUseCategory(n.getAttributes().getNamedItem("category").getNodeValue()));
		else if("OpExistNpc".equalsIgnoreCase(n.getNodeName()))
			cond = joinAnd(cond, new ConditionOpExistNpc(n.getAttributes().getNamedItem("type").getNodeValue()));

		return cond;
	}

	protected void parseTable(Node n)
	{
		NamedNodeMap attrs = n.getAttributes();
		String name = attrs.getNamedItem("name").getNodeValue();
		if(name.charAt(0) != '#')
			throw new IllegalArgumentException("Table name must start with #");
		StringTokenizer data;
		try
		{
			data = new StringTokenizer(n.getFirstChild().getNodeValue());
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("Table: " + name + " has no values");
		}
		GArray<String> array = new GArray<String>();
		while(data.hasMoreTokens())
			array.add(data.nextToken());
		Object[] res = array.toArray(new Object[array.size()]);
		setTable(name, res);
	}

	protected void parseBeanSet(Node n, StatsSet set, Integer level) throws Exception
	{
		String name = n.getAttributes().getNamedItem("name").getNodeValue().trim();
		String value = n.getAttributes().getNamedItem("val").getNodeValue().trim();
		char ch = value.length() == 0 ? ' ' : value.charAt(0);
		if(ch == '#')
		{
			Object tableVal = getTableValue(value, level);
			Number parsedVal = parseNumber(tableVal.toString());
			set.set(name, parsedVal == null ? tableVal : String.valueOf(parsedVal));
		}
		else if((Character.isDigit(ch) || ch == '-') && !value.contains(" ") && !value.contains(";"))
			set.set(name, String.valueOf(parseNumber(value)));
		else
			set.set(name, value);
	}

	/**
	 * Разбирает параметр Value как число, приводя его к Number, либо возвращает значение из таблицы если строка начинается с #
	 */
	protected Number parseNumber(String value)
	{
		if(value.charAt(0) == '#')
			value = getTableValue(value).toString();
		try
		{
			if(value.indexOf('.') == -1)
			{
				int radix = 10;
				if(value.length() > 2 && value.substring(0, 2).equalsIgnoreCase("0x"))
				{
					value = value.substring(2);
					radix = 16;
				}
				return Integer.valueOf(value, radix);
			}
			return Double.valueOf(value);
		}
		catch(NumberFormatException e)
		{
			return null;
		}
	}

	protected Condition joinAnd(Condition cond, Condition c)
	{
		if(cond == null)
			return c;
		if(cond instanceof ConditionLogicAnd)
		{
			((ConditionLogicAnd) cond).add(c);
			return cond;
		}
		ConditionLogicAnd and = new ConditionLogicAnd();
		and.add(cond);
		and.add(c);
		return and;
	}
}