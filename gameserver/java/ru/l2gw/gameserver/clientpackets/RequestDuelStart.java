package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.serverpackets.ExDuelAskStart;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

public class RequestDuelStart extends L2GameClientPacket
{
	// format: (ch)Sd
	private String _name;
	private int _duelType;

	@Override
	public void readImpl()
	{
		_name = readS();
		_duelType = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		L2Player target = L2ObjectsStorage.getPlayer(_name);
		if(player == null)
			return;

		if(player.isTransactionInProgress())
		{
			player.sendPacket(Msg.WAITING_FOR_ANOTHER_REPLY);
			return;
		}

		if(target == null || target == player)
		{
			player.sendPacket(Msg.THERE_IS_NO_OPPONENT_TO_RECEIVE_YOUR_CHALLENGE_FOR_A_DUEL);
			return;
		}

		if(!player.isInRange(target, 250))
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_CANNOT_RECEIVE_A_DUEL_CHALLENGE_BECAUSE_S1_IS_TOO_FAR_AWAY).addCharName(target));
			return;
		}

		if(_duelType == 1)
		{
			L2Party party = player.getParty();
			if(party == null || !party.isLeader(player) || party.containsMember(target))
			{
				player.sendPacket(Msg.YOU_ARE_UNABLE_TO_REQUEST_A_DUEL_AT_THIS_TIME);
				return;
			}

			L2Party party2 = target.getParty();
			if(party2 == null)
			{
				player.sendPacket(Msg.SINCE_THE_PERSON_YOU_CHALLENGED_IS_NOT_CURRENTLY_IN_A_PARTY_THEY_CANNOT_DUEL_AGAINST_YOUR_PARTY);
				return;
			}

			target = party2.getPartyLeader();
			if(target.isTransactionInProgress())
			{
				player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addCharName(target));
				return;
			}

			for(L2Player member : party.getPartyMembers())
			{
				SystemMessage msg = Duel.checkPlayer(member);
				if(msg != null)
				{
					player.sendPacket(msg);
					return;
				}
			}

			for(L2Player member : party2.getPartyMembers())
			{
				SystemMessage msg = Duel.checkPlayer(member);
				if(msg != null)
				{
					player.sendPacket(msg);
					return;
				}
			}

			player.setTransactionRequester(target);
			target.setTransactionRequester(player);

			target.sendPacket(new ExDuelAskStart(player.getName(), _duelType));
			player.sendPacket(new SystemMessage(SystemMessage.S1S_PARTY_HAS_BEEN_CHALLENGED_TO_A_DUEL).addCharName(target));
			target.sendPacket(new SystemMessage(SystemMessage.S1S_PARTY_HAS_CHALLENGED_YOUR_PARTY_TO_A_DUEL).addCharName(player));
		}
		else
		{
			if(target.isTransactionInProgress())
			{
				player.sendPacket(new SystemMessage(SystemMessage.S1_IS_BUSY_PLEASE_TRY_AGAIN_LATER).addCharName(target));
				return;
			}

			SystemMessage msg = Duel.checkPlayer(player);
			if(msg != null)
			{
				player.sendPacket(msg);
				return;
			}

			msg = Duel.checkPlayer(target);
			if(msg != null)
			{
				player.sendPacket(msg);
				return;
			}

			player.setTransactionRequester(target);
			target.setTransactionRequester(player);
			target.sendPacket(new ExDuelAskStart(player.getName(), _duelType));

			player.sendPacket(new SystemMessage(SystemMessage.S1_HAS_BEEN_CHALLENGED_TO_A_DUEL).addCharName(target));
			target.sendPacket(new SystemMessage(SystemMessage.S1_HAS_CHALLENGED_YOU_TO_A_DUEL).addCharName(player));
		}
	}
}