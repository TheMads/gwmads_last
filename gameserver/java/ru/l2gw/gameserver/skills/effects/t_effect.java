package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.listeners.MethodInvokeListener;
import ru.l2gw.extensions.listeners.events.MethodEvent;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2SummonInstance;
import ru.l2gw.gameserver.skills.Calculator;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.skills.AbnormalEffect;

import java.util.concurrent.ScheduledFuture;

/**
 * @author rage
 * @date 09.10.2009 16:35:47
 */
public class t_effect
{
	private ActionDispelListener _actionListener;
	private SelfActionDispelListener _selfActionListener;
	protected L2Effect _effect;
	protected EffectTemplate _template;
	private ScheduledFuture<EffectActionTask> _effectActionTask;

	public t_effect(L2Effect effect, EffectTemplate template)
	{
		_effect = effect;
		_template = template;
		_effect.attachEffect(this);
	}

	public boolean onActionTime()
	{
		return false;
	}

	public boolean isSuccess(boolean skillSuccess)
	{
		if(getSkill().isDebuff() && (getEffected().isInvul() || !getEffected().isMonster() && getEffected().isNpc() && ((L2NpcInstance) getEffected()).getTemplate().undying > 0))
			return false;

		skillSuccess = !getEffected().isStatActive(getSkill().isDebuff() ? Stats.BLOCK_DEBUFF : Stats.BLOCK_BUFF) && skillSuccess;

		if(skillSuccess)
		{
			Calculator calc = getEffected().getCalculator(Stats.BLOCK_BUFF_SLOT);
			if(calc != null && calc.size() > 0)
			{
				Env env = new Env(getEffector(), getEffected(), getSkill());
				for(Func func : calc.getFunctions())
				{
					func.calc(env);
					if(!env.success)
					{
						skillSuccess = false;
						break;
					}
				}
			}
		}

		if(skillSuccess && _template._activateRate > 0)
		{
			boolean chance = Rnd.chance(_template._activateRate);
			if(Config.SKILLS_SHOW_CHANCE && getEffector().isPlayer())
				getEffector().sendMessage(getClass().getSimpleName() + ": " + _template._activateRate + "% " + (chance ? "success" : "fail"));

			return chance;
		}

		return skillSuccess;
	}

	@SuppressWarnings("fallthrough")
	public void onStart()
	{
		L2Character effected = getEffected();
		if(getSkill().getAbnormalVe().mask != 0)
		{
			effected.startAbnormalEffect(getSkill().getAbnormalVe());
			switch(getSkill().getAbnormalVe())
			{
				case sleep:
				case stun:
				case av2_stun:
				case danceStun:
				case paralyze:
				case stone:
				case fear:
				case blue_mark:
					effected.abortAttack();
					effected.breakCast(true, true);
				case root:
				case av2_root:
					effected.stopMove();
					break;
				case silence:
					L2Skill castingSkill = effected.getCastingSkill();
					if(effected.isCastingNow() && castingSkill != null)
						for(FuncTemplate f : _template._funcTemplates)
							if(f._stat == Stats.BLOCK_SPELL && castingSkill.isMagic())
							{
								effected.breakCast(true, true);
								break;
							}
							else if(f._stat == Stats.BLOCK_PHYS_SKILLS && castingSkill.isPhysic())
							{
								effected.breakCast(true, true);
								break;
							}
					break;
			}
		}
		if(_template._attrs.getBool("dispelOnAction", false))
			effected.addMethodInvokeListener(_actionListener = new ActionDispelListener());
		if(_template._attrs.getBool("dispelOnSelfAction", false))
			effected.addMethodInvokeListener(_selfActionListener = new SelfActionDispelListener());
		if(_template._updatePet && effected.getPet() instanceof L2SummonInstance)
			effected.getPet().broadcastPetInfo();
	}

	public void onExit()
	{
		if(getSkill().getAbnormalVe().mask != 0)
			getEffected().stopAbnormalEffect(getSkill().getAbnormalVe());
		if(_template._attrs.getBool("dispelOnAction", false))
			getEffected().removeMethodInvokeListener(_actionListener);
		if(_template._attrs.getBool("dispelOnSelfAction", false))
			getEffected().removeMethodInvokeListener(_selfActionListener);
		if(_effectActionTask != null)
		{
			_effectActionTask.cancel(true);
			_effectActionTask = null;
		}
		if(_template._updatePet && getEffected().getPet() instanceof L2SummonInstance)
			getEffected().getPet().broadcastPetInfo();
	}
	
	protected L2Skill getSkill()
	{
		return _effect.getSkill();
	}

	protected L2Character getEffected()
	{
		return _effect.getEffected();
	}

	protected L2Character getEffector()
	{
		return _effect.getEffector();
	}

	public double calc()
	{
		return _template._val;
	}

	public double calcTickVal()
	{
		return 666 * _template._val * _template._ticks / 1000.;
	}

	protected synchronized void startActionTask(long period)
	{
		if(_effectActionTask != null)
		{
			_effectActionTask.cancel(true);
			_effectActionTask = null;
		}

		_effectActionTask = ThreadPoolManager.getInstance().scheduleEffect(new EffectActionTask(period), period);
	}

	public EffectTemplate getTemplate()
	{
		return _template;
	}

	private final class EffectActionTask implements Runnable
	{
		private long _period;

		public EffectActionTask(long period)
		{
			_period = period;
		}

		public void run()
		{
			try
			{
				if(_effect._state == L2Effect.EffectState.ACTING && !onActionTime())
				{
					_effect.exit();
					_effectActionTask = null;
					return;
				}
				_effectActionTask = ThreadPoolManager.getInstance().scheduleEffect(this, _period);
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}
	}

	private class ActionDispelListener implements MethodInvokeListener, MethodCollection
	{
		@Override
		public boolean accept(MethodEvent event)
		{
			return event.getMethodName().equals(onStartAttack) || event.getMethodName().equals(onStartCast) || event.getMethodName().equals(onStartAltCast) || event.getMethodName().equals(ReduceCurrentHp) || event.getMethodName().equals(onEffectAdd);
		}

		@Override
		public void methodInvoked(MethodEvent e)
		{
			_effect.exit();
		}
	}

	private class SelfActionDispelListener implements MethodInvokeListener, MethodCollection
	{
		@Override
		public boolean accept(MethodEvent event)
		{
			return event.getMethodName().equals(onStartAttack) || event.getMethodName().equals(onStartCast) || event.getMethodName().equals(onStartAltCast);
		}

		@Override
		public void methodInvoked(MethodEvent e)
		{
			_effect.exit();
		}
	}
}
