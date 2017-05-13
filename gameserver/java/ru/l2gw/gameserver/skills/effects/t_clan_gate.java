package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 18.02.2009
 * Time: 13:53:20
 */
public class t_clan_gate extends t_effect
{
	public t_clan_gate(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		if(!getEffector().isPlayer())
			return;

		L2Player player;
		player = (L2Player) getEffector();

		if(player.isAlikeDead())
			return;
		if(player.getClanId() == 0)
			return;
		if(!player.isClanLeader())
			return;

		super.onStart();
		player.setInsideZone(ZoneType.no_summon, true);
		player.getClan().broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.COURT_MAGICIAN_THE_PORTAL_HAS_BEEN_CREATED), player);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		getEffected().setInsideZone(ZoneType.no_summon, false);
	}
}
