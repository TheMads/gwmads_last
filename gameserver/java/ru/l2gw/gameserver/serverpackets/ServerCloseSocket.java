package ru.l2gw.gameserver.serverpackets;

/**
 * Чисто технический пакет, либо пакет С5. Во всяком случе С4 клиент на него никак не реагирует.
 * added by rage: Gracia тоже никак не реагирует и соединение не разрывает. Поэтому при заходе вторым окном
 * на одного и того-же чара первое окно просто подвисало.
 * В общем этот пакет использовать нельзя, и похоже он уже устарел и не используется.
 */
@Deprecated
public class ServerCloseSocket extends L2GameServerPacket
{
	@Override
	protected final void writeImpl()
	{
		writeC(0xB0);
		writeD(0x01); //Always 1??!?!?!
	}
}
