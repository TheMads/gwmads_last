/**
 *
 */
package ru.l2gw.gameserver.clientpackets;

/**
 * @author zabbix
 * Lets drink to code!
 */
public class DummyPacket extends L2GameClientPacket
{
	@Override
	public void readImpl()
	{}

	@Override
	public void runImpl()
	{
		_log.warn("DummyPacket? Disconnect this user");
		getClient().closeNow(false);
	}
}
