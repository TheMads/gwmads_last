package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.ExMPCCPartyInfoUpdate;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.util.List;

public class L2CommandChannel
{
	private List<L2Party> _commandChannelParties;
	private L2Player _commandChannelLeader;
	private int _commandChannelLvl;
	private boolean _lootingRights;
	private int _mpccId;

	/**
	 * Creates a New Command Channel and Add the Leaders party to the CC
	 * @param CommandChannelLeader
	 */
	public L2CommandChannel(L2Player leader)
	{
		_commandChannelLeader = leader;
		_commandChannelParties = new FastList<L2Party>();
		_commandChannelParties.add(leader.getParty());
		_commandChannelLvl = leader.getParty().getLevel();
		_lootingRights = false;
		leader.getParty().setCommandChannel(this);
		leader.getParty().broadcastToPartyMembers(Msg.ExMPCCOpen);
		_mpccId = IdFactory.getInstance().getNextId();
	}

	/**
	 * Adds a Party to the Command Channel
	 * @param Party
	 */
	public void addParty(L2Party party)
	{
		broadcastToChannelMembers(new ExMPCCPartyInfoUpdate(party, 1));
		_commandChannelParties.add(party);
		if(party.getLevel() > _commandChannelLvl)
			_commandChannelLvl = party.getLevel();
		party.setCommandChannel(this);
		party.broadcastToPartyMembers(Msg.ExMPCCOpen);
	}

	/**
	 * Removes a Party from the Command Channel
	 * @param Party
	 */
	public void removeParty(L2Party party)
	{
		_commandChannelParties.remove(party);
		_commandChannelLvl = 0;
		for(L2Party pty : _commandChannelParties)
			if(pty.getLevel() > _commandChannelLvl)
				_commandChannelLvl = pty.getLevel();
		party.setCommandChannel(null);
		party.broadcastToPartyMembers(Msg.ExMPCCClose);
		if(_commandChannelParties.size() < 2)
			disbandChannel();
		else
			broadcastToChannelMembers(new ExMPCCPartyInfoUpdate(party, 0));
	}

	/**
	 * Распускает Command Channel
	 */
	public void disbandChannel()
	{
		broadcastToChannelMembers(new SystemMessage(SystemMessage.THE_COMMAND_CHANNEL_HAS_BEEN_DISBANDED));
		for(L2Party party : _commandChannelParties)
			if(party != null)
			{
				party.setCommandChannel(null);
				party.broadcastToPartyMembers(Msg.ExMPCCClose);
			}
		_commandChannelParties = null;
		_commandChannelLeader = null;
	}

	/**
	 * @return overall count members of the Command Channel
	 */
	public int getMemberCount()
	{
		int count = 0;
		for(L2Party party : _commandChannelParties)
			if(party != null)
				count += party.getMemberCount();
		return count;
	}

	/**
	 * Broadcast packet to every channel member
	 * @param L2GameServerPacket
	 */
	public void broadcastToChannelMembers(L2GameServerPacket gsp)
	{
		if(_commandChannelParties != null && !_commandChannelParties.isEmpty())
			for(L2Party party : _commandChannelParties)
				if(party != null)
					party.broadcastToPartyMembers(gsp);
	}

	public void broadcastToPartyLeaders(L2GameServerPacket gsp)
	{
		if(!_commandChannelParties.isEmpty())
			for(L2Party party : _commandChannelParties)
				if(party != null && party.getPartyLeader() != null)
					party.getPartyLeader().sendPacket(gsp);

	}

	/**
	 * @return list of Parties in Command Channel
	 */
	public List<L2Party> getParties()
	{
		return _commandChannelParties;
	}

	/**
	 * @return list of all Members in Command Channel
	 */
	public List<L2Player> getMembers()
	{
		List<L2Player> members = new FastList<L2Player>();
		for(L2Party party : getParties())
			members.addAll(party.getPartyMembers());
		return members;
	}

	/**
	 * @return Level of CC
	 */
	public int getLevel()
	{
		return _commandChannelLvl;
	}

	/**
	 * @param sets the leader of the Command Channel
	 */
	public void setChannelLeader(L2Player newLeader)
	{
		_commandChannelLeader = newLeader;
		broadcastToChannelMembers(new SystemMessage(SystemMessage.COMMAND_CHANNEL_AUTHORITY_HAS_BEEN_TRANSFERRED_TO_S1).addString(newLeader.getName()));
	}

	/**
	 * @return the leader of the Command Channel
	 */
	public L2Player getChannelLeader()
	{
		return _commandChannelLeader;
	}

	public void setLootingRights(boolean lootingRights)
	{
		_lootingRights = lootingRights;
	}

	public boolean getLootingRights()
	{
		return _lootingRights;
	}

	public int getCommandChannelId()
	{
		return _mpccId;
	}

	public boolean containsMember(L2Character cha)
	{
		if(cha == null)
			return false;
		L2Player player = cha.getPlayer();
		if(player == null)
			return false;

		for(L2Player member : getMembers())
			if(member != null && member.getObjectId() == player.getObjectId())
				return true;

		return false;
	}

	/**
	 * Queen Ant, Core, Orfen, Zaken: MemberCount > 36<br>
	 * Baium: MemberCount > 56<br>
	 * Antharas: MemberCount > 225<br>
	 * Valakas: MemberCount > 99<br>
	 * normal RaidBoss: MemberCount > 18
	 *
	 * @param obj
	 * @return true if proper condition for RaidWar
	 */
	public boolean meetRaidWarCondition(L2Object obj)
	{
		if(!obj.isRaid())
			return false;
		int npcId = ((L2MonsterInstance) obj).getNpcId();
		switch(npcId)
		{
			case 29001: // Queen Ant
			case 29006: // Core
			case 29014: // Orfen
			case 29022: // Zaken
				return getMemberCount() > 36;
			case 29020: // Baium
				return getMemberCount() > 56;
			case 29019: // Antharas
				return getMemberCount() > 225;
			case 29028: // Valakas
				return getMemberCount() > 99;
			default: // normal Raidboss
				return getMemberCount() > 18;
		}
	}
}