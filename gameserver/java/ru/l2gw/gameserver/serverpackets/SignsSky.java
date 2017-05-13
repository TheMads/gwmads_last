package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.SevenSigns;

/**
 * Changes the sky color depending on the outcome
 * of the Seven Signs competition.
 *
 * packet type id 0xf8
 * format: c h
 */
// Не найден в пакетах Kamael
@Deprecated
public class SignsSky extends L2GameServerPacket
{
	public SignsSky(int state)
	{
		_state = state;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x41);

		int compWinner = SevenSigns.getInstance().getCabalWinner();

		if(SevenSigns.getInstance().isSealValidationPeriod() || _state > 0)
			if(compWinner == SevenSigns.CABAL_DAWN && _state != 1)
				writeH(258);
			else
				writeH(257);
		//else
		//writeH(256);
	}

	private static int _state = 0;
}