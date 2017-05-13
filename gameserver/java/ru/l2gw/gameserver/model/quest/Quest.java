package ru.l2gw.gameserver.model.quest;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.scripts.ScriptFile;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadGame;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.ExShowQuestMark;
import ru.l2gw.gameserver.serverpackets.ExShowScreenMessage;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Files;
import ru.l2gw.util.Location;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class Quest implements ScriptFile
{
	public static final String SOUND_ITEMGET = "ItemSound.quest_itemget";
	public static final String SOUND_ACCEPT = "ItemSound.quest_accept";
	public static final String SOUND_MIDDLE = "ItemSound.quest_middle";
	public static final String SOUND_FINISH = "ItemSound.quest_finish";
	public static final String SOUND_GIVEUP = "ItemSound.quest_giveup";
	public static final String SOUND_TUTORIAL = "ItemSound.quest_tutorial";
	public static final String SOUND_JACKPOT = "ItemSound.quest_jackpot";
	public static final String SOUND_HORROR2 = "SkillSound5.horror_02";
	public static final String SOUND_BEFORE_BATTLE = "Itemsound.quest_before_battle";
	public static final String SOUND_FANFARE_MIDDLE = "ItemSound.quest_fanfare_middle";
	public static final String SOUND_FANFARE1 = "ItemSound.quest_fanfare_1";
	public static final String SOUND_FANFARE2 = "ItemSound.quest_fanfare_2";
	public static final String SOUND_BROKEN_KEY = "ItemSound2.broken_key";
	public static final String SOUND_ENCHANT_SUCESS = "ItemSound3.sys_enchant_sucess";
	public static final String SOUND_ENCHANT_FAILED = "ItemSound3.sys_enchant_failed";
	public static final String SOUND_ED_CHIMES05 = "AmdSound.ed_chimes_05";
	public static final String SOUND_ARMOR_WOOD_3 = "ItemSound.armor_wood_3";
	public static final String SOUND_ITEM_DROP_EQUIP_ARMOR_CLOTH = "ItemSound.item_drop_equip_armor_cloth";

	public static enum QuestEventType
	{
		MOB_TARGETED_BY_SKILL(true), // onSkillUse action triggered when a character uses a skill on a mob
		ON_ATTACKED(true), // onAttack action triggered when a mob attacked by someone
		ON_KILLED(true), // onKill action triggered when a mob killed.
		ON_DECAY(true),
		QUEST_START(true), // onTalk action from start npcs
		QUEST_TALK(true), // onTalk action from npcs participating in a quest
		NPC_FIRST_TALK(false);

		// control whether this event type is allowed for the same npc template
		// in multiple quests
		// or if the npc must be registered in at most one quest for the
		// specified event
		private boolean _allowMultipleRegistration;

		QuestEventType(boolean allowMultipleRegistration)
		{
			_allowMultipleRegistration = allowMultipleRegistration;
		}

		public boolean isMultipleRegistrationAllowed()
		{
			return _allowMultipleRegistration;
		}
	}

	protected static Log _log = LogFactory.getLog("quest");

	/**
	 * HashMap containing events from String value of the event
	 */
	private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();

	/**
	 * HashMap containing lists of timers from the name of the timer
	 */
	private static Map<String, GCSArray<QuestTimer>> _allEventTimers = new FastMap<String, GCSArray<QuestTimer>>().shared();
	private GArray<Integer> _questitems = new GArray<Integer>();

	protected final int _questId;
	protected final String _name;
	protected final String _descr;
	protected final boolean _isCustom;

	public static final int CREATED = 0;
	public static final int STARTED = 1;
	public static final int COMPLETED = 2;

	public static final String[] STATES = {"Start", "Started", "Completed"};

	/**
	 * (Constructor)Add values to class variables and put the quest in HashMaps.
	 *
	 * @param questId : int pointing out the ID of the quest
	 * @param name	: String corresponding to the name of the quest
	 * @param descr   : String for the description of the quest
	 */
	public Quest()
	{
		_name = getClass().getSimpleName();
		_questId = Integer.parseInt(_name.split("_")[1]);
		_descr = "";
		_isCustom = false;

		if(_questId != 0)
			QuestManager.addQuest(this);
		else
			_allEventsS.put(_name, this);
	}

	public Quest(int questId, String name, String descr)
	{
		this(questId, name, descr, false);
	}

	public Quest(int questId, String name, String descr, boolean custom)
	{
		_questId = questId;
		_name = name;
		_descr = descr;
		_isCustom = custom;

		if(questId != 0)
			QuestManager.addQuest(this);
		else
			_allEventsS.put(name, this);
	}

	public void onLoad()
	{
	}

	public void onReload()
	{
	}

	public void onShutdown()
	{
	}

	/**
	 * Этот метод для регистрации квестовых вещей, которые будут удалены
	 * при прекращении квеста, независимо от того, был он закончен или
	 * прерван. <strong>Добавлять сюда награды нельзя</strong>.
	 */
	public void addQuestItem(int... ids)
	{
		for(int id : ids)
			if(id != 0)
				addQuestItem(id);
	}

	/**
	 * Этот метод для регистрации квестовых вещей, которые будут удалены
	 * при прекращении квеста, независимо от того, был он закончен или
	 * прерван. <strong>Добавлять сюда награды нельзя</strong>.
	 */
	public void addQuestItem(int id)
	{
		L2Item i = null;
		try
		{
			i = ItemTable.getInstance().getTemplate(id);
		}
		catch(Exception e)
		{
			_log.warn(this + " Warning: unknown item " + i + " (" + id + ") in quest drop in " + getName());
		}

		if(Config.ALT_SHOW_QUEST_LOAD && (i == null || i.getType2() != L2Item.TYPE2_QUEST))
			_log.warn(this + "Warning: non-quest item " + i + " (" + id + ") in quest drop in " + getName());

		if(Config.ALT_SHOW_QUEST_LOAD && _questitems.contains(id))
			_log.warn(this + "Warning: " + i + " (" + id + ") multiple times in quest drop in " + getName());

		_questitems.add(id);
	}

	public GArray<Integer> getItems()
	{
		return _questitems;
	}

	/**
	 * Update informations regarding quest in database.<BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Get ID state of the quest recorded in object qs</LI>
	 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
	 *
	 * @param qs : QuestState
	 */
	public static void updateQuestInDb(QuestState qs)
	{
		updateQuestVarInDb(qs, "<state>", qs.getState());
	}

	/**
	 * Insert in the database the quest for the player.
	 *
	 * @param qs	: QuestState pointing out the state of the quest
	 * @param var   : String designating the name of the variable for the quest
	 * @param value : String designating the value of the variable for the quest
	 */
	public static void updateQuestVarInDb(QuestState qs, String var, String value)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("REPLACE INTO character_quests (char_id,name,var,value) VALUES (?,?,?,?)");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.setString(3, var);
			statement.setString(4, value);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn(qs.getQuest() + " could not insert char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Delete the player's quest from database.
	 *
	 * @param qs : QuestState pointing out the player's quest
	 */
	public static void deleteQuestInDb(QuestState qs)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn(qs.getQuest() + " could not delete char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Delete a variable of player's quest from the database.
	 *
	 * @param qs  : object QuestState pointing out the player's quest
	 * @param var : String designating the variable characterizing the quest
	 */
	public static void deleteQuestVarInDb(QuestState qs, String var)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? AND name=? AND var=?");
			statement.setInt(1, qs.getPlayer().getObjectId());
			statement.setString(2, qs.getQuest().getName());
			statement.setString(3, var);
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn(qs.getQuest() + " could not delete char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Add quests to the L2PCInstance of the player.<BR><BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2Player
	 *
	 * @param player : Player who is entering the world
	 */
	public static void playerEnter(L2Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement invalidQuestData = null;
		PreparedStatement invalidQuestDataVar = null;
		ResultSet rset = null;
		try
		{
			// Get list of quests owned by the player from database
			con = DatabaseFactory.getInstance().getConnection();

			invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE char_id=? and name=?");
			invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE char_id=? and name=? and var=?");
			statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE char_id=? AND var=?");
			statement.setInt(1, player.getObjectId());
			statement.setString(2, "<state>");
			rset = statement.executeQuery();
			while(rset.next())
			{
				// Get ID of the quest and ID of its state
				String questId = rset.getString("name");
				String state = rset.getString("value");

				if(state.equalsIgnoreCase("Start")) // невзятый квест
				{
					invalidQuestData.setInt(1, player.getObjectId());
					invalidQuestData.setString(2, questId);
					invalidQuestData.executeUpdate();
					continue;
				}

				// Search quest associated with the ID
				Quest q = QuestManager.getQuest(questId);
				if(q == null)
				{
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					else
						_log.warn("Unknown quest " + questId + " for player " + player.getName());
					continue;
				}

				// Create an object State containing the state of the quest
				int stateId = getStateId(state);

				if(stateId < 0)
				{
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestData.setInt(1, player.getObjectId());
						invalidQuestData.setString(2, questId);
						invalidQuestData.executeUpdate();
					}
					else
						_log.warn("Unknown state " + state + " in quest " + questId + " for player " + player.getName());
					continue;
				}
				// Create a new QuestState for the player that will be added to the player's list of quests
				new QuestState(q, player, stateId);
			}
			invalidQuestData.close();
			DbUtils.closeQuietly(statement, rset);

			// Get list of quests owned by the player from the DB in order to add variables used in the quest.
			statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE char_id=?");
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
			{
				String questId = rset.getString("name");
				String var = rset.getString("var");
				String value = rset.getString("value");
				// Get the QuestState saved in the loop before
				QuestState qs = player.getQuestState(questId);
				if(qs == null)
				{
					if(Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuestDataVar.setInt(1, player.getObjectId());
						invalidQuestDataVar.setString(2, questId);
						invalidQuestDataVar.setString(3, var);
						invalidQuestDataVar.executeUpdate();
					}
					else
						_log.warn("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
					continue;
				}
				// Add parameter to the quest
				qs.setInternal(var, value);
			}
		}
		catch(Exception e)
		{
			_log.warn("could not insert char quest:", e);
		}
		finally
		{
			DbUtils.closeQuietly(invalidQuestData);
			DbUtils.closeQuietly(invalidQuestDataVar);
			DbUtils.closeQuietly(con, statement, rset);
		}

		// events
		for(String name : _allEventsS.keySet())
			player.processQuestEvent(name, "enter");
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for Attack Events.<BR>
	 * <BR>
	 *
	 * @param attackId
	 * @return int : attackId
	 */
	public L2NpcTemplate addAttackId(int attackId)
	{
		return addEventId(attackId, QuestEventType.ON_ATTACKED);
	}

	public void addAttackId(int[] attackIds)
	{
		for(int attackId : attackIds)
			addEventId(attackId, QuestEventType.ON_ATTACKED);
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for the specified Event type.<BR>
	 * <BR>
	 *
	 * @param npcId	 : id of the NPC to register
	 * @param eventType : type of event being registered
	 * @return int : npcId
	 */
	public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
	{
		try
		{
			L2NpcTemplate t = NpcTable.getTemplate(npcId);
			if(t != null)
				t.addQuestEvent(eventType, this);
			return t;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public void addKillId(int... killIds)
	{
		for(int killid : killIds)
			addEventId(killid, QuestEventType.ON_KILLED);
	}

	public void addKillId(Set<Integer> npcIds)
	{
		for(int npcId : npcIds)
			addEventId(npcId, QuestEventType.ON_KILLED);
	}

	public void addStartNpc(int[] npcIds)
	{
		for(int npcId : npcIds)
			addEventId(npcId, QuestEventType.QUEST_START);
	}

	/**
	 * Add this quest to the list of quests that the passed mob will respond to
	 * for Kill Events.<BR>
	 * <BR>
	 *
	 * @param killId
	 * @return int : killId
	 */
	public L2NpcTemplate addKillId(int killId)
	{
		return addEventId(killId, QuestEventType.ON_KILLED);
	}

	public void addDecayId(int... decayIds)
	{
		for(int decayId : decayIds)
			addDecayId(decayId);
	}

	public L2NpcTemplate addDecayId(int decayId)
	{
		return addEventId(decayId, QuestEventType.ON_DECAY);
	}

	/**
	 * Add this quest to the list of quests that the passed npc will respond to
	 * for Skill-Use Events.<BR>
	 * <BR>
	 *
	 * @param npcId : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate addSkillUseId(int npcId)
	{
		return addEventId(npcId, QuestEventType.MOB_TARGETED_BY_SKILL);
	}

	/**
	 * Add the quest to the NPC's startQuest
	 * Вызывает addTalkId
	 *
	 * @param npcId
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate addStartNpc(int npcId)
	{
		addTalkId(npcId);
		return addEventId(npcId, QuestEventType.QUEST_START);
	}

	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 *
	 * @param npcId
	 * @return L2NpcTemplate : Start NPC
	 */
	public L2NpcTemplate addFirstTalkId(int npcId)
	{
		return addEventId(npcId, QuestEventType.NPC_FIRST_TALK);
	}

	public void addFirstTalkId(int[] npcIds)
	{
		for(int npcId : npcIds)
			addFirstTalkId(npcId);
	}

	/**
	 * Add this quest to the list of quests that the passed npc will respond to
	 * for Talk Events.<BR>
	 * <BR>
	 *
	 * @param talkId : ID of the NPC
	 * @return int : ID of the NPC
	 */
	public L2NpcTemplate addTalkId(int talkId)
	{
		return addEventId(talkId, Quest.QuestEventType.QUEST_TALK);
	}

	public void addTalkId(int... talkIds)
	{
		for(int talkId : talkIds)
			addTalkId(talkId);
	}

	public void cancelQuestTimer(String name, L2NpcInstance npc, L2Player player)
	{
		QuestTimer timer = getQuestTimer(name, npc, player);
		if(timer != null)
			timer.cancel();

		removeQuestTimer(timer);
	}

	/**
	 * Return description of the quest
	 *
	 * @return String
	 */
	public String getDescr()
	{
		return _descr;
	}

	/**
	 * Return name of the quest
	 *
	 * @return String
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * Return ID of the quest
	 *
	 * @return int
	 */
	public int getQuestIntId()
	{
		return _questId;
	}

	/*
	Return false if quest is not global (by default)
	 */
	public boolean isGlobal()
	{
		return false;
	}

	public QuestTimer getQuestTimer(String name, L2NpcInstance npc, L2Player player)
	{
		if(_allEventTimers.get(name) == null)
			return null;
		for(QuestTimer timer : _allEventTimers.get(name))
			if(timer.isMatch(this, name, npc, player))
				return timer;
		return null;
	}

	public GCSArray<QuestTimer> getQuestTimers(String name)
	{
		return _allEventTimers.get(name);
	}

	/**
	 * Add a new QuestState to the database and return it.
	 *
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2Player player)
	{
		QuestState qs = new QuestState(this, player, 0);
		Quest.updateQuestInDb(qs);
		return qs;
	}

	public void notifyAttack(L2NpcInstance npc, QuestState qs, L2Skill skill)
	{
		String res;
		try
		{
			res = onAttack(npc, qs, skill);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyAttack(L2NpcInstance npc, L2Player player, L2Skill skill)
	{
		String res;
		try
		{
			res = onAttack(npc, player, skill);
		}
		catch(Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}

	public void notifyDecayd(L2NpcInstance npc)
	{
		try
		{
			onDecay(npc);
		}
		catch(Exception e)
		{
		}
	}

	public void notifyDeath(L2NpcInstance killer, L2Character victim, QuestState qs)
	{
		String res;
		try
		{
			res = onDeath(killer, victim, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyEvent(String event, QuestState qs)
	{
		String res;
		try
		{
			res = onEvent(event, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return;
		}
		showResult(qs.getPlayer(), res);
	}

	public void notifyEvent(String event, L2NpcInstance npc, L2Player player)
	{
		String res;
		try
		{
			res = onEvent(event, npc, player);
		}
		catch(Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(player, res);
	}

	public void notifyKill(L2NpcInstance npc, QuestState qs)
	{
		try
		{
			onKill(npc, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
		}
	}

	/**
	 * @param npc
	 * @param player
	 * @return always null not recomanded for typical quest
	 */
	public void notifyKill(L2NpcInstance npc, L2Player player)
	{
		try
		{
			onKill(npc, player);
		}
		catch(Exception e)
		{
			showError(player, e);
		}
	}

	/**
	 * Override the default NPC dialogs when a quest defines this for the given NPC
	 */
	public final boolean notifyFirstTalk(L2NpcInstance npc, L2Player player)
	{
		String res;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch(Exception e)
		{
			showError(player, e);
			return true;
		}
		player.setLastNpc(npc);
		// if the quest returns text to display, display it. Otherwise, use the default npc text.
		return showResult(player, res);
	}

	public boolean notifyTalk(L2NpcInstance npc, QuestState qs)
	{
		String res;
		try
		{
			res = onTalk(npc, qs);
		}
		catch(Exception e)
		{
			showError(qs.getPlayer(), e);
			return true;
		}
		qs.getPlayer().setLastNpc(npc);

		return showResult(qs.getPlayer(), res);
	}

	@SuppressWarnings("unused")
	public String onAttack(L2NpcInstance npc, QuestState qs, L2Skill skill)
	{
		return null;
	}

	public String onAttack(L2NpcInstance npc, L2Player player, L2Skill skill)
	{
		QuestState qs = player.getQuestState(getName());
		if(qs != null && !qs.isCompleted())
			notifyAttack(npc, qs, skill);

		return null;
	}

	@SuppressWarnings("unused")
	public void onDecay(L2NpcInstance npc)
	{
	}

	@SuppressWarnings("unused")
	public String onDeath(L2NpcInstance killer, L2Character victim, QuestState qs)
	{
		return null;
	}

	@SuppressWarnings("unused")
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}

	@SuppressWarnings("unused")
	public String onEvent(String event, L2NpcInstance npc, L2Player player)
	{
		return null;
	}

	@SuppressWarnings("unused")
	public void onKill(L2NpcInstance npc, QuestState qs)
	{
	}

	/**
	 * @param npc
	 * @param player
	 * @return
	 */
	public void onKill(L2NpcInstance npc, L2Player player)
	{
		QuestState qs = player.getQuestState(getName());
		if(qs != null && !qs.isCompleted())
			notifyKill(npc, qs);
	}

	@SuppressWarnings("unused")
	public void onPlayerKill(L2Player killer, L2Player killed)
	{
	}

	@SuppressWarnings("unused")
	public void onPlayerKillParty(L2Player killer, L2Player killed, QuestState qs)
	{
	}

	@SuppressWarnings("unused")
	public String onFirstTalk(L2NpcInstance npc, L2Player player)
	{
		return null;
	}

	@SuppressWarnings("unused")
	public String onTalk(L2NpcInstance npc, QuestState qs)
	{
		return null;
	}

	public void onOlympiadEnd(OlympiadGame og, QuestState qs)
	{
	}

	public void removeQuestTimer(QuestTimer timer)
	{
		if(timer == null)
			return;
		GCSArray<QuestTimer> timers = getQuestTimers(timer.getName());
		if(timers == null || timers.size() < 1)
			return;
		timers.remove(timer);
	}

	public void onQuestSelect(int reply, L2Player player)
	{
		QuestState qs = player.getQuestState(getName());
		if(qs == null)
		{
			if(!isCustom())
			{
				if(!player.isQuestContinuationPossible())
					return;

				if(player.getQuestCount() >= Config.ALT_MAX_QUESTS)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(player.getLastNpc().getObjectId());
					html.setFile("data/html/fullquest.htm");
					player.sendPacket(html);
					return;
				}
			}

			qs = newQuestState(player);
		}
		else if(!qs.getQuest().isCustom() && !player.isQuestContinuationPossible())
			return;

		if(qs == null)
			return;

		onQuestSelect(reply, qs);
	}

	public void onQuestSelect(int reply, QuestState qs)
	{}

	public void onQuestStart(L2Player talker)
	{}

	/**
	 * Show message error to player who has an access level greater than 0
	 *
	 * @param player : L2PcInstance
	 * @param t	  : Throwable
	 */
	private void showError(L2Player player, Throwable t)
	{
		_log.warn(this + " error: " + t);
		t.printStackTrace();
		if(player.isGM())
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			String res = "<html><body><title>Script error</title>" + sw.toString() + "</body></html>";
			showResult(player, res);
		}
	}

	public void showPage(String file, L2Player player)
	{
	 	showResult(player, "npchtm:" + file);
	}

	public void showQuestPage(String file, L2Player player)
	{
	 	showResult(player, file);
	}

	public void showQuestMark(L2Player player)
	{
		player.sendPacket(new ExShowQuestMark(getQuestIntId()));
	}

	public static void showQuestMark(L2Player player, int questId)
	{
		player.sendPacket(new ExShowQuestMark(questId));
	}

	public String showHtmlFile(L2Player player, String fileName, String toReplace[], String replaceWith[])
	{
		return showHtmlFile(player, fileName, toReplace, replaceWith, true);
	}
	
	public String showHtmlFile(L2Player player, String fileName, String toReplace[], String replaceWith[], boolean questWindow)
	{
		String content;

		// for scripts
		if(fileName.contains("/"))
			content = Files.read(fileName, player);
		else
		{
			String _path = getClass().toString();
			_path = _path.substring(6, _path.lastIndexOf(".")) + ".";
			content = Files.read("data/scripts/" + _path.replace(".", "/") + fileName, player);
		}

		if(content == null)
			content = "Can't find file '" + fileName + "'";

		if(player != null && player.getTarget() != null)
			content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));

		// Make a replacement inside before sending html to client
		if(toReplace != null && replaceWith != null && toReplace.length == replaceWith.length)
			for(int i = 0; i < toReplace.length; i++)
			{
				if(toReplace[i] != null && replaceWith[i] != null)
					content = content.replace(toReplace[i], Matcher.quoteReplacement(replaceWith[i]));
			}

		// Send message to client if message not empty
		if(content != null && player != null)
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			if(questWindow && !isCustom())
				npcReply.setQuest(getQuestIntId());
			npcReply.setHtml(content);
			player.sendPacket(npcReply);
		}

		return content;
	}

	/**
	 * Show a message to player.<BR><BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :<BR>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message hold in "res" is shown in chat box</LI>
	 *
	 * @param player : QuestState
	 * @param res	: String pointing out the message to show at the player
	 * @return boolean
	 */
	private boolean showResult(L2Player player, String res)
	{
		if(res == null)
			return true;
		if(res.isEmpty())
			return false;

		boolean questWindow = true;
		if(res.startsWith("npchtm:"))
		{
			res = res.replaceFirst("npchtm:", "");
			questWindow = false;
		}

		if(res.startsWith("no_quest") || res.equalsIgnoreCase("noquest") || res.equalsIgnoreCase("no-quest"))
			showHtmlFile(player, "data/html/no-quest.htm", null, null, false);
		else if(res.startsWith("highlevel"))
			showHtmlFile(player, "data/html/highlevel.htm", null, null, false);
		else if(res.equalsIgnoreCase("completed"))
			showHtmlFile(player, "data/html/completed-quest.htm", null, null, false);
		else if(res.endsWith(".htm"))
			showHtmlFile(player, res, null, null, questWindow);
		else
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			if(questWindow && !isCustom())
				npcReply.setQuest(getQuestIntId());
			npcReply.setHtml(res);
			player.sendPacket(npcReply);
		}
		return true;
	}

	/**
	 * Add a timer to the quest, if it doesn't exist already
	 *
	 * @param name:   name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time:   time in ms for when to fire the timer
	 * @param npc:    npc associated with this timer (can be null)
	 * @param player: player associated with this timer (can be null)
	 */
	public void startQuestTimer(String name, long time, L2NpcInstance npc, L2Player player)
	{
		startQuestTimer(name, time, npc, player, false);
	}

	public void startQuestTimer(String name, long time, L2NpcInstance npc, L2Player player, boolean isGlobal)
	{
		// Add quest timer if timer doesn't already exist
		GCSArray<QuestTimer> timers = getQuestTimers(name);
		if(timers == null)
		{
			timers = new GCSArray<QuestTimer>();
			timers.add(new QuestTimer(this, name, time, npc, player, isGlobal));
			_allEventTimers.put(name, timers);
		}
		// a timer with this name exists, but may not be for the same set of npc and player
		else // if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
			// nulls act as wildcards
			if(getQuestTimer(name, npc, player) == null)
				timers.add(new QuestTimer(this, name, time, npc, player, isGlobal));
	}

	protected String str(long i)
	{
		return String.valueOf(i);
	}

	protected boolean isdigit(String s)
	{
		try
		{
			Integer.parseInt(s);
		}
		catch(NumberFormatException e)
		{
			return false;
		}
		return true;
	}

	public final void saveGlobalQuestVar(String var, String value)
	{
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.setString(3, value);
			statement.executeUpdate();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn(this + " could not insert global quest variable:", e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	public final String loadGlobalQuestVar(String var)
	{
		String result = "";
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			ResultSet rs = statement.executeQuery();
			if(rs.first())
				result = rs.getString(1);
			rs.close();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn(this + "could not load global quest variable:" + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
		return result;
	}

	public final void deleteGlobalQuestVar(String var)
	{
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement;
			statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
			statement.setString(1, getName());
			statement.setString(2, var);
			statement.executeUpdate();
			statement.close();
		}
		catch(Exception e)
		{
			_log.warn(this + " could not delete global quest variable: " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
			}
		}
	}

	public L2NpcInstance addSpawn(int npcId, L2Character cha)
	{
		return addSpawn(npcId, new Location(cha.getX(), cha.getY(), cha.getZ(), cha.getHeading()), false, 0);
	}

	public L2NpcInstance addSpawn(int npcId, Location loc)
	{
		return addSpawn(npcId, loc, false, 0);
	}

	public L2NpcInstance addSpawn(int npcId, Location loc, boolean randomOffset)
	{
		return addSpawn(npcId, loc, randomOffset, 0);
	}

	public L2NpcInstance addSpawn(int npcId, Location loc, boolean randomOffset, int despawnDelay)
	{
		L2NpcInstance result;
		try
		{
			L2NpcTemplate template = NpcTable.getTemplate(npcId);
			if(template != null)
			{
				// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
				// reaches here, xyz have become 0!  Also, a questdev might have purposely set xy to 0,0...however,
				// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc!  This will NOT work
				// with quest spawns!  For both of the above cases, we need a fail-safe spawn.  For this, we use the
				// default spawn location, which is at the player's loc.
				if((loc.getX() == 0) && (loc.getY() == 0))
				{
					_log.warn(this + " Failed to adjust bad locks for quest spawn!  Spawn aborted!");
					return null;
				}

				if(randomOffset)
					loc = Location.coordsRandomize(loc, 150);

				L2Spawn spawn = new L2Spawn(template);
				spawn.setLoc(loc);
				spawn.stopRespawn();
				result = spawn.spawnOne();

				if(despawnDelay > 0)
					ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);

				return result;
			}
		}
		catch(Exception e1)
		{
			_log.warn(this + " Could not spawn Npc " + npcId);
		}

		return null;
	}

	private class DeSpawnScheduleTimerTask implements Runnable
	{
		L2NpcInstance _npc = null;

		public DeSpawnScheduleTimerTask(L2NpcInstance npc)
		{
			_npc = npc;
		}

		public void run()
		{
			onDespawned(_npc);
			_npc.onDecay();
		}
	}

	public void onDespawned(L2NpcInstance npc)
	{
	}

	public static void giveQuestForTerritory(int terrId, String questName, String startMessage)
	{
		GArray<L2Player> players = new GArray<L2Player>();

		for(int objectId : TerritoryWarManager.getRegisteredMerc(terrId))
		{
			L2Player member = L2ObjectsStorage.getPlayer(objectId);
			if(member != null && member.getQuestState(questName) == null)
				players.add(member);
		}

		for(int clanId : TerritoryWarManager.getRegisteredClans(terrId))
		{
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if(clan != null)
				for(L2Player member : clan.getOnlineMembers(""))
					if(member != null && member.getQuestState(questName) == null)
						players.add(member);
		}

		ExShowScreenMessage message = null;
		if(startMessage != null)
			message = new ExShowScreenMessage(startMessage, 5000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER);

		for(L2Player member : players)
		{
			Quest q = QuestManager.getQuest(questName);
			if(q != null)
			{
				QuestState qs = q.newQuestState(member);
				qs.set("cond", "1");
				qs.setState(STARTED);
				member.setVar("twq_" + q.getQuestIntId(), "true", (int) (TerritoryWarManager.getWar().getWardEndDate() / 1000) + 60);
				if(message != null)
					member.sendPacket(message);
			}
		}
	}

	public static void removeQuestForTerritory(int terrId, String questName, String endMessage)
	{
		ExShowScreenMessage message = null;
		if(endMessage != null)
			message = new ExShowScreenMessage(endMessage, 5000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER);

		for(int objectId : TerritoryWarManager.getRegisteredMerc(terrId))
		{
			L2Player member = L2ObjectsStorage.getPlayer(objectId);
			if(member != null && member.getQuestState(questName) != null)
			{
				member.getQuestState(questName).exitCurrentQuest(true);
				if(message != null)
					member.sendPacket(message);
			}
		}

		for(int clanId : TerritoryWarManager.getRegisteredClans(terrId))
		{
			L2Clan clan = ClanTable.getInstance().getClan(clanId);
			if(clan != null)
				for(L2Player member : clan.getOnlineMembers(""))
					if(member != null && member.getQuestState(questName) != null)
					{
						member.getQuestState(questName).exitCurrentQuest(true);
						if(message != null)
							member.sendPacket(message);
					}
		}
	}

	public static int getStateId(String state)
	{
		for(int i = 0; i < STATES.length; i++)
			if(STATES[i].equals(state))
				return i;

		return -1;
	}

	private QuestState checkQuestState(L2Player player, int cond)
	{
		QuestState qs = player.getQuestState(getName());
		if(qs != null && (cond < 0 || qs.getCond() == cond))
			return qs;
		return null;
	}

	public GArray<QuestState> getPartyMembersWithQuest(L2Player killer, int cond)
	{
		GArray<QuestState> members = new GArray<QuestState>(9);
		L2Party party = killer.getParty();
		QuestState qs;
		if(party != null)
			for(L2Player member : party.getPartyMembers())
			{
				if(killer.isInRange(member, Config.ALT_PARTY_DISTRIBUTION_RANGE) && (qs = checkQuestState(member, cond)) != null)
					members.add(qs);
			}
		else if((qs = checkQuestState(killer, cond)) != null)
			members.add(qs);

		return members;
	}

	public QuestState getRandomPartyMemberWithQuest(L2Player killer, int cond)
	{
		GArray<QuestState> members = getPartyMembersWithQuest(killer, cond);
		if(members.size() > 0)
			return members.get(Rnd.get(members.size()));

		return null;
	}

	private QuestState checkMemoState(L2Player player, int minState, int maxState)
	{
		QuestState qs = player.getQuestState(getName());
		if(qs != null && (minState < 0 || qs.getMemoState() >= minState && qs.getMemoState() <= maxState))
			return qs;
		return null;
	}

	public GArray<QuestState> getPartyMembersWithMemoState(L2Player killer, int state)
	{
		return getPartyMembersWithMemoState(killer, state, state);
	}

	public GArray<QuestState> getPartyMembersWithMemoState(L2Player killer, int minState, int maxState)
	{
		GArray<QuestState> members = new GArray<>(9);
		L2Party party = killer.getParty();
		QuestState qs;
		if(party != null)
			for(L2Player member : party.getPartyMembers())
			{
				if(killer.isInRange(member, Config.ALT_PARTY_DISTRIBUTION_RANGE) && (qs = checkMemoState(member, minState, maxState)) != null)
					members.add(qs);
			}
		else if((qs = checkMemoState(killer, minState, maxState)) != null)
			members.add(qs);

		return members;
	}

	public QuestState getRandomPartyMemberWithMemoState(L2Player killer, int state)
	{
		GArray<QuestState> members = getPartyMembersWithMemoState(killer, state);
		if(members.size() > 0)
			return members.get(Rnd.get(members.size()));

		return null;
	}

	public QuestState getRandomPartyMemberWithMemoState(L2Player killer, int minState, int maxState)
	{
		GArray<QuestState> members = getPartyMembersWithMemoState(killer, minState, maxState);
		if(members.size() > 0)
			return members.get(Rnd.get(members.size()));

		return null;
	}

	public boolean isCustom()
	{
		return _isCustom;
	}

	public static boolean contains(int[] array, int id)
	{
		for(int i : array)
			if(i == id)
				return true;
		return false;
	}

	@Override
	public String toString()
	{
		return _name;
	}
}
