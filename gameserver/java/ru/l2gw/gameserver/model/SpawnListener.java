package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2NpcInstance;

public interface SpawnListener
{
	public void npcSpawned(L2NpcInstance npc);
}
