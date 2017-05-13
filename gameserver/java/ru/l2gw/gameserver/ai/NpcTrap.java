package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;

/**
 * @author rage
 * @date 26.11.2009 9:37:13
 */
public class NpcTrap extends Trap
{
	public NpcTrap(L2Character actor)
	{
		super(actor);
		int lifeTime = getInt("trap_life_time", 0);
		if(lifeTime > 0)
			_thisTrap.setLifeTime(lifeTime);
	}

	@Override
	protected boolean thinkActive()
	{
		if(_thisTrap.isDead() || castSkill)
			return true;

		if(_thisTrap.getTrapLifeTime() > 0 && _thisTrap.getTrapLifeTime() < System.currentTimeMillis())
		{
			_thisTrap.doDie(null);
			return true;
		}

		if(isDetected != _thisTrap.isDetected())
		{
			if(isDetected)
				for(L2Player player : L2World.getAroundPlayers(_thisTrap))
					if(_thisTrap.getPlayer() == null || player != _thisTrap.getPlayer())
						player.removeVisibleObject(_thisTrap);

			isDetected = _thisTrap.isDetected();
		}

		if(trapSkill != null && !_thisTrap.isSkillDisabled(trapSkill.getId()) && _thisTrap.isActive())
			for(L2Character cha : _thisTrap.getKnownCharacters(trapRange))
				if((!cha.isPlayer() || !((L2Player) cha).isInvisible()) && trapSkill.checkTarget(_thisTrap, trapSkill.getAimingTarget(_thisActor, cha), false, true) == null)
				{
					_thisTrap.setDetected(5);
					castSkill = true;

					ThreadPoolManager.getInstance().scheduleEffect(new CastSkill(_thisTrap, trapSkill, cha), 2000);
					break;
				}

		return true;
	}

	@Override
	protected void onEvtDead(L2Character killer)
	{
		_thisTrap.setDetected(0);
		castSkill = false;
		if(_skills.size() > 0)
			trapSkill = _skills.get(Rnd.get(_skills.size()));
		super.onEvtDead(killer);
	}
}
