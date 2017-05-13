package ru.l2gw.gameserver.clientpackets;

/**
 * This class ...
 *
 * @version $Revision: 1.3.4.2 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestShortCutDel extends L2GameClientPacket
{
	private int _slot;
	private int _page;

	/**
	 * format:		cd
	 */
	@Override
	public void readImpl()
	{
		int id = readD();
		_slot = id % 12;
		_page = id / 12;
	}

	@Override
	public void runImpl()
	{
		// client dont needs confirmation. this packet is just to inform the server
		if(getClient().getPlayer() != null)
			getClient().getPlayer().deleteShortCut(_slot, _page);
	}
}
