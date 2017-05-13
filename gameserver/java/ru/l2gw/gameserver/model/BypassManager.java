package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CommunityBoard.CommunityBoardManager;
import ru.l2gw.gameserver.instancemanager.CommunityBoard.ICommunityBoardHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BypassManager
{
	private static final Log _log = LogFactory.getLog(BypassManager.class.getName());

	private static final Pattern p = Pattern.compile("\"(bypass +[-h ]*)(.+?)\"");
	private static final Pattern l = Pattern.compile("\"(link +)(.+?)\"");

	public static enum BypassType
	{
		ENCODED,
		ENCODED_BBS,
		SIMPLE,
		SIMPLE_BBS,
		SIMPLE_DIRECT
	}

	private static final String[] simple_bypasses = {
			"manor_menu_select?",
			"_diary?",
			"_olympiad?",
			"_match?"
	};

	private static final String[] simple_bbs_bypasses = {
			"_bbshome",
			"_bbsgetfav",
			"_bbslink",
			"_bbsloc",
			"_bbsclan",
			"_bbsmemo",
			"_maillist_0_1_0_",
			"_friendlist_0_",
			"_bbsaddfav_List"
	};

	public static BypassType getBypassType(String bypass)
	{
		switch(bypass.charAt(0))
		{
			case '0':
				return BypassType.ENCODED;
			case '1':
				return BypassType.ENCODED_BBS;
			default:
				for(String simple_bypass : simple_bypasses)
					if(bypass.startsWith(simple_bypass))
						return BypassType.SIMPLE;

				if(bypass.startsWith("_bbsclan_clanhome;"))
					return BypassType.SIMPLE_BBS;

				bypass = bypass.intern(); //что бы можно было сравнивать через ==
				for(String simple_bbs_bypass : simple_bbs_bypasses)
					if(simple_bbs_bypass == bypass)
						return BypassType.SIMPLE_BBS;

				return BypassType.SIMPLE_DIRECT;
		}
	}

	public static String encode(String html, boolean bbs, L2Player player)
	{
		Matcher m = p.matcher(html);
		StringBuffer sb = new StringBuffer();
		GArray<String> bypassStorage = player.getStoredBypasses(bbs);
		GArray<String> links = player.getStoredLinks();

		while(m.find())
		{
			String bypass = m.group(2);
			String code = bypass;
			String params = "";
			int i = bypass.indexOf(" $");
			boolean use_params = i >= 0;
			if(use_params)
			{
				code = bypass.substring(0, i);
				params = bypass.substring(i).replace("$", "\\$");
			}

			if(bbs)
				m.appendReplacement(sb, m.group(0).replace(m.group(2), "1" + Integer.toHexString(bypassStorage.size()) + params));
			else
				m.appendReplacement(sb, m.group(0).replace(m.group(2), "0" + Integer.toHexString(bypassStorage.size()) + params));

			bypassStorage.add(code);
		}

		m.appendTail(sb);

		m = l.matcher(sb.toString());
		sb = new StringBuffer();
		while(m.find())
		{
			String link = m.group(2);
			m.appendReplacement(sb, m.group(0).replace(m.group(2), "0" + Integer.toHexString(links.size())));
			links.add(link);
		}

		m.appendTail(sb);
		return sb.toString();
	}

	public static DecodedBypass decode(String bypass, boolean bbs, L2Player player)
	{
		GArray<String> bypassStorage = player.getStoredBypasses(bbs);

		synchronized(bypassStorage)
		{
			String[] bypass_parsed = bypass.split(" ");
			int idx = Integer.parseInt(bypass_parsed[0].substring(1), 16);
			String bp;

			try
			{
				bp = bypassStorage.get(idx);
			}
			catch(Exception e)
			{
				bp = null;
			}

			if(bp == null)
			{
				_log.warn("Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName());
				return null;
			}

			DecodedBypass result;
			ICommunityBoardHandler handler = null;
			if(bbs)
			{
				handler = CommunityBoardManager.getInstance().getCommunityHandler(bp);
				if(handler == null)
				{
					_log.warn("No bbs bypass handler for: " + bp + " Player: " + player.getName());
					return  null;
				}
			}

			result = new DecodedBypass(bp, handler);
			for(int i = 1; i < bypass_parsed.length; i++)
				result.bypass += " " + bypass_parsed[i];
			result.trim();

			return result;
		}
	}

	public static class DecodedBypass
	{
		public String bypass;
		public ICommunityBoardHandler handler;

		public DecodedBypass(String _bypass, ICommunityBoardHandler _handler)
		{
			bypass = _bypass;
			handler = _handler;
		}

		public DecodedBypass trim()
		{
			bypass = bypass.trim();
			return this;
		}
	}
}