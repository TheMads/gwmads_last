package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.PetitionManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.GmListTable;

public final class RequestPetitionCancel extends L2GameClientPacket
{
	//private int _unknown;

	@Override
	protected void readImpl()
	{
		//_unknown = readD(); This is pretty much a trigger packet.
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(PetitionManager.getInstance().isPlayerInConsultation(player))
		{
			if(player.isGM())
				PetitionManager.getInstance().endActivePetition(player);
			else
				player.sendPacket(new SystemMessage(SystemMessage.PETITION_UNDER_PROCESS));
		}
		else if(PetitionManager.getInstance().isPlayerPetitionPending(player))
		{
			if(PetitionManager.getInstance().cancelActivePetition(player))
			{
				int numRemaining = Config.MAX_PETITIONS_PER_PLAYER - PetitionManager.getInstance().getPlayerTotalPetitionCount(player);
				player.sendPacket(new SystemMessage(SystemMessage.THE_PETITION_WAS_CANCELED_YOU_MAY_SUBMIT_S1_MORE_PETITIONS_TODAY).addString(String.valueOf(numRemaining)));
				String msgContent = player.getName() + " has canceled a pending petition.";
				GmListTable.broadcastToGMs(new Say2(player.getObjectId(), Say2C.HERO_VOICE, "Petition System", msgContent));
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.FAILED_TO_CANCEL_PETITION_PLEASE_TRY_AGAIN_LATER));
		}
		else
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_NOT_SUBMITTED_A_PETITION));
	}
}
