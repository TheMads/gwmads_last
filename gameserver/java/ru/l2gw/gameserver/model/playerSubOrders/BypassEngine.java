package ru.l2gw.gameserver.model.playerSubOrders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.instancemanager.CommunityBoard.CommunityBoardManager;
import ru.l2gw.gameserver.model.BypassManager;
import ru.l2gw.gameserver.model.BypassManager.BypassType;
import ru.l2gw.gameserver.model.BypassManager.DecodedBypass;
import ru.l2gw.gameserver.model.L2Player;

public class BypassEngine
{
	private static final Log _log = LogFactory.getLog(BypassEngine.class.getName());

	public static DecodedBypass decodeBypass(String bypass, L2Player player)
	{
		BypassType bpType = BypassManager.getBypassType(bypass);
		boolean bbs = bpType == BypassType.ENCODED_BBS || bpType == BypassType.SIMPLE_BBS;
		if(bpType == BypassType.ENCODED || bpType == BypassType.ENCODED_BBS)
			return BypassManager.decode(bypass, bbs, player);
		if(bpType == BypassType.SIMPLE)
			return new DecodedBypass(bypass, null).trim();
		if(bpType == BypassType.SIMPLE_BBS && !bypass.startsWith("_bbsscripts"))
			return new DecodedBypass(bypass, CommunityBoardManager.getInstance().getCommunityHandler(bypass)).trim();

		_log.warn("Direct access to bypass: " + bypass + " / Player: " + player.getName());
		return null;
	}

	public static void cleanBypasses(boolean bbs, L2Player player)
	{
		synchronized(player.getStoredBypasses(bbs))
		{
			player.getStoredBypasses(bbs).clearSize();
		}

		if(!bbs)
			synchronized(player.getStoredLinks())
			{
				player.getStoredLinks().clearSize();
			}
	}

	public static String encodeBypasses(String htmlCode, boolean bbs, L2Player player)
	{
		synchronized(player.getStoredBypasses(bbs))
		{
			return BypassManager.encode(htmlCode, bbs, player);
		}
	}
}