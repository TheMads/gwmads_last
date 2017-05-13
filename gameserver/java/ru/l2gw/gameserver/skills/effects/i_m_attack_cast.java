package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;

import java.lang.ref.WeakReference;

/**
 * @author rage
 * @date 20.10.2010 19:50:00
 */
public class i_m_attack_cast extends i_m_attack
{
	private final long[] _delays;

	public i_m_attack_cast(EffectTemplate template)
	{
		super(template);
		String[] delays = template._attrs.getString("options", "").split(";");
		_delays = new long[delays.length];
		int i = 0;
		for(String delay : delays)
			if(!delay.isEmpty())
				_delays[i++] = (long) (Double.parseDouble(delay) * 1000);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		ThreadPoolManager.getInstance().scheduleEffect(new CastTask(this, cha, targets, ss, counter), _delays[0]);
	}

	private void doRealEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		super.doEffect(cha, targets, ss, counter);
	}

	private static class CastTask implements Runnable
	{
		private final i_m_attack_cast _effect;
		private final WeakReference<L2Character> _cha;
		private final GArray<Env> _targets;
		private final int _ss;
		private final boolean _counter;
		private int _count = 0;

		public CastTask(i_m_attack_cast effect, L2Character cha, GArray<Env> targets, int ss, boolean counter)
		{
			_effect = effect;
			_cha = new WeakReference<L2Character>(cha);
			_targets = targets;
			_ss = ss;
			_counter = counter;
		}

		public void run()
		{
			_count++;
			L2Character cha = _cha.get();
			if(cha == null || !cha.isCastingNow() || cha.getCastingSkill() != _effect.getSkill() || _count > _effect._delays.length)
				return;

			L2Character target = cha.getCastingTarget();
			if(target == null || target.isDead() || !target.isInRange(cha, _effect.getSkill().getCastRange() + 500) || target.isInZonePeace() || !GeoEngine.canSeeTarget(cha, target))
			{
				cha.abortCast();
				return;
			}

			_effect.doRealEffect(cha, _targets, _ss, _counter);
			if(_count < _effect._delays.length)
				ThreadPoolManager.getInstance().scheduleEffect(this, _effect._delays[_count]);
		}
	}
}
