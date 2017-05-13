package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.ai.L2CharacterAI;
import ru.l2gw.gameserver.ai.L2StaticObjectAI;
import ru.l2gw.gameserver.geodata.GeoControl;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.instance.Instance;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.ConfirmDlg;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.StaticObject;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2CharTemplate;
import ru.l2gw.gameserver.templates.L2Weapon;

import java.util.HashMap;

public class L2DoorInstance extends L2Character implements GeoControl
{
	protected static Log _log = LogFactory.getLog(L2DoorInstance.class.getName());

	protected final int _doorId;
	protected final String _name;
	private boolean _isOpen;
	public boolean _geoOpen;
	private boolean _unlockable;
	private boolean _isHPVisible;
	private SiegeUnit _siegeUnit;
	public L2Territory _geoPos;
	private int _closeTime;

	private boolean _destroyable;
	private final boolean _isWall;
	private int _grade;
	private double _hpMult = 0;

	private HashMap<Long, Byte> _geoAround;

	public L2Territory getGeoPos()
	{
		return _geoPos;
	}

	public void setGeoPos(L2Territory value)
	{
		_geoPos = value;
	}

	public HashMap<Long, Byte> getGeoAround()
	{
		return _geoAround;
	}

	public void setGeoAround(HashMap<Long, Byte> value)
	{
		_geoAround = value;
	}

	@Override
	public L2CharacterAI getAI()
	{
		if(_ai == null)
			_ai = new L2StaticObjectAI(this);
		return _ai;
	}

	public L2DoorInstance(int objectId, L2CharTemplate template, int doorId, String name, boolean unlockable, boolean destroyable, int grade, boolean showHp)
	{
		super(objectId, template);
		_doorId = doorId;
		_name = name;
		_isWall = name.contains("_wall_");
		_unlockable = unlockable;
		_destroyable = destroyable;
		_grade = grade;
		_isHPVisible = showHp;
		_geoOpen = true;
		_geoPos = new L2Territory("door_" + doorId);
		_closeTime = 0;
	}

	public L2DoorInstance(int objectId, L2DoorInstance door)
	{
		super(objectId, door.getTemplate());
		_doorId = door._doorId;
		_name = door._name;
		_isWall = door._isWall;
		_unlockable = door._unlockable;

		_destroyable = door._destroyable;
		_grade = door._grade;
		_isHPVisible = door._isHPVisible;
		_geoOpen = true;
		_geoPos = door._geoPos;
		_closeTime = door._closeTime;
		_geoAround = new HashMap<Long, Byte>(door._geoAround.size());
		_geoAround.putAll(door._geoAround);
	}

	public final boolean isUnlockable()
	{
		return _unlockable;
	}

	@Override
	public final byte getLevel()
	{
		return 1;
	}

	/**
	 * @return Returns the doorId.
	 */
	public int getDoorId()
	{
		return _doorId;
	}

	/**
	 * @return Returns the open.
	 */
	public boolean isOpen()
	{
		return _isOpen;
	}

	/**
	 * @param open The open to set.
	 */
	public synchronized void setOpen(boolean open)
	{
		_isOpen = open;
	}

	public int getDamage()
	{
		int dmg = 6 - (int) Math.ceil(getCurrentHp() / getMaxHp() * 6);
		if(dmg > 6)
			return 6;
		if(dmg < 0)
			return 0;
		return dmg;
	}

	//TODO разобраться
	public boolean isEnemyOf(@SuppressWarnings("unused") L2Character cha)
	{
		return true;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		if(!isDestroyable())
			return false;
		if(attacker == null)
			return false;

		if(getReflection() > 0)
			return isDestroyable();

		L2Player player = attacker.getPlayer();
		return player != null && _siegeUnit != null && (_siegeUnit.getSiege() != null && _siegeUnit.getSiege().isInProgress() || (_siegeUnit.isFort && TerritoryWarManager.isFortInWar(_siegeUnit)));
	}

	public boolean isDestroyable()
	{
		return _destroyable;
	}

	public int getGrade()
	{
		return _grade;
	}

	@Override
	public void updateAbnormalEffect()
	{
	}

	/**
	 * Return null.<BR><BR>
	 */
	@Override
	public L2ItemInstance getActiveWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getActiveWeaponItem()
	{
		return null;
	}

	@Override
	public L2ItemInstance getSecondaryWeaponInstance()
	{
		return null;
	}

	@Override
	public L2Weapon getSecondaryWeaponItem()
	{
		return null;
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(player == null)
			return;

		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		if(this != player.getTarget())
		{
			if(player.setTarget(this))
			{
				player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));

				if(isAttackable(player, false, false))
					//player.sendPacket(new DoorStatusUpdate(this));
					player.sendPacket(new StaticObject(this));
			}
		}
		else
		{
			//player.sendPacket(new MyTargetSelected(getObjectId(), player.getLevel()));
			if(isAttackable(player, false, false))
			{
				if(Math.abs(player.getZ() - getZ()) < 400) // this max heigth difference might need some tweaking
					player.getAI().Attack(this, false, dontMove);
				else
					player.sendActionFailed();
			}
			else if(!isInRange(player, getInteractDistance(player)))
				if(!dontMove)
					player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
				else
					player.sendActionFailed();
			else if(_siegeUnit != null)
			{
				if(player.getClanId() == 0 || _siegeUnit.getOwnerId() != player.getClanId())
					player.sendActionFailed();
				else if(_siegeUnit.getSiege() != null && _siegeUnit.getSiege().isInProgress())
					player.sendActionFailed();
				else if(_siegeUnit.isClanHall)
				{
					if((player.getClanPrivileges() & L2Clan.CP_CH_OPEN_DOOR) == L2Clan.CP_CH_OPEN_DOOR)
					{
						if(!isOpen())
							player.sendPacket(new ConfirmDlg(SystemMessage.WOULD_YOU_LIKE_TO_OPEN_THE_GATE, 0, getDoorId()));
						else
							player.sendPacket(new ConfirmDlg(SystemMessage.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE, 0, getDoorId()));
					}
					else
						player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
				}
				else if(_siegeUnit.isFort)
				{
					boolean ok = true;
					for(L2Character cha : getKnownCharacters(200))
						if(cha instanceof L2DoormenInstance)
						{
							ok = false;
							break;
						}

					if(ok)
					{
						if((player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR)
						{
							if(!isOpen())
								player.sendPacket(new ConfirmDlg(SystemMessage.WOULD_YOU_LIKE_TO_OPEN_THE_GATE, 0, getDoorId()));
							else
								player.sendPacket(new ConfirmDlg(SystemMessage.WOULD_YOU_LIKE_TO_CLOSE_THE_GATE, 0, getDoorId()));
						}
						else
							player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT));
					}
				}
			}
		}

		player.sendActionFailed();
	}

	@Override
	public void broadcastStatusUpdate()
	{
		//DoorStatusUpdate su = new DoorStatusUpdate(this);
		StaticObject su = new StaticObject(this);
		for(L2Player player : L2World.getAroundPlayers(this))
			player.sendPacket(su);
	}

	public void onOpen()
	{
		if(_closeTime > 0)
			scheduleCloseMe(_closeTime * 1000L);
	}

	public void onClose()
	{
		closeMe();
	}

	/**
	 * Вызывает задание на закрытие двери через заданное время.
	 *
	 * @param delay Время в миллисекундах
	 */
	public final void scheduleCloseMe(long delay)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new CloseTask(), delay);
	}

	public final void closeMe()
	{
		//_log.info("Open me");
		if(getCurrentHp() == 0 && getMaxHp() != 0)
		{
			_log.info("currentHP = " + String.valueOf(getCurrentHp()) + "maxHP = " + String.valueOf(getMaxHp()));
			return;
		}

		synchronized(this)
		{
			if(!_isOpen)
				return;

			_isOpen = false;
		}

		setGeoOpen(false);
		broadcastStatusUpdate();
		fireMethodInvoked(MethodCollection.onDoorOpenClose, new Integer[]{0});
	}

	public final void openMe()
	{
		synchronized(this)
		{
			if(_isOpen)
				return;

			_isOpen = true;
		}

		setGeoOpen(true);
		broadcastStatusUpdate();
		fireMethodInvoked(MethodCollection.onDoorOpenClose, new Integer[]{1});
	}

	@Override
	public String toString()
	{
		return "door[id=" + _doorId + ";objId=" + getObjectId() + ";refId=" + _reflection + ";]";
	}

	public String getDoorName()
	{
		return _name;
	}

	public void setSiegeUnit(SiegeUnit clanhall)
	{
		_siegeUnit = clanhall;
	}

	public SiegeUnit getSiegeUnit()
	{
		return _siegeUnit;
	}

	@Override
	public void doDie(L2Character killer)
	{
		Siege s = SiegeManager.getSiege(this);
		if(s != null)
		{
			for(SiegeClan sc : s.getDefenderClans().values())
			{
				L2Clan clan = sc.getClan();
				if(clan != null)
					for(L2Player player : clan.getOnlineMembers(""))
						if(player != null)
							if(s.getSiegeUnit().isCastle)
								player.sendPacket(new SystemMessage(SystemMessage.THE_CASTLE_GATE_HAS_BEEN_BROKEN_DOWN));
							else if(s.getSiegeUnit().isFort)
								player.sendPacket(new SystemMessage(SystemMessage.ENEMY_BLOOD_PLEDGES_HAVE_INTRUDED_INTO_THE_FORTRESS));
			}

			for(SiegeClan sc : s.getAttackerClans().values())
			{
				L2Clan clan = sc.getClan();
				if(clan != null)
					for(L2Player player : clan.getOnlineMembers(""))
						if(player != null)
							if(s.getSiegeUnit().isCastle)
								player.sendPacket(new SystemMessage(SystemMessage.THE_CASTLE_GATE_HAS_BEEN_BROKEN_DOWN));
							else if(s.getSiegeUnit().isFort)
								player.sendPacket(new SystemMessage(SystemMessage.ENEMY_BLOOD_PLEDGES_HAVE_INTRUDED_INTO_THE_FORTRESS));
			}
		}

		setGeoOpen(true);

		super.doDie(killer);
	}

	@Override
	public void spawnMe()
	{
		super.spawnMe();
		closeMe();
		setGeoOpen(_isOpen);
	}

	public boolean isHPVisible()
	{
		return _isHPVisible;
	}

	public void setHPVisible(boolean val)
	{
		_isHPVisible = val;
	}

	@Override
	public int getMaxHp()
	{
		if(_hpMult > 0)
			return (int) (super.getMaxHp() * _hpMult);
		return super.getMaxHp();
	}

	@Override
	public int getPDef(L2Character target)
	{
		if(getSiegeUnit() != null && getSiegeUnit().isCastle)
		{
			switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
			{
				case SevenSigns.CABAL_DAWN:
					return (int) (super.getPDef(target) * Config.SIEGE_DAWN_GATES_PDEF_MULT);
				case SevenSigns.CABAL_DUSK:
					return (int) (super.getPDef(target) * Config.SIEGE_DUSK_GATES_PDEF_MULT);
			}
		}
		return super.getPDef(target);
	}

	@Override
	public int getMDef(L2Character target, L2Skill skill)
	{
		if(getSiegeUnit() != null && getSiegeUnit().isCastle)
		{
			switch(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
			{
				case SevenSigns.CABAL_DAWN:
					return (int) (super.getMDef(target, skill) * Config.SIEGE_DAWN_GATES_MDEF_MULT);
				case SevenSigns.CABAL_DUSK:
					return (int) (super.getMDef(target, skill) * Config.SIEGE_DUSK_GATES_MDEF_MULT);
			}
		}
		return super.getMDef(target, skill);
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	/**
	 * Двери на осадах уязвимы во время осады.
	 * Остальные двери не уязвимы вообще.
	 *
	 * @return инвульная ли дверь.
	 */
	@Override
	public boolean isInvul()
	{
		if(getReflection() > 0)
			return !isDestroyable();

		return !(_siegeUnit != null && (_siegeUnit.getSiege() != null && _siegeUnit.getSiege().isInProgress() || _siegeUnit.isFort && TerritoryWarManager.isFortInWar(_siegeUnit)));
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	/**
	 * Устанавливает значение закрытости\открытости в геодате<br>
	 *
	 * @param val новое значение
	 */
	private void setGeoOpen(boolean val)
	{
		if(_reflection > 0 && Config.DEBUG_INSTANCES)
			Instance._log.info(this + " setGeoOpen: " + _geoOpen + " == " + val);
		if(_geoOpen == val)
			return;

		_geoOpen = val;

		if(val)
			GeoEngine.returnGeoAtControl(this);
		else
			GeoEngine.applyControl(this);
	}

	private class CloseTask implements Runnable
	{
		public void run()
		{
			onClose();
		}
	}

	public void setHpMult(double hpMult)
	{
		_hpMult = hpMult;
	}

	public int getXMin()
	{
		return _geoPos.getXmin();
	}

	public int getYMin()
	{
		return _geoPos.getYmin();
	}

	public int getZMin()
	{
		return _geoPos.getZmin();
	}

	public int getXMax()
	{
		return _geoPos.getXmax();
	}

	public int getYMax()
	{
		return _geoPos.getYmax();
	}

	public int getZMax()
	{
		return _geoPos.getZmax();
	}

	public void setCloseTime(int time)
	{
		_closeTime = time;
	}

	@Override
	public void decayMe()
	{
		boolean d = _hidden;
		super.decayMe();
		if(!d)
			GeoEngine.deleteControl(this);
	}

	public boolean isWall()
	{
		return _isWall;
	}

	@Override
	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		if(!isDestroyable() || target == null)
			return L2Skill.TargetType.door;

		if(getReflection() > 0 && isDestroyable())
		    return L2Skill.TargetType.enemy;

		L2Player player = target.getPlayer();

		if(player != null && (_siegeUnit != null && (_siegeUnit.getSiege() != null && _siegeUnit.getSiege().isInProgress() || _siegeUnit.isFort && TerritoryWarManager.isFortInWar(_siegeUnit))))
			return L2Skill.TargetType.enemy;

		return L2Skill.TargetType.door;
	}
}

