package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShip;
import ru.l2gw.gameserver.model.entity.vehicle.L2AirShipDock;
import ru.l2gw.gameserver.model.entity.vehicle.L2Vehicle;
import ru.l2gw.gameserver.model.entity.vehicle.RouteStation;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * @author rage
 * @date 07.05.2010 10:50:16
 */
public class VehicleManager
{
	private static VehicleManager _instance;
	private static Log _log = LogFactory.getLog("vehicle");

	private FastMap<Integer, L2Vehicle> _vehicles;
	private FastMap<Integer, L2AirShipDock> _docks;

	private VehicleManager()
	{
		load();
	}

	public static VehicleManager getInstance()
	{
		if(_instance == null)
			_instance = new VehicleManager();
		return _instance;
	}

	public L2Vehicle getVehicleByObjectId(int objectId)
	{
		if(_vehicles != null)
			return _vehicles.get(objectId);

		return null;
	}

	private void load()
	{
		_log.info("VehicleManager: initializing");

		try
		{
			File file = new File(Config.DATAPACK_ROOT + "/data/vehicledata.xml");

			if(!file.exists())
			{
				_log.warn("VehicleManager: file no found: " + file.getName() + " load aborted");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			_vehicles = new FastMap<Integer, L2Vehicle>().shared();
			_docks = new FastMap<Integer, L2AirShipDock>().shared();

			Document doc = factory.newDocumentBuilder().parse(file);
			for(Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
				if("list".equals(list.getNodeName()))
				{
					for(Node vehNode = list.getFirstChild(); vehNode != null; vehNode = vehNode.getNextSibling())
					{
						if("vehicle".equals(vehNode.getNodeName()))
						{
							String name = vehNode.getAttributes().getNamedItem("name").getNodeValue();
							L2Vehicle vehicle;
							if(vehNode.getAttributes().getNamedItem("airship") != null && vehNode.getAttributes().getNamedItem("airship").getNodeValue().equalsIgnoreCase("true"))
								vehicle = new L2AirShip(IdFactory.getInstance().getNextId(), new L2CharTemplate(getEmptyStatsSet()));
							else
								vehicle = new L2Vehicle(IdFactory.getInstance().getNextId(), new L2CharTemplate(getEmptyStatsSet()));
							vehicle.setName(name);

							for(Node n = vehNode.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if("broadcast".equals(n.getNodeName()))
								{
									int x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
									int y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
									int z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());

									vehicle.addBroadcastPoint(new Location(x, y, z));
								}
								else if("route".equals(n.getNodeName()))
								{
									for(Node r = n.getFirstChild(); r != null; r = r.getNextSibling())
									{
										if("station".equals(r.getNodeName()))
										{
											RouteStation rs = RouteStation.parseStation(r);
											if(rs != null)
												vehicle.addRouteStation(rs);
										}
									}
								}
							}
							_vehicles.put(vehicle.getObjectId(), vehicle);
						}
						else if("dock".equals(vehNode.getNodeName()))
						{
							String name = vehNode.getAttributes().getNamedItem("name").getNodeValue();
							int id = Integer.parseInt(vehNode.getAttributes().getNamedItem("id").getNodeValue());

							L2AirShipDock dock = new L2AirShipDock(id, name);
							dock.setDockZone(Integer.parseInt(vehNode.getAttributes().getNamedItem("zoneId").getNodeValue()));

							for(Node n = vehNode.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if("teleport".equals(n.getNodeName()))
								{
									dock.setClientDockId(Integer.parseInt(n.getAttributes().getNamedItem("dockId").getNodeValue()));
									for(Node t = n.getFirstChild(); t != null; t = t.getNextSibling())
										if("point".equals(t.getNodeName()))
										{
											StatsSet tp = new StatsSet();
											tp.set("port_id", t.getAttributes().getNamedItem("id").getNodeValue());
											tp.set("ep", t.getAttributes().getNamedItem("ep").getNodeValue());
											tp.set("x", t.getAttributes().getNamedItem("x").getNodeValue());
											tp.set("y", t.getAttributes().getNamedItem("y").getNodeValue());
											tp.set("z", t.getAttributes().getNamedItem("z").getNodeValue());
											dock.addTeleport(tp);
										}
								}
								else if("route".equals(n.getNodeName()))
								{
									String type = n.getAttributes().getNamedItem("type").getNodeValue();
									for(Node r = n.getFirstChild(); r != null; r = r.getNextSibling())
										if("station".equals(r.getNodeName()))
										{
											RouteStation rs = RouteStation.parseStation(r);
											if(rs != null)
												dock.addRouteStation(type, rs);
										}
								}
							}
							_docks.put(id, dock);
						}
					}
				}

			_log.info("VehicleManager: loaded " + _vehicles.size() + " vehicles.");
			_log.info("VehicleManager: loaded " + _docks.size() + " docks.");

			for(L2Vehicle vehicle : _vehicles.values())
				vehicle.spawnMe();
		}
		catch(Exception e)
		{
			_log.warn("VehicleManager: load error: " + e);
			e.printStackTrace();
		}
	}

	public L2AirShipDock getDockById(int dockId)
	{
		return _docks.get(dockId);
	}

	public void addVehicle(L2Vehicle vehicle)
	{
		_vehicles.put(vehicle.getObjectId(), vehicle);
	}

	public void removeVehicle(int objectId)
	{
		_vehicles.remove(objectId);
	}

	public static StatsSet getEmptyStatsSet()
	{
		StatsSet npcDat = new StatsSet();
		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseCpMax", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseCpReg", 0);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseCritRate", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("baseWalkSpd", 0);
		return npcDat;
	}
}
