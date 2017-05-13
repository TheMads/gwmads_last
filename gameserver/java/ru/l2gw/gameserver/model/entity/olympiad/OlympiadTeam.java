package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2CubicInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.commons.arrays.GCSArray;
import ru.l2gw.util.Location;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author rage
 * @date 05.05.11 11:38
 */
public class OlympiadTeam
{
	private final GCSArray<OlympiadUserInfo> _team;
	private final int _gameType;
	private int _leaderObjectId;
	private String _name;
	private String _disconnectedPlayer;
	private int _arenaId = -1;

	public OlympiadTeam(int gameType)
	{
		_gameType = gameType;
		if(_gameType == 0)
			_team = new GCSArray<>(3);
		else
			_team = new GCSArray<>(1);
	}

	public void addPlayer(L2Player player)
	{
		_team.add(new OlympiadUserInfo(player));
	}

	public void setLeader(L2Player player)
	{
		_leaderObjectId = player.getObjectId();
		_name = player.getName();
	}

	public int getLeaderObjectId()
	{
		return _leaderObjectId;
	}

	public void setOlympiadGameId(int gameId)
	{
		_arenaId = gameId;
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
				player.setOlympiadGameId(gameId);
		}
	}

	public GCSArray<OlympiadUserInfo> getPlayersInfo()
	{
		return _team;
	}

	public boolean checkParty()
	{
		L2Player player = _team.get(0).getPlayer();
		if(player == null)
		{
			_disconnectedPlayer = _team.get(0).getName();
			Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: player " + _team.get(0).getName() + " is null.");
			return false;
		}
		L2Party party = player.getParty();
		if(party == null)
		{
			Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: party is null.");
			return false;
		}
		if(party.getMemberCount() != 3)
		{
			Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: wrong members count: " + party.getPartyMembers());
			return false;
		}
		if(party.getPartyLeaderOID() != _leaderObjectId)
		{
			Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: wrong party leader: " + party.getPartyLeader() + " expected: player " + _name);
			return false;
		}

		for(L2Player member : party.getPartyMembers())
			if(!contains(member.getObjectId()))
			{
				_disconnectedPlayer = member.getName();
				Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: wrong party member: " + member);
				return false;
			}
		for(OlympiadUserInfo oui : _team)
			if(!party.containsMember(oui.getPlayer()))
			{
				_disconnectedPlayer = oui.getName();
				Olympiad._olyLog.warn("OG(" + _arenaId + ") party check: no party member: player " + oui.getName());
				return false;
			}

		return true;
	}

	public boolean isOnline()
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null && !player.isInOfflineMode() && player.getOlympiadGameId() >= 0 && !player.inObserverMode() && !player.isDeleting())
				return true;
			else
				_disconnectedPlayer = oui.getName();
		}

		return false;
	}

	public boolean isAllOnline()
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player == null || player.isInOfflineMode() || player.getOlympiadGameId() == -1 || player.inObserverMode() || player.isDeleting())
			{
				_disconnectedPlayer = oui.getName();
				return false;
			}
		}

		return true;
	}

	public boolean isDead()
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null && !player.isDead() && player.isOnline() && !player.isDeleting() && player.getOlympiadGameId() >= 0)
				return false;
		}
		return true;
	}

	public boolean isInZone(L2Zone.ZoneType zt)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player == null || player.isInZone(zt))
			{
				_disconnectedPlayer = oui.getName();
				return true;
			}
		}
		return false;
	}

	public void sendPacket(L2GameServerPacket gsp)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
				player.sendPacket(gsp);
		}
	}

	public String getDisconnectedPlayerName()
	{
		return _disconnectedPlayer;
	}

	public String getName()
	{
		return _name;
	}

	public int getGameType()
	{
		return _gameType;
	}

	public void preparePlayers(int side)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player == null)
				continue;

			if(player.getReflection() != 0)
				player.setReflection(0);

			player.setOlympiadSide(side);

			Olympiad._olyLog.info("OG(" + _arenaId + ") Teleport prepare player " + player.getName());

			player.setStablePoint(player.getLoc());

			if(player.isSitting())
				player.standUp();

			//Abort casting if player casting
			if(player.isCastingNow())
				player.abortCast();

			player.setTarget(null);

			player.setIsInOlympiadMode(true);

			//Remove Clan Skills
			if(player.getClanId() != 0)
			{
				for(L2Skill skill : player.getClan().getAllSkills())
					player.removeSkill(skill, false);

				if(player.getClan().getHasCastle() > 0)
					TerritoryWarManager.getTerritoryById(player.getClan().getHasCastle() + 80).removeSkills(player);
			}

			//Remove Hero Skills
			if(player.isHero())
			{
				player.removeSkillById(395);
				player.removeSkillById(396);
				player.removeSkillById(1374);
				player.removeSkillById(1375);
				player.removeSkillById(1376);
			}

			//Remove Buffs
			player.stopAllEffects();

			//Remove Summon's Buffs
			if(player.getPet() != null)
			{
				L2Summon summon = player.getPet();
				summon.stopAllEffects();

				if(summon instanceof L2PetInstance)
					summon.unSummon();
			}

			// Unsummon Cubics
			if(player.getCubics().size() > 0)
			{
				boolean update = false;
				for(L2CubicInstance cubic : player.getCubics())
					if(cubic != null && cubic.givenByOther())
					{
						player.delCubic(cubic.getId());
						cubic.deleteMe();
						update = true;
					}

				if(update)
					player.broadcastUserInfo();
			}

			// unsummon agathion
			if(player.getAgathionId() != 0)
				player.setAgathion(0);

			//Remove player from his party
			if(_gameType != 0 && player.getParty() != null)
			{
				L2Party party = player.getParty();
				party.removePartyMember(player);
			}

			//Remove Hero Weapons
			// check to prevent the using of weapon/shield on strider/wyvern
			L2ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);

			if(wpn == null)
				wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LRHAND);

			if(wpn != null && wpn.isHeroItem())
			{
				player.getInventory().unEquipItemAndSendChanges(wpn);
				player.abortAttack();
				player.broadcastUserInfo(true);
			}

			player.unEquipInappropriateItems();

			//Remove ss/sps/bsps automation
			ConcurrentSkipListSet<Integer> activeSoulShots = player.getAutoSoulShot();
			for(int itemId : activeSoulShots)
			{
				player.removeAutoSoulShot(itemId);
				player.sendPacket(new ExAutoSoulShot(itemId, false));
				SystemMessage sm = new SystemMessage(SystemMessage.THE_AUTOMATIC_USE_OF_S1_WILL_NOW_BE_CANCELLED);
				sm.addItemName(itemId);
				player.sendPacket(sm);
			}

			for(L2Skill skill : player.getAllSkills())
				if(player.isSkillDisabled(skill.getId()) && skill.getReuseDelay() <= 900000)
					player.enableSkill(skill.getId());

			player.sendPacket(new SkillCoolTime(player));
			player.sendPacket(new SkillList(player));
		}
	}

	public void setTeam(int team)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
				player.setTeam(team);
		}
	}

	public void restoreHpMp()
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
			{
				//Set HP/CP/MP to Max
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
			}
		}
	}

	public void teleToLocation(Location loc, int reflection)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
				player.teleToLocation(loc, reflection);
		}
	}

	public void setIsOlympiadStart(boolean b)
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null)
				player.setIsOlympiadStart(b);
		}
	}

	public void addDamage(int objectId, int damage)
	{
		for(OlympiadUserInfo oui : _team)
			if(oui.getObjectId() == objectId)
			{
				oui.addDamage(damage);
				break;
			}
	}

	public boolean contains(int objectId)
	{
		for(OlympiadUserInfo oui : _team)
			if(oui.getObjectId() == objectId)
				return true;

		return false;
	}

	public int getPoints()
	{
		int p = 0;
		for(OlympiadUserInfo oui : _team)
			p += oui.getPoints();

		return p;
	}

	public void revive()
	{
		for(OlympiadUserInfo oui : _team)
		{
			L2Player player = oui.getPlayer();
			if(player != null && player.isDead())
			{
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentMp(player.getMaxMp());
				player.broadcastPacket(new Revive(player));
			}
		}
	}
}
