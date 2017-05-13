package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.InstanceManager;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.duel.Duel;
import ru.l2gw.gameserver.model.entity.duel.PartyDuel;
import ru.l2gw.gameserver.model.entity.instance.InstanceTemplate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 *  sample
 *  2a
 *  01 00 00 00
 *
 *  format  chddd
 */

public class RequestDuelAnswerStart extends L2GameClientPacket
{
	private int _response;
	private int _duelType;
	@SuppressWarnings("unused")
	private int _unk1;

	@Override
	public void readImpl()
	{
		_duelType = readD();
		_unk1 = readD();
		_response = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		L2Player requestor = player.getTransactionRequester();
		if(requestor == null)
			return;

		if(_response == 1)
		{
			SystemMessage msg1, msg2;
			if(_duelType == 1)
			{
				L2Party party = player.getParty();
				if(party == null)
				{
					requestor.sendPacket(Msg.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					player.setTransactionRequester(null);
					requestor.setTransactionRequester(null);
					return;
				}

				for(L2Player member : party.getPartyMembers())
				{
					msg1 = Duel.checkPlayer(member);
					if(msg1 != null)
					{
						player.sendPacket(msg1);
						requestor.sendPacket(Msg.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
						player.setTransactionRequester(null);
						requestor.setTransactionRequester(null);
						return;
					}
				}

				party = requestor.getParty();
				if(party == null)
				{
					player.sendPacket(Msg.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					player.setTransactionRequester(null);
					requestor.setTransactionRequester(null);
					return;
				}

				for(L2Player member : party.getPartyMembers())
				{
					msg1 = Duel.checkPlayer(member);
					if(msg1 != null)
					{
						requestor.sendPacket(msg1);
						player.sendPacket(Msg.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
						player.setTransactionRequester(null);
						requestor.setTransactionRequester(null);
						return;
					}
				}

				InstanceTemplate it = InstanceManager.getInstance().getInstanceTemplateById(1);
				if(InstanceManager.getInstance().getInstanceCount(1) >= it.getMaxCount())
				{
					requestor.sendPacket(Msg.THE_OPPOSING_PARTY_IS_CURRENTLY_UNABLE_TO_ACCEPT_A_CHALLENGE_TO_A_DUEL);
					player.setTransactionRequester(null);
					requestor.setTransactionRequester(null);
					return;
				}

				msg1 = new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_PARTY_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addCharName(requestor);
				msg2 = new SystemMessage(SystemMessage.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_DUEL_AGAINST_THEIR_PARTY_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS).addCharName(player);
			}
			else
			{
				msg1 = Duel.checkPlayer(requestor);
				if(msg1 != null)
				{
					player.sendPacket(msg1);
					requestor.sendPacket(msg1);
					player.setTransactionRequester(null);
					requestor.setTransactionRequester(null);
					return;
				}

				msg1 = Duel.checkPlayer(player);
				if(msg1 != null)
				{
					player.sendPacket(msg1);
					requestor.sendPacket(msg1);
					player.setTransactionRequester(null);
					requestor.setTransactionRequester(null);
					return;
				}

				msg1 = new SystemMessage(SystemMessage.YOU_HAVE_ACCEPTED_S1S_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg1.addString(requestor.getName());

				msg2 = new SystemMessage(SystemMessage.S1_HAS_ACCEPTED_YOUR_CHALLENGE_TO_A_DUEL_THE_DUEL_WILL_BEGIN_IN_A_FEW_MOMENTS);
				msg2.addString(player.getName());
			}

			player.sendPacket(msg1);
			requestor.sendPacket(msg2);

			if(_duelType == 1)
				new PartyDuel(requestor, player);
			else
				new Duel(requestor, player);
		}
		else if(_response == 0)
		{
			SystemMessage msg;
			if(_duelType == 1)
				msg = new SystemMessage(SystemMessage.THE_OPPOSING_PARTY_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
			else
			{
				msg = new SystemMessage(SystemMessage.S1_HAS_DECLINED_YOUR_CHALLENGE_TO_A_DUEL);
				msg.addString(player.getName());
			}
			requestor.sendPacket(msg);
		}
		else
		 	requestor.sendPacket(new SystemMessage(SystemMessage.C1_IS_SET_TO_REFUSE_DUEL_REQUESTS_AND_CANNOT_RECEIVE_A_DUEL_REQUEST).addCharName(player));

		player.setTransactionRequester(null);
		requestor.setTransactionRequester(null);
	}
}