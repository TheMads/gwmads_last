package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.PetitionManager;
import ru.l2gw.gameserver.instancemanager.PetitionManager.PetitionType;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.GmListTable;

public final class RequestPetition extends L2GameClientPacket
{
	protected static Log _log = LogFactory.getLog(RequestPetition.class.getName());

	private String _content;
	private int _type; // 1 = on : 0 = off;

	@Override
	protected void readImpl()
	{
		_content = readS();
		_type = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(GmListTable.getAllVisibleGMs().size() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NOT_ANY_GMS_THAT_ARE_PROVIDING_CUSTOMER_SERVICE_CURRENTLY));
			return;
		}

		if(!PetitionManager.getInstance().isPetitioningAllowed())
		{
			player.sendPacket(new SystemMessage(SystemMessage.CANNOT_CONNECT_TO_PETITION_SERVER));
			return;
		}

		if(PetitionManager.getInstance().isPlayerPetitionPending(player))
		{
			player.sendPacket(new SystemMessage(SystemMessage.ALREADY_APPLIED_FOR_PETITION));
			return;
		}

		if(PetitionManager.getInstance().getPendingPetitionCount() == Config.MAX_PETITIONS_PENDING)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THE_PETITION_SYSTEM_IS_CURRENTLY_UNAVAILABLE_PLEASE_TRY_AGAIN_LATER));
			return;
		}

		int totalPetitions = PetitionManager.getInstance().getPlayerTotalPetitionCount(player) + 1;
		if(totalPetitions > Config.MAX_PETITIONS_PER_PLAYER)
		{
			player.sendPacket(new SystemMessage(SystemMessage.WE_HAVE_RECEIVED_S1_PETITIONS_FROM_YOU_TODAY_AND_THAT_IS_THE_MAXIMUM_THAT_YOU_CAN_SUBMIT_IN_ONE_DAY_YOU_CANNOT_SUBMIT_ANY_MORE_PETITIONS));
			return;
		}

		if(_content.length() > 255)
		{
			player.sendPacket(new SystemMessage(SystemMessage.PETITIONS_CANNOT_EXCEED_255_CHARACTERS));
			return;
		}

		if(_type >= PetitionType.values().length)
		{
			_log.warn("RequestPetition: Invalid petition type : " + _type);
			return;
		}

		int petitionId = PetitionManager.getInstance().submitPetition(player, _content, _type);
		player.sendPacket(new SystemMessage(SystemMessage.PETITION_APPLICATION_ACCEPTED_RECEIPT_NO_IS_S1).addNumber(petitionId));
		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_SUBMITTED_S1_PETITIONS_YOU_MAY_SUBMIT_S2_MORE_PETITIONS_TODAY).addNumber(totalPetitions).addNumber(Config.MAX_PETITIONS_PER_PLAYER - totalPetitions));
		player.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_S1_PETITIONS_PENDING).addNumber(PetitionManager.getInstance().getPendingPetitionCount()));
	}
}
