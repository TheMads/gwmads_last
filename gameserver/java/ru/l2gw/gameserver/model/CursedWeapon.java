package ru.l2gw.gameserver.model;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.util.Location;

public class CursedWeapon
{
	private final String _name;
	private final int _itemId;
	private final Integer _skillId;
	private final int _skillMaxLevel;
	private int _dropRate;
	private int _durationMin;
	private int _durationMax;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;
	private int _transformationId;
	private int _transformationTemplateId;
	private String _transformationName;

	public enum CursedWeaponState
	{
		NONE,
		ACTIVATED,
		DROPPED,
	}

	private CursedWeaponState _state = CursedWeaponState.NONE;

	private int _nbKills = 0;
	private long _endTime = 0;

	private int _playerId = 0;
	private L2Player _player = null;
	private L2ItemInstance _item = null;
	private int _playerKarma = 0;
	private int _playerPkKills = 0;
	private Location _loc = null;

	public CursedWeapon(int itemId, Integer skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId, 0);
	}

	public void initWeapon()
	{
		_state = CursedWeaponState.NONE;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	/**
	 * Drop of CursedWeapon
	 */
	public boolean dropIt(L2NpcInstance attackable, L2Player killer)
	{
		boolean success = false;

		if(attackable != null)
		{
			if(Rnd.get(100000000) <= _dropRate)
			{
				_item = ItemTable.getInstance().createItem("CursedWeapon.dropIt", _itemId, 1, killer, attackable);
				if(_item != null)
				{
					_player = null;
					_playerId = 0;
					_playerKarma = 0;
					_playerPkKills = 0;
					_state = CursedWeaponState.DROPPED;

					if(_endTime == 0)
						_endTime = System.currentTimeMillis() + getRndDuration() * 60000;

					_item.dropToTheGround(killer, attackable);
					_loc = _item.getLoc();

					_item.setDropTime(0);

					// RedSky and Earthquake
					ExRedSky packet = new ExRedSky(10);
					Earthquake eq = new Earthquake(killer.getLoc(), 30, 12);
					for(L2Player aPlayer : L2ObjectsStorage.getAllPlayers())
					{
						aPlayer.sendPacket(packet);
						aPlayer.sendPacket(eq);
					}
					success = true;
				}
			}
		}
		else if(!Rnd.chance(_disapearChance))
		{

			if(_player == null)
			{
				System.out.println("CursedWeapon owner is null!! WTF??!!");
				return false;
			}

			L2ItemInstance oldItem = _player.getInventory().getItemByItemId(_itemId);
			if(oldItem == null)
				return false;

			long oldCount = oldItem.getCount();
			_player.sendPacket(new ValidateLocation(_player));

			L2ItemInstance dropedItem = _player.getInventory().dropItem("DieDrop", oldItem.getObjectId(), oldCount, _player, killer);
			if(dropedItem == null)
				return false;

			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			_player.setTransformation(0);
			_player.setTransformationName(null);
			_player.removeSkill(SkillTable.getInstance().getInfo(_skillId, _player.getSkillLevel(_skillId)), false);
			_player.removeSkill(SkillTable.getInstance().getInfo(_itemId == 8190 ? 3329 : 3328, 1), false);
			_player.removeSkill(SkillTable.getInstance().getInfo(3330, 1), false);
			_player.removeSkill(SkillTable.getInstance().getInfo(3331, 1), false);
			_player.removeSkill(SkillTable.getInstance().getInfo(3630, 1), false);
			_player.removeSkill(SkillTable.getInstance().getInfo(3631, 1), false);
			_player.sendPacket(new SkillList(_player));
			_player.abortAttack();

			_playerId = 0;
			_playerKarma = 0;
			_playerPkKills = 0;
			_state = CursedWeaponState.DROPPED;

			dropedItem.dropToTheGround(_player, (L2NpcInstance) null);
			_loc = dropedItem.getLoc();

			dropedItem.setDropTime(0);
			_item = dropedItem;

			_player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_DROPPED_S1).addItemName(dropedItem.getItemId()));

			_player.refreshExpertisePenalty();
			_player.broadcastUserInfo(true);

			Earthquake eq = new Earthquake(_player.getLoc(), 30, 12);
			_player.broadcastPacket(eq);

			success = true;
		}
		return success;
	}

	private void giveSkill()
	{
		int level = 1 + _nbKills / _stageKills;
		if(level > _skillMaxLevel)
			level = _skillMaxLevel;

		_player.addSkill(SkillTable.getInstance().getInfo(_skillId, level), false);
		_player.addSkill(SkillTable.getInstance().getInfo(_itemId == 8190 ? 3329 : 3328, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3330, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3331, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3630, 1), false);
		_player.addSkill(SkillTable.getInstance().getInfo(3631, 1), false);
		_player.sendPacket(new SkillList(_player));
	}

	/**
	 * вызывается при загрузке оружия
	 */
	public boolean reActivate()
	{
		if(getTimeLeft() <= 0)
		{
			if(_playerId != 0)
				// to be sure, that cursed weapon will deleted in right way
				_state = CursedWeaponState.ACTIVATED;
			return false;
		}
		else if(_playerId == 0)
		{
			if(_loc == null)
				return false;

			_item = ItemTable.getInstance().createItem("CursedWeapon.reActivate", _itemId, 1, null, null);
			if(_item == null)
				return false;

			_item.dropMe(null, _loc);
			_item.setDropTime(0);

			_state = CursedWeaponState.DROPPED;
		}
		else
			_state = CursedWeaponState.ACTIVATED;
		return true;
	}

	public void activate(L2Player player, L2ItemInstance item)
	{
		// оружие уже в руках игрока или новый игрок
		if(_state != CursedWeaponState.ACTIVATED || _playerId != player.getObjectId())
		{
			_playerKarma = player.getKarma();
			_playerPkKills = player.getPkKills();
		}

		player.stopEffects("transformation");

		_state = CursedWeaponState.ACTIVATED;
		_player = player;
		_playerId = player.getObjectId();

		player.setCursedWeaponEquippedId(_itemId);
		player.setTransformationName(_transformationName);
		player.setTransformationTemplate(_transformationTemplateId);
		player.setKarma(9999999);
		player.setPkKills(_nbKills);

		if(player.isInParty())
			player.getParty().oustPartyMember(player);

		if(player.getMountEngine().isMounted())
			player.getMountEngine().dismount();

		if(_endTime == 0)
			_endTime = System.currentTimeMillis() + getRndDuration() * 60000;

		giveSkill();

		_item = item;
		GArray<L2ItemInstance> items = player.getInventory().equipItemAndRecord(_item);
		player.sendChanges();
		player.sendPacket(new InventoryUpdate(items));
		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1).addItemName(_item.getItemId()));

		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());

		player.setTransformation(_transformationId);
	}

	public void increaseKills()
	{
		if(_player == null)
		{
			System.out.println("CursedWeapon owner is null!! WTF??!!");
			return;
		}
		_nbKills++;

		_player.setPkKills(_nbKills);
		_player.broadcastUserInfo(true);

		if(_nbKills % _stageKills == 0 && _nbKills <= _stageKills * (_skillMaxLevel - 1))
			giveSkill();

		// Reduce time-to-live
		_endTime -= _durationLost * 60000;
	}

	public void increaseLevel()
	{
		setNbKills(getStageKills() - 1);
		increaseKills();
	}

	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}

	public void setDropRate(int dropRate)
	{
		_dropRate = dropRate;
	}

	public void setDurationMin(int duration)
	{
		_durationMin = duration;
	}

	public void setDurationMax(int duration)
	{
		_durationMax = duration;
	}

	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}

	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}

	public void setTransformationId(int transformationId)
	{
		_transformationId = transformationId;
	}

	public int getTransformationId()
	{
		return _transformationId;
	}

	public void setTransformationTemplateId(int transformationTemplateId)
	{
		_transformationTemplateId = transformationTemplateId;
	}

	public void setTransformationName(String name)
	{
		_transformationName = name;
	}

	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}

	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}

	public void setPlayerKarma(int playerKarma)
	{
		_playerKarma = playerKarma;
	}

	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}

	public void setState(CursedWeaponState state)
	{
		_state = state;
	}

	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}

	public void setPlayer(L2Player player)
	{
		_player = player;
	}

	public void setItem(L2ItemInstance item)
	{
		_item = item;
	}

	public void setLoc(Location loc)
	{
		_loc = loc;
	}

	public CursedWeaponState getState()
	{
		return _state;
	}

	public boolean isActivated()
	{
		return _state == CursedWeaponState.ACTIVATED;
	}

	public boolean isDropped()
	{
		return _state == CursedWeaponState.DROPPED;
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public L2ItemInstance getItem()
	{
		return _item;
	}

	public Integer getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public L2Player getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	/**
	 * Возвращает позицию (x, y, z)
	 * @return Location
	 */
	public Location getLoc()
	{
		return _loc;
	}

	public int getRndDuration()
	{
		if(_durationMin > _durationMax)
			_durationMax = 2 * _durationMin;
		return Rnd.get(_durationMin, _durationMax);
	}

	public boolean isActive()
	{
		return _state == CursedWeaponState.ACTIVATED || _state == CursedWeaponState.DROPPED;
	}

	public int getLevel()
	{
		if(_nbKills > _stageKills * _skillMaxLevel)
			return _skillMaxLevel;
		return _nbKills / _stageKills;
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public Location getWorldPosition()
	{
		if(isActivated())
		{
			if(_player != null && _player.isOnline())
				return _player.getLoc();
		}
		else if(isDropped())
			if(_item != null)
				return _item.getLoc();

		return null;
	}
}