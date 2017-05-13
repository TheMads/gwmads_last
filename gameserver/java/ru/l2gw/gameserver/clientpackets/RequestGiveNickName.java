package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ClanMember;
import ru.l2gw.gameserver.model.L2Player;

public class RequestGiveNickName extends L2GameClientPacket
{
	//Format: cSS
	private String _target;
	private String _title;

	@Override
	public void readImpl()
	{
		_target = readS();
		_title = readS();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(!_title.equals("") && !StringUtil.isMatchingRegexp(_title, Config.CLAN_TITLE_TEMPLATE))
		{
			player.sendMessage("Incorrect title.");
			return;
		}

		// Дворяне могут устанавливать/менять себе title
		if(player.isNoble() && _target.matches(player.getName()))
		{
			player.setTitle(_title);
			player.sendPacket(Msg.TITLE_HAS_CHANGED);
			player.sendChanges();
			return;
		}
		// Can the player change/give a title?
		else if((player.getClanPrivileges() & L2Clan.CP_CL_GIVE_TITLE) != L2Clan.CP_CL_GIVE_TITLE)
			return;

		if(player.getClan().getLevel() < 3)
		{
			player.sendPacket(Msg.TITLE_ENDOWMENT_IS_ONLY_POSSIBLE_WHEN_CLANS_SKILL_LEVELS_ARE_ABOVE_3);
			return;
		}

		L2ClanMember member = player.getClan().getClanMember(_target);
		if(member != null)
		{
			member.setTitle(_title);
			if(member.isOnline())
			{
				member.getPlayer().sendPacket(Msg.TITLE_HAS_CHANGED);
				member.getPlayer().sendChanges();
			}
		}
		else
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.clientpackets.RequestGiveNickName.NotInClan", player));

	}
}