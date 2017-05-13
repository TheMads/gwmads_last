package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

class c_rest extends t_effect
{
	public c_rest(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

/*
	@Override
	public EffectType getEffectType()
	{
		return EffectType.continuous;
	}
*/

	/** Notify started */
	@Override
	public void onStart()
	{
		if(((L2Player) getEffected()).getMountEngine().isMounted())
		{
			_effect.exit();
			return;
		}
		setRelax(true);
		super.onStart();
		startActionTask(3000);
	}

	@Override
	public void onExit()
	{
		setRelax(false);
		super.onExit();
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead() || !getEffected().isSitting())
			return false;

		if(getEffected().getCurrentHp() >= getEffected().getMaxHp())
		{
			getEffected().sendPacket(new SystemMessage(SystemMessage.HP_WAS_FULLY_RECOVERED_AND_SKILL_WAS_REMOVED));
			return false;
		}

		double manaDam = calcTickVal();

		if(manaDam > getEffected().getCurrentMp())
		{
			getEffected().sendPacket(Msg.NOT_ENOUGH_MP);
			return false;
		}

		getEffected().reduceCurrentMp(manaDam, null);
		return true;
	}

	private void setRelax(boolean val)
	{
		((L2Player) getEffected()).setRelax(val);
	}
}
