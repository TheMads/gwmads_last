package ru.l2gw.gameserver.skills.funcs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.L2Augmentation;
import ru.l2gw.gameserver.serverpackets.ExMagicSkillUseInAirShip;
import ru.l2gw.gameserver.serverpackets.MagicSkillLaunched;
import ru.l2gw.gameserver.serverpackets.MagicSkillUse;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.List;

/**
 * @author: rage
 * @date: 26.10.11 17:03
 */
public class FuncTriggerSkill extends Func
{
	protected static final Log _log = LogFactory.getLog(FuncTriggerSkill.class);
	protected L2Character owner;
	protected StatsSet attrs;
	protected L2Skill triggerSkill;
	protected double chance;
	protected TriggerTarget target;
	protected boolean debug = false;

	public enum TriggerTarget
	{
		enemy_all,
		pc,
		mob
	}

	public FuncTriggerSkill(Stats stat, int order, Object owner, double value)
	{
		super(stat, order, owner, value);
	}

	@Override
	public void setAttributes(StatsSet set)
	{
		attrs = set;
		triggerSkill = attrs.getSkill("skill");
		chance = attrs.getDouble("chance");
	}

	@Override
	public void setCharacter(L2Character cha)
	{
		owner = cha;
	}

	@Override
	public void calc(Env env)
	{
		if(Rnd.chance(chance))
		{
			L2Skill trigger = triggerSkill;
			if(trigger != null && !env.target.isAlikeDead())
			{
				if(trigger.getIncreaseLevel() > 0 && env.target.getEffectBySkillId(trigger.getId()) != null)
				{
					L2Effect ef = env.target.getEffectBySkillId(trigger.getId());
					if(ef != null && ef.getSkillLevel() <= trigger.getIncreaseLevel())
						trigger = SkillTable.getInstance().getInfo(trigger.getId(), ef.getSkillLevel() == trigger.getIncreaseLevel() ? trigger.getIncreaseLevel() : ef.getSkillLevel() + 1);
				}

				if(trigger == null || env.character.isSkillDisabled(trigger.getId()))
					return;

				if(trigger.getCastRange() > 0 && !env.character.isInRange(env.target, trigger.getCastRange() + (int) env.character.getColRadius()) || trigger.getPreCondition() != null && !trigger.getPreCondition().test(env))
					return;

				if(debug)
					_log.info(getClass().getSimpleName() + " " + trigger.getId() + " --> " + env.target.getName());

				env.target = trigger.getAimingTarget(env.character, env.target);

				if(trigger.checkTarget(env.character, env.target, false, true) != null)
				{
					if(debug)
						_log.info(getClass().getSimpleName() + " " + trigger.getId() + " --> " + env.target.getName() + " check target fail!");
					return;
				}

				List<L2Character> targets = trigger.getTargets(env.character, env.target, false);

				try
				{
					if(_funcOwner instanceof L2Augmentation && env.character != env.target)
						env.character.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_ACTIVATED).addSkillName(trigger.getId()));

					trigger.useSkill(env.character, targets);
					long reuse = Formulas.calcSkillReuseDelay(env.character, trigger);
					if(reuse > 0)
					{
						env.character.disableSkill(trigger.getId(), reuse);
					}

					if(targets.size() > 1)
						for(L2Character cha : targets)
						{
							cha.broadcastPacket(cha.isInAirShip() ? new ExMagicSkillUseInAirShip(cha, cha, trigger.getId(), trigger.getLevel(), 0, 0, trigger.isBuff()) : new MagicSkillUse(cha, cha, trigger.getId(), trigger.getLevel(), 0, 0, trigger.isBuff()));
							cha.broadcastPacket(new MagicSkillLaunched(cha.getObjectId(), trigger.getId(), trigger.getLevel(), cha, trigger.isBuff()));
							if(cha.isMoving)
								cha.broadcastMove();
						}
					else
					{
						env.character.broadcastPacket(env.character.isInAirShip() ? new ExMagicSkillUseInAirShip(env.character, env.target, trigger.getId(), trigger.getLevel(), 0, 0, trigger.isBuff()) : new MagicSkillUse(env.character, env.target, trigger.getId(), trigger.getLevel(), 0, 0, trigger.isBuff()));
						env.character.broadcastPacket(new MagicSkillLaunched(env.character.getObjectId(), trigger.getId(), trigger.getLevel(), env.target, trigger.isBuff()));
						if(env.character.isMoving)
							env.character.broadcastMove();
					}
				}
				catch(Exception e)
				{
				}
			}
		}
	}
}
