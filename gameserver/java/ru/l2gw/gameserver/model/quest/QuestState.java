package ru.l2gw.gameserver.model.quest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.crontab.Crontab;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.util.Files;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestState
{
	protected static Log _log = LogFactory.getLog(Quest.class.getName());

	private static Crontab _dayleReuse = new Crontab("30 6 * * *");
	/**
	 * Player who engaged the quest
	 */
	private L2Player _player;

	/**
	 * Quest associated to the QuestState
	 */
	private Quest _quest;

	/**
	 * State of the quest
	 */
	private int _stateId;

	/**
	 * List of couples (variable for quest,value of the variable for quest)
	 */
	private Map<String, String> _vars = new ConcurrentHashMap<>();

	/**
	 * Constructor of the QuestState : save the quest in the list of quests of the player.<BR/><BR/>
	 * <p/>
	 * <U><I>Actions :</U></I><BR/>
	 * <LI>Save informations in the object QuestState created (Quest, Player, Completion, State)</LI>
	 * <LI>Add the QuestState in the player's list of quests by using setQuestState()</LI>
	 * <LI>Add drops gotten by the quest</LI>
	 * <BR/>
	 *
	 * @param quest  : quest associated with the QuestState
	 * @param player : L2Player pointing out the player
	 */
	public QuestState(Quest quest, L2Player player, int stateId)
	{
		_quest = quest;
		_player = player;

		// Save the state of the quest for the player in the player's list of quest onwed
		_player.setQuestState(this);
		_stateId = stateId;
	}

	/**
	 * Add XP and SP as quest reward
	 * <br><br>
	 * Метод учитывает рейты!
	 */
	public void addExpAndSp(long exp, long sp)
	{
		if(exp > 0)
			_player.addExpAndSp((long)(exp * Config.RATE_QUESTS_EXPSP), 0);
		if(sp > 0)
			_player.addExpAndSp(0, (long)(sp * Config.RATE_QUESTS_EXPSP));
	}

	/**
	 * Add player to get notification of characters death
	 *
	 * @param character : L2Character of the character to get notification of death
	 */
	public void addNotifyOfDeath(L2Character character)
	{
		if(character == null)
			return;
		character.addNotifyQuestOfDeath(this);
	}

	/**
	 * Возвращает истину, если у игрока есть хоть один предмет
	 *
	 * @param itemId : ID вещи
	 */
	public boolean haveQuestItems(int itemId)
	{
		return haveQuestItems(itemId, 1);
	}

	/**
	 * Возвращает истину, если указанных вещей не менее
	 * требуемого значения
	 *
	 * @param itemId   : ID вещи
	 * @param quantity : требуемое количество
	 */
	public boolean haveQuestItems(int itemId, long quantity)
	{
		return _player.getItemCountByItemId(itemId) >= quantity;
	}

	/**
	 * Destroy element used by quest when quest is exited
	 *
	 * @param repeatable
	 * @return QuestState
	 */
	public QuestState exitCurrentQuest(boolean repeatable)
	{
		return exitCurrentQuest(repeatable, false);
	}

	public QuestState exitCurrentQuest(boolean repeatable, boolean dayle)
	{
		// Clean drops
		if(_quest.getItems() != null)
			// Go through values of class variable "drops" pointing out mobs that drop for quest
			for(Integer itemId : _quest.getItems())
			{
				// Get [item from] / [presence of the item in] the inventory of the player
				L2ItemInstance item = _player.getInventory().getItemByItemId(itemId);
				if(item == null || itemId == 57)
					continue;
				long count = item.getCount();
				// If player has the item in inventory, destroy it (if not gold)
				_player.destroyItemByItemId("ExitQuest", itemId, count, null, true);
				_player.getWarehouse().destroyItemByItemId("ExitQuest", itemId, count, _player, null);
			}

		// If quest is repeatable, delete quest from list of quest of the player and from database (quest CAN be created again => repeatable)
		if(repeatable)
		{
			_player.delQuestState(_quest.getName());
			Quest.deleteQuestInDb(this);
			_vars = null;
		}
		else
		{ // Otherwise, delete variables for quest and update database (quest CANNOT be created again => not repeatable)
			try
			{
				if(_vars != null && !_vars.isEmpty())
					for(String var : _vars.keySet())
						unset(var);
			}
			catch(NullPointerException e)
			{
			}
			setState(Quest.COMPLETED);
			Quest.updateQuestInDb(this);
			if(dayle)
				set("<dayle>", (int) (_dayleReuse.timeNextUsage(System.currentTimeMillis()) / 1000));
		}
		_player.sendPacket(new QuestList(_player));
		return this;
	}

	/**
	 * Return the value of the variable of quest represented by "var"
	 *
	 * @param var : name of the variable of quest
	 * @return Object
	 */
	public Object get(String var)
	{
		if(_vars == null)
			return null;
		return _vars.get(var);
	}

	/**
	 * Return the value of the variable of quest represented by "var"
	 *
	 * @param var : String designating the variable for the quest
	 * @return int
	 */
	public int getInt(String var)
	{
		if(_vars == null)
			return 0;

		int varint = 0;
		try
		{
			if(_vars.get(var) != null)
				varint = Integer.parseInt(_vars.get(var));
		}
		catch(Exception e)
		{
			_log.warn(getPlayer().getName() + ": variable " + var + " isn't an integer: " + _vars.get(var) + " " + e);
		}
		return varint;
	}

	/**
	 * Return all quest variables
	 *
	 * @return Map<String,String>
	 */
	public Map<String, String> getAllVars()
	{
		return _vars;
	}

	/**
	 * Return item number which is equipped in selected slot
	 *
	 * @return int
	 */
	public int getItemEquipped(int loc)
	{
		return getPlayer().getInventory().getPaperdollItemId(loc);
	}

	/**
	 * NOTE: This is to be deprecated; replaced by
	 * Quest.getPcSpawn(L2Player) For now, I shall leave it as is Return a
	 * QuestPcSpawn for current player instance
	 */
	public final QuestPcSpawn getPcSpawn()
	{
		return QuestPcSpawnManager.getInstance().getPcSpawn(getPlayer());
	}

	/**
	 * Return the L2Player
	 *
	 * @return L2Player
	 */
	public L2Player getPlayer()
	{
		return _player;
	}

	/**
	 * Return the quest
	 *
	 * @return Quest
	 */
	public Quest getQuest()
	{
		return _quest;
	}

	/**
	 * Return the quantity of one sort of item hold by the player
	 *
	 * @param itemId : ID of the item wanted to be count
	 * @return int
	 */
	public long getQuestItemsCount(int itemId)
	{
		return _player.getInventory().getCountOf(itemId);
	}

	/**
	 * Return the QuestTimer object with the specified name
	 *
	 * @return QuestTimer<BR> Return null if name does not exist
	 */
	public final QuestTimer getQuestTimer(String name)
	{
		return getQuest().getQuestTimer(name, null, getPlayer());

	}

	public String getState()
	{
		return Quest.STATES[_stateId];
	}

	/**
	 * Добавить предмет игроку
	 * By default if item is adena rates 'll be applyed, else no
	 *
	 * @param itemId
	 * @param count
	 */
	public void giveItems(int itemId, long count)
	{
		_player.addItem("Quest", itemId, count, null, true);
	}

	public void rateAndGive(int itemId, long count)
	{
		long resultCount = itemId == 57 ? (long) (count * Config.RATE_QUESTS_DROP_ADENA) : (long) (count * Config.RATE_QUESTS_DROP_REWARD);
		_player.addItem("Quest", itemId, resultCount, null, true);
	}

	public boolean rollAndGive(int itemId, long count, double chance)
	{
		chance *= Config.RATE_QUESTS_DROP_CHANCE;
		if(Rnd.chance(chance))
		{
			long resultCount = itemId == 57 ? (long) (count * Config.RATE_QUESTS_DROP_ADENA) : Rnd.get(count, (long) (count * Config.RATE_QUESTS_DROP_REWARD));
			_player.addItem("Quest", itemId, resultCount, null, true);
			return true;
		}
		return false;
	}

	public boolean rollAndGiveLimited(int itemId, long count, double chance, long limit)
	{
		chance *= Config.RATE_QUESTS_DROP_CHANCE;
		if(Rnd.chance(chance))
		{
			long resultCount = itemId == 57 ? (long) (count * Config.RATE_QUESTS_DROP_ADENA) : Rnd.get(count, (long) (count * Config.RATE_QUESTS_DROP_REWARD));

			if(_player.getItemCountByItemId(itemId) + resultCount > limit)
				resultCount = limit - _player.getItemCountByItemId(itemId);

			if(resultCount < 1)
				return false;

			_player.addItem("Quest", itemId, Rnd.get(count, resultCount), null, true);
			return true;
		}
		return false;
	}

	/**
	 * Return true if quest completed, false otherwise
	 *
	 * @return boolean
	 */
	public boolean isCompleted()
	{
		int dayle = getInt("<dayle>");
		if(dayle > 0 && dayle * 1000L < System.currentTimeMillis())
		{
			unset("<dayle>");
			if(_stateId == Quest.COMPLETED)
			{
				_stateId = Quest.CREATED;
				Quest.updateQuestInDb(this);
			}
		}
		return _stateId == Quest.COMPLETED;
	}

	/**
	 * Return true if quest just created, false otherwise
	 *
	 * @return boolean
	 */
	public boolean isCreated()
	{
		int dayle = getInt("<dayle>");
		if(dayle > 0 && dayle * 1000L < System.currentTimeMillis())
		{
			unset("<dayle>");
			if(_stateId == Quest.COMPLETED)
			{
				_stateId = Quest.CREATED;
				Quest.updateQuestInDb(this);
			}
		}

		return _stateId == Quest.CREATED;
	}

	/**
	 * Return true if quest started, false otherwise
	 *
	 * @return boolean
	 */
	public boolean isStarted()
	{
		return _stateId == Quest.STARTED;
	}

	/**
	 * Return value of parameter "val" after adding the couple (var,val) in class variable "vars".<BR><BR>
	 * <U><I>Actions :</I></U><BR>
	 * <LI>Initialize class variable "vars" if is null</LI>
	 * <LI>Initialize parameter "val" if is null</LI>
	 * <LI>Add/Update couple (var,val) in class variable FastMap "vars"</LI>
	 * <LI>If the key represented by "var" exists in FastMap "vars", the couple (var,val) is updated in the database. The key is known as
	 * existing if the preceding value of the key (given as result of function put()) is not null.<BR>
	 * If the key doesn't exist, the couple is added/created in the database</LI>
	 *
	 * @param var : String indicating the name of the variable for quest
	 * @param val : String indicating the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	public String set(String var, String val)
	{
		if(_vars == null)
			_vars = new ConcurrentHashMap<>();
		if(val == null)
			val = "";
		_vars.put(var, val);
		Quest.updateQuestVarInDb(this, var, val);
		if(var.equals("cond"))
			_player.sendPacket(new QuestList(_player));
		return val;
	}

	public void set(String var, int val)
	{
		set(var, String.valueOf(val));
	}

	public void setCond(int val)
	{
		set("cond", val);
	}

	public int getCond()
	{
		return getInt("cond");
	}

	public void setMemoState(int state)
	{
		set("ex_cond", state);
	}

	public void setMemoStateEx(int id, int state)
	{
		set("ex_" + id, state);
	}

	public int getMemoState()
	{
		return getInt("ex_cond");
	}

	public int getMemoStateEx(int id)
	{
		return getInt("ex_" + id);
	}

	/**
	 * Add parameter used in quests.
	 *
	 * @param var : String pointing out the name of the variable for quest
	 * @param val : String pointing out the value of the variable for quest
	 * @return String (equal to parameter "val")
	 */
	public String setInternal(String var, String val)
	{
		if(_vars == null)
			_vars = new ConcurrentHashMap<>();
		if(val == null)
			val = "";
		_vars.put(var, val);
		return val;
	}

	/**
	 * Return state of the quest after its initialization.<BR><BR>
	 * <U><I>Actions :</I></U>
	 * <LI>Remove drops from previous state</LI>
	 * <LI>Set new state of the quest</LI>
	 * <LI>Add drop for new state</LI>
	 * <LI>Update information in database</LI>
	 * <LI>Send packet QuestList to client</LI>
	 *
	 * @param state
	 * @return object
	 */
	public Object setState(int state)
	{
		return setState(state, true);
	}

	public Object setState(int state, boolean sendMark)
	{
		// set new state
		_stateId = state;

		if(sendMark && !getQuest().isCustom() && isStarted())
			_player.sendPacket(new ExShowQuestMark(getQuest().getQuestIntId()));

		Quest.updateQuestInDb(this);
		_player.sendPacket(new QuestList(_player));
		return state;
	}

	/**
	 * Send a packet in order to play sound at client terminal
	 *
	 * @param sound
	 */
	public void playSound(String sound)
	{
		_player.sendPacket(new PlaySound(sound));
	}

	public void playTutorialVoice(String voice)
	{
		playTutorialVoice(voice, 0);
	}

	public void playTutorialVoice(String voice, int unk)
	{
		_player.sendPacket(new PlaySound(2, voice, 0, 0, _player.getLoc(), unk));
	}

	public void onTutorialClientEvent(int number)
	{
		_player.sendPacket(new TutorialEnableClientEvent(number));
	}

	public void closeTutorialHTML()
	{
		_player.sendPacket(Msg.TutorialCloseHtml);
	}

	public void showQuestionMark(int number)
	{
		_player.sendPacket(new TutorialShowQuestionMark(number));
	}

	public void showTutorialHTML(String html)
	{
		String text = Files.read("data/scripts/quests/_255_Tutorial/" + html, _player);
		if(text == null || text.equalsIgnoreCase(""))
			text = "<html><body>File data/scripts/quests/_255_Tutorial/" + html + " not found or file is empty.</body></html>";
		_player.sendPacket(new TutorialShowHtml(text));
	}

	public void showRadar(int x, int y, int z, int type)
	{
		_player.radar.showRadar(x, y, z, type);
	}

	public void deleteRadar(int x, int y, int z, int type)
	{
		_player.radar.removeMarker(x, y, z, type);
	}

	public void showSocial(int social)
	{
		_player.sendPacket(new SocialAction(_player.getObjectId(), social));
	}

	/**
	 * Start a timer for quest.<BR><BR>
	 *
	 * @param name<BR> The name of the timer. Will also be the value for event of onEvent
	 * @param time<BR> The milisecond value the timer will elapse
	 */
	public void startQuestTimer(String name, long time)
	{
		getQuest().startQuestTimer(name, time, null, getPlayer());
	}

	/**
	 * Удаляет указанные предметы из инвентаря игрока, и обновляет инвентарь
	 *
	 * @param itemId : id удаляемого предмета
	 * @param count  : число удаляемых предметов<br>
	 *               Если count передать -1, то будут удалены все указанные предметы.
	 * @return Количество удаленных предметов
	 */
	public long takeItems(int itemId, long count)
	{
		// Get object item from player's inventory list
		L2ItemInstance item = _player.getInventory().getItemByItemId(itemId);
		if(item == null)
			return 0;
		
		if(item.isStackable())
		{
			// Tests on count value in order not to have negative value
			if(count < 0 || count > item.getCount())
				count = item.getCount();

			// Destroy the quantity of items wanted
			_player.destroyItemByItemId("Quest", itemId, count, _player, true);
			return count;
		}
		else 
		{
			long del = 0;

			while((item = _player.getInventory().getItemByItemId(itemId)) != null && count != 0)
			{
				_player.destroyItem("Quest", item.getObjectId(), item.getCount(), _player, true);
				del++;
				if(count > 0)
					count--;
			}
			
			return del;
		}
	}

	/**
	 * Remove the variable of quest from the list of variables for the quest.<BR><BR>
	 * <U><I>Concept : </I></U>
	 * Remove the variable of quest represented by "var" from the class variable FastMap "vars" and from the database.
	 *
	 * @param var : String designating the variable for the quest to be deleted
	 * @return String pointing out the previous value associated with the variable "var"
	 */
	public String unset(String var)
	{
		if(_vars == null)
			return null;

		String old = null;
		try
		{
			old = _vars.remove(var);
		}
		catch(NullPointerException e)
		{
		}

		if(old != null)
			Quest.deleteQuestVarInDb(this, var);
		return old;
	}
}