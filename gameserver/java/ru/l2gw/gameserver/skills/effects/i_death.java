package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 30.12.2009 9:42:13
 */
public class i_death extends i_effect
{
	private final double _lethal1, _lethal2;
	public i_death(EffectTemplate template)
	{
		super(template);
		_lethal1 = template._attrs.getDouble("lethal1", 0);
		_lethal2 = template._attrs.getDouble("lethal2", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
			if(!env.target.isDead() && !env.target.isRaid() && !env.target.isLethalImmune() && !env.target.isStatActive(Stats.BLOCK_HP) && (env.target.isMonster() || (env.target instanceof L2Playable && cha.getLevel() >= env.target.getLevel() - 3)))
			{
				// 2nd lethal effect activate (cp,hp to 1 or if env.target is npc then hp to 1)
				double lethal1 = _lethal1 * Formulas.getC4LevelMod(cha.getLevel(), env.target.getLevel());
				double lethal2 = _lethal2 * Formulas.getC4LevelMod(cha.getLevel(), env.target.getLevel());

				if(Config.SKILLS_SHOW_CHANCE && cha.isPlayer())
				{
					cha.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.Formulas.Chance", cha).addString("Lethal1").addNumber((int) lethal1));
					cha.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.Formulas.Chance", cha).addString("Lethal2").addNumber((int) lethal2));
				}

				if(lethal2 > 0 && Rnd.chance(Formulas.calcLethal(cha, env.target, lethal2)))
				{
					if(env.target.isMonster() || env.target instanceof L2Summon)
						env.target.reduceHp(env.target.getCurrentHp() - 1, cha, false, false);
					else if(env.target.isPlayer()) // If is a active player set his HP and CP to 1
					{
						L2Player player = env.target.getPlayer();
						if(!player.isInvul())
						{
							player.setCurrentHp(1);
							player.setCurrentCp(1);
						}
					}
					cha.sendPacket(Msg.LETHAL_STRIKE);
					cha.sendPacket(Msg.YOUR_LETHAL_STRIKE_WAS_SUCCESSFUL);
				}
				else if(lethal1 > 0 && Rnd.chance(Formulas.calcLethal(cha, env.target, lethal1)))
				{
					cha.sendPacket(Msg.HALF_KILL);

					if(env.target.isPlayer())
					{
						L2Player player = env.target.getPlayer();
						if(!player.isInvul())
							player.setCurrentCp(1); // Set CP to 1

						player.sendPacket(Msg.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL);
					}
					else if(env.target.isMonster() || env.target instanceof L2Summon) // If is a monster remove first damage and after 50% of current hp
						env.target.reduceHp(env.target.getCurrentHp() / 2, cha, false, false);
				}
			}
	}
}
