package ru.l2gw.gameserver.handler;

import ru.l2gw.gameserver.model.L2Character;

/**
 * @author: rage
 * @date: 21.06.12 22:28
 */
public interface IOnDieHandler
{
	public void onDie(L2Character self, L2Character killer);
}
