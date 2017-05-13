package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.serverpackets.KeyPacket;
import ru.l2gw.gameserver.serverpackets.SendStatus;

public class ProtocolVersion extends L2GameClientPacket
{
	private KeyPacket pk;
	private int _version;
	//private byte[] _data = new byte[256];
	//private static byte[] _xorB = {(byte) 0x4C, (byte) 0x32, (byte) 0x52, (byte) 0x2D, (byte) 0x44, (byte) 0x52, (byte) 0x69, (byte) 0x4E};

	/**
	 * packet type id 0x0E
	 * format:	cdbd
	 */

	@Override
	public void readImpl()
	{
		GameClient _client = getClient();

		if(_buf.remaining() < 4)
			_version = (byte) readC();
		else
			_version = readD();

		if((_version == -3  || _version == -2 )&& Config.ALLOW_SEND_STATUS)
		{
			_client.close(new SendStatus(_version));
			return;
		}
		else if(_buf.remaining() == 0)
		{
			_client.close(new KeyPacket(null));
			return;
		}

		pk = new KeyPacket(_client.enableCrypt());
	}

	@Override
	public void runImpl()
	{
		if(_version == -2 || _version == -3)
			return;

		GameClient _client = getClient();

		if(_version < Config.MIN_PROTOCOL_REVISION || _version > Config.MAX_PROTOCOL_REVISION)
		{
			_log.info("Client Protocol Revision: " + _version + ", client IP: " + _client.getIpAddr() + " not allowed. Supported protocols: from " + Config.MIN_PROTOCOL_REVISION + " to " + Config.MAX_PROTOCOL_REVISION + ". Closing connection.");
			_client.close(new KeyPacket(null));
			return;
		}

		_client.setRevision(_version);

		if(Config.DEBUG)
			_log.info("Client Protocol Revision is ok: " + _version);

		_client.client_lang = -1;

		_log.info(_client + " send KeyPacket, lang=" +_client.client_lang);
		_client.sendPacket(pk);
	}
}