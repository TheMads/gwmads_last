package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 13.07.2010 15:09:55
 */
public class i_spoil extends i_effect
{
	public i_spoil(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(!isSuccess(env))
				continue;

			L2MonsterInstance monster = (L2MonsterInstance) env.target;
			if(monster.isSpoiled())
				continue;

			monster.setSpoiled(true);
		}
	}

	private boolean isSuccess(Env env)
	{
		if(!env.target.isMonster())
			return false;

		if(((L2MonsterInstance) env.target).isSpoiled())
		{
			env.character.sendPacket(Msg.IT_HAS_ALREADY_BEEN_SPOILED);
			return false;
		}

		boolean success;
		if(!Config.ALT_SPOIL_FORMULA)
		{
			int monsterLevel = env.target.getLevel();
			int modifier = Math.abs(monsterLevel - env.character.getLevel());
			double rateOfSpoil = Config.BASE_SPOIL_RATE;

			if(modifier > 8)
				rateOfSpoil = rateOfSpoil - rateOfSpoil * (modifier - 8) * 9 / 100;

			rateOfSpoil = rateOfSpoil * getSkill().getMagicLevel() / monsterLevel;

			if(rateOfSpoil < Config.MINIMUM_SPOIL_RATE)
				rateOfSpoil = Config.MINIMUM_SPOIL_RATE;
			else if(rateOfSpoil > 99.)
				rateOfSpoil = 99.;

			if(Config.SKILLS_SHOW_CHANCE)
				env.character.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.skillclasses.Spoil.Chance", env.character).addNumber((int) rateOfSpoil));
			success = Rnd.chance((int) rateOfSpoil);
		}
		else
			success = env.success;

		if(success)
			env.character.sendPacket(new SystemMessage(SystemMessage.THE_SPOIL_CONDITION_HAS_BEEN_ACTIVATED));
		else
			env.character.sendPacket(new SystemMessage(SystemMessage.S1_HAS_FAILED).addSkillName(getSkill().getDisplayId()));

		return success;
	}
}
