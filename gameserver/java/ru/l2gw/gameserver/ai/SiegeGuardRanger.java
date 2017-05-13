package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.model.L2Character;

public class SiegeGuardRanger extends SiegeGuard
{
	public SiegeGuardRanger(L2Character actor)
	{
		super(actor);
		_isMobile = false;
	}

	@Override
	protected boolean createNewTask()
	{
		// Удаляем все задания
		clearTasks();

		L2Character _temp_attack_target = getAttackTarget();

		// Новая цель исходя из агрессивности
		L2Character hated = _thisActor.isConfused() ? _temp_attack_target : _thisActor.getMostHated();

		if(hated != null && hated != _thisActor)
			_temp_attack_target = hated;
		else
		{
			returnHome();
			return false;
		}

		addAttackDesire(_temp_attack_target, 1, DEFAULT_DESIRE);
		return true;
	}
}