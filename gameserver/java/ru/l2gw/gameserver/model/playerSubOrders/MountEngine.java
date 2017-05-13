package ru.l2gw.gameserver.model.playerSubOrders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.arrays.GSArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.Ride;
import ru.l2gw.gameserver.serverpackets.SetupGauge;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2PetTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;

import static ru.l2gw.gameserver.serverpackets.SetupGauge.GREEN;

public class MountEngine
{
	protected static Log _log = LogFactory.getLog(MountEngine.class.getName());

	private final L2Player _player;

	private int _controlItemObjId;
	private int _mountType;
	private int _currentMeal;
	private int _mountObjId;
	private L2PetTemplate _petTemplate;

	private ScheduledFuture<RidePetEating> _unRideTask;
	private GSArray<ScheduledFuture<?>> _otherDisMountTasks = new GSArray<ScheduledFuture<?>>(3);

	public MountEngine(L2Player player)
	{
		_player = player;
	}

	public void setMount(int npcId)
	{
		L2NpcTemplate npc = NpcTable.getTemplate(npcId);
		if(npc == null)
			return;

		L2PetTemplate petTemplate = new L2PetTemplate(npc.getSet(), npc.getAIParams());
		petTemplate.level = _player.getLevel();
		petTemplate.food = new GArray<Integer>(0);
		petTemplate.attack_speed_on_ride = npc.basePAtkSpd;
		petTemplate.mattack_on_ride = npc.baseMAtk;
		petTemplate.pattack_on_ride = npc.basePAtk;
		petTemplate.speed_on_ride_g = npc.baseRunSpd;
		petTemplate.speed_on_ride_s = npc.baseRunSpd;
		petTemplate.speed_on_ride_f = npc.baseRunSpd;

		setMount(petTemplate, 0);
	}

	public void setMount(L2PetTemplate petTemplate, final int objId)
	{
		if(_player.isCursedWeaponEquipped() || petTemplate == null)
			return;

		if(getUnRideTask() != null)
			cancelUnRideTask();

		cancelOtherDisMount();

		_petTemplate = petTemplate;
		_mountObjId = objId;

		if(_player.getPet() != null)
		{
			_controlItemObjId = _player.getPet().getControlItemObjId();
			_mountObjId = _player.getPet().getObjectId();
		}

		if(_petTemplate.meal_in_normal_on_ride > 0)
		{
			if(_player.getPet() != null)
				_currentMeal = _player.getPet().getCurrentFed();
			else
				_currentMeal = _petTemplate.max_meal;

			_player.sendPacket(new SetupGauge(GREEN, 3600000, (int)(3600000L * _currentMeal / _petTemplate.max_meal)));
			_unRideTask = ThreadPoolManager.getInstance().scheduleAi(new RidePetEating(), 10000, true);
		}

		switch(_petTemplate.npcId)
		{
			case PetDataTable.STRIDER_WIND_ID:
			case PetDataTable.STRIDER_STAR_ID:
			case PetDataTable.STRIDER_TWILIGHT_ID:
			case PetDataTable.RED_STRIDER_WIND_ID:
			case PetDataTable.RED_STRIDER_STAR_ID:
			case PetDataTable.RED_STRIDER_TWILIGHT_ID:
			case PetDataTable.GUARDIANS_STRIDER_ID:
				_player.setRiding(true);
				setMountType(1);
				break;
			case PetDataTable.WYVERN_ID:
				_player.setFlying(true);
				_player.addSkill(SkillTable.getInstance().getInfo(4289, 1), false); // add skill "Wyvern Breath"
				setMountType(2);
				break;
			case PetDataTable.WGREAT_WOLF_ID:
			case PetDataTable.FENRIR_WOLF_ID:
			case PetDataTable.WFENRIR_WOLF_ID:
				_player.setRiding(true);
				setMountType(3);
				break;
			case PetDataTable.AGATION_WOLF_ID:
				_player.setRiding(true);
				setMountType(3);
				break;
			case PetDataTable.GRAY_HORSE_ID: // Gray Horse
				_player.setRiding(true);
				setMountType(4);
				break;
		}

		L2ItemInstance wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		GArray<L2ItemInstance> items = new GArray<L2ItemInstance>(2);
		if(wpn != null)
		{
			_player.sendDisarmMessage(wpn);
			items.addAll(_player.getInventory().unEquipItemAndRecord(wpn));
		}

		wpn = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if(wpn != null)
		{
			_player.sendDisarmMessage(wpn);
			items.addAll(_player.getInventory().unEquipItemAndRecord(wpn));
		}

		if(items.size() > 0)
			_player.sendPacket(new InventoryUpdate(items));

		_player.refreshExpertisePenalty();
		_player.abortAttack();
		_player.abortCast();

		_mountObjId = objId;

		_player.broadcastPacket(new Ride(_player));
		_player.broadcastUserInfo();
	}

	public void dismount()
	{
		_player.setFlying(false);
		_player.setRiding(false);
		if(getMountNpcId() == PetDataTable.WYVERN_ID)
			_player.moveToLocation(_player.getX() + 1, _player.getY() + 1, _player.getZ(), 0, false); // затык на виверну
		_player.removeSkillById(L2Skill.SKILL_WYVERN_BREATH);
		_player.removeSkillFromShortCut(L2Skill.SKILL_WYVERN_BREATH);
		_player.stopEffect(L2Skill.SKILL_HINDER_STRIDER);
		storeFeedLevel();
		cancelUnRideTask();
		cancelOtherDisMount();
		_controlItemObjId = 0;
		_mountType = 0;
		_currentMeal = 0;
		_mountObjId = 0;
		_petTemplate = null;
		_player.sendPacket(new SetupGauge(GREEN, 0));

		if(!_player.isDead())
			_player.broadcastPacket(new Ride(_player));
		_player.broadcastUserInfo();
	}

	public boolean canDismount()
	{
		return _unRideTask == null || _petTemplate == null ||  _currentMeal > _petTemplate.max_meal * _petTemplate.hungry_limit;
	}

	private void storeFeedLevel()
	{
		synchronized(Boolean.valueOf(_player.getPet() == null))//Недаём вызвать пета пока не сохраним
		{
			Connection con = null;
			PreparedStatement statement = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("UPDATE pets SET fed=? WHERE item_obj_id=?");
				statement.setInt(1, _currentMeal);
				statement.setInt(2, _controlItemObjId);
				statement.execute();
			}
			catch(final Exception e)
			{
				_log.warn("could not store Pet feed after ride from item[" + getControlItemObjId() + "]: " + e);
				e.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(con, statement);
			}
		}
	}

	public L2PetTemplate getPetTemplate()
	{
		return _petTemplate;
	}

	public void addMeal(int meal)
	{
		_currentMeal = Math.min(_currentMeal + meal, _petTemplate.max_meal);
		_player.sendPacket(new SetupGauge(GREEN, 3600000, (int)(3600000L * _currentMeal / _petTemplate.max_meal)));
	}

	private class RidePetEating implements Runnable
	{
		public void run()
		{
			try
			{
				if(_currentMeal > 0)
				{
					_currentMeal -= _player.isInCombat() ? _petTemplate.meal_in_battle_on_ride : _petTemplate.meal_in_normal_on_ride;
					if(_currentMeal < 0)
						_currentMeal = 0;

					if(_currentMeal < _petTemplate.max_meal * 0.36)
						_player.sendPacket(new SystemMessage(SystemMessage.YOUR_PET_IS_VERY_HUNGRY));
				}
				else
				{
					_player.getMountEngine().dismount();
					if(Config.ALT_DROP_HUNGRY_PET_CONTROL_ITEM)
						_player.destroyItem("HungryPet", getControlItemObjId(), 1, null, true);

					return;
				}

				_player.sendPacket(new SetupGauge(GREEN, 3600000, (int)(3600000L * _currentMeal / _petTemplate.max_meal)));
				_unRideTask = ThreadPoolManager.getInstance().scheduleAi(this, 10000, true);
			}
			catch(final Throwable e)
			{
				_log.warn(_player + " RidePetEating task error: " + e);
				e.printStackTrace();
			}
		}
	}

	public int getMountNpcLevel()
	{
		return _petTemplate != null ? _petTemplate.level : 0;
	}

	public void cancelUnRideTask()
	{
		if(_unRideTask != null)
		{
			_unRideTask.cancel(false);
			_unRideTask = null;
		}
	}

	public void cancelOtherDisMount()
	{
		for(ScheduledFuture<?> f : _otherDisMountTasks)
			if(f != null)
			{
				f.cancel(false);
				f = null;
			}
	}

	public void addOtherDisMountTask(ScheduledFuture<?> otherDisMountTasks)
	{
		getOtherDisMountTasks().add(otherDisMountTasks);
	}

	public GSArray<ScheduledFuture<?>> getOtherDisMountTasks()
	{
		return _otherDisMountTasks;
	}

	@SuppressWarnings("unchecked")
	public ScheduledFuture<RidePetEating> getUnRideTask()
	{
		return _unRideTask;
	}

	public int getControlItemObjId()
	{
		return _controlItemObjId;
	}

	public int getMountObjId()
	{
		return _mountObjId;
	}

	public int getMountNpcId()
	{
		return _petTemplate != null ? _petTemplate.npcId : 0;
	}

	public boolean isMounted()
	{
		return _petTemplate != null;
	}

	public int getMountType()
	{
		return _mountType;
	}

	public int getRideState()
	{
		return _petTemplate != null ? _petTemplate.ride_state : L2Skill.RideState.ride_none.mask;
	}

	public void setMountType(final int mountType)
	{
		_mountType = mountType;
	}

	public int getMountSpeed()
	{
		if(_petTemplate == null)
			return 0;

		int speed = _petTemplate.speed_on_ride_g;
		if(_player.isSwimming())
			speed = _petTemplate.speed_on_ride_s;
		else if(_player.isFlying())
			speed = _petTemplate.speed_on_ride_f;

		if(_petTemplate.level - _player.getLevel() > 10)
			return speed / 2;

		return speed;
	}

	public int getPAtkSpd()
	{
		return _petTemplate != null ? _petTemplate.attack_speed_on_ride : 0;
	}

	public float getMAtk()
	{
		return _petTemplate != null ? _petTemplate.mattack_on_ride : 0;
	}

	public float getPAtk()
	{
		return _petTemplate != null ? _petTemplate.mattack_on_ride : 0;
	}
}
