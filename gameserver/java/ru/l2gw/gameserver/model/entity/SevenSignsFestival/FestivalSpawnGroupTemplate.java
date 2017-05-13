package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import org.w3c.dom.Node;

/*
 * @author rage
 */
public class FestivalSpawnGroupTemplate
{
	public final int groupId;
	public final int minSpawn;
	public final int maxSpawn;
	public final int coordSpawnId;
	public final int initialDelay;
	public final int initialRespawnDelay;
	public final int finalRespawnDelay;
	public final boolean randomSpawn;
	public final boolean randomCoord;

	FestivalSpawnGroupTemplate(Node s)
	{
		Node _groupId = s.getAttributes().getNamedItem("groupId");
		Node _minSpawn = s.getAttributes().getNamedItem("minSpawn");
		Node _maxSpawn = s.getAttributes().getNamedItem("maxSpawn");
		Node _coordSpawnId = s.getAttributes().getNamedItem("coordSpawnId");
		Node _initialDelay = s.getAttributes().getNamedItem("initialDelay");
		Node _initialRespawnDelay = s.getAttributes().getNamedItem("initialRespawnDelay");
		Node _finalRespawnDelay = s.getAttributes().getNamedItem("finalRespawnDelay");
		Node _randomSpawn = s.getAttributes().getNamedItem("randomSpawn");
		Node _randomCoord = s.getAttributes().getNamedItem("randomCoord");

		groupId = _groupId != null ? Integer.parseInt(_groupId.getNodeValue()) : 0;
		minSpawn = _minSpawn != null ? Integer.parseInt(_minSpawn.getNodeValue()) : 1;
		maxSpawn = _maxSpawn != null ? Integer.parseInt(_maxSpawn.getNodeValue()) : 1;
		coordSpawnId = _coordSpawnId != null ? Integer.parseInt(_coordSpawnId.getNodeValue()) : 0;
		initialDelay = _initialDelay != null ? Integer.parseInt(_initialDelay.getNodeValue()) : 0;
		initialRespawnDelay = _initialRespawnDelay != null ? Integer.parseInt(_initialRespawnDelay.getNodeValue()) : 60;
		finalRespawnDelay = _finalRespawnDelay != null ? Integer.parseInt(_finalRespawnDelay.getNodeValue()) : 10;
		randomSpawn = _randomSpawn != null && _randomSpawn.getNodeValue().equalsIgnoreCase("true");
		randomCoord = _randomCoord == null || _randomCoord.getNodeValue().equalsIgnoreCase("true");
	}
}