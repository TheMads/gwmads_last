package ru.l2gw.gameserver.instancemanager;

import javolution.util.FastMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.Fortress;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.fortress.CombatFlag;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Map;

public final class FortressSiegeManager extends SiegeManager
{
	private static FortressSiegeManager _instance;

	public static FortressSiegeManager getInstance()
	{
		if(_instance == null)
			_instance = new FortressSiegeManager();

		return _instance;
	}

	public FortressSiegeManager()
	{
		load();
	}

	public static void reload()
	{
		load();
	}

	public static void load()
	{
		try
		{
			_log.info("FortressSiegeManager: loading data...");
			File file = new File(Config.FORTRESS_DATA_FILE);

			if (!file.exists()) {
				if (Config.DEBUG)
					_log.info("The " + Config.FORTRESS_DATA_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			int registartionTime = 50 * 60000;
			int countdownTime = 10 * 60000;
			int holdTime = 75;
			int rebelTime = 2;
			int crpWin = 200;
			int famePoints = 31;
			int siegeLength = 60;
			int siegeClanMinLevel = 4;
			int nextSiege = 4;
			int castleTax = 12500;
			Map<Integer, Integer> supplyboxes = new FastMap<Integer, Integer>();

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("forts".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("common".equalsIgnoreCase(d.getNodeName()))
							{
								for(Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if("Registration".equalsIgnoreCase(c.getNodeName()))
										registartionTime = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("Countdown".equalsIgnoreCase(c.getNodeName()))
										countdownTime = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue()); 
									else if("SiegeLength".equalsIgnoreCase(c.getNodeName()))
										siegeLength = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("Hold".equalsIgnoreCase(c.getNodeName()))
										holdTime = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("Rebel".equalsIgnoreCase(c.getNodeName()))
										rebelTime = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue()); 	
									else if("SiegeClanMinLevel".equalsIgnoreCase(c.getNodeName()))
										siegeClanMinLevel = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("CRPWin".equalsIgnoreCase(c.getNodeName()))
										crpWin = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("NextSiegePeriod".equalsIgnoreCase(c.getNodeName()))
										nextSiege = Integer.parseInt(c.getAttributes().getNamedItem("time").getNodeValue());
									else if("FamePoints".equalsIgnoreCase(c.getNodeName()))
										famePoints = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("CastleTax".equalsIgnoreCase(c.getNodeName()))
										castleTax = Integer.parseInt(c.getAttributes().getNamedItem("val").getNodeValue());
									else if("supplyboxes".equalsIgnoreCase(c.getNodeName()))
									{
										for(Node s = c.getFirstChild(); s != null; s = s.getNextSibling())
											if("supplybox".equalsIgnoreCase(s.getNodeName()))
											{
												int level = Integer.parseInt(s.getAttributes().getNamedItem("level").getNodeValue());
												int npc = Integer.parseInt(s.getAttributes().getNamedItem("npc").getNodeValue());
												supplyboxes.put(level, npc);
											}
									}


								}
							}
							else if("fort".equalsIgnoreCase(d.getNodeName()))
							{
								int fortId = (d != null) ? Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue()) : 0;
								if(fortId != 0)
								{
									SiegeUnit unit = ResidenceManager.getInstance().getBuildingById(fortId);
									if(unit.isFort)
											unit.parseFort(d);
									ResidenceManager.getInstance().getBuildingById(fortId).loadReinforces();
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					_log.warn("CastleSiegeManager: can't load castle data" + e);
					e.printStackTrace();
				}
			}

			for(Fortress fort : ResidenceManager.getInstance().getFortressList())
			{
				fort.setHoldTime(holdTime);
				fort.setRebelTime(rebelTime);
				fort.setCastleTax(castleTax);
				fort.setSupplyboxes(supplyboxes);
				fort.getSiege().setNextSiegePeriod(nextSiege * 60 * 60000);
				fort.getSiege().setSiegeClanMinLevel(siegeClanMinLevel);
				fort.getSiege().setRegistrationTime(registartionTime);
				fort.getSiege().setCountdownTime(countdownTime);
				fort.getSiege().setCRPWin(crpWin);
				fort.getSiege().setFamePoints(famePoints);
				fort.getSiege().setSiegeLength(siegeLength);
				fort.getSiege().startAutoTask();
				fort.getSiege().getZone().setActive(false);
				fort.spawnFlagPoles();
				fort.getSiege().unspawnCommanders();
				fort.getSiege().spawnMerchant();
				fort.startHoldTask();
			}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading siege data.");
			e.printStackTrace();
		}
	}

	public final boolean checkIfOkToSummon(L2Character cha, boolean isCheckOnly)
	{
		if(!(cha.isPlayer()))
			return false;

		SystemMessage sm = new SystemMessage(SystemMessage.S1);
		L2Player player = (L2Player) cha;
		Fortress fort = ResidenceManager.getInstance().getFortressByObjectInSiegeZone(player);

		if(fort == null || fort.getId() <= 0)
			sm.addString("You must be on fort ground to summon this");
		else if(!fort.getSiege().isInProgress())
			sm.addString("You can only summon this during a siege.");
		else if(player.getClanId() != 0 && fort.getSiege().getAttackerClan(player.getClanId()) == null)
			sm.addString("You can only summon this as a registered attacker.");
		else
			return true;

		if(!isCheckOnly)
			player.sendPacket(sm);
		return false;
	}

	public boolean isCombat(int itemId)
	{
		return (itemId == 9819);
	}

	public void activateCombatFlag(L2Player player, L2ItemInstance item)
	{
		try
		{
			SiegeUnit fort = ResidenceManager.getInstance().getFortressByObjectInSiegeZone(player);

			for(CombatFlag cf : fort.getFlagList())
				if(cf.itemInstance == item)
				{
					cf.checkPlayer(player, item);
					break;
				}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean checkIfCanPickup(L2Player player)
	{
		Siege siege = getSiege(player);
		return !(siege == null || siege.getAttackerClan(player.getClanId()) == null || player.getMountEngine().isMounted() || player.isCombatFlagEquipped());
	}

	public void dropCombatFlag(L2Player player)
	{
		SiegeUnit unit = ResidenceManager.getInstance().getBuildingById(player.getSiegeId());
		if(unit != null && unit.isFort)
			for(CombatFlag cf : unit.getFlagList())
				if(cf.playerId == player.getObjectId())
				{
					cf.dropIt();
					cf.spawnMe();
					return;
				}
	}

	public static Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY());
	}

	public static Siege getSiege(int x, int y)
	{
		Fortress fortress = ResidenceManager.getInstance().getFortressBySiegeZoneCoord(x, y);
		if(fortress != null)
			return fortress.getSiege();
		return null;
	}
}