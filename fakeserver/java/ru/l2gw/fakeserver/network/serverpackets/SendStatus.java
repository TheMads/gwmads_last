package ru.l2gw.fakeserver.network.serverpackets;

import ru.l2gw.fakeserver.FakeServer;

/**
 * @author: rage
 * @date: 18.04.13 13:44
 */
public class SendStatus extends ServerPacket
{
	private final int opcode;

	public SendStatus(int opcode)
	{
		this.opcode = opcode;
	}

	@Override
	protected void writeImpl()
	{
		if(opcode == -3)
		{
			_log.info("STATUS: Status request recieved from " + getClient().toString());

			writeC(0x2E); // Packet ID
			writeD(0x01); // World ID
			writeD(FakeServer.getServerManager().getMax()); // Max Online
			writeD(FakeServer.getServerManager().getOnline1()); // Current Online
			writeD(FakeServer.getServerManager().getOnline2()); // Current Online
			writeD(FakeServer.getServerManager().getStore()); // Priv.Sotre Chars

			writeH(0x30);
			writeH(0x2C);

			writeH(0x35);
			writeH(0x31);

			writeH(0x30);
			writeH(0x2c);
			writeH(0x37);
			writeH(0x37);
			writeH(0x37);
			writeH(0x35);
			writeH(0x38);
			writeH(0x2c);

			writeH(0x36);
			writeH(0x35);
			writeH(0x30);
			writeH(0x36);
			writeH(0x00);
			writeH(0x77);
			writeH(0x00);
			writeH(0xb7);
			writeH(0x00);
			writeH(0x9f);

			writeD(0x00);
			writeD(0x00);

			writeH(0x00);
			writeH(0x41);
			writeH(0x75);
			writeH(0x67);
			writeH(0x20);
			writeH(0x32);
			writeH(0x39);
			writeH(0x20);
			writeH(0x32);
			writeH(0x30);
			writeH(0x30);
			writeH(0x39);
			writeH(0x00);
			writeH(0x30);
			writeH(0x32);
			writeH(0x3a);
			writeH(0x34);
			writeH(0x30);
			writeH(0x3a);
			writeH(0x34);
			writeH(0x33);
			writeH(0x00);
			writeH(0x57);
			writeH(0x00);
			writeH(0x5d11);
			writeH(0x601f);
		}
		else if(opcode == -2)
		{
			writeC(0x00);
			writeD(0x01);
		}
	}
}
