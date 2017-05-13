package ru.l2gw.gameserver.model;

import java.util.Vector;

import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.serverpackets.ExClosePartyRoom;
import ru.l2gw.gameserver.serverpackets.ExPartyRoomMember;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.PartyMatchDetail;
import ru.l2gw.gameserver.serverpackets.PartyMatchList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class PartyRoom
{
	private final int _id;
	private int _minLevel;
	private int _maxLevel;
	private int _lootDist;
	private int _maxMembers;
	private String _title;
	private Vector<L2Player> _members = new Vector<L2Player>();

	public PartyRoom(int id, int minLevel, int maxLevel, int maxMembers, int lootDist, String title, L2Player leader)
	{
		_id = id;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_maxMembers = maxMembers;
		_lootDist = lootDist;
		_title = title;
		_members.add(leader);
		leader.setPartyRoom(_id);
	}

	public void addMember(L2Player member)
	{
		if(!_members.contains(member))
		{
			_members.add(member);
			member.setPartyRoom(_id);
			for(L2Player player : _members)
				if(player != null)
				{
					player.sendPacket(new PartyMatchList(this));
					player.sendPacket(new ExPartyRoomMember(this, player));
					player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_ENTERED_THE_PARTY_ROOM).addString(member.getName()));
				}
			PartyRoomManager.getInstance().removeFromWaitingList(member);
			member.broadcastUserInfo(true);
		}
	}

	public void removeMember(L2Player member, boolean oust)
	{
		_members.remove(member);
		member.setPartyRoom(0);
		if(_members.size() == 0)
			PartyRoomManager.getInstance().removeRoom(getId());
		for(L2Player player : _members)
			if(player != null)
			{
				player.sendPacket(new PartyMatchList(this));
				player.sendPacket(new ExPartyRoomMember(this, player));
				if(oust)
					player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_OUSTED_FROM_THE_PARTY_ROOM).addString(member.getName()));
				else
					player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_LEFT_THE_PARTY_ROOM).addString(member.getName()));
			}
		member.sendPacket(new ExClosePartyRoom());
		member.sendPacket(new PartyMatchDetail(member));
		if(oust)
			member.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_BEEN_OUSTED_FROM_THE_PARTY_ROOM));
		else
			member.sendPacket(new SystemMessage(SystemMessage.S1_HAS_LEFT_THE_PARTY_ROOM));
		PartyRoomManager.getInstance().addToWaitingList(member);
		member.broadcastUserInfo(true);
	}

	public void broadcastPacket(L2GameServerPacket packet)
	{
		for(L2Player player : _members)
			if(player != null)
				player.sendPacket(packet);
	}

	public void updateInfo()
	{
		for(L2Player player : _members)
			if(player != null)
			{
				player.sendPacket(new PartyMatchList(this));
				player.sendPacket(new ExPartyRoomMember(this, player));
			}
	}

	public Vector<L2Player> getMembers()
	{
		return _members;
	}

	public int getMembersSize()
	{
		return _members.size();
	}

	public int getId()
	{
		return _id;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}

	public int getMaxMembers()
	{
		return _maxMembers;
	}

	public int getLootDist()
	{
		return _lootDist;
	}

	public String getTitle()
	{
		return _title;
	}

	public L2Player getLeader()
	{
		if(_members.isEmpty())
			return null;
		return _members.get(0);
	}

	public void setMinLevel(int minLevel)
	{
		_minLevel = minLevel;
	}

	public void setMaxLevel(int maxLevel)
	{
		_maxLevel = maxLevel;
	}

	public void setMaxMembers(int maxMembers)
	{
		_maxMembers = maxMembers;
	}

	public void setLootDist(int lootDist)
	{
		_lootDist = lootDist;
	}

	public void setTitle(String title)
	{
		_title = title;
	}

	public int getLocation()
	{
		return PartyRoomManager.getInstance().getLocation(getLeader());
	}
}