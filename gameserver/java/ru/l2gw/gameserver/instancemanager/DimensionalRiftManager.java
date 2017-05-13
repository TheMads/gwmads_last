package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.DimensionalRift.DimensionalRift;
import ru.l2gw.gameserver.model.entity.DimensionalRift.DimensionalRiftRoom;
import ru.l2gw.gameserver.model.zone.L2RiftRoomZone;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.math.Rnd;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author rage
 */
public class DimensionalRiftManager
{

	private static Log _log = LogFactory.getLog(DimensionalRiftManager.class.getName());
	private static DimensionalRiftManager _instance;
	private FastMap<Byte, FastList<DimensionalRiftRoom>> _roomsMap = new FastMap<Byte, FastList<DimensionalRiftRoom>>();
	private FastList<DimensionalRift> _rifts = new FastList<DimensionalRift>();
	private static int roomsCount = 0;

	public static DimensionalRiftManager getInstance()
	{
		if(_instance == null)
			_instance = new DimensionalRiftManager();

		return _instance;
	}

	public void init()
	{
		loadSpawns();
	}

	public void addZone(L2RiftRoomZone zone)
	{
		FastList<DimensionalRiftRoom> rooms = null;
		if(_roomsMap.containsKey(zone.getRoomType()))
			rooms = _roomsMap.get(zone.getRoomType());

		if(rooms == null)
			rooms = new FastList<DimensionalRiftRoom>();

		rooms.add(new DimensionalRiftRoom(zone));
		_roomsMap.put(zone.getRoomType(), rooms);
		roomsCount++;
	}

	public void loadSpawns()
	{
		int countGood = 0;
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT + "/data/dimensionalRift.xml");
			if(!file.exists())
				throw new IOException();

			Document doc = factory.newDocumentBuilder().parse(file);
			NamedNodeMap attrs;
			byte type, roomId;
			L2Spawn spawnDat;
			L2NpcTemplate template;

			for(Node rift = doc.getFirstChild(); rift != null; rift = rift.getNextSibling())
			{
				if("rift".equalsIgnoreCase(rift.getNodeName()))
				{
					for(Node area = rift.getFirstChild(); area != null; area = area.getNextSibling())
					{
						if("area".equalsIgnoreCase(area.getNodeName()))
						{
							attrs = area.getAttributes();
							type = Byte.parseByte(attrs.getNamedItem("type").getNodeValue());

							if(!_roomsMap.containsKey(type))
							{
								_log.warn("DimensionalRiftManaher: room type " + type + " not found!");
								continue;
							}

							for(Node r = area.getFirstChild(); r != null; r = r.getNextSibling())
							{
								if("room".equalsIgnoreCase(r.getNodeName()))
								{
									attrs = r.getAttributes();
									roomId = Byte.parseByte(attrs.getNamedItem("id").getNodeValue());

									DimensionalRiftRoom room = getRoom(type, roomId);
									if(room == null)
									{
										_log.warn("DimensionalRiftManaher: room type: " + type + " room id: " + roomId + " not found!");
										continue;
									}

									for(Node spawn = r.getFirstChild(); spawn != null; spawn = spawn.getNextSibling())
									{
										if("spawn".equalsIgnoreCase(spawn.getNodeName()))
										{
											attrs = spawn.getAttributes();
											int mobId = Integer.parseInt(attrs.getNamedItem("mobId").getNodeValue());
											int delay = Integer.parseInt(attrs.getNamedItem("delay").getNodeValue());
											int count = Integer.parseInt(attrs.getNamedItem("count").getNodeValue());

											template = NpcTable.getTemplate(mobId);
											if(template == null)
											{
												_log.warn("DimensionalRiftManaher: Template " + mobId + " not found!");
												continue;
											}

											Node nx = attrs.getNamedItem("x");
											Node ny = attrs.getNamedItem("y");
											Node nz = attrs.getNamedItem("z");
											Node nh = attrs.getNamedItem("heading");

											spawnDat = new L2Spawn(template);
											spawnDat.setAmount(count);
											spawnDat.setRespawnDelay(delay);

											if(nx != null && ny != null && nz != null)
											{
												int x = Integer.parseInt(nx.getNodeValue());
												int y = Integer.parseInt(ny.getNodeValue());
												int z = Integer.parseInt(nz.getNodeValue());
												int h = -1;
												if(nh != null)
													h = Integer.parseInt(nh.getNodeValue());

												spawnDat.setLocx(x);
												spawnDat.setLocy(y);
												spawnDat.setLocz(z);
												spawnDat.setHeading(h);

											}
											else
												spawnDat.setTerritory(room.getTerritory());

											room.addSpawn(spawnDat);
											countGood++;
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("Error on loading dimensional rift spawns: " + e);
			e.printStackTrace();
		}
		_log.info("DimensionalRiftManager: Loaded " + roomsCount + " dimensional rooms.");
		_log.info("DimensionalRiftManager: Loaded " + countGood + " dimensional rift spawns.");
	}

	public DimensionalRiftRoom getRoom(byte type, byte roomId)
	{
		if(!_roomsMap.containsKey(type))
			return null;

		FastList<DimensionalRiftRoom> rooms = _roomsMap.get(type);

		if(rooms == null || rooms.isEmpty())
			return null;

		for(DimensionalRiftRoom room : rooms)
			if(room.getRoomId() == roomId)
				return room;

		return null;
	}

	public synchronized List<DimensionalRiftRoom> getFreeRooms(byte type, boolean allowBoss)
	{
		List<DimensionalRiftRoom> ret = new FastList<DimensionalRiftRoom>();

		for(DimensionalRiftRoom room : _roomsMap.get(type))
			if(!room.isBusy() && !(room.isBossRoom() && !allowBoss))
				ret.add(room);

		return ret;
	}

	public void start(L2Player player, byte type)
	{
		List<DimensionalRiftRoom> rooms = getFreeRooms(type, false);

		if(rooms.size() > 2)
			_rifts.add(new DimensionalRift(player.getParty(), rooms.get(Rnd.get(rooms.size()))));
	}

	public void removeRift(DimensionalRift rift)
	{
		_rifts.remove(rift);
	}

	public void clearAllZones()
	{
		for(DimensionalRift rift : _rifts)
			if(rift != null)
				rift.manualExit();

		_rifts.clear();
		_roomsMap = new FastMap<Byte, FastList<DimensionalRiftRoom>>();
		roomsCount = 0;
	}

	public boolean isPartyInRift(L2Party party)
	{
		for(DimensionalRift rift : _rifts)
			if(rift.getParty() == party)
				return true;
		return false;
	}
}
