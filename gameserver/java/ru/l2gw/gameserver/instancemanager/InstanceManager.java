package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.instance.InstanceTemplate;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.npcmaker.DefaultMaker;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ReflectionTable;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: rage
 * @date: 23.07.2009 19:15:38
 */
public class InstanceManager
{
	private static InstanceManager _instance;
	private static Log _log = LogFactory.getLog("instances");

	private Map<Integer, InstanceTemplate> _instanceTemplates;
	private GCSArray<Instance> _instances;
	private List<Integer> _types;

	private InstanceManager()
	{
		_instances = new GCSArray<>();
		load();
	}

	public static InstanceManager getInstance()
	{
		if(_instance == null)
			_instance = new InstanceManager();
		return _instance;
	}

	private void load()
	{
		GArray<File> _files = new GArray<>();

		try
		{
			File dir = new File("data/instances");

			if(!dir.exists())
			{
				_log.fatal("InstanceManager: " + dir.getAbsolutePath() + " does not exists!");
				return;
			}

			File[] files = dir.listFiles();

			for(File f : files)
			{
				if(f.getName().endsWith(".xml"))
					_files.add(f);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to load instance files.");
		}

		_instanceTemplates = new HashMap<>();

		try
		{
			for(File file : _files)
			{
				_log.info("InstanceManager: load file " + file.getName());

				if(!file.exists())
				{
					if(Config.DEBUG)
						_log.info("The " + file.getName() + " file is missing.");
					continue;
				}

				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);

				Document doc = factory.newDocumentBuilder().parse(file);

				for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if("instances".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node i = n.getFirstChild(); i != null; i = i.getNextSibling())
						{
							if("instance".equalsIgnoreCase(i.getNodeName()))
							{
								int id = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
								_instanceTemplates.put(id, InstanceTemplate.parseInstance(i));
							}
						}
					}
				}
			}
			_log.info("InstanceManager: loaded " + _instanceTemplates.size() + " instance templates");
		}
		catch(Exception e)
		{
			_log.warn("Error while loading instances " + e);
			e.printStackTrace();
		}
	}

	public InstanceTemplate getInstanceTemplateById(int id)
	{
		return _instanceTemplates.get(id);
	}

	public synchronized void removeInstance(int instId, int reflection)
	{
		if(Config.DEBUG_INSTANCES)
			_log.info("InstanceManager: removeInstance id: " + instId + " refId=" + reflection + ";");

		for(Instance inst : _instances)
			if(inst.getTemplate().getId() == instId && inst.getReflection() == reflection)
			{
				if(Config.DEBUG_INSTANCES)
					_log.info("InstanceManager: removeInstance instance: " + inst);
				_instances.remove(inst);
				break;
			}

		Reflection ref = ReflectionTable.getInstance().getById(reflection);
		if(ref != null)
		{
			ref.collapse();
			ReflectionTable.getInstance().removeReflection(reflection);
		}
	}

	public Instance createNewInstance(int instanceId, List<L2Player> party)
	{
		InstanceTemplate template = _instanceTemplates.get(instanceId);

		if(template == null)
		{
			_log.info("InstanceManager: no instance template for id: " + instanceId);
			return null;
		}

		Constructor<?> constructor = null;
		if(template.getClassName() != null)
		{
			try
			{
				if("OlympiadInstance".equals(template.getClassName()))
					constructor = OlympiadInstance.class.getConstructors()[0];
				else
					constructor = Scripts.getInstance().getClasses().get("instances." + template.getClassName()).getRawClass().getConstructors()[0];
			}
			catch(Exception e1)
			{
				_log.warn("InstanceManager: instances/" + template.getClassName() + " not found!");
				e1.printStackTrace();
			}
		}

		int rId = ReflectionTable.getInstance().createNewReflection();
		Instance inst;

		if(constructor != null)
			try
			{
				inst = (Instance) constructor.newInstance(template, rId);
			}
			catch(Exception e)
			{
				inst = new Instance(template, rId);
			}
		else
			inst = new Instance(template, rId);

		inst.addMembers(party);

		if(Config.DEBUG)
			_log.info("InstanceManager: add instance to list " + inst);
		_instances.add(inst);
		inst.startInstance();
		return inst;
	}

	public void addZone(L2Zone zone)
	{
		if(!_instanceTemplates.containsKey(zone.getEntityId()))
		{
			_log.warn("InstanceManager: no instance template for zone: " + zone + " instance id: " + zone.getEntityId());
			return;
		}

		_instanceTemplates.get(zone.getEntityId()).registerZone(zone);
	}

	public Instance getInstanceByPlayer(L2Player player)
	{
		int objectId = player.getObjectId();

		if(Config.DEBUG)
			_log.info("InstanceManager: getInstanceByPlayer objectId: " + objectId);

		for(Instance inst : _instances)
		{
			if(Config.DEBUG)
				_log.info("InstanceManager: getInstanceByPlayer check: " + inst);
			if(inst.isInside(objectId))
			{
				if(Config.DEBUG)
					_log.info("InstanceManager: getInstanceByPlayer return: " + inst);
				return inst;
			}
		}

		return null;
	}

	public int getInstanceCount(int instId)
	{
		int c = 0;
		for(Instance inst : _instances)
			if(inst != null && inst.getTemplate().getId() == instId)
				c++;

		return c;
	}

	public List<Integer> getInstanceTypes()
	{
		if(_types == null)
		{
			_types = new ArrayList<>();
			for(InstanceTemplate it : _instanceTemplates.values())
				if(!_types.contains(it.getType()))
					_types.add(it.getType());
		}

		return _types;
	}

	public static boolean enterInstance(int instId, L2Player player, L2NpcInstance npc, int questId)
	{
		InstanceTemplate it = InstanceManager.getInstance().getInstanceTemplateById(instId);

		if(it == null)
		{
			_log.warn(player + " try to enter instance id: " + instId + " but no instance template!");
			return false;
		}

		if(questId == 0)
			questId = it.getCheckQuest();

		Instance inst = getInstance().getInstanceByPlayer(player);
		List<L2Player> party = new ArrayList<>();

		if(inst != null)
		{
			if(inst.getTemplate().getId() != instId)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON));
				return false;
			}
			if(player.getLevel() < it.getMinLevel() || player.getLevel() > it.getMaxLevel())
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
				return false;
			}
			else if(questId > 0 && !player.isQuestStarted(questId))
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
				return false;
			}

			if(it.isDispelBuff())
				for(L2Effect e : player.getAllEffects())
				{
					if(e.getNext() != null && e.getNext().isInUse() && e.getNext().getSkill().getBuffProtectLevel() < 1)
						e.getNext().exit();

					if(e.getSkill().getBuffProtectLevel() < 1 && !e.getSkill().getAbnormals().contains("transformation"))
						e.exit();
				}

			player.setStablePoint(player.getLoc());
			player.teleToLocation(inst.getStartLoc(), inst.getReflection());
			return true;
		}

		if(it.getMaxCount() > 0 && InstanceManager.getInstance().getInstanceCount(instId) >= it.getMaxCount())
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_MAXIMUM_NUMBER_OF_INSTANCE_ZONES_HAS_BEEN_EXCEEDED_YOU_CANNOT_ENTER));
			return false;
		}

		if(it.getMinParty() > 1)
		{
			if(player.getParty() == null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_CURRENTLY_IN_A_PARTY_SO_YOU_CANNOT_ENTER));
				return false;
			}
			else if(!player.getParty().isLeader(player))
			{
				player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER));
				return false;
			}

			List<L2Player> partyList = player.getParty().getPartyMembers();
			L2CommandChannel channel = null;

			if(it.getMaxParty() > 9)
			{
				channel = player.getParty().getCommandChannel();
				if(channel == null && it.getMinParty() > 9)
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_ENTER_BECAUSE_YOU_ARE_NOT_ASSOCIATED_WITH_THE_CURRENT_COMMAND_CHANNEL));
					return false;
				}
				else if(channel != null && channel.getChannelLeader() != player)
				{
					player.sendPacket(new SystemMessage(SystemMessage.ONLY_A_PARTY_LEADER_CAN_TRY_TO_ENTER));
					return false;
				}

				if(channel != null)
					partyList = channel.getMembers();
			}

			if(partyList.size() > it.getMaxParty() || partyList.size() < it.getMinParty())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT));
				return false;
			}

			boolean ok = true;
			for(L2Player member : partyList)
				if(member.getLevel() < it.getMinLevel() || member.getLevel() > it.getMaxLevel())
				{
					if(channel != null)
						channel.broadcastToChannelMembers(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
					else
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
					ok = false;
				}
				else if(member.getVar("instance-" + it.getType()) != null || InstanceManager.getInstance().getInstanceByPlayer(member) != null)
				{
					if(channel != null)
						channel.broadcastToChannelMembers(new SystemMessage(SystemMessage.C1_MAY_NOT_RE_ENTER_YET).addCharName(member));
					else
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_MAY_NOT_RE_ENTER_YET).addCharName(member));
					ok = false;
				}
				else if(questId > 0 && !member.isQuestStarted(questId))
				{
					if(channel != null)
						channel.broadcastToChannelMembers(new SystemMessage(SystemMessage.C1S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
					else
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(member));
					ok = false;
				}
				else if(!npc.isInRange(member, 300))
				{
					if(channel != null)
						channel.broadcastToChannelMembers(new SystemMessage(SystemMessage.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addCharName(member));
					else
						player.getParty().broadcastToPartyMembers(new SystemMessage(SystemMessage.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addCharName(member));
					ok = false;
				}

			if(!ok)
				return false;

			party.addAll(partyList);
		}
		else
		{
			if(player.getLevel() < it.getMinLevel() || player.getLevel() > it.getMaxLevel())
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1S_LEVEL_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
				return false;
			}
			else if(player.getVar("instance-" + it.getType()) != null)
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1_MAY_NOT_RE_ENTER_YET).addCharName(player));
				return false;
			}
			else if(questId > 0 && !player.isQuestStarted(questId))
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1S_QUEST_REQUIREMENT_IS_NOT_SUFFICIENT_AND_CANNOT_BE_ENTERED).addCharName(player));
				return false;
			}
			else if(!npc.isInRange(player, 300))
			{
				player.sendPacket(new SystemMessage(SystemMessage.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addCharName(player));
				return false;
			}

			party.add(player);
		}

		inst = InstanceManager.getInstance().createNewInstance(instId, party);

		if(inst != null)
			for(L2Player member : party)
				if(member != null)
				{
					if(it.isDispelBuff())
						for(L2Effect e : member.getAllEffects())
						{
							if(e.getNext() != null && e.getNext().isInUse() && e.getNext().getSkill().getBuffProtectLevel() < 1)
								e.getNext().exit();

							if(e.getSkill().getBuffProtectLevel() < 1 && !e.getSkill().getAbnormals().contains("transformation"))
								e.exit();
						}

					member.setStablePoint(member.getLoc());
					member.teleToLocation(inst.getStartLoc(), inst.getReflection());
				}

		return true;
	}

	public Instance getInstanceByReflection(int refId)
	{
		for(Instance inst : _instances)
			if(inst.getReflection() == refId)
				return inst;

		return null;
	}

	public DefaultMaker getNpcMaker(int refId, String makerName)
	{
		Instance inst = getInstanceByReflection(refId);
		if(inst == null)
			return null;

		return inst.getMaker(makerName);
	}

	public L2NpcInstance getNpcById(L2Object obj, int npcId)
	{
		if(obj != null)
			return getNpcById(obj.getReflection(), npcId);
		return null;
	}

	public L2NpcInstance getNpcById(int refId, int npcId)
	{
		Instance inst = getInstanceByReflection(refId);
		if(inst == null)
			return null;

		Reflection ref = ReflectionTable.getInstance().getById(refId);
		if(ref == null)
			return null;

		for(L2Object obj : ref.getAllObjects())
			if(obj instanceof L2NpcInstance && ((L2NpcInstance) obj).getNpcId() == npcId)
				return (L2NpcInstance) obj;

		return null;
	}
}
