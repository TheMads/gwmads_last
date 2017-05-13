package ru.l2gw.gameserver.model.entity.duel;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;

import java.lang.ref.WeakReference;

/**
* @author rage
* @date 18.05.11 11:52
*/
public class PlayerCondition
{
	private WeakReference<L2Player> _player;
	private double _hp;
	private double _mp;
	private double _cp;
	private FastList<L2Effect> _debuffs;

	public PlayerCondition(L2Player player)
	{
		if(player == null)
			return;
		_player = new WeakReference<L2Player>(player);
		_hp = player.getCurrentHp();
		_mp = player.getCurrentMp();
		_cp = player.getCurrentCp();
	}

	public void restoreCondition()
	{
		L2Player player = _player.get();
		if(player == null)
			return;

		player.setCurrentHp(_hp);
		player.setCurrentMp(_mp);
		player.setCurrentCp(_cp);

		if(_debuffs != null) // Debuff removal
			for(L2Effect temp : _debuffs)
				if(temp != null)
					temp.exit();
	}

	public void registerDebuff(L2Effect debuff)
	{
		if(_debuffs == null)
			_debuffs = new FastList<L2Effect>();

		_debuffs.add(debuff);
	}

	public L2Player getPlayer()
	{
		return _player.get();
	}
}
