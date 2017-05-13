package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.extensions.ccpGuard.ConfigProtect;
import ru.l2gw.extensions.ccpGuard.packets.ProtectPing;

/**
 * @author rage
 * @date 07.08.2009 12:30:42
 */
public class NetPingResponse extends L2GameClientPacket
{
	private short _rnd;
	private short _seq;

	@Override
	protected void readImpl()
	{
		_rnd = (short) readH();
		_seq = (short) readH();
	}

	@Override
	protected void runImpl()
	{
		if(getClient() == null)
			return;

		getClient().pingReceived(_rnd, _seq);
		if(getClient()._prot_info.protect_used && ConfigProtect.PROTECT_SHOW_PING)
			sendPacket(new ProtectPing());
	}
}
