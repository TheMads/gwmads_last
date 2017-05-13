package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.SevenSigns;

/**
 * Seven Signs Info
 *
 * packet id 0x73
 * format: cc
 *
 * Пример пакета с оффа (828 протокол):
 * 73 01 01
 *
 * Возможные варианты использования данного пакета:
 * 0 0 - Обычное небо???
 * 1 1 - Dusk Sky
 * 2 2 - Dawn Sky???
 * 3 3 - Небо постепенно краснеет (за 10 секунд)
 *
 * Возможно и другие вариации, эффект не совсем понятен.
 * 1 0
 * 0 1
 */
public class SSQInfo extends L2GameServerPacket
{
	public static int sky;
	public SSQInfo()
	{
		sky = SevenSigns.getInstance().getSky();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x73);
		writeH(sky);
	}
}