package ru.l2gw.extensions.scripts;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.util.Location;
import ru.l2gw.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @Author: Diamond
 * @Date: 7/6/2007
 * @Time: 5:22:23
 */
public class Functions
{
	public static L2Object self;
	public static L2NpcInstance npc;
	protected final static Log _log = LogFactory.getLog("scripts");  

	/**
	 * Вызывает метод с задержкой
	 * @param object - от чьего имени вызывать
	 * @param sClass - вызываемый класс
	 * @param sMethod - вызываемый метод
	 * @param args - массив аргуметов
	 * @param variables - список выставляемых переменных
	 * @param delay - задержка в миллисекундах
	 */
	public static ScheduledFuture<?> executeTask(final L2Object object, final String sClass, final String sMethod, final Object[] args, final HashMap<String, Object> variables, long delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
			public void run()
			{
				if(object != null)
					object.callScripts(sClass, sMethod, args, variables);
			}
		}, delay);
	}

	public static ScheduledFuture<?> executeTask(final String sClass, final String sMethod, final Object[] args, final HashMap<String, Object> variables, long delay)
	{
		return ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){
			public void run()
			{
				callScripts(sClass, sMethod, args, variables);
			}
		}, delay);
	}

	public static ScheduledFuture<?> executeTask(final L2Object object, final String sClass, final String sMethod, final Object[] args, long delay)
	{
		return executeTask(object, sClass, sMethod, args, null, delay);
	}

	public static ScheduledFuture<?> executeTask(final String sClass, final String sMethod, final Object[] args, long delay)
	{
		return executeTask(sClass, sMethod, args, null, delay);
	}

	public static Object callScripts(String _class, String method, Object[] args, HashMap<String, Object> variables)
	{
		if(ru.l2gw.extensions.scripts.Scripts.loading)
			return null;

		ScriptObject o;

		Script scriptClass = Scripts.getInstance().getClasses().get(_class);

		if(scriptClass == null)
			return null;

		try
		{
			o = scriptClass.newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(variables != null)
			for(Map.Entry<String, Object> obj : variables.entrySet())
				try
				{
					o.setProperty(obj.getKey(), obj.getValue());
				}
				catch(Exception e)
				{}

		return o.invokeMethod(method, args);
	}

	public static void show(String text, L2Player self)
	{
		if(text == null || self == null)
			return;
		NpcHtmlMessage msg = new NpcHtmlMessage(self.getLastNpc() != null ? self.getLastNpc().getObjectId() : 5);

		// Не указываем явно язык
		if(text.endsWith(".html-ru") || text.endsWith(".htm-ru"))
			text = text.substring(0, text.length() - 3);

		// приводим нашу html-ку в нужный вид
		if(text.endsWith(".html") || text.endsWith(".htm"))
			msg.setFile(text);
		else
			msg.setHtml(Strings.bbParse(text));
		self.sendPacket(msg);
	}

	public static void show(CustomMessage message, L2Player self)
	{
		show(message.toString(), self);
	}

	public static void sendMessage(String text, L2Player self)
	{
		self.sendMessage(text);
	}

	public static void sendMessage(CustomMessage message, L2Player self)
	{
		self.sendMessage(message);
	}

	public static void npcSay(L2NpcInstance npc, int chatType, String text)
	{
		npcSay(npc, chatType, -1, text);
	}

	public static void npcSay(L2NpcInstance npc, int chatType, int stringId, String... params)
	{
		if(npc == null)
			return;

		npc.broadcastPacket(new NpcSay(npc, chatType, stringId, params));
	}

	public static void npcSayInRange(L2NpcInstance npc, int chatType, String text, int range)
	{
		npcSayInRange(npc, chatType, -1, range, text);
	}

	public static void npcSayInRange(L2NpcInstance npc, int chatType, int stringId, int range, String... params)
	{
		if(npc == null)
			return;

		NpcSay cs = new NpcSay(npc, chatType, stringId, params);
		for(L2Player player : npc.getAroundPlayers(range))
			player.sendPacket(cs);
	}

	public static void npcSayCustom(L2NpcInstance npc, int chatType, String address, String[] replaces)
	{
		if(npc == null)
			return;

		for(L2Player player : L2World.getAroundPlayers(npc))
		{
			CustomMessage cm = new CustomMessage(address, player);
			if(replaces != null)
				for(String replace : replaces)
					cm.addString(replace);
			player.sendPacket(new NpcSay(npc, chatType, cm.toString()));
		}
	}

	public static void npcSayCustomInRange(L2NpcInstance npc, int chatType, String address, String[] replaces, int range)
	{
		if(npc == null)
			return;

		for(L2Player player : L2World.getAroundPlayers(npc, range, Config.PLAYER_VISIBILITY_Z))
		{
			CustomMessage cm = new CustomMessage(address, player);
			if(replaces != null)
				for(String replace : replaces)
					cm.addString(replace);
			player.sendPacket(new NpcSay(npc, chatType, cm.toString()));
		}
	}

	public static void whisperFStr(L2NpcInstance npc, L2Player player, int fStringId, String... params)
	{
		if(npc == null || player == null)
			return;

		player.sendPacket(new NpcSay(npc, Say2C.TELL, fStringId, params));
	}

	public static void broadcastOnScreenMsg(L2Character cha, int range, int text_align, int big_fond, int deco, int time, String custom)
	{
		for(L2Player player : cha.getAroundLivePlayers(range))
		{
			CustomMessage cm = new CustomMessage(custom, player);
			player.sendPacket(new ExShowScreenMessage(cm.toString(), time, text_align, big_fond, deco));
		}
	}

	public static void broadcastOnScreenMsg(L2Character cha, int range, int text_align, int big_font, int deco, int time, int npcMsgId, String... params)
	{
		ExShowScreenMessage msg = new ExShowScreenMessage(text_align, 0, 0, big_font, 0, deco, time, 0, npcMsgId, params);
		for(L2Player player : cha.getAroundLivePlayers(range))
			player.sendPacket(msg);
	}

	public static void showOnScreentMsg(L2Player c0, int align, int unk1, int bigFong, int unk2, int unk3, int deco, int time, int unk4, int fString, String... params)
	{
		c0.sendPacket(new ExShowScreenMessage(align, unk1, unk2, bigFong, unk3, deco, time, unk4, fString, params));
	}

	public static void broadcastOnScreenMsgFStr(L2Character cha, int range, int align, int unk1, int bigFong, int unk2, int unk3, int deco, int time, int unk4, int fString, String... params)
	{
		cha.broadcastPacket(new ExShowScreenMessage(align, unk1, unk2, bigFong, unk3, deco, time, unk4, fString, params), range);
	}

	public static void broadcastSystemMessageFStr(L2Character cha, int range, int fString, String... params)
	{
		cha.broadcastPacket(new SystemMessage(fString, params), range);
	}

	public static void showSystemMessageFStr(L2Character talker, int fString, String... params)
	{
		if(talker != null)
			talker.sendPacket(new SystemMessage(fString, params));
	}

	public static void startScenePlayer(L2Player player, int scentId)
	{
		if(player != null)
			player.showQuestMovie(scentId);
	}

	public static void startScenePlayerAround(L2Character cha, int scentId, int range, int zRange)
	{
		for(L2Player player : cha.getAroundPlayers(range, zRange))
			player.showQuestMovie(scentId);
	}

	public static void sendUIEventFStr(L2Character cha, int p1, int p2, int p3, String s1, String s2, String s3, String s4, String s5, int fString, String... params)
	{
		if(cha != null)
			cha.sendPacket(new ExSendUIEvent(cha.getObjectId(), p1, p2, p3, s1, s2, s3, s4, s5, fString, params));
	}

	public static void changeZoneInfo(L2Character cha, int zone, int status)
	{
		if(cha != null)
			cha.sendPacket(new ExChangeZoneInfo(zone, status));
	}

	public static void voiceNpcEffect(L2Character cha, String s0, int p1)
	{
		if(cha != null)
			cha.sendPacket(new PlaySound(s0));
	}

	public static void sendSysMessage(L2Player player, String message)
	{
		if(player == null)
			return;

		player.sendPacket(new Say2(0, Say2C.ALL, "SYS", message));
	}

	/**
	 * Добавляет предмет в инвентарь чара
	 * @param playable Владелец инвентаря
	 * @param item_id ID предмета
	 * @param count количество
	 */
	public static void addItem(L2Playable playable, int item_id, long count)
	{
		if(playable == null || count < 1)
			return;

		L2Playable player;
		if(playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;

		player.getInventory().addItem("Scripts.addItem", item_id, count, player.getPlayer(), null);

		if(item_id == 57)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_OBTAINED_S1_ADENA).addNumber(count));
		else if(count > 1)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S2_S1_S).addItemName(item_id).addNumber(count));
		else
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED__S1).addItemName(item_id));
	}

	/**
	 * Возвращает количество предметов в инвентаре чара.
	 * @param playable Владелец инвентаря
	 * @param item_id ID предмета
	 * @return количество
	 */
	public static long getItemCount(L2Playable playable, int item_id)
	{
		long count = 0;
		L2Playable player;
		if(playable != null && playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;
		Inventory inv = player.getInventory();
		if(inv == null)
			return 0;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
			if(item.getItemId() == item_id)
				count += item.getCount();
		return count;
	}

	public static void removeItemByObjId(L2Playable playable, int item_obj_id, long count)
	{
		if(playable == null || count < 1)
			return;

		L2Player player = playable.getPlayer();
		Inventory inv = player.getInventory();
		if(inv == null)
			return;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
		{
			if(item.getObjectId() != item_obj_id || count <= 0)
				continue;
			long item_count = item.getCount();
			int item_id = item.getItemId();
			long removed = count > item_count ? item_count : count;
			player.destroyItem("RemoveItem", item_obj_id, removed, player, false);

			if(item_id == 57)
				player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(removed));
			else if(removed > 1)
				player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(item_id).addNumber(removed));
			else
				player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item_id));
		}
	}

	/**
	 * Удаляет предметы из инвентаря чара.
	 * @param playable Владелец инвентаря
	 * @param item_id ID предмета
	 * @param count количество
	 */
	public static void removeItem(L2Playable playable, int item_id, long count)
	{
		if(playable == null || count < 1)
			return;

		L2Playable player;
		if(playable.isSummon())
			player = playable.getPlayer();
		else
			player = playable;
		Inventory inv = player.getInventory();
		if(inv == null)
			return;
		long removed = count;
		L2ItemInstance[] items = inv.getItems();
		for(L2ItemInstance item : items)
			if(item.getItemId() == item_id && count > 0)
			{
				long item_count = item.getCount();
				long rem = count <= item_count ? count : item_count;
				player.getInventory().destroyItemByItemId("RemoveItem", item_id, rem, player.getPlayer(), null);
				count -= rem;
			}
		removed -= count;
		if(item_id == 57)
			player.sendPacket(new SystemMessage(SystemMessage.S1_ADENA_DISAPPEARED).addNumber(removed));
		else if(removed > 1)
			player.sendPacket(new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED).addItemName(item_id).addNumber(removed));
		else
			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED).addItemName(item_id));
	}

	public static boolean ride(L2Player player, Integer pet)
	{
		if(player.getMountEngine().isMounted())
		{
			if(player.getMountEngine().getMountNpcId() == player.getTransformation())
				player.setTransformation(0);
			player.getMountEngine().dismount();
		}

		if(player.isPetSummoned())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_HAVE_A_PET));
			return false;
		}

		if(player.getTransformation() != 0)
		{
			player.sendPacket(Msg.YOU_CANNOT_MOUNT_BECAUSE_YOU_DO_NOT_MEET_THE_REQUIREMENTS);
			return false;
		}

		player.getMountEngine().dismount();
		return true;
	}

	public static void unRide(L2Player player)
	{
		if(player.getMountEngine().getMountNpcId() == player.getTransformation())
			player.setTransformation(0);
		player.getMountEngine().dismount();
	}

	public static L2NpcInstance spawn(Location loc, int npcId)
	{
		try
		{
			L2Spawn spawn = new L2Spawn(NpcTable.getTemplate(npcId));
			spawn.setLoc(loc);
			return spawn.doSpawn(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static boolean checkPlayerCondition(L2Player player)
	{
		return player != null && !Olympiad.isRegisteredInComp(player) && !player.isInOlympiadMode() && !player.isInCombat() && !player.inObserverMode() && !player.isInvisible() && !player.isInDuel() && player.isInZonePeace() && player.getReflection() == 0 && !player.isInZone(L2Zone.ZoneType.no_escape) && !player.isInZone(L2Zone.ZoneType.no_summon);
	}
}