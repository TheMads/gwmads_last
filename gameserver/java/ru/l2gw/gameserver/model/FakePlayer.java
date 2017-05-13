package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.ai.L2PlayerAI;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.instances.L2HennaInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.tables.CharTemplateTable;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2PlayerTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: rage
 * @date: 18.09.13 20:43
 */
public class FakePlayer extends L2Player
{
	private long despawnTime;

	public FakePlayer(int objectId, L2PlayerTemplate template, String accountName)
	{
		super(objectId, template, accountName);
	}

	public FakePlayer(int objectId, L2PlayerTemplate template)
	{
		super(objectId, template);
	}

	public static FakePlayer create(short classId, byte sex, String accountName, final String name, final byte hairStyle, final byte hairColor, final byte face, int despawnMin)
	{
		L2PlayerTemplate template = CharTemplateTable.getInstance().getTemplate(classId, sex != 0);

		// Create a new L2Player with an account name
		FakePlayer player = new FakePlayer(IdFactory.getInstance().getNextId(), template, accountName);

		player.user_variables = new ConcurrentHashMap<>();
		player.setName(name);
		player.setTitle("");
		player.setHairStyle(hairStyle);
		player.setHairColor(hairColor);
		player.setFace(face);
		player.setRunning();
		player.setEntering(false);

		final L2SubClass subClass = new L2SubClass();
		subClass.setBase(true);
		subClass.setClassId((short) template.classId.getId());
		subClass.setLevel((byte) 1);
		subClass.setExp(0);
		subClass.setSp(0);
		subClass.setHp(player.getMaxHp());
		subClass.setMp(player.getMaxMp());
		subClass.setCp(player.getMaxCp());
		subClass.setActive(true);
		subClass.setDeathPenalty(new DeathPenalty(player, (byte) 0));
		subClass.setSlot((byte) 0);
		subClass.setPlayer(player);

		player._classlist.put(subClass.getClassId(), subClass);
		player.setActiveSubClass(subClass.getClassId(), false);

		player.despawnTime = System.currentTimeMillis() + despawnMin * 60000L;

		player.setAI(new L2PlayerAI(player));

		GArray<StatsSet> items = Config.CUSTOM_INITIAL_EQUIPMENT ? CharTemplateTable.getInitialCustomEquipment(template.classId) : CharTemplateTable.getInitialEquipment(template.classId);
		if(items != null)
			for(StatsSet itemInfo : items)
			{
				try
				{
					L2ItemInstance item = ItemTable.getInstance().createItem("CharacterCreate", itemInfo.getInteger("itemId"), itemInfo.getLong("count"), player, null);
					player.getInventory().addItem("NewChar", item, player, null);

					if(item.isEquipable())
						if(player.getInventory().getItemInBodySlot(item.getBodyPart()) == null)
							player.getInventory().equipItem(item);
				}
				catch(Exception e)
				{
					// quite
				}
			}

		if(Config.START_ITEMS.length > 1)
			try
			{
				for(int i = 0; i < Config.START_ITEMS.length; i += 2)
					player.getInventory().addItem("StartItems", Config.START_ITEMS[i], Config.START_ITEMS[i + 1], player, null);
			}
			catch (Exception e)
			{
				_log.warn("CharacterCreate: error adding start items: " + e, e);
			}

		if(Config.CHARACTER_CREATE_LEVEL > 1)
		{
			player.setExp(Experience.LEVEL[Config.CHARACTER_CREATE_LEVEL]);
			player.setLevel(Config.CHARACTER_CREATE_LEVEL);
		}
		else if(Config.FIRST_CHARACTER_LEVEL > 0)
		{
			player.setExp(Experience.LEVEL[Config.FIRST_CHARACTER_LEVEL]);
			player.setLevel(Config.FIRST_CHARACTER_LEVEL);
			if(Config.FIRST_CHARACTER_WH_ADENA > 0)
			{
				L2ItemInstance adena = ItemTable.getInstance().createItem("FirstChar", 57, Config.FIRST_CHARACTER_WH_ADENA, player, null);
				adena.setOwnerId(player.getObjectId());
				adena.setLocation(L2ItemInstance.ItemLocation.WAREHOUSE);
				adena.updateDatabase(true);
			}
		}

		return player;
	}

	public long getDespawnTime()
	{
		return despawnTime;
	}

	@Override
	protected boolean createDb()
	{
		return true;
	}

	@Override
	public void storeHWID(String HWID, Log log)
	{
	}

	@Override
	public void updateOnlineStatus()
	{
	}

	@Override
	public void store()
	{
	}

	@Override
	public L2Skill removeSkill(L2Skill skill, boolean fromDB, boolean force)
	{
		return super.removeSkill(skill, false, force);
	}

	@Override
	public void restoreSummon()
	{
	}

	@Override
	public void storeDisableSkills()
	{
	}

	@Override
	public void storeEffects()
	{}

	@Override
	public void restoreEffects()
	{}

	@Override
	public void restoreDisableSkills()
	{}

	@Override
	public boolean removeHenna(int slot, L2NpcInstance npc)
	{
		return true;
	}

	@Override
	public boolean addHenna(final L2HennaInstance henna)
	{
		return true;
	}

	@Override
	public void restoreRecipeBook()
	{}

	@Override
	public String toString()
	{
		return "FakePlayer '" + getName() + "'";
	}

	@Override
	public String getLastHWID()
	{
		return "FAKE_HWID";
	}
/*
	@Override
	public boolean isMovementDisabled()
	{
		_log.info(this + "isMovementDisabled: " + (isStatActive(Stats.BLOCK_MOVE) || isActionsBlocked() || isSitting() || isStunned() || isRooted() || isSleeping() || isParalyzed() || isImobilised() || isAlikeDead() || _overloaded || isDisabled() || isAttackingNow() || isCastingNow()));
		_log.info(this + "1: " + isStatActive(Stats.BLOCK_MOVE));
		_log.info(this + "2: " + isActionsBlocked());
		_log.info(this + "3: " + isSitting());
		_log.info(this + "4: " + isStunned());
		_log.info(this + "5: " + isRooted());
		_log.info(this + "6: " + isSleeping());
		_log.info(this + "7: " + isParalyzed());
		_log.info(this + "8: " + isImobilised());
		_log.info(this + "9: " + isAlikeDead());
		_log.info(this + "10: " + _overloaded);
		_log.info(this + "11: " + isDisabled());
		_log.info(this + "12: " + isAttackingNow());
		_log.info(this + "13: " + isCastingNow());

		return isStatActive(Stats.BLOCK_MOVE) || isActionsBlocked() || isSitting() || isStunned() || isRooted() || isSleeping() || isParalyzed() || isImobilised() || isAlikeDead() || _overloaded || isDisabled() || isAttackingNow() || isCastingNow();
	}
*/
	@Override
	public void spawnMe()
	{
		super.spawnMe();
		ThreadPoolManager.getInstance().scheduleAi(new Runnable()
		{
			@Override
			public void run()
			{
				if(!FakePlayer.this.isDeleting() && !FakePlayer.this.isDead())
				{
					if(Rnd.chance(Config.FAKE_PLAYER_WALK_CHANCE))
					{
						Location loc = Location.coordsRandomize(FakePlayer.this, Config.FAKE_PLAYER_WALK_MIN, Config.FAKE_PLAYER_WALK_MAX);
						FakePlayer.this.moveToLocation(loc, 0, true);
					}
					ThreadPoolManager.getInstance().scheduleAi(this, Rnd.get(Config.FAKE_PLAYER_WALK_TIME_MIN, Config.FAKE_PLAYER_WALK_TIME_MAX), false);
				}
			}
		}, Rnd.get(Config.FAKE_PLAYER_WALK_TIME_MIN, Config.FAKE_PLAYER_WALK_TIME_MAX), false);
	}
}
