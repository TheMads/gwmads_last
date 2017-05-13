package ru.l2gw.gameserver.skills.skillclasses;

import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.List;

public class Default extends L2Skill
{
	public Default(StatsSet set)
	{
		super(set);
	}

	@Override
	public void useSkill(L2Character cha, List<L2Character> targets)
	{
		if(cha.isPlayer())
		{
			cha.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.skillclasses.Default.NotImplemented", cha).addNumber(getId()).addString("" + getSkillType()));
			cha.sendActionFailed();
		}
		else
			_log.info(cha + " try to use: " + this);
	}
}
