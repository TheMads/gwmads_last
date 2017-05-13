package ru.l2gw.gameserver.serverpackets;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Player;

import java.util.HashMap;

/**
 * Format: (c) d[dS]
 * d: list size
 * [
 *   d: char ID
 *   S: char Name
 * ]
 *
 * Пример с оффа:
 * C2 02 00 00 00 D0 33 08 00 43 00 4B 00 4A 00 49 00 41 00 44 00 75 00 4B 00 00 00 D0 A7 09 00 53 00 65 00 6B 00 61 00 73 00 00 00
 */
public class PackageToList extends L2GameServerPacket
{
	private boolean can_writeImpl = false;
	private FastList<CharInfo> chars = new FastList<CharInfo>();

	@Override
	final public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		HashMap<Integer, String> characters = player.getAccountChars();

		if(characters == null)
			return;
		
		// No other chars in the account of this player
		if(characters.size() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THAT_CHARACTER_DOES_NOT_EXIST));
			return;
		}
		for(Integer objectId : characters.keySet())
			chars.add(new CharInfo(characters.get(objectId), objectId));
		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		writeC(0xC8);
		writeD(chars.size());
		for(CharInfo _char : chars)
		{
			writeD(_char._id); // Character object id
			writeS(_char._name); // Character name
		}
		chars.clear();
	}

	static class CharInfo
	{
		public String _name;
		public int _id;

		public CharInfo(String __name, int __id)
		{
			_name = __name;
			_id = __id;
		}
	}
}