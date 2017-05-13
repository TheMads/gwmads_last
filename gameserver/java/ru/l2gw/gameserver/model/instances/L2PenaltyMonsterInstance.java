package ru.l2gw.gameserver.model.instances;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.lang.ref.WeakReference;

public final class L2PenaltyMonsterInstance extends L2MonsterInstance
{
	private WeakReference<L2Player> ptk;

	public L2PenaltyMonsterInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public L2Character getMostHated()
	{
		L2Player p = getPtk();
		L2Character p2 = super.getMostHated();
		if(p == null)
			return p2;
		if(p2 == null)
			return p;
		return getDistance3D(p) > getDistance3D(p2) ? p2 : p;
	}

	public void SetPlayerToKill(L2Player ptk)
	{
		setPtk(ptk);
		if(Rnd.get(100) <= 80)
			broadcastPacket(new Say2(getObjectId(), Say2C.ALL, getName(), "mmm your bait was delicious"));
		getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, ptk, 10);
		getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, ptk);
	}

	@Override
	public void doDie(L2Character killer)
	{
		if(Rnd.get(100) <= 75)
		{
			Say2 cs = new Say2(getObjectId(), Say2C.ALL, getName(), "I will tell fishes not to take your bait");
			broadcastPacket(cs);
		}
		super.doDie(killer);
	}

	public L2Player getPtk()
	{
		if(ptk == null)
			return null;

		L2Player p = ptk.get();
		if(p == null)
			ptk = null;

		return p;
	}

	public void setPtk(L2Player ptk)
	{
		this.ptk = new WeakReference<L2Player>(ptk);
	}
}
