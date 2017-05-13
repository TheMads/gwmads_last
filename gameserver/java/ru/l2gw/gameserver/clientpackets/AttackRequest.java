package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing", "unused" })
public class AttackRequest extends L2GameClientPacket
{
	// cddddc
	private int _objectId;
	private int _originX;
	private int _originY;
	private int _originZ;
	private int _attackId;

	@Override
	public void readImpl()
	{
		_objectId = readD();
		_originX = readD();
		_originY = readD();
		_originZ = readD();
		_attackId = readC(); // 0 for simple click   1 for shift-click
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(System.currentTimeMillis() - player.getLastPacket() < 100)
		{
			player.sendActionFailed();
			return;
		}

		player.setLastPacket();

		if(player.isOutOfControl())
		{
			player.sendActionFailed();
			return;
		}

		if(AdminTemplateManager.checkBoolean("noAttack", player))
		{
			player.sendActionFailed();
			return;
		}

		L2Object target = player.getVisibleObject(_objectId);

		if(target == null)
		{
			// Для провалившихся предметов, чтобы можно было все равно поднять
			target = L2ObjectsStorage.findObject(_objectId);
			if(target == null || !(target instanceof L2ItemInstance))
			{
				player.sendActionFailed();
				return;
			}
		}

		if(player.getTarget() != target)
		{
			target.onAction(player, _attackId == 1);
			return;
		}

		if(target.getObjectId() != player.getObjectId() && player.getPrivateStoreType() == L2Player.STORE_PRIVATE_NONE && player.getTransactionRequester() == null)
			target.onForcedAttack(player, _attackId == 1);
	}
}