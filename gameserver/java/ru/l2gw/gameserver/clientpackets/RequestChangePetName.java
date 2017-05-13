package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RequestChangePetName extends L2GameClientPacket
{
	// format: cS
	private String _name;

	@Override
	public void readImpl()
	{
		_name = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;
		L2Summon pet = player.getPet();
		if(player.isPetSummoned() && pet.getName() == null)
		{
			if(_name.length() > 8)
			{
				sendPacket(new SystemMessage(SystemMessage.YOUR_PETS_NAME_CAN_BE_UP_TO_8_CHARACTERS));
				return;
			}

			if(!StringUtil.isMatchingRegexp(_name, Config.EnTemplate))
				if(!Config.Lang.equalsIgnoreCase("ru") || !StringUtil.isMatchingRegexp(_name, Config.RusTemplate))
				{
					player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_A_SPACE_IN_THE_NAME));
					return;
				}

			Connection con = null;
			PreparedStatement statement = null;
			ResultSet rset = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				statement = con.prepareStatement("SELECT * FROM pets WHERE name like ?");
				statement.setString(1, _name);
				rset = statement.executeQuery();
				if(rset.next())
				{
					player.sendPacket(new SystemMessage(SystemMessage.ALREADY_IN_USE_BY_ANOTHER_PET));
					return;
				}
			}
			catch(final Exception e)
			{
			}
			finally
			{
				DbUtils.closeQuietly(con, statement, rset);
			}

			pet.setName(_name);
			pet.broadcastPetInfo();

			if(pet instanceof L2PetInstance)
			{
				L2PetInstance _pet = (L2PetInstance) pet;
				L2ItemInstance controlItem = _pet.getControlItem();
				_pet.store(player.getObjectId());
				if(controlItem != null)
				{
					controlItem.setCustomType2(1);
					controlItem.setPriceToSell(0); // Костыль, иначе CustomType2 = 1 не пишется в базу
					controlItem.updateDatabase();
					_pet.InventoryUpdateControlItem();
				}
			}

		}
	}
}
