package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.commons.arrays.GArray;

import java.util.HashMap;

/**
 * @author rage
 * @date 05.08.2010 15:05:10
 */
public class i_give_skill extends i_effect
{
	private final HashMap<Integer, Integer> _skills;

	public i_give_skill(EffectTemplate template)
	{
		super(template);
		String[] skills = template._attrs.getString("options", "").split(";");
		_skills = new HashMap<Integer, Integer>(skills.length);
		for(String skill : skills)
			if(!skill.isEmpty())
				_skills.put(Integer.parseInt(skill.split("-")[0]), Integer.parseInt(skill.split("-")[1]));
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(env.target.isPlayer())
			{
				L2Player target = (L2Player) env.target;
				for(Integer skillId : _skills.keySet())
				{
					target.addSkill(SkillTable.getInstance().getInfo(skillId, _skills.get(skillId)), true);
					target.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addSkillName(skillId));
				}
				target.sendPacket(new SkillList(target));
			}
	}
}
