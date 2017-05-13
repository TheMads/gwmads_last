package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.instances.L2CubicInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 24.09.2009 14:18:16
 */
public class i_target_me extends i_effect
{
	public i_target_me(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(cha instanceof L2CubicInstance)
			cha = cha.getPlayer();

		for(Env env : targets)
			if(!env.target.isRaid() && (env.target.isMonster() || env.target instanceof L2Playable) && !env.target.isStatActive(Stats.BLOCK_DEBUFF))
			{
				boolean success = false;
				if(getSkill().getActivateRate() > 0 && Rnd.chance(Math.min(getSkill().getActivateRate(), getSkill().getActivateRate() * Formulas.getLevelMod(getSkill().getMagicLevel(), env.target.getLevel()))))
					success = true;
				else if(getSkill().getActivateRate() < 1)
					success = true;

				if(success)
				{
					if(env.target.setTarget(cha))
					{
						if(env.target.isPlayer())
						{
							if(calc() > 0)
								env.target.getPlayer().blockTargetTime = System.currentTimeMillis() + (long) (calc() * 1000);
							env.target.sendPacket(new MyTargetSelected(cha.getObjectId(), 0));

							if(cha.isNpc() && (cha.isAttackable(env.target, false, false) || ((L2NpcInstance) cha).isShowHp()))
							{
								StatusUpdate su = new StatusUpdate(cha.getObjectId());
								su.addAttribute(StatusUpdate.CUR_HP, (int) cha.getCurrentHp());
								su.addAttribute(StatusUpdate.MAX_HP, cha.getMaxHp());
								env.target.sendPacket(su);
							}
						}
						env.target.getAI().setAttackTarget(cha);
					}
				}
			}
	}
}
