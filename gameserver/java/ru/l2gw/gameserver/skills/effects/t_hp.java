package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.ExRegenMax;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author: rage
 * @date: 17.07.2010 21:23:31
 */
public class t_hp extends t_effect
{
	public t_hp(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		if(getEffected().isPlayer() && getSkill().isPotion())
			getEffected().sendPacket(new ExRegenMax((int)(_effect.getAbnormalTime() / 1000), _template._ticks, calc()));
		startActionTask(3000);
	}

	@Override
	public boolean onActionTime()
	{
		L2Character effected = getEffected();
		L2Character effector = getEffector();
		
		if(effected == null || effector == null || effected.isDead())
			return false;

		double hp = calcTickVal();

		if(hp > 0) // heal
		{
			if(effected.isStatActive(Stats.BLOCK_HEAL))
				return true;

			int hpLimit = (int)effected.calcStat(Stats.HP_LIMIT, effected.getMaxHp(), null, null);
			int newHp = (int)hp;

			if(effected.getCurrentHp() +  newHp > hpLimit)
				newHp = (int)(hpLimit - effected.getCurrentHp());

			if(newHp < 0)
				newHp = 0;

			effected.setCurrentHp(effected.getCurrentHp() + newHp);
			return true;
		}
		else // dot
		{
			if(effected.isStatActive(Stats.BLOCK_HP) || effected.isInvul())
				return true;

			boolean pvp = effected.isPlayable() && (effector.isPlayable() || effector.isCubic()) && effected != effector;
			double damage = -hp;
			if(pvp)
			{
				damage = effected.getCurrentCp() - damage;
				hp = damage < 0 ? -damage : 0;

				if(damage < 0)
					damage = 0;

				effected.setCurrentCp(damage);
				L2Player effectorPlayer = effector.getPlayer();
				L2Player effectedPlayer = effected.getPlayer();
				if(effectorPlayer != null && effectedPlayer != null && effectorPlayer.isOlympiadStart() && effectedPlayer.isOlympiadStart() && effectorPlayer.getOlympiadGameId() == effectedPlayer.getOlympiadGameId())
					Olympiad.addReceivedDamage(effectedPlayer.getOlympiadGameId(), effectedPlayer.getObjectId(), (int) -calcTickVal());
			}
			else
				hp = damage;

			if(hp > 0)
			{
				if(effected.isNpc())
					((L2NpcInstance) effected).addDamage(effector, (int) hp);

				hp = Math.max(effected.getCurrentHp() - hp, 1);
				effected.setCurrentHp(hp);
			}

			effected.stopEffects("sleep", "meditation", "hide");
		}
		return true;
	}
}
