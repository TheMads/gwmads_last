package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.entity.fieldcycle.FieldCycle;
import ru.l2gw.gameserver.model.entity.fieldcycle.FieldStep;
import ru.l2gw.gameserver.model.entity.fieldcycle.IFieldCycleMaker;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.tables.DoorTable;
import ru.l2gw.gameserver.tables.SpawnTable;
import ru.l2gw.util.Location;

import javax.xml.parsers.DocumentBuilderFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;

/**
 * @author: rage
 * @date: 11.12.11 17:15
 */
public class FieldCycleManager
{
	private static final Log _log = LogFactory.getLog(FieldCycleManager.class);
	private static final Log _logPoints = LogFactory.getLog("fieldcycle");
	private static FieldCycle[] fieldCycles;

	public static void load()
	{
		_log.info("FieldCycleManager: loading data.");
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(Config.FIELDCYCLE_FILE);
			int count = 0;
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("fieldcycle".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("cycle".equalsIgnoreCase(d.getNodeName()))
							{
								int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
								FieldCycle fc = new FieldCycle(id);
								count++;
								for(Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if("step".equalsIgnoreCase(c.getNodeName()))
									{
										int stepId = Integer.parseInt(c.getAttributes().getNamedItem("id").getNodeValue());
										long step_point = Long.parseLong(c.getAttributes().getNamedItem("step_point").getNodeValue());
										long lock_time = Long.parseLong(c.getAttributes().getNamedItem("lock_time").getNodeValue());
										long drop_time = Long.parseLong(c.getAttributes().getNamedItem("drop_time").getNodeValue());
										long interval_time = Long.parseLong(c.getAttributes().getNamedItem("interval_time").getNodeValue());
										long interval_point = Long.parseLong(c.getAttributes().getNamedItem("interval_point").getNodeValue());

										FieldStep fs = new FieldStep(id, stepId, step_point, lock_time, drop_time, interval_time, interval_point);

										Node node = c.getAttributes().getNamedItem("change_time");
										if(node != null && node.getNodeValue() != null)
											fs.setChangeTime(node.getNodeValue());

										for(Node s = c.getFirstChild(); s != null; s = s.getNextSibling())
										{
											if("area_on".equalsIgnoreCase(s.getNodeName()))
											{
												String areaName = s.getAttributes().getNamedItem("name").getNodeValue();
												L2Zone zone = ZoneManager.getInstance().getZoneByName(areaName);
												if(zone == null)
													_log.info("FieldCycleManager: warning: FieldCycle " + id + " step " + stepId + " has no zone: " + areaName);
												else
													fs.addAreaOn(zone);
											}
											else if("open_door".equalsIgnoreCase(s.getNodeName()))
											{
												String doorName = s.getAttributes().getNamedItem("name").getNodeValue();
												L2DoorInstance door = DoorTable.getInstance().getDoorByName(doorName);
												if(door == null)
													_log.info("FieldCycleManager: warning: FieldCycle " + id + " step " + stepId + " has no door: " + doorName);
												else
													fs.addOpenDoor(door);
											}
											else if("map".equalsIgnoreCase(s.getNodeName()))
											{
												String loc = s.getAttributes().getNamedItem("point").getNodeValue();
												int stringId = Integer.parseInt(s.getAttributes().getNamedItem("string").getNodeValue());
												fs.setMapString(loc, stringId);
											}
											else if("restart".equals(s.getNodeName()))
											{
												String points = s.getAttributes().getNamedItem("range").getNodeValue();
												Matcher m = SpawnTable.tp.matcher(points);
												L2Territory terr = new L2Territory("field_cycle_" + id + "_" + stepId);

												while(m.find())
													terr.add(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));

												if(terr.getCoords().size() < 3)
													_log.warn("FieldCycleManager: can't parse territory FieldCycle " + id + " step " + stepId);
												fs.setRestartRange(terr);
												for(Node p = s.getFirstChild(); p != null; p = p.getNextSibling())
												{
													if("normal".equalsIgnoreCase(p.getNodeName()))
													{
														fs.addNormalPoint(Location.parseLoc(p.getAttributes().getNamedItem("point").getNodeValue()));
													}
													else if("chao".equalsIgnoreCase(p.getNodeName()))
													{
														fs.addChaoPoint(Location.parseLoc(p.getAttributes().getNamedItem("point").getNodeValue()));
													}
												}
											}
										}
										fc.addStep(fs);
									}
								}

								if(fieldCycles == null)
								{
									fieldCycles = new FieldCycle[id + 1];
									fieldCycles[id] = fc;
								}
								else if(fieldCycles.length <= id)
								{
									int len = fieldCycles.length;
									FieldCycle[] tmp = new FieldCycle[id + 1];
									System.arraycopy(fieldCycles, 0, tmp, 0, len);
									tmp[id] = fc;
									fieldCycles = tmp;
								}
								else
									fieldCycles[id] = fc;
							}
						}
					}
				}
				catch(Exception e)
				{
					_log.warn("FieldCycleManager: can't load fieldcycle data: " + e);
					e.printStackTrace();
				}
			}

			_log.info("FiledCycleManager: loaded " + count + " field cycles.");

			Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt = null;
			ResultSet rs = null;

			stmt = con.prepareStatement("SELECT * FROM field_cycle ORDER BY 1");
			rs = stmt.executeQuery();
			while(rs.next())
			{
				int fieldId = rs.getInt("field_id");
				long point = rs.getLong("point");
				int step = rs.getInt("step");
				long stepChange = rs.getInt("step_changed_time") * 1000L;
				long pointChange = rs.getInt("point_changed_time") * 1000L;

				FieldCycle fc = fieldId < fieldCycles.length ? fieldCycles[fieldId] : null;
				if(fc == null)
				{
					_log.warn("FieldCycleManager: can't restore data for FieldCycle " + fieldId);
					continue;
				}

				fc.setCurrentPoint(point);
				fc.setCurrentStep(step);
				fc.setStepChangeTime(stepChange);
				fc.setPointChangeTime(pointChange);
			}

			DbUtils.closeQuietly(con, stmt, rs);

			for(FieldCycle fc : fieldCycles)
			{
				if(fc != null)
				{
					fc.startCycle();
				}
			}
		}
		catch(Exception e)
		{
			_log.warn("FieldCycleManager: Error while loading fieldcycle data.");
			e.printStackTrace();
		}
	}

	public static void shutdown()
	{
		for(FieldCycle fc : fieldCycles)
		{
			if(fc != null)
			{
				fc.shutdown();
			}
		}
	}

	public static int getStep(int fieldId)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return -1;

		return fieldCycles[fieldId].getStep();
	}

	public static long getPoint(int fieldId)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return -1;

		return fieldCycles[fieldId].getPoint();
	}

	public static void addPoint(String process, int fieldId, long point)
	{
		addPoint(process, fieldId, point, null);
	}

	public static void addPoint(String process, int fieldId, long point, L2Character actor)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return;

		fieldCycles[fieldId].addPoint(process, point, actor);
	}

	public static void setStep(String process, int fieldId, int step)
	{
		setStep(process, fieldId, step, null);
	}

	public static void setStep(String process, int fieldId, int step, L2Character actor)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return;

		fieldCycles[fieldId].setStep(process, step, actor);
	}

	public static void registerStepChanged(int fieldId, IFieldCycleMaker maker)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return;

		fieldCycles[fieldId].registerStepChanged(maker);
	}

	public static void registerStepExpired(int fieldId, IFieldCycleMaker maker)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return;

		fieldCycles[fieldId].registerStepExpired(maker);
	}

	public static int getMapString(int fieldId)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return 0;

		return fieldCycles[fieldId].getMapString();
	}

	public static Location getMapLoc(int fieldId)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return null;

		return fieldCycles[fieldId].getMapLoc();
	}

	public static FieldCycle getFieldCycle(int fieldId)
	{
		if(fieldId < 0 || fieldId >= fieldCycles.length || fieldCycles[fieldId] == null)
			return null;

		return fieldCycles[fieldId];
	}

	public static Location getRestartPoint(L2Player player)
	{
		if(player == null)
			return null;

		for(FieldCycle fc : fieldCycles)
			if(fc != null && fc.isInRestartRange(player))
				return fc.getRestartPoint(player);

		return null;
	}
}
