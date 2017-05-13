package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.tables.CharNameTable;
import ru.l2gw.gameserver.serverpackets.ExIsCharNameCreatable;

public class RequestCharacterNameCreatable extends L2GameClientPacket
{
	private String _nickname;

	@Override
	protected void readImpl()
	{
		_nickname  = readS();
	}

	@Override
	protected void runImpl()
	{
		if(CharNameTable.getInstance().accountCharNumber(getClient().getLoginName()) >= 8)
		{
			sendPacket(new ExIsCharNameCreatable(ExIsCharNameCreatable.REASON_TOO_MANY_CHARACTERS));
			return;
		}
		if(!StringUtil.isMatchingRegexp(_nickname, Config.EnTemplate))
		{
			sendPacket(new ExIsCharNameCreatable(ExIsCharNameCreatable.REASON_16_ENG_CHARS));
			return;
		}
		if(CharNameTable.getInstance().doesCharNameExist(_nickname))
		{
			sendPacket(new ExIsCharNameCreatable(ExIsCharNameCreatable.REASON_NAME_ALREADY_EXISTS));
			return;
		}

		sendPacket(new ExIsCharNameCreatable(ExIsCharNameCreatable.REASON_CREATION_OK));
	}
}