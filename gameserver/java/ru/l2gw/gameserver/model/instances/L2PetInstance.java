package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.taskmanager.DecayTaskManager;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2PetTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Future;

public class L2PetInstance extends L2Summon
{
	private int _level;
	@SuppressWarnings("unused")
	private int _currentMeal;
	protected PetInventory _inventory;
	private final int _controlItemObjId;
	private boolean _respawned;
	@SuppressWarnings("unchecked")
	private Future<?> _feedTask;
	protected L2PetTemplate _petTemplate;

	private int lostExp;

	class FeedTask implements Runnable
	{
		private long _hungryTime = 0;
		private long _nextFeedTime = 0;

		public void run()
		{
			try
			{
				if(_nextFeedTime < System.currentTimeMillis())
				{
					setCurrentFed(getCurrentFed() - (isInCombat() ? _petTemplate.meal_in_battle : _petTemplate.meal_in_normal));
					_nextFeedTime = System.currentTimeMillis() + 10000;
				}

				L2Player owner = getPlayer();

				if(getCurrentFed() > 0)
					_hungryTime = 0;
				else
				{
					if(_hungryTime == 0)
						_hungryTime = System.currentTimeMillis() + 300000;
					else if(_hungryTime < System.currentTimeMillis())
					{
						setCurrentFed(0);
						stopFeed();
						if((Config.ALT_DROP_HUNGRY_PET_CONTROL_ITEM || _petTemplate.food.size() < 1) && owner != null)
							giveAllToOwner();
						unSummon();
						if((Config.ALT_DROP_HUNGRY_PET_CONTROL_ITEM || _petTemplate.food.size() < 1) && owner != null)
							owner.destroyItem("HungryPet", getControlItemObjId(), 1, L2PetInstance.this, true);
						return;
					}
				}

				if(_petTemplate.food.size() > 0)
				{
					L2ItemInstance food = null;
					for(int itemId : _petTemplate.food)
						if((food = getInventory().getItemByItemId(itemId)) != null)
							break;

					if(food != null && getCurrentFed() < getMaxMeal() * _petTemplate.hungry_limit)
						ItemTable.useHandler(L2PetInstance.this, food);

					if(owner != null)
					{
						if(getCurrentFed() < getMaxMeal() * _petTemplate.hungry_limit * 0.5)
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY);
						if(getCurrentFed() < getMaxMeal() * _petTemplate.hungry_limit * 0.2)
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY);
						if(getCurrentFed() == 0)
						{
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY);
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
							owner.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);
						}
					}
				}
				broadcastStatusUpdate();
			}
			catch(final Throwable e)
			{
				_log.warn("", e);
			}
		}
	}

	public boolean tryEquipItem(final L2ItemInstance item)
	{
		if(!item.isEquipable())
			return false;

		if(item.isEquipped())
		{
			getInventory().unEquipItemAndSendChanges(item);
			broadcastPetInfo();
			return true;
		}
		else if(item.checkEquipCondition(this))
		{
			GArray<L2ItemInstance> items = getInventory().equipItemAndRecord(item);
			broadcastPetInfo();
			getPlayer().sendPacket(new PetInventoryUpdate(items));
			return true;
		}

		return false;
	}

	public static L2PetInstance spawnPet(L2NpcTemplate template, L2Player owner, L2ItemInstance control)
	{
		L2PetInstance result = restore(control, template, owner);
		if(result != null)
			result.InventoryUpdateControlItem();
		return result;
	}

	/**
	 * Загрузка уже существующего пета
	 */
	public L2PetInstance(Integer objectId, L2PetTemplate template, L2Player thisOwner, Integer controlObjId, Long exp, Integer lvl)
	{
		super(objectId, template, thisOwner);
		_petTemplate = template;
		_controlItemObjId = controlObjId;
		_level = lvl;
		long nextLevelExp = _petTemplate.exp;
		if(PetDataTable.getInstance().getInfo(getNpcId(), (byte) (_level + 1)) != null)
			nextLevelExp = PetDataTable.getInstance().getInfo(getNpcId(), (byte) (_level + 1)).exp;

		_exp = Math.min(Math.max(exp, _petTemplate.exp), nextLevelExp);
		_inventory = new PetInventory(this);
		_inventory.restore();

		startFeed();
	}

	/**
	 * Создание нового пета
	 */
	public L2PetInstance(Integer objectId, L2PetTemplate template, L2Player thisOwner, Integer itemObjId, Integer lvl)
	{
		super(objectId, template, thisOwner);
		_petTemplate = template;
		_controlItemObjId = itemObjId;
		_inventory = new PetInventory(this);
		_level = lvl;

		startFeed();
	}

	@Override
	public L2PetTemplate getTemplate()
	{
		return _petTemplate;
	}

	@Override
	public final byte getLevel()
	{
		return (byte) _level;
	}

	public boolean isRespawned()
	{
		return _respawned;
	}

	@Override
	public int getSummonType()
	{
		return 2;
	}

	/**
	 * @return Returns ObjectId for pet control item
	 */
	@Override
	public int getControlItemObjId()
	{
		return _controlItemObjId;
	}

	@SuppressWarnings("unused")
	public int getControlItemId()
	{
		return _petTemplate.controlItemId;
	}

	public L2ItemInstance getControlItem()
	{
		return getControlItemObjId() > 0 ? getPlayer().getInventory().getItemByObjectId(getControlItemObjId()) : null;
	}

	public void InventoryUpdateControlItem()
	{
		L2ItemInstance controlItem = getControlItem();
		if(controlItem == null)
			return;
		controlItem.setEnchantLevel(_level);
		getPlayer().sendPacket(new InventoryUpdate().addModifiedItem(controlItem));
	}

	@Override
	public int getMaxHp()
	{
		return (int) calcStat(Stats.MAX_HP, _petTemplate.org_hp, null, null);
	}

	@Override
	public int getMaxMp()
	{
		return (int) calcStat(Stats.MAX_MP, _petTemplate.org_mp, null, null);
	}

	@Override
	public int getMaxMeal()
	{
		return _petTemplate.max_meal;
	}

	@Override
	public int getMAtk(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.MAGIC_ATTACK, _petTemplate.org_mattack, target, skill);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		return (int) calcStat(Stats.MAGIC_DEFENCE, _petTemplate.org_mdefend, target, skill);
	}

	@Override
	public int getPAtk(L2Character target)
	{
		return (int) calcStat(Stats.POWER_ATTACK, _petTemplate.org_pattack, target, null);
	}

	@Override
	public int getPAtkSpd()
	{
		L2Weapon weapon = getActiveWeaponItem();
		return Math.max((int) (calcStat(Stats.POWER_ATTACK_SPEED, weapon == null ? _template.basePAtkSpd : Formulas.getPAtkSpdFromBase(weapon.attackSpeed, getDEX()), null, null) / getArmourExpertisePenalty()), 1);
	}

	@Override
	public int getPDef(L2Character target)
	{
		return (int) calcStat(Stats.POWER_DEFENCE, _petTemplate.org_pdefend, target, null);
	}

	@Override
	public int getCriticalHit(final L2Character target, final L2Skill skill)
	{
		int val = super.getCriticalHit(target, skill);
		if(Config.LIM_CRIT != 0 && val > Config.LIM_CRIT)
			val = Config.LIM_CRIT;
		return val;

	}

	@Override
	public int getMaxLoad()
	{
		return (int) calcStat(Stats.MAX_LOAD, Formulas.CONbonus[getCON()] * 69000 / 2, null, null);
	}

	@Override
	public int getCurrentFed()
	{
		return _currentMeal;
	}

	public void setCurrentFed(final int num)
	{
		_currentMeal = Math.max(Math.min(num, _petTemplate.max_meal), 0);
	}

	public void setLevel(final byte level)
	{
		_level = level;
	}

	public void setRespawned(final boolean respawned)
	{
		_respawned = respawned;
	}

	@Override
	public void addExpAndSp(final long addToExp, final long addToSp)
	{
		_sp += addToSp;
		if(_sp > Integer.MAX_VALUE)
			_sp = Integer.MAX_VALUE;

		addExp(addToExp);

		if(getPlayer() == null)
			return;

		if(addToExp > 0 || addToSp > 0)
			getPlayer().sendPacket(new SystemMessage(SystemMessage.THE_PET_ACQUIRED_EXPERIENCE_POINTS_OF_S1).addNumber(addToExp));

		broadcastPetInfo();
	}

	private void addExp(long addToExp)
	{
		_exp += addToExp;

		if(_exp < getExpForMinLevel())
			_exp = getExpForMinLevel();

		if(_exp < 0)
			_exp = 0;

		while(_exp >= getExpForNextLevel() && _level < Config.ALT_PET_MAX_LEVEL)
			increaseLevel();

		if(_exp > getMaxExp())
			_exp = getMaxExp();

		while(_exp < getExpForThisLevel() && _level > getMinLevel())
			decreaseLevel();
	}

	public int getMinLevel()
	{
		return 1;
	}

	public long getMaxExp()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), 87).exp;
	}

	private void deathPenalty()
	{
		if(!isInZoneBattle())
		{
			final int lvl = getLevel();
			final double percentLost = -0.07 * lvl + 6.5;
			// Calculate the Experience loss
			lostExp = (int) Math.round((getExpForNextLevel() - getExpForThisLevel()) * percentLost / 100);
			addExpAndSp(-lostExp, 0);
		}
	}

	@Override
	public long getExpForThisLevel()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), _level).exp;
	}

	public long getExpForMinLevel()
	{
		return PetDataTable.getInstance().getInfo(getNpcId(), getMinLevel()).exp;
	}

	@Override
	public long getExpForNextLevel()
	{
		if(PetDataTable.getInstance().getInfo(getNpcId(), (byte) (_level + 1)) == null)
		{
			_log.warn(this + " has no exp value for next level: " + (_level + 1));
			return 0L;
		}
		return PetDataTable.getInstance().getInfo(getNpcId(), (byte) (_level + 1)).exp;
	}

	public void increaseLevel()
	{
		if(getPlayer() == null)
			return;

		if(_level < Config.ALT_PET_MAX_LEVEL)
			_level++;

		updateControlItem();
		updateData();

		setCurrentHpMp(getMaxHp(), getMaxMp());
		broadcastStatusUpdate();
		sendPetInfo();//To change PAtk if need penalty

		getPlayer().sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2PetInstance.PetLevelUp", getPlayer()).addNumber(_level));
		broadcastPacket(new SocialAction(getObjectId(), SocialAction.SocialType.LEVEL_UP));
		store(getPlayer().getObjectId());
	}

	public void decreaseLevel()
	{
		if(getPlayer() == null)
			return;

		if(_level > getMinLevel())
			_level--;

		updateControlItem();
		updateData();
		setCurrentHpMp(getMaxHp(), getMaxMp());
		broadcastStatusUpdate();
		sendPetInfo();//To change PAtk if need penalty
		store(getPlayer().getObjectId());
	}

	private void updateData()
	{
		_petTemplate = PetDataTable.getInstance().getInfo(getTemplate().npcId, _level);
	}

	/**
	 * Return null.<BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		L2ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		return item != null && item.getItem() instanceof L2Weapon ? item : null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		L2ItemInstance item = getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		return item != null && item.getItem() instanceof L2Weapon ? (L2Weapon) item.getItem() : null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		// temporary? unavailable
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		// temporary? unavailable
		return null;
	}

	@Override
	public PetInventory getInventory()
	{
		return _inventory;
	}

	@Override
	public void doPickupItem(final L2Object object)
	{
		getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE, null, null);

		final L2Player owner = getPlayer();
		if(owner == null)
			return;

		if(!(object instanceof L2ItemInstance))
		{
			getPlayer().sendActionFailed();
			return;
		}

		L2ItemInstance target = (L2ItemInstance) object;

		if(target.isFortFlag())
		{
			getPlayer().sendPacket(new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1).addItemName(target.getItemId()));
			return;
		}

		if(CursedWeaponsManager.getInstance().isCursed(target.getItemId()))
		{
			getPlayer().sendPacket(new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1).addItemName(target.getItemId()));
			return;
		}

		synchronized(target)
		{
			if(!target.isVisible())
			{
				getPlayer().sendActionFailed();
				return;
			}

			if(getInventory().getTotalWeight() + target.getItem().getWeight() * target.getCount() > getMaxLoad())
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessage.EXCEEDED_PET_INVENTORYS_WEIGHT_LIMIT));
				getPlayer().sendActionFailed();
				return;
			}

			if(!target.isCanBePickuped(this))
			{
				SystemMessage sm;
				if(target.getItemId() == 57)
				{
					sm = new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1_ADENA);
					sm.addNumber(target.getCount());
				}
				else
				{
					sm = new SystemMessage(SystemMessage.FAILED_TO_PICK_UP_S1);
					sm.addItemName(target.getItemId());
				}
				owner.sendPacket(sm);
				owner.sendActionFailed();
				return;
			}

			if(target.isHerb())
			{
				for(final L2Skill skills : target.getItem().getAttachedSkills())
					owner.altUseSkill(skills, this, target);

				target.decayMe();
				L2World.removeObject(target);
				return;
			}

			target.pickupMe(this);
		}

		if(getPlayer().getParty() == null || getPlayer().getParty().getLootDistribution() == L2Party.ITEM_LOOTER)
		{
			if(target.getItemId() == 57)
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2_ADENA).addString("Your pet").addNumber(target.getCount()));
			else if(target.getCount() == 1)
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2).addString("Your pet").addItemName(target.getItemId()));
			else
				owner.sendPacket(new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S3_S2).addString("Your pet").addItemName(target.getItemId()).addNumber(target.getCount()));

			synchronized(_inventory)
			{
				getInventory().addItem("Pickup", target, owner, this);
			}
			//sendItemList();
			sendPetInfo();
		}
		else
			owner.getParty().distributeItem(getPlayer(), target);
		broadcastPickUpMsg(target);

		getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
		if(getFollowStatus())
			followOwner();
	}

	@Override
	public void deleteMe()
	{
		destroyControlItem(); // this should also delete the pet from the db
		super.deleteMe();
	}

	@Override
	public void reduceHp(double damage, L2Character attacker, boolean directHp, boolean reflect)
	{
		if(attacker instanceof L2Playable && isInZoneBattle() != attacker.isInZoneBattle())
		{
			attacker.getPlayer().sendPacket(Msg.INVALID_TARGET);
			return;
		}

		super.reduceHp(damage, attacker, directHp, reflect);
		// this is usually only called in combat

		if(!isDead() && attacker != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.THE_PET_RECEIVED_DAMAGE_OF_S2_CAUSED_BY_S1).addCharName(attacker);
			sm.addNumber((int) damage);

			if(getPlayer() != null)
				getPlayer().sendPacket(sm);
		}
	}

	@Override
	public synchronized void doDie(L2Character killer)
	{
		super.doDie(killer);

		if(getPlayer() == null)
		{
			onDecay();
			return;
		}

		stopFeed();
		deathPenalty();

		getPlayer().sendPacket(new SystemMessage(SystemMessage.THE_PET_HAS_BEEN_KILLED_ID_YOU_DO_NOT_RESURRECT_IT_WITHIN_24_HOURS_THE_PETS_BODY_WILL_DISAPPEAT_ALONG_WITH_ALL_THE_PETS_ITEMS));
		DecayTaskManager.getInstance().addDecayTask(this, 86400000);
	}

	@Override
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancelDecayTask(this);
	}

	/**
	 * Remove the Pet from DB and its associated item from the player inventory
	 */
	public void destroyControlItem()
	{
		if(getPlayer() == null || getControlItemObjId() == 0)
			return;

		try
		{
			getPlayer().getInventory().destroyItem("DeletePet", getControlItemObjId(), 1, getPlayer(), this);
			getPlayer().sendPacket(new SystemMessage(SystemMessage.YOUR_PETS_CORPSE_HAS_DECAYED));
		}
		catch(final Exception e)
		{
			_log.warn("Error while destroying control item: " + e);
		}
	}

	/**
	 * @return Returns the mountable.
	 */
	@Override
	public boolean isMountable()
	{
		return _petTemplate.speed_on_ride_g > 0;
	}

	public static L2PetInstance restore(L2Player player)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId, name, level, curHp, curMp, exp, sp, fed, item_obj_id FROM pets WHERE owner_id=?");
			statement.setInt(1, player.getObjectId());
			rset = statement.executeQuery();
			L2PetInstance pet;
			int staticUseLevel;
			if(rset.next())
			{
				final int itemObjId = rset.getInt("item_obj_id");
				L2ItemInstance controlItem = player.getInventory().getItemByObjectId(itemObjId);

				if(controlItem == null)
				{
					_log.warn("Restore pet without control item: " + player + " item objectId: " + itemObjId);
					return null;
				}

				L2Object existingPet = L2ObjectsStorage.findObject(rset.getInt("objId"));
				if(existingPet != null)
				{
					player.sendPacket(Msg.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
					_log.warn("PetSummon: try to summon dupe: " + player + " " + controlItem + " already summoned: " + existingPet);
					((L2PetInstance) existingPet).unSummon();
					return null;
				}

				int npcId = PetDataTable.getSummonId(controlItem);
				if(npcId == 0)
					return null;

				L2NpcTemplate template = NpcTable.getTemplate(npcId);

				final int currentLevel = rset.getByte("level");
				long staticBaseExp = rset.getLong("exp");
				// Sin Eater
				if(template.getNpcId() == PetDataTable.SIN_EATER_ID)
				{
					staticUseLevel = controlItem.getEnchantLevel();
					if(staticUseLevel <= 0)
					{
						staticUseLevel = player.getLevel();
						staticBaseExp = PetDataTable.getInstance().getInfo(template.getNpcId(), currentLevel).exp;
					}
				}
				else if(PetDataTable.getInstance().getInfo(template.getNpcId(), template.level).sync_level)
					staticUseLevel = player.getLevel();
				else
					staticUseLevel = currentLevel == 0 ? template.level : currentLevel;

				final L2PetTemplate petTemplate = PetDataTable.getInstance().getInfo(template.getNpcId(), staticUseLevel);

				Constructor<?> constructor;
				try
				{
					constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructor(Integer.class, L2PetTemplate.class, L2Player.class, Integer.class, Long.class, Integer.class);
					//constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructors()[0];
				}
				catch(ClassNotFoundException e)
				{
					_log.warn("Class npc.model." + petTemplate + "Instance.java not found or loaded with errors");
					throw new ClassNotFoundException();
				}

				pet = (L2PetInstance) constructor.newInstance(rset.getInt("objId"), petTemplate, player, itemObjId, staticBaseExp, staticUseLevel);

				pet.setRespawned(true);
				pet.setName(rset.getString("name"));
				pet.setSp(rset.getInt("sp"));
				pet.setCurrentFed(rset.getInt("fed"));
				pet.restoreSummonEffects();
				pet.setCurrentHpMp(rset.getDouble("curHp"), rset.getDouble("curMp"));
				pet.setCurrentCp(pet.getMaxCp());
				return pet;
			}
		}
		catch(final Exception e)
		{
			return null;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return null;
	}

	private static L2PetInstance restore(final L2ItemInstance controlItem, final L2NpcTemplate template, final L2Player thisOwner)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		L2PetTemplate petTemplate = null;
		try
		{
			if(thisOwner.getInventory().getItemByObjectId(controlItem.getObjectId()) == null)
			{
				_log.warn("Restore pet without control item: " + thisOwner + " item: " + controlItem);
				return null;
			}

			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId, name, level, curHp, curMp, exp, sp, fed FROM pets WHERE item_obj_id=?");
			statement.setInt(1, controlItem.getObjectId());
			rset = statement.executeQuery();
			L2PetInstance pet;
			int staticUseLevel;
			final int itemObjId = controlItem.getObjectId();

			if(!rset.next())
			{
				// Sin Eater
				if(template.getNpcId() == PetDataTable.SIN_EATER_ID)
				{
					staticUseLevel = controlItem.getEnchantLevel();
					if(staticUseLevel <= 0)
						staticUseLevel = thisOwner.getLevel();
				}
				else if(PetDataTable.getInstance().getInfo(template.getNpcId(), template.level).sync_level)
					staticUseLevel = thisOwner.getLevel();
				else
					staticUseLevel = template.level;

				petTemplate = PetDataTable.getInstance().getInfo(template.getNpcId(), staticUseLevel);

				Constructor<?> constructor;
				try
				{
					constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructor(Integer.class, L2PetTemplate.class, L2Player.class, Integer.class, Integer.class);
					//constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructors()[1];
				}
				catch(ClassNotFoundException e)
				{
					_log.warn("Class npc.model." + petTemplate + "Instance.java not found or loaded with errors");
					throw new ClassNotFoundException();
				}
				pet = (L2PetInstance) constructor.newInstance(IdFactory.getInstance().getNextId(), petTemplate, thisOwner, itemObjId, staticUseLevel);

				return pet;
			}
			else
			{
				L2Object existingPet = L2ObjectsStorage.findObject(rset.getInt("objId"));
				if(existingPet != null)
				{
					thisOwner.sendPacket(Msg.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME);
					_log.warn("PetSummon: try to summon dupe: " + thisOwner + " " + controlItem + " already summoned: " + existingPet);
					((L2PetInstance) existingPet).unSummon();
					return null;
				}

				final int currentLevel = rset.getByte("level");
				long staticBaseExp = rset.getLong("exp");
				// Sin Eater
				if(template.getNpcId() == PetDataTable.SIN_EATER_ID)
				{
					staticUseLevel = controlItem.getEnchantLevel();
					if(staticUseLevel <= 0)
					{
						staticUseLevel = thisOwner.getLevel();
						staticBaseExp = PetDataTable.getInstance().getInfo(template.getNpcId(), currentLevel).exp;
					}
				}
				else if(PetDataTable.getInstance().getInfo(template.getNpcId(), template.level).sync_level)
					staticUseLevel = thisOwner.getLevel();
				else
					staticUseLevel = currentLevel == 0 ? template.level : currentLevel;


				petTemplate = PetDataTable.getInstance().getInfo(template.getNpcId(), staticUseLevel);

				Constructor<?> constructor;
				try
				{
					constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructor(Integer.class, L2PetTemplate.class, L2Player.class, Integer.class, Long.class, Integer.class);
					//constructor = Class.forName("ru.l2gw.gameserver.model.instances." + petTemplate.type + "Instance").getConstructors()[0];
				}
				catch(ClassNotFoundException e)
				{
					_log.warn("Class npc.model." + petTemplate + "Instance.java not found or loaded with errors");
					throw new ClassNotFoundException();
				}
				pet = (L2PetInstance) constructor.newInstance(rset.getInt("objId"), petTemplate, thisOwner, itemObjId, staticBaseExp, staticUseLevel);
			}

			pet.setRespawned(true);
			pet.setName(rset.getString("name"));
			pet.setSp((int) Math.min(rset.getLong("sp"), Integer.MAX_VALUE));
			pet.setCurrentFed(rset.getInt("fed"));
			pet.restoreSummonEffects();
			pet.setCurrentHpMp(rset.getDouble("curHp"), rset.getDouble("curMp"));
			pet.setCurrentCp(pet.getMaxCp());
			return pet;
		}
		catch(final Exception e)
		{
			_log.warn("could not restore Pet data from item[id=" + controlItem.getItemId() + ";objectId=" + controlItem.getObjectId() + "]: template: " + petTemplate + " " + e);
			e.printStackTrace();
			return null;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void store(int ownerId)
	{
		if(getControlItemObjId() == 0)
			return;

		String req;
		if(!isRespawned())
			req = "INSERT INTO pets (name,level,curHp,curMp,exp,sp,fed,objId,owner_id,item_obj_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
		else
			req = "UPDATE pets SET name=?,level=?,curHp=?,curMp=?,exp=?,sp=?,fed=?,objId=?,owner_id=? WHERE item_obj_id = ?";
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(req);
			statement.setString(1, _name);
			statement.setInt(2, _level);
			statement.setDouble(3, getCurrentHp());
			statement.setDouble(4, getCurrentMp());
			statement.setLong(5, _exp);
			statement.setLong(6, _sp);
			statement.setInt(7, _currentMeal);
			statement.setInt(8, _objectId);
			statement.setInt(9, ownerId);
			statement.setInt(10, _controlItemObjId);
			statement.executeUpdate();
			_respawned = true;
		}
		catch(final Exception e)
		{
			_log.warn("could not store pet data: " + e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	protected synchronized void stopFeed()
	{
		if(_feedTask != null)
		{
			_feedTask.cancel(false);
			_feedTask = null;
			if(Config.DEBUG)
				_log.info(this + " feed task stop");
		}
	}

	@Override
	public synchronized void startFeed()
	{
		// stop feeding task if its active
		if(_feedTask != null)
			stopFeed();

		if(_feedTask == null && !isDead())
			_feedTask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(new FeedTask(), 10000, 1000);
	}

	@Override
	public synchronized void unSummon()
	{
		if(unSummonStarted)
			return;

		unSummonStarted = true;

		stopFeed();
		setTarget(null);
		stopHpMpRegeneration();
		synchronized(getAI())
		{
			stopMove();
		}
		decayMe();
		getAI().stopFollow();
		detachAI();
		storeSummonEffects();

		L2Player player = getPlayer();

		if(player != null)
		{
			L2ItemInstance controlItem = getControlItem();
			if(controlItem != null)
			{
				if(_inventory.getItemsList().size() > 0)
				{
					if(!player.isDeleting())
						player.sendPacket(Msg.THERE_ARE_ITEMS_IN_YOUR_PET_INVENTORY_RENDERING_YOU_UNABLE_TO_SELL_TRADE_DROP_PET_SUMMONING_ITEM_PLEASE_EMPTY_YOUR_PET_INVENTORY);
					controlItem.setCustomFlags(L2ItemInstance.FLAG_PET_INVENTORY);
					_inventory.deleteMe();
				}
				else
					controlItem.setCustomFlags(0);
				controlItem.updateDatabase();
			}

			player.sendPacket(new PetDelete(getObjectId(), 2));
			if(player.getParty() != null)
				player.getParty().broadcastToPartyMembers(player, new ExPartyPetWindowDelete(this));

			if(player.getTargetId() == getObjectId())
			{
				player.setTarget(null);
				if(player.getAI().getAttackTarget() == this)
					player.getAI().setAttackTarget(null);
			}

			player.sendPacket(new DeleteObject(this));
			player.setPet(null);
			setOwner(null);
		}

		store(player != null && player.isDeleting() ? player.getObjectId() : 0);
		stopAllEffects();
		L2World.removeObject(this);
		L2ObjectsStorage.remove(_storedId);
		setReflection(0);
	}

	@Override
	public void sendDamageMessage(L2Character target, int damage, boolean miss, boolean pcrit, boolean block)
	{
		if(getPlayer() != null)
		{
			if(block)
			{
				getPlayer().sendPacket(Msg.THE_ATTACK_HAS_BEEN_BLOCKED);
				return;
			}
			else if(miss)
			{
				getPlayer().sendPacket(new SystemMessage(SystemMessage.S1S_ATTACK_WENT_ASTRAY).addCharName(this));
				if(target != null && target.isPlayer())
					target.sendPacket(new SystemMessage(SystemMessage.S1_HAS_EVADED_S2S_ATTACK).addCharName(target).addCharName(this));
				return;
			}

			if(pcrit)
				getPlayer().sendPacket(Msg.PETS_CRITICAL_HIT);

			getPlayer().sendPacket(new SystemMessage(SystemMessage.THE_PET_GAVE_DAMAGE_OF_S1).addNumber(damage));
		}
	}

	@Override
	public int getSkillLevel(int skillId)
	{
		if(_skills == null || _skills.get(skillId) == null)
			return -1;
		final int lvl = getLevel();
		return lvl > 70 ? 7 + (lvl - 70) / 5 : lvl / 10;
	}

	@Override
	public boolean consumeItem(final int itemConsumeId, final int itemCount, boolean sendMessage)
	{
		return destroyItemByItemId("Consume", itemConsumeId, itemCount, getPlayer(), sendMessage);
	}

	/**
	 * Destroys item from inventory and send a Server->Client InventoryUpdate packet to the L2PcInstance.
	 *
	 * @param process	 : String Identifier of process triggering this action
	 * @param count	   : int Quantity of items to be destroyed
	 * @param reference   : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @param sendMessage : boolean Specifies whether to send message to Client about this action
	 * @return boolean informing if the action was successfull
	 */
	public boolean destroyItemByItemId(String process, int itemId, long count, L2Object reference, boolean sendMessage)
	{
		L2ItemInstance item = _inventory.destroyItemByItemId(process, itemId, count, getPlayer(), reference);

		L2Player owner = getPlayer();
		if(item == null)
		{
			if(sendMessage && owner != null)
				owner.sendPacket(Msg.INCORRECT_ITEM_COUNT);

			return false;
		}

		if(sendMessage && owner != null)
		{

			SystemMessage sm;
			if(count > 1)
			{
				sm = new SystemMessage(SystemMessage.S2_S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
				sm.addNumber(count);
			}
			else
			{
				sm = new SystemMessage(SystemMessage.S1_HAS_DISAPPEARED);
				sm.addItemName(itemId);
			}

			sm.addNumber(count);
			sm.addItemName(item.getItemId());
			owner.sendPacket(sm);
		}
		return true;
	}

	@Override
	public void sendChanges()
	{
		broadcastStatusUpdate();
		if(getPlayer() == null)
			unSummon();
		else
			getPlayer().sendPacket(new PetItemList(this));
	}

	@Override
	public boolean isHungry()
	{
		return _currentMeal < _petTemplate.max_meal * _petTemplate.hungry_limit;
	}

	public void updateControlItem()
	{
		final L2ItemInstance controlItem = getControlItem();
		if(controlItem == null)
			return;
		controlItem.setEnchantLevel(_level);
		controlItem.setCustomType2(getName() == null ? 0 : 1);

		final L2Player owner = getPlayer();
		if(owner != null)
			getPlayer().sendPacket(new InventoryUpdate().addModifiedItem(controlItem));
	}

	@Override
	public void sendPetInfo()
	{
		if(getPlayer() != null)
			getPlayer().sendPacket(new PetInfo(this, 1));
	}

	public void doRevive(final double percent)
	{
		restoreExp(percent);
		doRevive();
		stopDecay();
		startFeed();
		setRunning();
		setFollowStatus(true);
	}

	public void restoreExp(final double percent)
	{
		if(lostExp != 0)
		{
			addExp((long) (lostExp * percent / 100.));
			lostExp = 0;
		}
	}

	@Override
	public float getExpPenalty()
	{
		return _petTemplate.exp_type;
	}

	public int getLostExp()
	{
		return lostExp;
	}

	public int getWeaponItemId()
	{
		return getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND);
	}

	public int getArmorItemId()
	{
		return getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST);
	}

	public int getSoulshotConsumeCount()
	{
		return _petTemplate.soulshot_count;
	}

	public int getSpiritshotConsumeCount()
	{
		return _petTemplate.spiritshot_count;
	}

	public void giveAllToOwner()
	{
		try
		{
			synchronized(_inventory)
			{
				for(final L2ItemInstance giveit : _inventory.getItems())
					if(getPlayer().getInventory().validateWeight(giveit) && getPlayer().getInventory().validateCapacity(giveit))
						getInventory().transferItem("TransferFromPetAll", giveit.getObjectId(), giveit.getCount(), getPlayer().getInventory(), getPlayer(), this);
					else
					{
						L2ItemInstance dropit = _inventory.dropItem("PetDrop", giveit.getObjectId(), giveit.getCount(), getPlayer(), null);
						if(dropit != null)
							dropit.dropMe(this, getLoc().changeZ(25));
					}
			}
		}
		catch(final Exception e)
		{
			_log.warn("Give all items error " + e);
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " " + (getName() == null ? "no name exist" : getName()) + " (" + getNpcId() + ":" + getControlItemObjId() + ") owner " + getPlayer();
	}
}
