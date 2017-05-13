package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;

public class RequestPetGetItem extends L2GameClientPacket
{
	// format: cd
	private int _objectId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.getPet() != null && player.getPet() instanceof L2PetInstance)
		{
			L2PetInstance pet = (L2PetInstance) player.getPet();
			if(!player.isPetSummoned() || pet.isDead() || pet.isOutOfControl())
			{
				player.sendActionFailed();
				return;
			}

			L2Object item = L2World.getAroundObjectById(pet, _objectId);
			if(item instanceof L2ItemInstance)
				pet.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, item, null);
		}
		else
			player.sendActionFailed();
	}
}
