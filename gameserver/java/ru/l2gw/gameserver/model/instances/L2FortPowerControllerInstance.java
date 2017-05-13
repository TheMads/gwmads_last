package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.math.Rnd;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

/**
 * @author rage
 * @date 14.07.2009 9:32:01
 */
public class L2FortPowerControllerInstance extends L2NpcInstance
{
	private static final String _path = "data/html/fortress/powercontrol/";
	private static final int TRY_LIMIT = 3;
	protected String _file;
	protected int PASS_LEN;
	private int _currentTry;
	private int _currentPos;
	private String _pass;
	private long _disableTime;
	private boolean _disabled;
	private SiegeUnit _fortress;

	public L2FortPowerControllerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	public void onSpawn()
	{
		super.onSpawn();

		_currentTry = 0;
		_currentPos = 0;
		_disableTime = 0;
		_disabled = false;
		_pass = "";
		_file = "subpower";
		PASS_LEN = 3;

		_fortress = getBuilding(1);
		if(_fortress == null)
			_log.warn("Warning: " + this + " has no fortress!");
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(command.startsWith("pass"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();

			if(_disableTime > System.currentTimeMillis())
				showChatWindow(player, 3);
			else if(_disabled)
				showChatWindow(player, 2);
			else if(_currentPos < PASS_LEN)
			{
				_currentPos++;
				List<String> replace = new FastList<String>();
				_pass += st.nextToken();
				replace.add("%passwd_status%");
				replace.add(_pass);
				replace.add("%try_limit%");
				replace.add(String.valueOf(TRY_LIMIT - _currentTry));
				showChatWindow(player, 4, replace);
			}
			else
			{
				_currentTry++;
				_pass = "";
				_currentPos = 0;

				ClassId cId = ClassId.values()[player.getActiveClass()];

				if(cId.getLevel() == 4)
					cId = cId.getParent(player.getSex());

				int chance = cId == ClassId.warsmith ? 50 : 30;

				if(Rnd.chance(chance))
				{
					_disabled = true;
					_currentTry = 0;
					showChatWindow(player, 2);
				}
				else if(_currentTry == TRY_LIMIT)
				{
					_disableTime = System.currentTimeMillis() + 30000;
					_currentTry = 0;
					showChatWindow(player, 3);
				}
				else
				{
					List<String> replace = new FastList<String>();
					replace.add("%passwd_status%");
					replace.add("INCORRECT");
					replace.add("%try_limit%");
					replace.add(String.valueOf(TRY_LIMIT - _currentTry));
					showChatWindow(player, 4, replace);
				}
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	public boolean isDisabled()
	{
		return _disabled;
	}

	public void reset()
	{
		_currentTry = 0;
		_currentPos = 0;
		_disableTime = 0;
		_disabled = false;
		_pass = "";
	}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	private void showChatWindow(L2Player player, int val, List<String> replaces)
	{
		if(!_fortress.getSiege().checkIsAttacker(player.getClanId()))
		{
			player.sendActionFailed();
			return;
		}

		player.setLastNpc(this);
		String filename = _path + _file + "-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
		html.setFile(filename);

		if(replaces != null)
			for(int i = 0; i < replaces.size(); i += 2)
				html.replace(replaces.get(i), Matcher.quoteReplacement(replaces.get(i + 1)));

		player.setLastNpc(this);
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		if(!_fortress.getSiege().checkIsAttacker(player.getClanId()))
		{
			player.sendActionFailed();
			return;
		}

		String filename = _path + _file;

		if(val == 0)
			filename += ".htm";
		else
			filename += "-" + val + ".htm";

		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);

		player.setLastNpc(this);
		player.sendPacket(html);
	}
}
